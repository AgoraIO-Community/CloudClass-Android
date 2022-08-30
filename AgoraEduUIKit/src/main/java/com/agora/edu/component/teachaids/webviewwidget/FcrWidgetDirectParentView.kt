package com.agora.edu.component.teachaids.webviewwidget

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import java.util.HashMap
import kotlin.math.roundToInt

/**
 * author : wf
 * date : 2022/6/7
 * description :包裹widget，使widget可移动的container
 */
class FcrWidgetDirectParentView : AbsAgoraEduComponent {
    private val tag = "WidgetDirectParentView"
    private var lastX: Int = 0//actiondown的时候记录坐标x
    private var lastY: Int = 0
    private var curX: Int = 0
    private var curY: Int = 0
    private var offsetX: Int = 0
    private var offsetY: Int = 0
    private var curZIndex = 0
    private var localUserGranted = false //当前本地用户是否授权

    private var fcrWebViewContainerComponent: FcrWebViewContainerComponent? = null

    // 位置和宽高比例信息的默认值
    private val defaultPositionPercent = 0.5F
    private val defaultSizeWidthPercent = 0.54F
    private val defaultSizeHeightPercent = 0.71F
    private var parentView: ViewGroup? = null
    private var widgetId: String = ""

    constructor(context: Context) : super(context)
    constructor(context: Context, parentView: ViewGroup, widgetId: String, fcrWebViewContainerComponent1: FcrWebViewContainerComponent) : super(
        context
    ) {
        this.parentView = parentView
        this.widgetId = widgetId
        this.fcrWebViewContainerComponent = fcrWebViewContainerComponent1
    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val boardWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet2 = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet2.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                    if (localUser.role == AgoraEduContextUserRole.Student) {
                        var granted = false
                        if (packet2.body is MutableList<*>) { // 白板开关的格式
                            granted = (packet2.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                        } else { // 白板授权的格式
                            val bodyStr = GsonUtil.gson.toJson(packet2.body)
                            val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                            if (agoraBoard.granted) {
                                granted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                            }
                        }
                        localUserGranted = granted
                    }
                }
            }
        }
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.widgetContext()?.addWidgetMessageObserver(boardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        if (localUserGranted || eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
            curX = (event.x).toInt()
            curY = (event.y).toInt()
            val medWidth = parentView!!.width - (this.width ?: 0)
            val medHeight = parentView!!.height - (this.height ?: 0)
            var availableLeft = this.left//当前widget左边可移动的距离
            var availableTop = this.top//当前widget上边可移动的距离
            var availableRight = medWidth - availableLeft//当前widget右边可移动的距离
            var availableBottom = medHeight - availableTop//当前widget右边可移动的距离
            val layoutParams = layoutParams as? MarginLayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = curX
                    lastY = curY
                }
                MotionEvent.ACTION_MOVE -> {
                    offsetX = (event.x - lastX).toInt()
                    offsetY = (event.y - lastY).toInt()
                    if (offsetX < 0 && -offsetX < availableLeft || offsetX > 0 && offsetX < availableRight) {
                        offsetLeftAndRight(offsetX)
                    }
                    if (offsetY < 0 && -offsetY < availableTop || offsetY > 0 && offsetY < availableBottom) {
                        offsetTopAndBottom(offsetY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // 发送update消息,同步给远端
//                    this.z = curZIndex++.toFloat()//设置zIndex z轴层级
                    val sizeWidthPercent = left / medWidth.toFloat()
                    val sizeHeightPercent = top / medHeight.toFloat()
                    val newFrame = AgoraWidgetFrame(sizeWidthPercent, sizeHeightPercent, defaultSizeWidthPercent, defaultSizeHeightPercent)
                    eduContext?.widgetContext()?.updateSyncFrame(newFrame, widgetId)//同步位置信息给远端
                    //更新zIndex到远端
                    val extraProperties: MutableMap<String, Any> = HashMap()
                    extraProperties["zIndex"] = fcrWebViewContainerComponent?.curMaxZIndex!! + 1
                    fcrWebViewContainerComponent?.webViewWidgets?.get(widgetId)?.let {
                        it.updateRoomProperties(extraProperties, mutableMapOf(), null)
                    }
                    //修改当前view的layoutParams属性，防止父布局重绘的时候，当前view回到原始位置
                    layoutParams?.leftMargin = left
                    layoutParams?.rightMargin = top
                    layoutParams?.setMargins(left,top,0,0)
                    setLayoutParams(layoutParams)
                }
            }
            return true
        }
        return false
    }
}

interface IFcrLocalSyncFrameListener {
    // 单个widget被拖拽的回调
    fun onLocalFrameUpdated(syncFrame: AgoraWidgetFrame, widgetId: String)
}