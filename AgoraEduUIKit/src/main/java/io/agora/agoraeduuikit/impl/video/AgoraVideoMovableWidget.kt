package io.agora.agoraeduuikit.impl.video

import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame

/**
 * author : cjw
 * date : 2022/3/22
 * description : 可移动的widget；可被拖动，或跟随老师
 * movable widget, can drawable or follow teacher
 */
open class AgoraVideoMovableWidget : AgoraBaseWidget() {

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

    override fun onSyncFrameUpdated(frame: AgoraWidgetFrame) {
        super.onSyncFrameUpdated(frame)
        // if frame position is valid, update widget position follow frame
        if (frame.positionValid()) {
            (container?.parent as? ViewGroup)?.let { parent ->
                val medWidth = parent.width - (container?.width ?: 0)
                val medHeight = parent.height - (container?.height ?: 0)
                val left = medWidth * frame.x!!
                val top = medHeight * frame.y!!
                LogX.i(
                    TAG, "parentWidth:${parent.width}, parentHeight:${parent.height}, " +
                        "width:${container?.width}, height:${container?.height}, " +
                        "medWidth:$medWidth, medHeight:$medHeight"
                )
                val layoutParams = container?.layoutParams as? ConstraintLayout.LayoutParams
                layoutParams?.let {
                    it.endToEnd = UNSET
                    it.bottomToBottom = UNSET
                    it.leftMargin = left.toInt()
                    it.topMargin = top.toInt()
                    container?.post { container?.layoutParams = it }
                }
            }
        }
    }
}