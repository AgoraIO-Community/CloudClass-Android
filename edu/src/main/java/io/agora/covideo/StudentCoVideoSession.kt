package io.agora.covideo

import android.content.Context
import io.agora.education.api.EduCallback
import io.agora.education.api.room.EduRoom
import io.agora.covideo.CoVideoState.Applying
import io.agora.covideo.CoVideoState.CoVideoing
import java.lang.ref.WeakReference

internal abstract class StudentCoVideoSession(
        context: Context,
        eduRoom: EduRoom
) {
    var context: WeakReference<Context> = WeakReference(context)
    var eduRoom: WeakReference<EduRoom> = WeakReference(eduRoom)
    var curCoVideoState = CoVideoState.DisCoVideo

    /*是否允许举手
    * 1:允许  0:不允许*/
    var enableCoVideo = false

    /*是否允许举手即上台
    * 0:允许  1:不允许*/
    var autoCoVideo: Boolean = false

    companion object {
        /*是否打开举手开关
        * 是否允许举手即上台*/
        const val HANDUPSTATES = "handUpStates"
    }

    abstract fun syncCoVideoSwitchState(map: MutableMap<String, Any>?)

    /**检查是否老师是否在线，老师不在线无法举手
     * onSuccess->允许举手*/
    abstract fun isAllowCoVideo(callback: EduCallback<Unit>)

    protected fun refreshProcessUuid() {
//        processUuid = Random.nextInt().toString()
    }

    fun isApplying(): Boolean {
        return curCoVideoState == Applying
    }

    fun isCoVideoing(): Boolean {
        return curCoVideoState == CoVideoing
    }

    /**本地用户(举手、连麦)被老师同意/(拒绝、打断)
     * @param onStage 是否连麦*/
    abstract fun onLinkMediaChanged(onStage: Boolean)

    /**连麦中被老师打断*/
    abstract fun abortCoVideoing(): Boolean

    fun clear() {
        context.clear()
        eduRoom.clear()
    }
}