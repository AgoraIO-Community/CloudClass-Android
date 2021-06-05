package io.agora.privatechat

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.agora.base.callback.Callback
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.RetrofitManager
import io.agora.edu.classroom.BaseManager
import io.agora.edu.classroom.bean.PropertyData
import io.agora.edu.common.bean.sidechat.SideChatConfig
import io.agora.edu.common.bean.sidechat.SideChatStream
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.launch.AgoraEduSDK
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.StreamSubscribeOptions
import io.agora.education.api.stream.data.VideoStreamType
import io.agora.education.api.user.EduUser
import io.agora.educontext.EduContextPrivateChatInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.context.PrivateChatContext
import org.json.JSONException

class PrivateChatManager(
        context: Context,
        private val privateChatContext: PrivateChatContext?,
        currentRoom: EduRoom?,
        launchConfig: AgoraEduLaunchConfig,
        eduUser: EduUser) : BaseManager(context, launchConfig, currentRoom, eduUser) {
    override var tag = "PrivateChatManager"

    private var fromUserId: String? = null
    private var toUserUuid: String? = null
    private var chatStarted: Boolean = false

    private var mLocalUserUuid: String? = null

    /**
     * Should be called only once duration the lifecycle
     */
    fun setLocalUserUuid(localUserUuid: String) {
        mLocalUserUuid = localUserUuid
    }

    fun startPrivateChat(toUserUuid: String, callback: Callback<ResponseBody>): Boolean {
        if (isPrivateChatStarted()) {
            Log.e(tag, "private chat has already started, peer id $toUserUuid")
            return false
        }

        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), PrivateChatService::class.java)
                .startPrivateChat(launchConfig.appId, launchConfig.roomUuid, toUserUuid)
                .enqueue(RetrofitManager.Callback<ResponseBody>(0, object : ThrowableCallback<ResponseBody?> {
                    override fun onSuccess(res: ResponseBody?) {
                        callback.onSuccess(res)
                        setPrivateChat(true, launchConfig.userUuid, toUserUuid)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        if (callback is ThrowableCallback<*>) {
                            (callback as ThrowableCallback<ResponseBody?>).onFailure(throwable)
                        }
                    }
                }))
        return true
    }

    fun stopSideVoiceChat(callback: Callback<ResponseBody>): Boolean {
        if (!isPrivateChatStarted()) {
            Log.e(tag, "private chat has already stopped")
            return false
        }

        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), PrivateChatService::class.java)
                .finishPrivateChat(launchConfig.appId, launchConfig.roomUuid, toUserUuid!!)
                .enqueue(RetrofitManager.Callback<ResponseBody>(0, object : ThrowableCallback<ResponseBody?> {
                    override fun onSuccess(res: ResponseBody?) {
                        callback.onSuccess(res)
                        setPrivateChat(false, null, null)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        if (callback is ThrowableCallback<*>) {
                            (callback as ThrowableCallback<ResponseBody?>).onFailure(throwable)
                        }
                    }
                }))
        return true
    }

    @Synchronized
    private fun setPrivateChat(started: Boolean, fromUserId: String?, toUserId: String?) {
        chatStarted = started
        this.fromUserId = fromUserId
        toUserUuid = toUserId
    }

    @Synchronized
    private fun isPrivateChatStarted(): Boolean {
        return chatStarted
    }

    fun notifyRoomPropertiesChangedForSideChat(classRoom: EduRoom, cause: MutableMap<String, Any>?) {
        if (cause != null && cause.isNotEmpty()) {
            when (cause[PropertyData.CMD].toString().toFloat().toInt()) {
                PropertyData.SIDE_CHAT_CREATE -> {
                    Log.i(tag, "private chat created, json ${cause[PropertyData.DATA].toString()}")
                    handleSideChatPropertiesChanged(classRoom.roomProperties)
                }

                PropertyData.SIDE_CHAT_DESTROY -> {
                    Log.i(tag, "private chat deleted, json ${cause[PropertyData.DATA].toString()}")
                    applySideChatRules(false, null)
                    setPrivateChat(false, null, null)

                    privateChatContext?.getHandlers()?.forEach { h ->
                        h.onPrivateChatEnded()
                    }
                }
            }
        } else {
            Log.i(tag, "initialize private chat, json ${classRoom.roomProperties[SideChatConfig.KeyPrefix]}")
            handleSideChatPropertiesChanged(classRoom.roomProperties)
        }
    }

    private fun handleSideChatPropertiesChanged(properties: Map<String, Any?>) {
        parseSideChatProperties(properties)?.let { config ->
            parseSideChatConfig(config)
        }
    }

    private fun parseSideChatProperties(properties: Map<String, Any?>): SideChatConfig? {
        val group: Map<String, Any?>? = properties[SideChatConfig.KeyPrefix] as? Map<String, Any?>
        group?.values?.forEach { value ->
            return try {
                val toJson = Gson().toJson(value)
                Gson().fromJson(toJson, SideChatConfig::class.java)
            } catch (e: JSONException) {
                null
            }
        }

        return null
    }

    private fun parseSideChatConfig(config: SideChatConfig) {
        if (config.users.size == 2 &&
                config.users[0].userUuid.isNotBlank() &&
                config.users[1].userUuid.isNotBlank()) {
            setPrivateChat(true, config.users[0].userUuid, config.users[1].userUuid)

            val info = EduContextPrivateChatInfo(
                    EduContextUserInfo(fromUserId!!, "",
                            properties = getAgoraCustomProps(fromUserId!!)),
                    EduContextUserInfo(toUserUuid!!, "",
                            properties = getAgoraCustomProps(toUserUuid!!)))

            privateChatContext?.getHandlers()?.forEach { h ->
                h.onPrivateChatStarted(info)
            }

            applySideChatRules(true, config)
        } else {
            applySideChatRules(false, null)
        }
    }

    private fun applySideChatRules(started: Boolean, config: SideChatConfig?) {
        eduRoom?.getLocalUser(object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                res?.let { localUser ->
                    var localUserInGroup = false

                    config?.users?.forEach { user ->
                        if (user.userUuid == launchConfig.userUuid) {
                            localUserInGroup = true
                        }
                    }

                    Log.d(tag, "apply private chat rules, local user " +
                            "${localUser.userInfo.userName} in the group: $localUserInGroup," +
                            " private chat started: $started")

                    if (started) {
                        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
                            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                res?.let { streamList ->
                                    val privateChatStreamGroup: MutableMap<String, SideChatStream> = mutableMapOf()
                                    config?.streams?.forEach { groupStream ->
                                        privateChatStreamGroup[groupStream.streamUuid] = groupStream
                                    }

                                    streamList.forEach { roomStream ->
                                        var subscribeAudio = roomStream.hasAudio
                                        var subscribeVideo = roomStream.hasVideo
                                        var isGroupStream = false
                                        if (privateChatStreamGroup.containsKey(roomStream.streamUuid)) {
                                            isGroupStream = true
                                            // If a group stream's audio or video is explicitly made
                                            // private to this group, the users in the group must subscribe
                                            // it's audio or video, while the users NOT in the group must
                                            // explicitly unsubscribe the audio or video
                                            val groupStream = privateChatStreamGroup[roomStream.streamUuid]
                                            if (groupStream?.audio == 1) {
                                                subscribeAudio = localUserInGroup
                                            }

                                            if (groupStream?.video == 1) {
                                                subscribeVideo = localUserInGroup
                                            }
                                        } else if (localUserInGroup) {
                                            var audioPrivate = false
                                            var videoPrivate = false

                                            config?.streams?.forEach { stream ->
                                                if (stream.audio == 1) {
                                                    audioPrivate = true
                                                }

                                                if (stream.video == 1) {
                                                    videoPrivate = true
                                                }
                                            }

                                            // Note, if group audio or video is not made private
                                            // in the group, users in the group can subscribe
                                            // audio or video from so-called public group
                                            subscribeAudio = if (audioPrivate) false else roomStream.hasAudio
                                            subscribeVideo = if (videoPrivate) false else roomStream.hasVideo
                                        }

                                        Log.d(tag, "${roomStream.streamUuid} from ${roomStream.publisher.userName} " +
                                                "local user in group: $localUserInGroup, stream in the group: $isGroupStream, " +
                                                "subscribe audio: $subscribeAudio, subscribe video: $subscribeVideo")
                                        handleGroupStreamRules(localUser, roomStream.streamUuid, true,
                                                subscribeAudio, subscribeVideo)
                                    }
                                }
                            }

                            override fun onFailure(error: EduError) {
                                Log.e(tag, "${error.type} ${error.msg}")
                            }
                        })
                    } else {
                        // If the chat is destroyed, rollback the subscription
                        // config to the default group, or default config
                        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
                            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                res?.forEach { roomStream -> handleDefaultStreamRules(localUser, roomStream.publisher.userUuid) }
                            }

                            override fun onFailure(error: EduError) {
                                Log.e(tag, "${error.type} ${error.msg}")
                            }
                        })
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "${error.type} ${error.msg}")
            }
        })
    }

    private fun handleGroupStreamRules(localUser: EduUser, streamId: String, subscribe: Boolean,
                                       hasAudio: Boolean, hasVideo: Boolean) {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.forEach { streamInfo ->
                    if (streamInfo.streamUuid == streamId) {
                        // Find the stream info from local cache of the stream id
                        handleGroupStreamRules(localUser, streamInfo, subscribe, hasAudio, hasVideo)
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun handleDefaultStreamRules(localUser: EduUser, userId: String) {
        // rollback the subscription states to the case when there is
        // no private chat group. And the local user will subscribe all
        // streams that are valid in the room
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.forEach { streamInfo ->
                    if (streamInfo.publisher.userUuid != userId) {
                        Log.d(tag, "restore stream subscription for stream " +
                                "${streamInfo.streamUuid}, from ${streamInfo.publisher.userName}")
                        handleGroupStreamRules(localUser, streamInfo, true,
                                streamInfo.hasAudio, streamInfo.hasVideo)
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun handleGroupStreamRules(localUser: EduUser, streamInfo: EduStreamInfo,
                                       subscribe: Boolean, hasAudio: Boolean, hasVideo: Boolean) {
        eduRoom?.let {
            if (subscribe && streamInfo.publisher.userUuid != launchConfig.userUuid) {
                localUser.subscribeStream(streamInfo,
                        StreamSubscribeOptions(hasAudio, hasVideo, VideoStreamType.HIGH),
                        object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                Log.d(tag, "subscribe remote stream ${streamInfo.streamUuid} of " +
                                        "user ${streamInfo.publisher.userName}, audio: $hasAudio, video: $hasVideo, success")
                            }

                            override fun onFailure(error: EduError) {
                                Log.e(tag, "subscribe remote stream ${streamInfo.streamUuid} of " +
                                        "user ${streamInfo.publisher.userName}, audio: $hasAudio, video: $hasVideo, fails")
                            }
                        })
            } else if (!subscribe) {
                localUser.unSubscribeStream(streamInfo,
                        StreamSubscribeOptions(hasAudio, hasVideo, VideoStreamType.HIGH),
                        object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                Log.d(tag, "unsubscribe remote stream ${streamInfo.streamUuid} of " +
                                        "user ${streamInfo.publisher.userName} success")
                            }

                            override fun onFailure(error: EduError) {
                                Log.e(tag, "unsubscribe remote stream ${streamInfo.streamUuid} of " +
                                        "user ${streamInfo.publisher.userName} fails")
                            }
                        })
            }
        }
    }
}
