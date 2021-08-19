package io.agora.edu.classroom

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herewhite.sdk.domain.SceneState
import io.agora.edu.R
import io.agora.edu.classroom.bean.PropertyData
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduUser
import io.agora.educontext.EduContextScreenShareState
import io.agora.educontext.EduContextPool
import io.agora.rte.RteEngineImpl
import java.util.*

class ScreenShareManager(
        context: Context,
        private val eduContext: EduContextPool?,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser
) : BaseManager(context, launchConfig, eduRoom, eduUser) {
    override var tag = "ScreenShareManager"

    // identify whether the current display is the screenShare whiteboard courseware
    // get value from roomProperties
    private val screenShareKey = "screen"
    private val selectedKey = "selected"

    // the corresponding scenePath of Whiteboard in screen sharing
    private val screenShareScenePath = "/screenShare"

    private var remoteScreenStream: EduStreamInfo? = null

    @Volatile
    private var curScreenShareState = EduContextScreenShareState.Stop.value

    var screenShareStateChangedListener = object : (Boolean) -> Unit {
        override fun invoke(p1: Boolean) {

        }
    }

    var getWhiteBoardCurScenePathListener = object : () -> String? {
        override fun invoke(): String? {
            return null
        }

    }

    // remote rtc uid of online
    private val remoteOnlineUids: MutableList<String> = Collections.synchronizedList(mutableListOf())

    fun updateRemoteOnlineUids(uuids: MutableList<String>) {
        uuids.forEach {
            updateRemoteOnlineUids(it, true)
        }
    }

    fun updateRemoteOnlineUids(uuid: String, online: Boolean) {
        Log.e(tag, "onUser---uuid->$uuid, online->$online")
        if (online && !remoteOnlineUids.contains(uuid)) {
            remoteOnlineUids.add(uuid)
        } else {
            remoteOnlineUids.remove(uuid)
        }
    }

    fun isScreenSharing(): Boolean {
        return curScreenShareState == EduContextScreenShareState.Start.value
    }

    fun setScreenShareState(state: EduContextScreenShareState) {
        if (state.value != curScreenShareState) {
            curScreenShareState = state.value
            screenShareStateChangedListener(state.value == EduContextScreenShareState.Start.value)
        }
    }

    fun renderScreenShare(container: ViewGroup?, streamUuid: String) {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.streamUuid == streamUuid }?.let {
                    val sharing = container != null
                    curScreenShareState = if (sharing) EduContextScreenShareState.Start.value else
                        EduContextScreenShareState.Stop.value
                    eduUser.setStreamView(it, launchConfig.roomUuid, container, sharing)
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    fun checkAndNotifyScreenShareStarted(streamEvents: MutableList<EduStreamEvent>) {
        remoteScreenStream = streamEvents.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        remoteScreenStream?.let {
            Log.e(tag, "checkAndNotifyScreenShareStarted")
            checkAndNotifyScreenShareState()
        }
    }

    /**
     * Especially check for local screen share stream when
     * joining room successfully that whether local user is
     * sharing his screen when he last time joined
     */
    fun checkAndNotifyScreenShareRestored() {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
                    remoteScreenStream = it
                    Log.e(tag, "checkAndNotifyScreenShareRestored")
                    checkAndNotifyScreenShareState()
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun checkAndNotifyScreenShareRemoved(streamEvents: MutableList<EduStreamEvent>) {
        remoteScreenStream = streamEvents?.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        remoteScreenStream?.let {
            Log.e(tag, "checkAndNotifyScreenShareRemoved")
            checkAndNotifyScreenShareState()
        }
    }

    /**
     * Called when the remote RTC stream changes
     * */
    fun checkAndNotifyScreenShareByRTC(uuid: String) {
        Log.e(tag, "checkAndNotifyScreenShareByRTC")
        checkAndNotifyScreenShareState()
    }

    /**
     * Called when the property selectScreenShare changes
     * https://confluence.agoralab.co/pages/viewpage.action?pageId=731587738
     * */
    fun checkAndNotifyScreenShareByProperty(cause: MutableMap<String, Any>?) {
        cause?.get(PropertyData.CMD)?.let {
            if (it.toString().toFloat().toInt() == PropertyData.SWITCH_SCREENSHARE_COURSEWARE) {
                Log.e(tag, "checkAndNotifyScreenShareByProperty")
                checkAndNotifyScreenShareState()
            }
        }
    }

    /**
     * Call when whiteboard scene changes
     * see https://confluence.agoralab.co/pages/viewpage.action?pageId=731587738
     * */
    fun checkAndNotifyScreenShareByScene(state: SceneState) {
        Log.e(tag, "checkAndNotifyScreenShareByScene")
        checkAndNotifyScreenShareState()
    }

    private fun checkAndNotifyScreenShareState() {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                // find screenShare stream
                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
                    remoteScreenStream = it
                    val state = if (remoteOnlineUids.contains(it.streamUuid)) {
                        EduContextScreenShareState.Start
                    } else {
                        EduContextScreenShareState.Pause
                    }

                    eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                        if (state.value == EduContextScreenShareState.Start.value &&
                            curScreenShareState == EduContextScreenShareState.Stop.value) {
                            h.onScreenShareTip(String.format(
                                context.getString(R.string.screen_share_start_message_format),
                                it.publisher.userName))
                        }

                        // purpose state is consistent with current state,return
                        if (curScreenShareState != state.value) {
                            h.onScreenShareStateUpdated(state, it.streamUuid)
                        }
                        h.onSelectScreenShare(state == EduContextScreenShareState.Start)
                    }

                    // If
                    if (curScreenShareState == EduContextScreenShareState.Start.value) {
                        setScreenShareBackgroundColor(it.streamUuid, Color.WHITE)
                    }
                } ?: run {
                    // There is no screen share stream cached by room, but currently
                    // remote screen stream is not.
                    remoteScreenStream?.let {
                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                            h.onScreenShareTip(
                                String.format(context.getString(R.string.screen_share_end_message_format), it.publisher.userName
                                )
                            )

                            if (curScreenShareState != EduContextScreenShareState.Stop.value) {
                                h.onScreenShareStateUpdated(EduContextScreenShareState.Stop, it.streamUuid)
                            }
                        }

                        remoteScreenStream = null
                    }
                }

//                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
//                    remoteScreenStream = it
                    // judge whether the whiteboard is switched successfully
                    // judge whether the screenShare courseware is selected
                    //val curScenePath = getWhiteBoardCurScenePathListener.invoke()
                    //Log.e(tag, "curScenePath->$curScenePath, selectScreenShare->$selectScreenShare")

                    // if (curScenePath?.startsWith(screenShareScenePath) == true || selectScreenShare) {
//                    if (isSelectedScreenShare()) {
//                        // determine if remote RTC stream contains screenShare stream
//                        Log.e(tag, "remoteOnlineUids->${Gson().toJson(remoteOnlineUids)}, streamUuid->${it.streamUuid}")
//                        val contains = remoteOnlineUids.contains(it.streamUuid)
//                        val state = if (contains) EduContextScreenShareState.Start else
//                            EduContextScreenShareState.Pause
//                        Log.e(tag, "contains->$contains, state->$state")
//
//                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
//                            if (state.value == EduContextScreenShareState.Start.value &&
//                                    curScreenShareState == EduContextScreenShareState.Stop.value) {
//                                h.onScreenShareTip(String.format(
//                                        context.getString(R.string.screen_share_start_message_format),
//                                        it.publisher.userName))
//                            }
//                            // purpose state is consistent with current state,return
//                            if (curScreenShareState != state.value) {
//                                h.onScreenShareStateUpdated(state, it.streamUuid)
//                            }
//                            h.onSelectScreenShare(state == EduContextScreenShareState.Start)
//                        }
//
//                        if (curScreenShareState == EduContextScreenShareState.Start.value) {
//                            setScreenShareBackgroundColor(it.streamUuid, Color.WHITE)
//                        }
//                    // } else if (curScenePath?.startsWith(screenShareScenePath) == false && !selectScreenShare) {
//                    } else {
//                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
//                            h.onSelectScreenShare(false)
//                        }
//                    }
//                    return
//                }
//                Log.e(tag, "there is no screenShare stream")
//                // there is no screenShare stream
//                remoteScreenStream?.let {
//                    eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
//                        h.onScreenShareTip(String.format(
//                                context.getString(R.string.screen_share_end_message_format),
//                                it.publisher.userName))
//                        // purpose state is consistent with current state,return
//                        if (curScreenShareState != EduContextScreenShareState.Stop.value) {
//                            h.onScreenShareStateUpdated(EduContextScreenShareState.Stop, it.streamUuid)
//                        }
//                    }
//
//                    remoteScreenStream = null
//                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun setScreenShareBackgroundColor(streamId: String, color: Int) {
        val format = "{\"che.video.render_background_color\":{\"uid\":%s,\"r\":%d,\"g\":%d,\"b\":%d}}"
        val value = String.format(format, streamId, Color.red(color), Color.green(color), Color.blue(color))
        Log.d(tag, "screen share bg color param $value")
        RteEngineImpl.setRtcParameters(value)
    }
}