package io.agora.edu.core.internal.edu.classroom

import android.content.Context
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herewhite.sdk.domain.SceneState
import io.agora.edu.R
import io.agora.edu.core.AgoraEduCoreConfig
import io.agora.edu.core.context.EduContextPool
import io.agora.edu.core.internal.edu.classroom.bean.PropertyData
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.EduRoom
import io.agora.edu.core.internal.framework.data.EduStreamEvent
import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.framework.data.VideoSourceType
import io.agora.edu.core.internal.framework.EduLocalUser
import io.agora.edu.core.context.EduContextScreenShareState
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import java.util.*

internal class ScreenShareManager(
        context: Context,
        private val eduContext: EduContextPool?,
        config: AgoraEduCoreConfig,
        eduRoom: EduRoom?,
        eduUser: EduLocalUser
) : BaseManager(context, config, eduRoom, eduUser, eduContext) {
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

    // must customize the implementation
    var screenShareStateChangedListener = object : (Boolean) -> Unit {
        override fun invoke(p1: Boolean) {

        }
    }

    // must customize the implementation
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
        AgoraLog.i("$tag:onUser---uuid:$uuid, online:$online")
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
                    eduUser.setStreamView(it, config.roomUuid, container, sharing)
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }


    //    h.onScreenShareTip(String.format(
//    context.getString(R.string.screen_share_end_message_format),
//    it.publisher.userName))
    fun checkAndNotifyScreenShareStarted(streamEvents: MutableList<EduStreamEvent>) {
        remoteScreenStream = streamEvents.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        remoteScreenStream?.let {
            AgoraLog.i("$tag:checkAndNotifyScreenShareStarted")
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
                    AgoraLog.i("$tag:checkAndNotifyScreenShareRestored")
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
            AgoraLog.i("$tag:checkAndNotifyScreenShareRemoved")
            checkAndNotifyScreenShareState()
        }
    }

    /**
     * Called when the remote RTC stream changes
     * */
    fun checkAndNotifyScreenShareByRTC(uuid: String) {
        AgoraLog.i("$tag:checkAndNotifyScreenShareByRTC")
        checkAndNotifyScreenShareState()
    }

    /**
     * Called when the property selectScreenShare changes
     * https://confluence.agoralab.co/pages/viewpage.action?pageId=731587738
     * */
    fun checkAndNotifyScreenShareByProperty(cause: MutableMap<String, Any>?) {
        cause?.get(PropertyData.CMD)?.let {
            if (it.toString().toFloat().toInt() == PropertyData.SWITCH_SCREENSHARE_COURSEWARE) {
                AgoraLog.i("$tag:checkAndNotifyScreenShareByProperty")
                checkAndNotifyScreenShareState()
            }
        }
    }

    /**
     * Call when whiteboard scene changes
     * see https://confluence.agoralab.co/pages/viewpage.action?pageId=731587738
     * */
    fun checkAndNotifyScreenShareByScene(state: SceneState) {
        AgoraLog.i("$tag:heckAndNotifyScreenShareByScene")
        checkAndNotifyScreenShareState()
    }

    private fun checkAndNotifyScreenShareState() {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                // find screenShare stream
                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
                    remoteScreenStream = it
                    // judge whether the whiteboard is switched successfully
                    // judge whether the screenShare courseware is selected
                    val selectScreenShare = isSelectedScreenShare()
                    val curScenePath = getWhiteBoardCurScenePathListener.invoke()
                    AgoraLog.i("$tag:curScenePath->$curScenePath, selectScreenShare->$selectScreenShare")
                    if (curScenePath?.startsWith(screenShareScenePath) == true || selectScreenShare) {
                        // determine if remote RTC stream contains screenShare stream
                        AgoraLog.i("$tag:remoteOnlineUids->${Gson().toJson(remoteOnlineUids)}, streamUuid->${it.streamUuid}")
                        val contains = remoteOnlineUids.contains(it.streamUuid)
                        val state = if (contains) EduContextScreenShareState.Start else
                            EduContextScreenShareState.Pause
                        AgoraLog.i("$tag:contains->$contains, state->$state")
                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                            if (state.value == EduContextScreenShareState.Start.value &&
                                    curScreenShareState == EduContextScreenShareState.Stop.value) {
                                h.onScreenShareTip(context.getString(R.string.screen_share_start_message_format))
                            }
                            // purpose state is consistent with current state,return
                            if (curScreenShareState != state.value) {
                                h.onScreenShareStateUpdated(state, it.streamUuid)
                            }
                            h.onSelectScreenShare(state == EduContextScreenShareState.Start)
                        }
                    } else if (curScenePath?.startsWith(screenShareScenePath) == false && !selectScreenShare) {
                        eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                            h.onSelectScreenShare(false)
                        }
                    }
                    return
                }
                AgoraLog.i("$tag:there is no screenShare stream")
                // there is no screenShare stream
                remoteScreenStream?.let {
                    eduContext?.screenShareContext()?.getHandlers()?.forEach { h ->
                        h.onScreenShareTip(context.getString(R.string.screen_share_end_message_format))
                        // purpose state is consistent with current state,return
                        if (curScreenShareState != EduContextScreenShareState.Stop.value) {
                            h.onScreenShareStateUpdated(EduContextScreenShareState.Stop, it.streamUuid)
                        }
                    }

                    remoteScreenStream=null
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    private fun isSelectedScreenShare(): Boolean {
        val screenJson = getProperty(eduRoom?.roomProperties, screenShareKey)
        val screenMap: MutableMap<String, Any>? = Gson().fromJson(screenJson,
                object : TypeToken<MutableMap<String, Any>>() {}.type)
        screenMap?.let {
            val selected = getProperty(screenMap, selectedKey)
            selected?.let {
                val tmp = it.toDouble().toInt() == 1
                return tmp
            }
        }
        return false
    }
}