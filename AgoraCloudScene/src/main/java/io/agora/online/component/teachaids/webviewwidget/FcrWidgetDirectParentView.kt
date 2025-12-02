package io.agora.online.component.teachaids.webviewwidget

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.component.teachaids.component.FCRLargeWindowContainerComponent
import io.agora.online.view.FcrDragTouchGroupView
import io.agora.online.easeim.utils.ScreenUtil
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.online.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * author : wf
 * date : 2022/6/7
 * description :包裹widget，使widget可移动的container
 */
class FcrWidgetDirectParentView : FcrDragTouchGroupView {
    private var widgetContainerComponent: AbsAgoraEduComponent? = null
    private var parentView: ViewGroup? = null
    private var widgetId: String = ""
    private var agoraLargeWindowContainer: View? = null //当前view所在的container
    private val TAG = "FcrWidgetDirectParentView"

    constructor(
        context: Context,
        parentView: ViewGroup,
        widgetId: String,
        fcrWidgetContainerComponent: AbsAgoraEduComponent,
    ) : super(context) {
        this.parentView = parentView
        this.widgetId = widgetId
        this.widgetContainerComponent = fcrWidgetContainerComponent
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        agoraLargeWindowContainer = agoraUIProvider.getLargeVideoArea()
        setDragRange(agoraLargeWindowContainer?.width ?: 0, agoraLargeWindowContainer?.height ?: 0)
        setEnableDrag(false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handleTouchEvent = super.onTouchEvent(event)
        val layoutParams = layoutParams as? MarginLayoutParams
        val isRemoveWidget = AtomicBoolean(false)

        if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
            setEnableDrag(true)
//            val medWidth = parentView!!.width - width
//            val medHeight = parentView!!.height - height
//            var availableLeft = left//当前widget左边可移动的距离
//            var availableTop = top//当前widget上边可移动的距离
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    //设置当前layoutParams，防止父布局刷新，该view回到原来的位置
                    layoutParams?.leftMargin = left
                    layoutParams?.topMargin = top
                    layoutParams?.setMargins(left, top, 0, 0)
                    setLayoutParams(layoutParams)
                    LogX.e(TAG,"group onTouchEvent = ACTION_UP")
                    //up的时候，判断是否移动到讲台
                    when (eduContext?.roomContext()?.getRoomInfo()?.roomType) {
                        RoomType.SMALL_CLASS -> { //小班课中
                            LogX.e(TAG,"group onTouchEvent current top = $top")
                            if (event.y < 0 && top < resources.getDimensionPixelSize(R.dimen.agora_small_video_h)) {//手指滑到讲台区域内条件
                                LogX.e(TAG,"group onTouchEvent 打开小窗 event.y = ${ScreenUtil.instance.px2dip(context, event.y)}")
                                //发送widget inactive消息 关闭大窗 恢复小窗
                                eduContext?.widgetContext()?.setWidgetInActive(widgetId, true)
                                isRemoveWidget.set(true)
                            }
                        }
                        RoomType.LARGE_CLASS -> {
                            //大班课中，手指移到老师讲台区域，发送消息 打开小窗
                            if (left + event.x > parentView!!.width && (top + event.y) < resources.getDimension(R.dimen.agora_large_video_h)) {
                                LogX.e(TAG,
                                    "group onTouchEvent 打开小窗 x = ${this.left + event.x} event.y = ${
                                        ScreenUtil.instance.px2dip(context, event.y)
                                    }"
                                )
                                //发送widget inactive消息; 本地如果是老师的流 老师回讲台
                                if ((widgetContainerComponent as FCRLargeWindowContainerComponent).localTeacherStreamInfo?.streamUuid == widgetId.split(
                                        "-"
                                    )[1]
                                ) {
                                    //大班课 如果是老师的流则关闭大窗 恢复小窗
                                    eduContext?.widgetContext()?.setWidgetInActive(widgetId, true)
                                    isRemoveWidget.set(true)
                                }
                            }
                        }
                        RoomType.ONE_ON_ONE -> {
                            //1v1中，手指移到讲台区域，发送消息 恢复小窗
                            if (left + event.x > parentView!!.width) {
                                eduContext?.widgetContext()?.setWidgetInActive(widgetId, true)
                                isRemoveWidget.set(true)
                            }
                        }

                        else -> {}
                    }

                    if (!isRemoveWidget.getAndSet(false)) {
                        // 发送update消息,同步给远端
//                    this.z = curZIndex++.toFloat()//设置zIndex z轴层级
                        var sizeWidthPercent = left * 1.0f
                        var sizeHeightPercent = top * 1.0f
                        if (parentView!!.width != width) {
                            sizeWidthPercent = left.toFloat() / (parentView!!.width - width)
                        }
                        if (parentView!!.height != height) {
                            sizeHeightPercent = top.toFloat() / (parentView!!.height - height)
                        }

                        val newFrame = AgoraWidgetFrame(
                            sizeWidthPercent,
                            sizeHeightPercent,
                            width / agoraLargeWindowContainer!!.width.toFloat(),
                            height / agoraLargeWindowContainer!!.height.toFloat()
                        )
                        LogX.e(
                            TAG,
                            "group onTouchEvent ACTION_UP updateSyncFrame = $newFrame zIndex=${widgetContainerComponent?.curMaxZIndex!!}"
                        )
                        if (!isDoubleClicked) {//如果是双击事件，则不用updateSyncFrame
                            eduContext?.widgetContext()?.updateSyncFrame(newFrame, widgetId)//同步位置信息给远端
                        }

                        //更新zIndex到远端
                        val extraProperties: MutableMap<String, Any> = HashMap()
                        widgetContainerComponent?.curMaxZIndex = widgetContainerComponent?.curMaxZIndex!! + 1
                        extraProperties["zIndex"] = widgetContainerComponent?.curMaxZIndex!!
                        //处理大窗widget的层级（zIndex）关系
                        when (eduContext?.roomContext()?.getRoomInfo()?.roomType) {
                            RoomType.SMALL_CLASS -> {
                                if (event.y > 0 || top > resources.getDimensionPixelSize(R.dimen.agora_small_video_h)) {//手指未滑到讲台区域,如果滑动到讲台区域就不更新zIndex
                                    (widgetContainerComponent as? FCRLargeWindowContainerComponent)?.videoWidgets?.get(widgetId)?.let {
                                        it.updateRoomProperties(extraProperties, mutableMapOf(), null)
                                    }
                                }
                            }
                            RoomType.LARGE_CLASS -> {
                                //如果没有滑动到讲台，就更新zIndex, 如果滑动到讲台区域就不更新zIndex
                                if (left + event.x < parentView!!.width ||
                                    (top + event.y) > resources.getDimension(R.dimen.agora_large_video_h)
                                ) {
                                    (widgetContainerComponent as? FCRLargeWindowContainerComponent)?.videoWidgets?.get(widgetId)?.let {
                                        it.updateRoomProperties(extraProperties, mutableMapOf(), null)
                                    }
                                }
                            }

                            else -> {}
                        }
                        //webview widget 层级处理
                        widgetContainerComponent?.widgetsMap?.get(widgetId)?.let {
                            it.updateRoomProperties(extraProperties, mutableMapOf(), null)
                        }
                    }
                }
            }
            return handleTouchEvent
        }
        return handleTouchEvent
    }

    //eventX:当前触摸点相对于view的x轴偏移
    private fun getDirection(
        eventX: Float,
        eventY: Float,
        largeWindowWidth: Float,
        largeWindowHeight: Float
    ): WidgetDirectionEnum {
        val offsetValue = 50
        if (eventX < offsetValue && eventY < offsetValue) {
            return WidgetDirectionEnum.LT//左上
        }
        if (largeWindowWidth - eventX < offsetValue && eventY < offsetValue) {
            return WidgetDirectionEnum.RT //右上
        }
        if (eventX < offsetValue && largeWindowHeight - eventY < offsetValue) {
            return WidgetDirectionEnum.LB//左下
        }
        if (largeWindowWidth - eventX < offsetValue && largeWindowHeight - eventY < offsetValue) {
            return WidgetDirectionEnum.RB//右下
        }
        return WidgetDirectionEnum.MOVE
    }
}