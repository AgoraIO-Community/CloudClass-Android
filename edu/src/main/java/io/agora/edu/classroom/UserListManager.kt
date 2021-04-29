package io.agora.edu.classroom

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.classroom.bean.PropertyCauseType
import io.agora.edu.common.bean.handsup.HandsUpAction
import io.agora.edu.common.bean.handsup.HandsUpConfig
import io.agora.edu.common.bean.handsup.HandsUpConfig.Companion.handsUpKey
import io.agora.edu.common.bean.handsup.HandsUpConfig.Companion.processesKey
import io.agora.edu.common.bean.handsup.HandsUpResData
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.Constants
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.EduContextUserRole
import io.agora.educontext.context.UserContext
import io.agora.rtc.IRtcEngineEventHandler

class UserListManager(
        context: Context,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser,
        private val userContext: UserContext
) : BaseManager(context, launchConfig, eduRoom, eduUser) {
    override var tag = "UserListManager"

    private val studentsKey = "students"
    private val nameKey = "name"
    private val rewardKey = "reward"

    var eventListener: UserListManagerEventListener? = null
        set(value) {
            baseManagerEventListener = value
            field = value
        }

    private val curCoHostList = mutableListOf<EduContextUserDetailInfo>()

    private fun backupCoHostList(list: MutableList<EduContextUserDetailInfo>) {
        curCoHostList.clear()
        list?.forEach {
            curCoHostList.add(it.copy())
        }
    }

    private fun getStudentName(userId: String): String {
        val studentsJson = getProperty(eduRoom?.roomProperties, studentsKey)
        val studentsMap: MutableMap<String, Any>? = Gson().fromJson(studentsJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val curStudentJson = getProperty(studentsMap, userId)
        val curStudentMap: MutableMap<String, Any>? = Gson().fromJson(curStudentJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val name = getProperty(curStudentMap, nameKey) ?: ""
        return name.replace("\"", "")
    }

    private fun getRewardCount(userId: String): Int {
        val studentsJson = getProperty(eduRoom?.roomProperties, studentsKey)
        val studentsMap: MutableMap<String, Any>? = Gson().fromJson(studentsJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val curStudentJson = getProperty(studentsMap, userId)
        val curStudentMap: MutableMap<String, Any>? = Gson().fromJson(curStudentJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val tmp = getProperty(curStudentMap, rewardKey) ?: "0"
        val reward = tmp.toString().toFloat().toInt()
        return reward
    }

    fun notifyUserList() {
        val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, handsUpKey)
        val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
        if (handsUpConfig == null) {
            Constants.AgoraLog.e("$tag->handsUpConfig is null!")
        }
        val coHosts: MutableList<String> = mutableListOf()
        handsUpConfig?.accepted?.forEach {
            coHosts.add(it.userUuid)
        }
        coHosts.sort()
        getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(userInfos: MutableList<EduUserInfo>?) {
                if (userInfos == null) {
                    return
                }
                var index = -1
                val list: MutableList<EduContextUserDetailInfo> = mutableListOf()
                userInfos.forEach { element ->
                    index++
                    val userInfo = userInfoConvert(element)
                    val userDetailInfo = EduContextUserDetailInfo(userInfo, element.streamUuid)
                    userDetailInfo.isSelf = userInfo.userUuid == launchConfig.userUuid
                    userDetailInfo.onLine = true
                    userDetailInfo.coHost = coHosts.contains(element.userUuid)
                    userDetailInfo.boardGranted = eventListener?.onGranted(element.userUuid)
                            ?: false
                    userDetailInfo.rewardCount = getRewardCount(element.userUuid)
                    notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                        override fun onSuccess(res: Unit?) {
                            getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                                override fun onSuccess(streams: MutableList<EduStreamInfo>?) {
                                    val stream = streams?.find {
                                        it.publisher.userUuid == userInfo.userUuid
                                    }
                                    userDetailInfo.enableVideo = stream?.hasVideo
                                            ?: false
                                    userDetailInfo.enableAudio = stream?.hasAudio
                                            ?: false
                                    list.add(userDetailInfo)
                                    if (index == userInfos.size - 1) {
                                        userContext.getHandlers()?.forEach {
                                            it.onUserListUpdated(list)
                                        }
                                        val infos = list.filter { it.coHost } as MutableList
                                        val userIds = infos.map { it.user.userUuid } as MutableList
                                        coHosts.removeAll(userIds)
                                        coHosts.forEach {
                                            val name = getStudentName(it)
                                            val userInfo = EduContextUserInfo(it, name, EduContextUserRole.Student)
                                            val offLineDetailInfo = EduContextUserDetailInfo(userInfo, "")
                                            offLineDetailInfo.isSelf = false
                                            offLineDetailInfo.onLine = false
                                            offLineDetailInfo.coHost = true
                                            offLineDetailInfo.boardGranted = eventListener?.onGranted(it)
                                                    ?: false
                                            offLineDetailInfo.rewardCount = getRewardCount(it)
                                            offLineDetailInfo.enableVideo = false
                                            offLineDetailInfo.enableAudio = false
                                            infos.add(offLineDetailInfo)
                                        }
                                        userContext.getHandlers()?.forEach {
                                            it.onCoHostListUpdated(infos)
                                        }
                                        backupCoHostList(infos)
                                    }
                                }

                                override fun onFailure(error: EduError) {
                                }
                            })
                        }

                        override fun onFailure(error: EduError) {
                        }
                    })
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun notifyListByPropertiesChanged(cause: MutableMap<String, Any>?) {
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyCauseType.CMD].toString().toFloat().toInt()
            if (causeType == PropertyCauseType.COVIDEO_CHANGED) {
                val dataJson = cause[PropertyCauseType.DATA].toString()
                val data = Gson().fromJson(dataJson, HandsUpResData::class.java)
                if (data.actionType == HandsUpAction.TeacherAccept.value ||
                        data.actionType == HandsUpAction.TeacherAbort.value) {
                    notifyUserList()
                }
            } else if (causeType == PropertyCauseType.REWARD_CHANGED) {
                notifyUserList()
                val data = cause[PropertyCauseType.DATA]
                data?.let {
                    if (data is MutableMap<*, *> && data.isNotEmpty()) {
                        val ids = mutableListOf<String>()
                        data.forEach {
                            if (!TextUtils.isEmpty(it.key.toString())) {
                                ids.add(it.key.toString())
                            }
                        }
//                        getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
//                            override fun onSuccess(res: MutableList<EduUserInfo>?) {
//                                res?.find { ids.contains(it.userUuid) }?.let {
//                                    val userInfo = userInfoConvert(it)
//                                    userContext.getHandlers()?.forEach { handler ->
//                                        handler.onUserReward(userInfo)
//                                    }
//                                }
//                            }
//
//                            override fun onFailure(error: EduError) {
//                            }
//                        })
                        this.curCoHostList.let { res ->
                            res?.find { ids.contains(it.user.userUuid) }?.let {
                                userContext.getHandlers()?.forEach { handler ->
                                    handler.onUserReward(it.user)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateAudioVolumeIndication(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?) {
        speakers?.forEach {
            if (it.uid == 0) {
                localStream?.let { stream ->
                    userContext.getHandlers()?.forEach { handler ->
                        handler.onVolumeUpdated(it.volume, stream.streamUuid)
                    }
                }
            } else {
                val longStreamUuid: Long = it.uid.toLong() and 0xffffffffL
                userContext.getHandlers()?.forEach { handler ->
                    handler.onVolumeUpdated(it.volume, longStreamUuid.toString())
                }
            }
        }
    }

    fun kickOut() {
        eventListener?.onKickOut()
        userContext.getHandlers()?.forEach {
            it.onKickOut()
        }
    }

    fun renderContainer(viewGroup: ViewGroup?, streamUuid: String) {
        getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.streamUuid == streamUuid }?.let {
                    eduUser.setStreamView(it, launchConfig.roomUuid, viewGroup)
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }
}

interface UserListManagerEventListener : BaseManagerEventListener {
    fun onGranted(userId: String): Boolean

    fun onKickOut()
}