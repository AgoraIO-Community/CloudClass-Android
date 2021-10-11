package io.agora.edu.core.internal.edu.classroom

import android.content.Context
import android.view.ViewGroup
import io.agora.edu.core.AgoraEduCoreConfig
import io.agora.edu.core.context.EduContextPool
import io.agora.edu.core.context.EduContextRenderConfig
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.EduRoom
import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.framework.data.VideoSourceType
import io.agora.edu.core.internal.framework.EduLocalUser
import io.agora.edu.core.internal.framework.EduUserInfo
import io.agora.edu.core.internal.framework.EduUserRole
import io.agora.edu.core.context.EduContextUserDetailInfo
import io.agora.edu.core.context.EduContextUserInfo
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.edu.uikit.interfaces.listeners.IAgoraUIVideoGroupListener
import java.util.concurrent.ConcurrentHashMap

internal class TeacherVideoManager(
        context: Context,
        config: AgoraEduCoreConfig,
        eduRoom: EduRoom?,
        eduUser: EduLocalUser,
        contextPool: EduContextPool,
        private val granted: (String) -> Boolean
) : BaseManager(context, config, eduRoom, eduUser, contextPool) {
    override var tag = "TeacherVideoManager"

    private val videoContext = contextPool.videoContext()

    var container: ViewGroup? = null
    var teacherCameraStreamUuid: String = ""

    private val curUserDetailInfoMap: MutableMap<EduContextUserInfo, EduContextUserDetailInfo> =
            ConcurrentHashMap()

    var screenShareStarted: (() -> Boolean) = object : (() -> Boolean) {
        override fun invoke(): Boolean {
            return false
        }
    }

    val videoGroupListener: IAgoraUIVideoGroupListener = object : IAgoraUIVideoGroupListener {
        override fun onUpdateVideo(enable: Boolean) {
            muteLocalVideo(!enable)
        }

        override fun onUpdateAudio(enable: Boolean) {
            muteLocalAudio(!enable)
        }

        override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
            renderVideo(viewGroup, streamUuid)
        }
    }

    fun renderVideo(viewGroup: ViewGroup?, streamUuid: String) {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.forEach {
                    if (it.streamUuid == streamUuid) {
                        container = viewGroup
                        teacherCameraStreamUuid = streamUuid
                        eduUser.setStreamView(it, config.roomUuid, viewGroup, !screenShareStarted())
                        return@forEach
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun updateAudioVolume(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?) {
        speakers?.forEach {
            if (it.uid == 0) {
                localStream?.let { stream ->
                    videoContext?.getHandlers()?.forEach { handler ->
                        handler.onVolumeUpdated(it.volume, stream.streamUuid)
                    }
                }
            } else {
                val longStreamUuid: Long = it.uid.toLong() and 0xffffffffL
                videoContext?.getHandlers()?.forEach { handler ->
                    handler.onVolumeUpdated(it.volume, longStreamUuid.toString())
                }
            }
        }
    }

    fun notifyUserDetailInfo(role: EduUserRole) {
        getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(res: MutableList<EduUserInfo>?) {
                res?.forEach {
                    if (it.role.value == role.value && it.role.value == EduUserRole.TEACHER.value) {
                        var userInfo = userInfoConvert(it)
                        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                res?.forEach { stream ->
                                    if (stream.publisher == it && stream.videoSourceType == VideoSourceType.CAMERA) {
                                        var userDetailInfo = EduContextUserDetailInfo(userInfo, stream.streamUuid)
                                        userDetailInfo.isSelf = false
                                        userDetailInfo.onLine = true
                                        userDetailInfo.coHost = true
                                        userDetailInfo.boardGranted = true
                                        userDetailInfo.enableVideo = stream.hasVideo
                                        userDetailInfo.enableAudio = stream.hasAudio
                                        notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                if (!curUserDetailInfoMap.containsValue(userDetailInfo)) {
                                                    curUserDetailInfoMap[userInfo] = userDetailInfo
                                                    videoContext?.getHandlers()?.forEach { handler ->
                                                        handler.onUserDetailInfoUpdated(userDetailInfo)
                                                    }
                                                }
                                            }

                                            override fun onFailure(error: EduError) {
                                            }
                                        })
                                        return@forEach
                                    }
                                }
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                        return
                    } else if (it.role.value == role.value && it.role.value == EduUserRole.STUDENT.value) {
                        val userInfo = userInfoConvert(it)
                        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                res?.forEach { stream ->
                                    if (stream.publisher == it && stream.videoSourceType == VideoSourceType.CAMERA) {
                                        var userDetailInfo = EduContextUserDetailInfo(userInfo, stream.streamUuid)
                                        userDetailInfo.isSelf = true
                                        userDetailInfo.onLine = true
                                        userDetailInfo.coHost = true
                                        userDetailInfo.boardGranted = granted(userInfo.userUuid)
                                        userDetailInfo.enableVideo = stream.hasVideo
                                        userDetailInfo.enableAudio = stream.hasAudio
                                        notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                if (curUserDetailInfoMap.containsValue(userDetailInfo)) {
                                                    curUserDetailInfoMap[userInfo] = userDetailInfo
                                                    videoContext?.getHandlers()?.forEach { handler ->
                                                        handler.onUserDetailInfoUpdated(userDetailInfo)
                                                    }
                                                }
                                            }

                                            override fun onFailure(error: EduError) {
                                            }
                                        })
                                        return@forEach
                                    }
                                }
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                        return
                    }
                }
                // role not exists in curFullUserList
                curUserDetailInfoMap.forEach { element ->
                    if (element.key.role.value == userRoleConvert(role).value) {
                        val userDetailInfo = element.value
                        userDetailInfo.onLine = false
                        curUserDetailInfoMap[element.key] = userDetailInfo
                        videoContext?.getHandlers()?.forEach { handler ->
                            handler.onUserDetailInfoUpdated(userDetailInfo)
                        }
                        return@forEach
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }
}