package io.agora.online.component.teachaids

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.agora.online.animator.AnimatorUtil
import io.agora.online.animator.FCRAnimatorListener
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame

/**
 * author : cjw
 * date : 2022/3/22
 * description : 可移动的widget，三件套的父类；可被拖动，或跟随老师
 * movable widget, parent of a three-piece suite, can drawable or follow teacher
 */
open class AgoraTeachAidMovableWidget : AgoraBaseWidget() {
    var zIndex = 0.0
    var lock = Any()
    var lastAnimator: ValueAnimator? = null
    var isFirst = true

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>?,
        keys: MutableList<String>,
        operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        if (properties.contains("zIndex")) {
            zIndex = properties["zIndex"] as? Double ?: 0.0
            container?.context?.let {
                ContextCompat.getMainExecutor(it).execute {
                    container?.z = zIndex.toFloat()
                }
            }
        }
    }

    override fun init(container: ViewGroup) {
        super.init(container)
        // syncFrame to remote if localUser is teacher
        if (widgetInfo?.localUserInfo?.userRole == AgoraEduContextUserRole.Teacher.value) {
            container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (container.width > 0 && container.height > 0) {
                        container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        (container.parent as? ViewGroup)?.let { parent ->
                            val medWidth = parent.width - container.width
                            val medHeight = parent.height - container.height
                            val xAxis = container.left.toFloat() / medWidth.toFloat()
                            val yAxis = container.top.toFloat() / medHeight.toFloat()
                            val frame = AgoraWidgetFrame(xAxis, yAxis)
                            // initial sync widget' position and size to remote
                            updateSyncFrame(frame)
                        }
                    }
                }
            })
        }
    }

    /**
     * 被动follow老师
     * TODO：缺老师端主动同步position/size到远端
     */
    override fun onSyncFrameUpdated(frame: AgoraWidgetFrame) {
        super.onSyncFrameUpdated(frame)
//        if (isFirst) {
//            isFirst = false
//            return
//        }
//        // if frame position is valid, update widget position follow frame
//        if (frame.positionValid()) {
//            ContextCompat.getMainExecutor(container?.context!!).execute {
//                val layoutParams = container?.layoutParams as? ConstraintLayout.LayoutParams
//                layoutParams?.let { params ->
//                    container?.clearAnimation()
//                    synchronized(lock) {
//                        (container?.parent as? ViewGroup)?.let { parent ->
//                            //LogX.e(TAG, "onSyncFrameUpdated------->${parent.height} ${container?.height} ${container?.measuredHeight}")
//                            val medWidth = parent.width - (container?.width ?: 0)
//                            val medHeight = parent.height - (container?.height ?: 0)
//                            moveAnim(medWidth, medHeight, frame, params)
//
////                            params.endToEnd = UNSET
////                            params.bottomToBottom = UNSET
////                            params.leftMargin = left.toInt()
////                            params.topMargin = top.toInt()
////                            container?.layoutParams = params
//                        }
//                    }
//                }
//            }
//        }
    }

    fun moveAnim(medWidth: Int, medHeight: Int, frame: AgoraWidgetFrame, params: ConstraintLayout.LayoutParams) {
        lastAnimator?.cancel()

        var lastleft = params.leftMargin
        var lastTop = params.topMargin


        LogX.e(TAG, "$frame|| w:h = $medWidth $medHeight")

        val left = medWidth * frame.x!!
        val top = medHeight * frame.y!!
        val disLeft = left - lastleft
        val disTop = top - lastTop

//        LogX.i(
//            TAG, "parentWidth:${parent.width}, parentHeight:${parent.height}, " +
//                    "width:${container?.width}, height:${container?.height}, " +
//                    "medWidth:$medWidth, medHeight:$medHeight"
//        )

        lastAnimator = AnimatorUtil.translate(0f, 1f, 100, object : FCRAnimatorListener() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                //LogX.e(TAG, "start : ${params.leftMargin} || ${params.topMargin}")

            }
            override fun onAnimationUpdate(fraction: Float) {
                super.onAnimationUpdate(fraction)

                //LogX.e(TAG, "onAnimationUpdate left=${(lastleft + disLeft * fraction).toInt()} top=${((lastTop + disTop * fraction).toInt())}")

                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                params.leftMargin = (lastleft + disLeft * fraction).toInt()
                params.topMargin = ((lastTop + disTop * fraction).toInt())
                container?.layoutParams = params
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if(isNeedRelayout()) { // 答题器需要更新list
                    val packet =
                        AgoraTeachAidWidgetInteractionPacket(AgoraTeachAidWidgetInteractionSignal.NeedRelayout, Unit)
                    sendMessage(GsonUtil.toJson(packet))
                }
            }
        })
    }

    open fun isNeedRelayout(): Boolean {
        return false
    }
}