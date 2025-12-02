package io.agora.online.component.teachaids.webviewwidget

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import io.agora.online.animator.AnimatorUtil
import io.agora.online.animator.FCRAnimatorListener
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.impl.managers.FCRWidgetSyncFrameObserver
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.FcrWebView
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.databinding.FcrOnlineWebviewContainerComponentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import kotlin.math.roundToInt

/**
 * author : wufang
 * date : 2022/5/30
 * description :打开alf文件，放webView的Widget的容器
 */
class FcrWebViewContainerComponent : AbsAgoraEduComponent, FCRWidgetSyncFrameObserver {
    private val tag = "FcrWebViewContainer"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineWebviewContainerComponentBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )

    private val dash = "-"
    private var localUserGranted = false //当前本地用户是否授权

    // 位置和宽高比例信息的默认值
    private val defaultPositionPercent = 0.5F
    private val defaultSizeWidthPercent = 0.54F
    private val defaultSizeHeightPercent = 0.71F
    private val strwebView: String = "webView"
    private val strmediaPlayer: String = "mediaPlayer"
    private val webViewWidgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            // 从讲台点击和拖拽时触发
            ContextCompat.getMainExecutor(context).execute {
                createWidget(widgetId)
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            // 从组件区域拖回去和双击时触发
            ContextCompat.getMainExecutor(context).execute {
                destroyWidget(widgetId)
            }
        }
    }

    private val whiteBoardWidgetMsgObserver = object : AgoraWidgetMessageObserver {
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

                        //有权限
//                        if (localUserGranted) {
//                            ContextCompat.getMainExecutor(context).execute {
//                                widgetsMap.forEach {
//                                    (it.value as? FcrWebViewWidget)?.webViewContent?.binding?.btnFullSize?.visibility = VISIBLE
//                                    (it.value as? FcrWebViewWidget)?.webViewContent?.binding?.btnClose?.visibility = VISIBLE
//                                }
//                            }
//                        } else {
//                            ContextCompat.getMainExecutor(context).execute {
//                                widgetsMap.forEach {
//                                    (it.value as? FcrWebViewWidget)?.webViewContent?.binding?.btnFullSize?.visibility = GONE
//                                    (it.value as? FcrWebViewWidget)?.webViewContent?.binding?.btnClose?.visibility = GONE
//                                }
//                            }
//                        }
                    }
                }
            }
        }
    }

    private val cloudDiskWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet2 = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet2.signal == AgoraBoardInteractionSignal.LoadAlfFile) { //打开alf文件
                packet2?.let {
                    //设置active状态，是否关闭当前widget： 0关闭 1激活
                    val bodyStr = GsonUtil.gson.toJson(packet2.body)
                    val data = GsonUtil.gson.fromJson(bodyStr, AgoraEduCourseware::class.java)
                    //老师端设置widget的active状态
                    curMaxZIndex += 1
                    val extraProperties: MutableMap<String, Any> = mutableMapOf()
                    extraProperties["webViewUrl"] = data?.resourceUrl ?: ""
                    extraProperties["zIndex"] = curMaxZIndex
                    //打开云盘中的alf课件 setWidgetActive 本地会收到active消息，打开webview widget
                    eduContext?.widgetContext()?.setWidgetActive(
                        widgetId = strwebView.plus(dash).plus(data.resourceUuid),
                        ownerUserUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid,
                        roomProperties = extraProperties,
                        syncFrame = AgoraWidgetFrame(
                            defaultPositionPercent,
                            defaultPositionPercent,
                            defaultSizeWidthPercent,
                            defaultSizeHeightPercent
                        )
                    )
                    return
                    //处理inactive的情况
                    // if widget is countdown/iclicker/vote, remove widget and its data.
//                    eduContext?.widgetContext()?.setWidgetInActive(widgetId = id, isRemove = true)
                }
            }
        }
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            //检查大窗widget状态，如果进入教室时大窗就是打开的，则创建widget，
            // 该widget可能渲染老师大窗视频流也可能是屏幕共享流
            eduContext?.widgetContext()?.getAllWidgetActive()?.forEach { entry ->
                if (entry.key.startsWith(strwebView) || entry.key.startsWith(strmediaPlayer)) {
                    if (entry.value) {
                        ContextCompat.getMainExecutor(context).execute {
                            createWidget(entry.key)
                        }
                    }
                }
            }
        }
    }

    // widget's direct container
    private val widgetContainerMap = mutableMapOf<String, ViewGroup>()
    private val animatorUtil = AnimatorUtil()

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.roomContext()?.addHandler(roomHandler)
        addOrRemoveActiveObserver()//观察Active状态
        eduContext?.widgetContext()?.addWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(cloudDiskWidgetMsgObserver, AgoraWidgetDefaultId.AgoraCloudDisk.id)
    }


    /**
     * 创建webView Widget
     */
    private fun createWidget(widgetId: String) {
        if (widgetsMap.contains(widgetId)) { // widgetId：streamWindow-resourceUuid
            LogX.w(tag, "'$widgetId' is already created")
            return
        }
        if (widgetId.contains(strwebView) || widgetId.contains(strmediaPlayer)) {
            val str = widgetId.split(dash)
            val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(str[0])
            widgetConfig?.let { config ->
                config.widgetId = widgetId
                // add syncFrameObserver
                addOrRemoveSyncFrameObserver(widgetId = widgetId)
                // create widget and init
                val webViewWidget = eduContext?.widgetContext()?.create(config)

                webViewWidget?.let {
                    // record widget
                    widgetsMap[widgetId] = it
                    // widget's direct parentView

                    val widgetDirectParent =
                        FcrWidgetDirectParentView(
                            context,
                            binding.root,
                            widgetId,
                            this) //widgetDirectParent: widget 的容器 (widget的父布局)
                    widgetDirectParent.initView(agoraUIProvider)
                    // 垂直层级follow老师端

                    getWidgetExtra(it)?.zIndex?.let { zIndex ->
                        widgetDirectParent.z = zIndex
                        curMaxZIndex = zIndex
                    }
                    // 获取动画起始位置
                    // 把widget添加到目的位置
                    val params = layoutWidgetDirectParent(binding.root, widgetId)//获取webview宽高参数
                    binding.root.addView(widgetDirectParent, params)
                    // init widget's ui
                    it.init(widgetDirectParent)//初始化webview widget
                    widgetContainerMap[widgetId] = widgetDirectParent
                    handleLargeWindowEvent(widgetId, true)
                    //如果创建webview widget的时候有权限，则显示按钮
//                    if (localUserGranted || eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
//                        ContextCompat.getMainExecutor(context).execute {
//                            (it as? FcrWebViewWidget)?.webViewContent?.binding?.btnFullSize?.visibility = VISIBLE
//                            (it as? FcrWebViewWidget)?.webViewContent?.binding?.btnClose?.visibility = VISIBLE
//                        }
//                    }
//                    //widget全屏按钮
//                    (it as? FcrWebViewWidget)?.webViewContent?.binding?.btnFullSize?.setOnClickListener {
//                        // 授权后，可以操作
//                        if (localUserGranted || eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
//                            binding.root.removeView(widgetDirectParent)
//                            val newParams = setWidgetFullSize(binding.root, widgetId)
//                            binding.root.addView(widgetDirectParent, newParams)
//                        }
//                    }
//                    //widget关闭按钮
//                    (it as? FcrWebViewWidget)?.webViewContent?.binding?.btnClose?.setOnClickListener {
//                        // 授权后，可以操作
//                        if (localUserGranted || eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
//                            eduContext?.widgetContext()?.setWidgetInActive(widgetId, isRemove = true)
////                            destroyWidget(widgetId)
//                        }
//                    }
                }
            }
        }
    }

    private fun setWidgetFullSize(allWidgetsContainer: ViewGroup, widgetId: String): LayoutParams {
        val frame = eduContext?.widgetContext()?.getWidgetSyncFrame(widgetId)
        if (frame?.x != 0f || frame?.y != 0f || frame?.width != 1f || frame?.height != 1f) {
            val newFrame = AgoraWidgetFrame(0f, 0f, 1f, 1f)
            // 计算widget的宽高，如果size比例信息为空，则使用默认比例
            val allWidgetsContainerWidth = allWidgetsContainer.width
            val width = allWidgetsContainerWidth.toFloat() * (newFrame?.width ?: defaultSizeWidthPercent)
            val allWidgetsContainerHeight = allWidgetsContainer.height
            val height = allWidgetsContainerHeight.toFloat() * (newFrame?.height ?: defaultSizeHeightPercent)
            val params = LayoutParams(width.roundToInt(), height.roundToInt())
            // 如果frame.size有效，则按照比例计算，否则直接放在allWidgetsContainer的中央
            params.leftMargin = (newFrame?.x?.let { it * (allWidgetsContainerWidth - width) }
                ?: defaultPositionPercent * (allWidgetsContainerWidth - width)).roundToInt()
            params.topMargin = (newFrame?.y?.let { it * (allWidgetsContainerHeight - height) }
                ?: defaultPositionPercent * (allWidgetsContainerHeight - height)).roundToInt()
            eduContext?.widgetContext()?.updateSyncFrame(newFrame, widgetId)
            return params
        } else {//获取widget原始宽高
            // 计算widget的宽高，如果size比例信息为空，则使用默认比例
            val allWidgetsContainerWidth = allWidgetsContainer.width
            val width = allWidgetsContainerWidth.toFloat() * (defaultSizeWidthPercent)
            val allWidgetsContainerHeight = allWidgetsContainer.height
            val height = allWidgetsContainerHeight.toFloat() * (defaultSizeHeightPercent)
            val params = LayoutParams(width.roundToInt(), height.roundToInt())
            // 如果frame.size有效，则按照比例计算，否则直接放在allWidgetsContainer的中央
            params.leftMargin = (defaultPositionPercent * (allWidgetsContainerWidth - width)).roundToInt()//(allWidgetsContainerWidth - width):MED
            params.topMargin = (defaultPositionPercent * (allWidgetsContainerHeight - height)).roundToInt()
            val newFrame = AgoraWidgetFrame(defaultPositionPercent, defaultPositionPercent, defaultSizeWidthPercent, defaultSizeHeightPercent)
            eduContext?.widgetContext()?.updateSyncFrame(newFrame, widgetId)
            return params
        }
    }

    /**
     * layout widget's direct parentView position
     * @param widgetId
     */
    @UiThread
    private fun layoutWidgetDirectParent(allWidgetsContainer: ViewGroup, widgetId: String): LayoutParams {
        // 获取初始的比例信息，
        val frame = eduContext?.widgetContext()?.getWidgetSyncFrame(widgetId)
        // 计算widget的宽高，如果size比例信息为空，则使用默认比例
        val allWidgetsContainerWidth = allWidgetsContainer.width
        val width = allWidgetsContainerWidth.toFloat() * (frame?.width ?: defaultSizeWidthPercent)
        val allWidgetsContainerHeight = allWidgetsContainer.height
        val height = allWidgetsContainerHeight.toFloat() * (frame?.height ?: defaultSizeHeightPercent)
        val params = LayoutParams(width.roundToInt(), height.roundToInt())
        // 如果frame.size有效，则按照比例计算，否则直接放在allWidgetsContainer的中央
        params.leftMargin = (frame?.x?.let { it * (allWidgetsContainerWidth - width) }
            ?: defaultPositionPercent * (allWidgetsContainerWidth - width)).roundToInt()
        params.topMargin = (frame?.y?.let { it * (allWidgetsContainerHeight - height) }
            ?: defaultPositionPercent * (allWidgetsContainerHeight - height)).roundToInt()
        return params
    }


    private fun getWidgetExtra(widget: AgoraBaseWidget): FcrWebViewWidgetExtra? {
        return widget.widgetInfo?.roomProperties?.let { GsonUtil.toJson(it) }?.let {
            GsonUtil.jsonToObject<FcrWebViewWidgetExtra>(it)
        }
    }

    /**
     * 销毁widget
     * 销毁时如果讲台处于打开状态且此用户没有下台，需要从组件区域的起始位置动画移动到讲台区域的目的位置
     */
    private fun destroyWidget(widgetId: String) {
        val destroyWidgetRunnable = {
            handleLargeWindowEvent(widgetId, false)
            // remove from map
            val widget = widgetsMap.remove(widgetId)
            // remove observer
            eduContext?.widgetContext()?.removeWidgetSyncFrameObserver(
                this@FcrWebViewContainerComponent, widgetId
            )
            widget?.let { it1 ->
                it1.container?.let { group ->
                    runOnUIThread { binding.root.removeView(group) }
                    widgetContainerMap.remove(widgetId)
                }
                it1.release()
            }
        }

        // 本地找不到对应的流信息，说明已经下台，直接销毁widget即可
        destroyWidgetRunnable.invoke()
    }

    override fun release() {
        super.release()
        widgetsMap.forEach {
            // remove UIDataProviderListener
            it.value.release()
            // remove syncFrame observer
            addOrRemoveSyncFrameObserver(add = false, it.key)
        }
        widgetsMap.clear()
        widgetContainerMap.clear()
        addOrRemoveActiveObserver(add = false)
        eduContext?.roomContext()?.removeHandler(roomHandler)
        eduContext?.widgetContext()?.removeWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.widgetContext()?.removeWidgetMessageObserver(cloudDiskWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    private fun addOrRemoveActiveObserver(add: Boolean = true) {
        if (add) {
            eduContext?.widgetContext()?.addWidgetActiveObserver(webViewWidgetActiveObserver, FcrWebView.id)
            eduContext?.widgetContext()?.addWidgetActiveObserver(webViewWidgetActiveObserver, AgoraWidgetDefaultId.FcrMediaPlayer.id)
        } else {
            eduContext?.widgetContext()?.removeWidgetActiveObserver(webViewWidgetActiveObserver, FcrWebView.id)
            eduContext?.widgetContext()?.removeWidgetActiveObserver(webViewWidgetActiveObserver, AgoraWidgetDefaultId.FcrMediaPlayer.id)
        }
    }

    private fun addOrRemoveSyncFrameObserver(add: Boolean = true, widgetId: String) {
        if (add) {
            eduContext?.widgetContext()?.addWidgetSyncFrameObserver(this, widgetId)
        } else {
            eduContext?.widgetContext()?.removeWidgetSyncFrameObserver(this, widgetId)
        }
    }

    /**
     * 发送渲染webview消息
     */
    private fun handleLargeWindowEvent(widgetId: String, active: Boolean) {
        val currentStreamUuid = widgetId.split(dash)[1] //云盘的resourceUuid

        val signal = if (active) {
            FcrWebViewInteractionSignal.FcrWebViewShowed
        } else {
            FcrWebViewInteractionSignal.FcrWebViewClosed
        }
        val webViewUrl = widgetsMap[widgetId]?.let { getWidgetExtra(it)?.webViewUrl } ?: ""

        val packet = FcrWebViewInteractionPacket(signal, webViewUrl)
        //发送给AgoraUILargeVideoWidget
        eduContext?.widgetContext()?.sendMessageToWidget(
            Gson().toJson(packet), FcrWebView.id + dash + currentStreamUuid
        )
    }

    // FCRWidgetSyncFrameObserver
    override fun onWidgetSyncFrameUpdated(syncFrame: AgoraWidgetFrame, widgetId: String) {
        Log.e(tag, "onWidgetSyncFrameUpdated:$syncFrame")
        // 根据老师发过来的syncFrame计算widget新的位置和宽高，并使用动画移动至目的地
        // 同时更新其view层级
        widgetContainerMap[widgetId]?.let { directParent ->
            widgetsMap[widgetId]?.let {
                getWidgetExtra(it)?.zIndex?.let { zIndex ->
                    directParent.z = zIndex
                    curMaxZIndex = zIndex
                }
            }
            animator1(directParent, syncFrame)
        }
    }

    /**
     * 根据syncFrame中的比例信息把directParent位移到目的位置，并在此过程中缩放directParent到目的宽高
     * 主要用在视频从讲台移动到组件区域的过程
     */
    //@Synchronized
    private fun animator1(directParent: View, syncFrame: AgoraWidgetFrame) {
        //所有widget的祖父布局
        val grandParent = binding.root
        // widget父布局的layoutParams
        val layoutParams = directParent.layoutParams as? RelativeLayout.LayoutParams
        layoutParams?.let { params ->
            // 祖父布局中对于widget的有效宽度(可以认为是白板的宽度)
            val allWidgetsContainerWidth = grandParent.width
            // widget宽度
            val width = syncFrame.width?.let { allWidgetsContainerWidth * it } ?: params.width.toFloat()
            // 祖父布局中对于widget的有效高度(可以认为是白板的高度)
            val allWidgetsContainerHeight = grandParent.height
            // widget高度
            val height = syncFrame.height?.let { allWidgetsContainerHeight * it } ?: params.height.toFloat()
            // widget在x轴上可移动的有效范围
            val medWidth = allWidgetsContainerWidth - width
            // widget在y轴上可移动的有效范围
            val medHeight = allWidgetsContainerHeight - height
            // widget的坐标
            val left = syncFrame.x?.let { medWidth * it }
            val top = syncFrame.y?.let { medHeight * it }
            LogX.i(
                "$tag->grandParentWidth:${grandParent.width}, grandParentHeight:${grandParent.height}, " +
                    "directParentWidth:${params.width}, directParentHeight:${params.height}, " + "medWidth:$medWidth, " +
                    "medHeight:$medHeight, left:$left, top:$top, width:$width, height:$height"
            )
            // 构建动画参数
            val parameters = arrayOfNulls<Float>(8)
            // widget位移起始点
            parameters[0] = params.leftMargin.toFloat()
            parameters[2] = params.topMargin.toFloat()
            if (syncFrame.positionValid()) {
                // widget位移终点
                parameters[1] = left!!
                parameters[3] = top!!
            }
            // widget缩放起点
            parameters[4] = params.width.toFloat()
            parameters[6] = params.height.toFloat()
            if (syncFrame.sizeValid()) {
                // widget缩放终点
                parameters[5] = width
                parameters[7] = height
            }
            ContextCompat.getMainExecutor(context).execute {
                animatorUtil.translateScale2(directParent, parameters[0]!!, parameters[1], parameters[2]!!,
                    parameters[3], parameters[4]!!, parameters[5], parameters[6]!!, parameters[7],
                    object : FCRAnimatorListener() {
                        override fun onAnimationUpdate(fraction: Float) {
                            super.onAnimationUpdate(fraction)
                            Log.e(tag, "fraction:$fraction")
                            // 根据fraction计算widget当前的坐标
                            if (syncFrame.positionValid()) {
                                val leftDiff: Float = left!! - parameters[0]!!
                                val leftTmp = leftDiff * fraction
                                val topDiff: Float = top!! - parameters[2]!!
                                val topTmp = topDiff * fraction
                                params.leftMargin = (parameters[0]!! + leftTmp).roundToInt()
                                params.topMargin = (parameters[2]!! + topTmp).roundToInt()
                            }
                            // 根据fraction计算widget当前的宽高
                            if (syncFrame.sizeValid()) {
                                val widthDiff = width - parameters[4]!!
                                val widthTmp = widthDiff * fraction
                                params.width = (parameters[4]!! + widthTmp).roundToInt()
                                val heightDiff = height - parameters[6]!!
                                val heightTmp = heightDiff * fraction
                                params.height = (parameters[6]!! + heightTmp).roundToInt()
                            }
                            directParent.layoutParams = params
                        }

                        override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                            directParent.requestLayout()
                        }
                    })
            }
        }
    }
}
