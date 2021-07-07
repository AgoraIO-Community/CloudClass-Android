package io.agora.covideo

import android.content.Context
import com.google.gson.Gson
import io.agora.edu.R
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.customMsgError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.covideo.CoVideoState.CoVideoing
import io.agora.covideo.CoVideoState.DisCoVideo

internal class StudentCoVideoHelper(
        context: Context,
        eduRoom: EduRoom) :
        StudentCoVideoSession(context, eduRoom) {

    init {
        val properties = eduRoom.roomProperties
        /*提取并同步当前的举手开关状态 和 举手即上台开关状态*/
        syncCoVideoSwitchState(properties)
    }

    /**同步当前的举手开关状态 和 举手即上台开关状态*/
    override fun syncCoVideoSwitchState(properties: MutableMap<String, Any>?) {
        properties?.let {
            for ((key, value) in properties) {
                if (key == HANDUPSTATES) {
                    val json = value.toString()
                    val coVideoSwitchStateInfo = Gson().fromJson(json, CoVideoSwitchStateInfo::class.java)
                    enableCoVideo = coVideoSwitchStateInfo.state == CoVideoSwitchState.ENABLE
                    autoCoVideo = coVideoSwitchStateInfo.autoCoVideo == CoVideoApplySwitchState.ENABLE
                }
            }
        }
    }

    /**检查是否老师是否在线，老师不在线无法举手*/
    override fun isAllowCoVideo(callback: EduCallback<Unit>) {
        eduRoom?.let {
            eduRoom.get()?.getFullUserList(object : EduCallback<MutableList<EduUserInfo>> {
                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                    res?.forEach {
                        if (it.role == EduUserRole.TEACHER) {
                            callback.onSuccess(Unit)
                            return
                        }
                    }
                    callback.onFailure(customMsgError(context.get()?.getString(R.string.there_is_no_teacher_disable_covideo)))
                }

                override fun onFailure(error: EduError) {
                    callback.onFailure(error)
                }
            })
        }
    }

    override fun onLinkMediaChanged(onStage: Boolean) {
        curCoVideoState = if (onStage) CoVideoing else DisCoVideo
        //TODO 重置UI
    }

    override fun abortCoVideoing(): Boolean {
        if (isCoVideoing()) {
            curCoVideoState = DisCoVideo
            return true
        }
        return false
    }
}