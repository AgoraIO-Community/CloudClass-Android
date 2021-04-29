package io.agora.edu.classroom

import android.content.Context
import android.view.ViewGroup
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.context.VideoContext
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoGroupListener
import java.util.concurrent.ConcurrentHashMap

class TeacherVideoManager(
        context: Context,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser,
        private val videoContext: VideoContext
) : BaseManager(context, launchConfig, eduRoom, eduUser) {
    override var tag = "TeacherVideoManager"

    var container: ViewGroup? = null
    var teacherStreamUuid: String = ""

    var managerEventListener: TeacherVideoManagerEventListener? = null

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
            getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                    res?.forEach {
                        if (it.streamUuid == streamUuid) {
                            container = viewGroup
                            teacherStreamUuid = streamUuid
                            eduUser.setStreamView(it, launchConfig.roomUuid, viewGroup, !screenShareStarted())
                            return@forEach
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        }
    }

    fun renderVideo(viewGroup: ViewGroup?, streamUuid: String) {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.forEach {
                    if (it.streamUuid == streamUuid) {
                        container = viewGroup
                        teacherStreamUuid = streamUuid
                        eduUser.setStreamView(it, launchConfig.roomUuid, viewGroup, !screenShareStarted())
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
                    videoContext.getHandlers()?.forEach { handler ->
                        handler.onVolumeUpdated(it.volume, stream.streamUuid)
                    }
                }
            } else {
                val longStreamUuid: Long = it.uid.toLong() and 0xffffffffL
                videoContext.getHandlers()?.forEach { handler ->
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
//                                        var userDetailInfo = curUserDetailInfoMap[userInfo]
//                                                ?: AgoraKitUserDetailInfo(userInfo, stream.streamUuid)
                                        var userDetailInfo = EduContextUserDetailInfo(userInfo, stream.streamUuid)
                                        userDetailInfo.isSelf = false
                                        userDetailInfo.onLine = true
                                        userDetailInfo.coHost = true
                                        userDetailInfo.boardGranted = true
                                        userDetailInfo.enableVideo = stream.hasVideo
                                        userDetailInfo.enableAudio = stream.hasAudio
                                        notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                if(!curUserDetailInfoMap.containsValue(userDetailInfo)) {
                                                    curUserDetailInfoMap[userInfo] = userDetailInfo
                                                    videoContext.getHandlers()?.forEach { handler ->
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
                        var userInfo = userInfoConvert(it)
                        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                res?.forEach { stream ->
                                    if (stream.publisher == it && stream.videoSourceType == VideoSourceType.CAMERA) {
//                                        var userDetailInfo = curUserDetailInfoMap[userInfo]
//                                                ?: AgoraKitUserDetailInfo(userInfo, stream.streamUuid)
                                        var userDetailInfo = EduContextUserDetailInfo(userInfo, stream.streamUuid)
                                        userDetailInfo.isSelf = true
                                        userDetailInfo.onLine = true
                                        userDetailInfo.coHost = true
                                        userDetailInfo.boardGranted =
                                                managerEventListener?.onGranted(userInfo.userUuid)
                                                        ?: false
                                        userDetailInfo.enableVideo = stream.hasVideo
                                        userDetailInfo.enableAudio = stream.hasAudio
                                        notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                if(curUserDetailInfoMap.containsValue(userDetailInfo)) {
                                                    curUserDetailInfoMap[userInfo] = userDetailInfo
                                                    videoContext.getHandlers()?.forEach { handler ->
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
                        var userDetailInfo = element.value
                        userDetailInfo.onLine = false
                        curUserDetailInfoMap[element.key] = userDetailInfo
                        videoContext.getHandlers()?.forEach { handler ->
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

interface TeacherVideoManagerEventListener : BaseManagerEventListener {
    fun onGranted(userId: String): Boolean
}