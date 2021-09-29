package io.agora.agoraeducore.core.internal.edu.classroom

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.FLEX
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.FLEX_PROPS_CHANGED
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.muteChatKey
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.muteKey
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.rewardKey
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.studentsKey
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.HandsUpConfig
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.EduRoom
import io.agora.agoraeducore.core.internal.framework.data.EduStreamInfo
import io.agora.agoraeducore.core.internal.framework.EduLocalUser
import io.agora.agoraeducore.core.internal.framework.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducontext.EduContextUserDetailInfo
import io.agora.agoraeducontext.EduContextUserInfo
import io.agora.agoraeducontext.EduContextUserRole.Companion.fromValue

internal class FlexPropsManager(
        context: Context,
        private val eduContext: EduContextPool,
        config: AgoraEduCoreConfig,
        eduRoom: EduRoom?,
        eduUser: EduLocalUser,
        private val granted: (String) -> Boolean
) : BaseManager(context, config, eduRoom, eduUser, eduContext) {
    override var tag = "FlexPropsManager"

    fun initRoomFlexProps() {
        val tmp = getProperty(eduRoom?.roomProperties, FLEX)
        Constants.AgoraLog.i("$tag:initRoomFlexProps:$tmp")
        tmp?.let {
            val roomFlexProps: MutableMap<String, Any> = Gson().fromJson(it,
                    object : TypeToken<MutableMap<String, Any>>() {}.type)
            eduContext?.roomContext()?.getHandlers()?.forEach { handler ->
                handler.onFlexRoomPropsInitialized(roomFlexProps)
            }
        }
    }

    fun notifyRoomFlexProps(changedProps: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                            operator: EduBaseUserInfo?) {
        if (cause == null || cause.isEmpty()) {
            return
        }
        val cmd = cause[PropertyData.CMD].toString().toFloat().toInt()
        // only process flexProps
        if (cmd != FLEX_PROPS_CHANGED) {
            return
        }
        var contextUserInfo: EduContextUserInfo? = null
        operator?.let {
            val userFlexProps = getUserFlexProps(it.userUuid)
            contextUserInfo = EduContextUserInfo(it.userUuid, it.userName, fromValue(it.role.value),
                    userFlexProps)
        }
        val tmp = getProperty(eduRoom?.roomProperties, FLEX)
        val roomFlexProps: MutableMap<String, Any> = Gson().fromJson(tmp,
                object : TypeToken<MutableMap<String, Any>>() {}.type)
        // del prefix
        val changed: MutableMap<String, Any> = mutableMapOf()
        var key = ""
        changedProps.forEach {
            key = it.key.removePrefix(FLEX.plus("."))
            changed[key] = Gson().toJson(it.value)
        }
        val flexCause = (cause[PropertyData.DATA] as? MutableMap<String, Any>) ?: mutableMapOf()
        eduContext?.roomContext()?.getHandlers()?.forEach {
            it.onFlexRoomPropsChanged(changed, roomFlexProps, flexCause, contextUserInfo)
        }
    }

    fun notifyUserFlexProps(userInfo: EduBaseUserInfo, changedProps: MutableMap<String, Any>,
                            cause: MutableMap<String, Any>?, operator: EduBaseUserInfo?) {
        if (cause == null || cause.isEmpty()) {
            return
        }
        val cmd = cause[PropertyData.CMD].toString().toFloat().toInt()
        // only process flexProps
        if (cmd != FLEX_PROPS_CHANGED) {
            return
        }
        var operatorInfo: EduContextUserInfo? = null
        operator?.let {
            val userFlexProps = getUserFlexProps(it.userUuid)
            operatorInfo = EduContextUserInfo(it.userUuid, it.userName, fromValue(it.role.value),
                    userFlexProps)
        }
        getCurRoomFullUser(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(res: MutableList<EduUserInfo>?) {
                res?.find { it == userInfo }?.let {
                    val tmp = getProperty(it.userProperties, FLEX)
                    val userFlexProps: MutableMap<String, Any> = Gson().fromJson(tmp,
                            object : TypeToken<MutableMap<String, Any>>() {}.type)
                    // del prefix
                    val changed: MutableMap<String, Any> = mutableMapOf()
                    var key = ""
                    changedProps.forEach { entry ->
                        key = entry.key.removePrefix(FLEX.plus("."))
                        changed[key] = entry.value.toString()
                    }
                    val flexCause = (cause[PropertyData.DATA] as? MutableMap<String, Any>) ?: mutableMapOf()

                    val handsUpConfig = parseHandsUpConfig()
                    if (handsUpConfig == null) {
                        Constants.AgoraLog.e("$tag->handsUpConfig is null!")
                    }
                    val coHosts: MutableList<String> = mutableListOf()
                    handsUpConfig?.accepted?.forEach { accepted ->
                        coHosts.add(accepted.userUuid)
                    }
                    coHosts.sort()
                    val contextUserInfo = userInfoConvert(it)
                    val userDetailInfo = EduContextUserDetailInfo(contextUserInfo, it.streamUuid)
                    userDetailInfo.isSelf = it.userUuid == config.userUuid
                    userDetailInfo.onLine = true
                    userDetailInfo.coHost = coHosts.contains(it.userUuid)
                    userDetailInfo.boardGranted = granted(it.userUuid)
                            ?: false
                    userDetailInfo.silence = getUserSilenceStatus(it.userProperties)
                    userDetailInfo.rewardCount = getRewardCount(it.userUuid)
                    notifyUserDeviceState(userDetailInfo, object : EduCallback<Unit> {
                        override fun onSuccess(res: Unit?) {
                            getCurRoomFullStream(object : EduCallback<MutableList<EduStreamInfo>> {
                                override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                                    val stream = res?.find { item ->
                                        item.publisher.userUuid == userInfo.userUuid
                                    }
                                    userDetailInfo.enableVideo = stream?.hasVideo
                                            ?: false
                                    userDetailInfo.enableAudio = stream?.hasAudio
                                            ?: false
                                    eduContext.userContext()?.getHandlers()?.forEach { handler ->
                                        handler.onFlexUserPropsChanged(changed, userFlexProps,
                                                flexCause, userDetailInfo, operatorInfo)
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

    private fun getRewardCount(userId: String): Int {
        val studentsJson = getProperty(eduRoom?.roomProperties, studentsKey)
        val studentsMap: MutableMap<String, Any>? = Gson().fromJson(studentsJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val curStudentJson = getProperty(studentsMap, userId)
        val curStudentMap: MutableMap<String, Any>? = Gson().fromJson(curStudentJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val tmp = getProperty(curStudentMap, rewardKey) ?: "0"
        return tmp.toFloat().toInt()
    }

    /**
     * @return is private chat prohibited
     * */
    private fun getUserSilenceStatus(properties: MutableMap<String, Any>): Boolean {
        val muteJson = getProperty(properties, muteKey)
        val muteMap: MutableMap<String, Any>? = Gson().fromJson(muteJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val tmp = getProperty(muteMap, muteChatKey)
        return tmp?.toFloat()?.toInt() == 1
    }

    private fun parseHandsUpConfig(): HandsUpConfig? {
        val processesJson = getProperty(eduRoom?.roomProperties, HandsUpConfig.processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, HandsUpConfig.handsUpKey)
        return Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
    }
}