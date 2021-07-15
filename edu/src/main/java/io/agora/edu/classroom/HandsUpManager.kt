package io.agora.edu.classroom

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.R
import io.agora.edu.classroom.bean.PropertyData
import io.agora.edu.classroom.bean.PropertyData.HANDSUP_ENABLE_CHANGED
import io.agora.edu.classroom.bean.PropertyData.COVIDEO_CHANGED
import io.agora.edu.common.bean.handsup.*
import io.agora.edu.common.impl.HandsUpImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.EduUser
import io.agora.education.impl.Constants
import io.agora.educontext.EduContextCallback
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextErrors
import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.context.HandsUpContext
import io.agora.uikit.impl.handsup.AgoraUIHandsUpState

class HandsUpManager(
        context: Context,
        private val handsUpContext: HandsUpContext?,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser
) : BaseManager(context, launchConfig, eduRoom, eduUser) {
    override var tag = "HandsUpManager"

    private val processesKey = "processes"
    private val handsUpKey = "handsUp"

    private var handsUp = HandsUpImpl(launchConfig.appId, launchConfig.roomUuid)

    fun initHandsUpData() {
        val handsUpConfig = parseHandsUpConfig()
        handsUpConfig?.let {
            val coHost = it.accepted?.contains(HandsUpAccept(eduUser.userInfo.userUuid)) == true
            handsUpContext?.getHandlers()?.forEach { h ->
                h.onHandsUpEnabled(it.enabled == HandsUpEnableState.Enable.value)
                h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, coHost)
            }
        }
    }

    private fun parseHandsUpConfig(): HandsUpConfig? {
        val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, handsUpKey)
        val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
        return handsUpConfig
    }

    fun notifyHandsUpEnable(cause: MutableMap<String, Any>?) {
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyData.CMD].toString().toFloat().toInt()
            if (causeType == HANDSUP_ENABLE_CHANGED) {
                val handsUpConfig = parseHandsUpConfig()
                handsUpConfig?.let {
                    val enable = it.enabled == HandsUpEnableState.Enable.value
                    handsUpContext?.getHandlers()?.forEach { h ->
                        h.onHandsUpTips(context.getString(
                                if (enable) R.string.handsupenable
                                else R.string.handsupdisable))
                        h.onHandsUpEnabled(enable)
                    }
                }
            }
        }
    }

    fun notifyHandsUpState(cause: MutableMap<String, Any>?) {
        val localUserUuid = eduUser.userInfo.userUuid
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyData.CMD].toString().toFloat().toInt()
            if (causeType == COVIDEO_CHANGED) {
                val dataJson = cause[PropertyData.DATA].toString()
                val data = Gson().fromJson(dataJson, HandsUpResData::class.java)
                var coHost = false
                var state = AgoraUIHandsUpState.Init
                when (data.actionType) {
                    HandsUpAction.StudentApply.value -> {
                        data?.addProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsUp
                                handsUpContext?.getHandlers()?.forEach { h ->
                                    h.onHandsUpTips(context.getString(R.string.handsupsuccess))
                                    h.onHandsUpStateUpdated(EduContextHandsUpState.HandsUp, coHost)
                                }
                            }
                        }
                    }
                    HandsUpAction.TeacherAccept.value -> {
                        data?.addAccepted?.find { it == HandsUpAccept(localUserUuid) }?.let {
                            // The teacher gave me permission to be coHost
                            coHost = isCoHost(localUserUuid)
                            state = AgoraUIHandsUpState.HandsUp
                            handsUpContext?.getHandlers()?.forEach { h ->
                                h.onHandsUpTips(context.getString(R.string.covideo_accept_interactive))
                                h.onHandsUpStateUpdated(EduContextHandsUpState.HandsUp, coHost)
                            }
                        }
                    }
                    HandsUpAction.TeacherReject.value -> {
                        val handsUpConfig = parseHandsUpConfig()
                        data?.removeProgress?.forEach {
                            // The teacher refused me to be coHost
                            if (it.userUuid == localUserUuid) {
                                coHost = handsUpConfig?.accepted?.contains(HandsUpAccept(eduUser.userInfo.userUuid)) == true
                                state = AgoraUIHandsUpState.HandsDown
                                handsUpContext?.getHandlers()?.forEach { h ->
                                    h.onHandsUpTips(context.getString(R.string.covideo_reject_interactive))
                                    h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, coHost)
                                }
                            }
                        }
                    }
                    HandsUpAction.StudentCancel.value -> {
                        data?.removeProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsDown
                                handsUpContext?.getHandlers()?.forEach { h ->
                                    h.onHandsUpTips(context.getString(R.string.cancelhandsupsuccess))
                                    h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, coHost)
                                }
                            }
                        }
                    }
                    HandsUpAction.TeacherAbort.value -> {
                        data?.removeAccepted?.find { it == HandsUpAccept(localUserUuid) }?.let {
                            coHost = false
                            state = AgoraUIHandsUpState.HandsDown
                            handsUpContext?.getHandlers()?.forEach { h ->
                                h.onHandsUpTips(context.getString(R.string.covideo_abort_interactive))
                                h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, coHost)
                            }
                        }
                    }
                    HandsUpAction.TeacherTimeout.value -> {
                        val handsUpConfig = parseHandsUpConfig()
                        data?.removeProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = handsUpConfig?.accepted?.contains(HandsUpAccept(eduUser.userInfo.userUuid)) == true
                                state = AgoraUIHandsUpState.HandsDown
                                handsUpContext?.getHandlers()?.forEach { h ->
                                    h.onHandsUpTips(context.getString(R.string.handsuptimeout))
                                    h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, coHost)
                                }
                            }
                        }
                    }
                    HandsUpAction.Carousel.value -> {
                        // system carousel event; refresh the hands up status according to
                        // whether you are in the carousel list
                        val handsUpConfig = parseHandsUpConfig()
                        handsUpConfig?.let {
                            val inProgress = it.progress?.contains(HandsUpProgress(eduUser.userInfo.userUuid)) == true
                            coHost = it.accepted?.contains(HandsUpAccept(eduUser.userInfo.userUuid)) == true
                            handsUpContext?.getHandlers()?.forEach { h ->
                                h.onHandsUpEnabled(it.enabled == HandsUpEnableState.Enable.value)
                                if(coHost) {
                                    h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, true)
                                } else {
                                    if(inProgress) {
                                        h.onHandsUpStateUpdated(EduContextHandsUpState.HandsUp, false)
                                    } else {
                                        h.onHandsUpStateUpdated(EduContextHandsUpState.HandsDown, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isCoHost(userUuid: String): Boolean {
        val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, handsUpKey)
        val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
        return handsUpConfig?.accepted?.contains(HandsUpAccept(userUuid)) ?: false
    }

    private fun handsUp(callback: EduContextCallback<Boolean>? = null) {
        handsUp.applyHandsUp(object : EduCallback<Boolean> {
            override fun onSuccess(res: Boolean?) {
                res?.let { success ->
                    if (success) {
                        callback?.onSuccess(true)
                    } else {
                        callback?.onFailure(EduContextErrors.DefaultError)
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog.e("$tag->type:${error.type},msg:${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
                handsUpContext?.getHandlers()?.forEach {
                    it.onHandsUpStateResultUpdated(EduContextError(error.type, error.msg))
                }
            }
        })
    }

    private fun handsDown(callback: EduContextCallback<Boolean>? = null) {
        handsUp.cancelApplyHandsUp(object : EduCallback<Boolean> {
            override fun onSuccess(res: Boolean?) {
                res?.let { success ->
                    if (success) {
                        callback?.onSuccess(true)
                    } else {
                        callback?.onFailure(EduContextErrors.DefaultError)
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog.e("$tag->type:${error.type},msg:${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
                handsUpContext?.getHandlers()?.forEach {
                    it.onHandsUpStateResultUpdated(EduContextError(error.type, error.msg))
                }
            }
        })
    }

    fun performHandsUp(state: EduContextHandsUpState, callback: EduContextCallback<Boolean>?) {
        if (state == EduContextHandsUpState.HandsUp) {
            handsUp(callback)
        } else if (state == EduContextHandsUpState.HandsDown) {
            handsDown(callback)
        }
    }
}

enum class HandsUpResult(val value: Int) {
    Init(0),
    Accept(1),
    Reject(2)
}