package io.agora.online.component.teachaids.component

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
import androidx.core.content.ContextCompat
import io.agora.online.component.common.AbsAgoraEduConfigComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.component.teachaids.AgoraTeachAidWidgetActiveStateChangeData
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionPacket
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionSignal.ActiveState
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionSignal.NeedRelayout
import io.agora.online.view.FcrDragGroupView
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId.*
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager
import io.agora.agoraeducore.core.internal.transport.OnAgoraTransportListener
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.*
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.component.teachaids.AgoraTeachAidCountDownWidget
import io.agora.online.component.teachaids.AgoraTeachAidIClickerWidget
import io.agora.online.component.teachaids.vote.AgoraTeachAidVoteWidget
import io.agora.online.config.FcrUIConfig
import io.agora.online.databinding.FcrOnlineEduTeachAidContainerComponentBinding

/**
 * author : cjw
 * date : 2022/2/16
 * description :
 */
class AgoraEduTeachAidContainerComponent : AbsAgoraEduConfigComponent<FcrUIConfig>, OnAgoraTransportListener {
    private val tag = "AgoraEduTeachAidsContainerComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineEduTeachAidContainerComponentBinding.inflate(LayoutInflater.from(context), this, true)
    private val teachAidWidgets = mutableMapOf<String, AgoraBaseWidget>()

    private val widgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            createWidget(widgetId)
        }

        override fun onWidgetInActive(widgetId: String) {
            destroyWidget(widgetId)
        }
    }

    // listen msg of countDownWidget closeSelf
    private val teachAidWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.jsonToObject<AgoraTeachAidWidgetInteractionPacket>(msg)
            packet?.let {
                when (packet.signal) {
                    ActiveState -> {//设置active状态，是否关闭当前widget： 0关闭 1激活
                        val data =
                            GsonUtil.jsonToObject<AgoraTeachAidWidgetActiveStateChangeData>(packet.body.toString())
                        if (data?.active == true) {
                            //老师端设置widget的active状态
                            eduContext?.widgetContext()?.setWidgetActive(widgetId = id, roomProperties = data.properties)
                            return
                        }
                        //处理inactive的情况
                        // if widget is countdown/iclicker/vote, remove widget and its data.
                        eduContext?.widgetContext()?.setWidgetInActive(widgetId = id, isRemove = true)
                    }
                    NeedRelayout -> {
                        // only the vote needs to be relayout, because it contain a recyclerView.
                        if (id == Polling.id) {
                            widgetDirectParentLayoutListener[id]?.let { relayoutWidget(binding.root, it.first, id) }
                        } else {
                        }
                    }
                }
            }
        }
    }

    // listen joinRoomSuccess event
    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            // check widget init status
            if (eduContext?.widgetContext()?.getWidgetActive(CountDown.id) == true) {
                createWidget(CountDown.id)
            }
            if (eduContext?.widgetContext()?.getWidgetActive(Selector.id) == true) {
                createWidget(Selector.id)
            }
            if (eduContext?.widgetContext()?.getWidgetActive(Polling.id) == true) {
                createWidget(Polling.id)
            }
            if (eduContext?.widgetContext()?.getWidgetActive(AgoraCloudDisk.id) == true) {
                createWidget(AgoraCloudDisk.id)
            }
        }
    }

    private fun createWidget(widgetId: String) {
        if (teachAidWidgets.contains(widgetId)) {
            LogX.w(tag, "'$widgetId' is already created, can not repeat create!")
            return
        }
        when (widgetId) {//for scene builder
            CountDown.id -> {
                if (!getUIConfig().counter.isVisible) {
                    return
                }
            }
            Selector.id -> {
                if (!getUIConfig().popupQuiz.isVisible) {
                    return
                }
            }
            Polling.id -> {
                if (!getUIConfig().poll.isVisible) {
                    return
                }
            }
        }
        LogX.w(tag, "create teachAid that of '$widgetId'")
        val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(widgetId)
        widgetConfig?.let { config ->
            val widget = eduContext?.widgetContext()?.create(config)
            widget?.let {
                LogX.w(tag, "successfully created '$widgetId'")
                when (widgetId) {
                    CountDown.id -> {
                        (it as? AgoraTeachAidCountDownWidget)?.getWidgetMsgObserver()?.let { observer ->
                            eduContext?.widgetContext()?.addWidgetMessageObserver(observer, widgetId)
                        }
                    }
                    Selector.id -> {
                        (it as? AgoraTeachAidIClickerWidget)?.getWidgetMsgObserver()?.let { observer ->
                            eduContext?.widgetContext()?.addWidgetMessageObserver(observer, widgetId)
                        }
                    }
                    Polling.id -> {
                        (it as? AgoraTeachAidVoteWidget)?.getWidgetMsgObserver()?.let { observer ->
                            eduContext?.widgetContext()?.addWidgetMessageObserver(observer, widgetId)
                        }
                    }
                }
                // record widget
                teachAidWidgets[widgetId] = widget
                ContextCompat.getMainExecutor(context).execute {
                    val dragView = FcrDragGroupView(context)
                    dragView.initView(agoraUIProvider)
                    binding.rootLayout.addView(dragView)
                    widget.init(dragView)
                }

                // create widgetContainer and add to binding.root(allWidgetsContainer)
//                ContextCompat.getMainExecutor(context).execute {
//                    val widgetContainer = managerWidgetsContainer(allWidgetsContainer = binding.root, widgetId = widgetId)
//                    LogX.i(tag, "successfully created '$widgetId' container")
//                    widgetContainer?.let { group ->
//                        LogX.w(tag, "initialize '$widgetId'")
//                        // init widget ui
//                        widget.init(group)
//                    }
//                }
            }
        }
    }

    /**
     * 新增widget只用传前两个参数
     * 删除widget全部参数都传
     */
    private fun managerWidgetsContainer(
        allWidgetsContainer: ViewGroup,
        widgetId: String,
        willRemovedWidgetDirectParent: ViewGroup? = null
    ): ViewGroup? {
        return if (willRemovedWidgetDirectParent == null) {
            // widget's direct parentView
            val widgetDirectParent = LinearLayout(context)
            widgetDirectParent.orientation = LinearLayout.VERTICAL

            val dragView = FcrDragGroupView(context)
            dragView.initView(agoraUIProvider)
            widgetDirectParent.addView(dragView)
            val params = layoutWidgetDirectParent(allWidgetsContainer, widgetDirectParent, widgetId)
            allWidgetsContainer.addView(widgetDirectParent, params)
            widgetDirectParent
        } else {
            widgetDirectParentLayoutListener[widgetId]?.second?.let {
                willRemovedWidgetDirectParent.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
            widgetDirectParentLayoutListener.remove(widgetId)
            allWidgetsContainer.removeView(willRemovedWidgetDirectParent)
            null
        }
    }

    // key: widgetId   value: whether to actively change the position of widget(or widget direct parentView)
    private val changeWidgetPositionBySelf = mutableMapOf<String, Boolean>()
    private val widgetDirectParentLayoutListener = mutableMapOf<String,
        Pair<ViewGroup, ViewTreeObserver.OnGlobalLayoutListener>>()
    var count = 0 // 计算2次，避免计算位置不对
    /**
     * layout widget's direct parentView position
     * @param widgetDirectParent widget's direct parentView
     * @param widgetId
     */
    private fun layoutWidgetDirectParent(
        allWidgetsContainer: ViewGroup, widgetDirectParent: LinearLayout,
        widgetId: String
    ): ConstraintLayout.LayoutParams {
        widgetDirectParent.gravity = Gravity.CENTER
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.startToStart = allWidgetsContainer.id
        params.topToTop = allWidgetsContainer.id
        // default center
        params.endToEnd = allWidgetsContainer.id
        params.bottomToBottom = allWidgetsContainer.id
        // try to get widgetDirectParent's width/height and relayout it
        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (widgetDirectParent.width > 0 && widgetDirectParent.height > 0) {
                    if (count > 1) {
                        widgetDirectParent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                    count++
                    // 当本次onGlobalLayout回掉是由于主动改变layoutParams导致的时，则忽略
                    if (changeWidgetPositionBySelf[widgetId] == true) {
                        changeWidgetPositionBySelf[widgetId] = false
                        return
                    }
                    changeWidgetPositionBySelf[widgetId] = true
                    relayoutWidget(allWidgetsContainer, widgetDirectParent, widgetId)
                }
            }
        }
        widgetDirectParent.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        // record for remove
        widgetDirectParentLayoutListener[widgetId] = Pair(widgetDirectParent, layoutListener)
        return params
    }

    private fun relayoutWidget(allWidgetsContainer: ViewGroup, widgetDirectParent: ViewGroup, widgetId: String) {
        // make sure frame info is latest; if frame is not empty,
        // update widgetDirectParent position follow frame
        val frame = eduContext?.widgetContext()?.getWidgetSyncFrame(widgetId) ?: return
        val medWidth = allWidgetsContainer.width - widgetDirectParent.width
        val medHeight = allWidgetsContainer.height - widgetDirectParent.height
        val left = medWidth * frame.x!!
        val top = medHeight * frame.y!!

        LogX.i(
            "$tag->parentWidth:${allWidgetsContainer.width}, parentHeight:${allWidgetsContainer.height}, " +
                    "width:${widgetDirectParent.width}, height:${widgetDirectParent.height}, " +
                    "medWidth:$medWidth, medHeight:$medHeight"+"|| frame.x:frame.y=${frame.x} : ${frame.y} "
        )
        val params = widgetDirectParent.layoutParams as? ConstraintLayout.LayoutParams
        LogX.e(tag, "<<<<<<<<<<relayoutWidget =${params?.leftMargin}:${params?.topMargin} to $left:$top")

        params?.endToEnd = UNSET
        params?.bottomToBottom = UNSET
        params?.leftMargin = left.toInt()
        params?.topMargin = top.toInt()
        if (Thread.currentThread().id != Looper.getMainLooper().thread.id) {
            widgetDirectParent.layoutParams = params
        } else {
            ContextCompat.getMainExecutor(context).execute {
                widgetDirectParent.layoutParams = params
            }
        }
    }

    private fun destroyWidget(widgetId: String) {
        // remove from map
        val widget = teachAidWidgets.remove(widgetId)
        // remove UIDataProviderListener
        when (widgetId) {
            CountDown.id -> {
                (widget as? AgoraTeachAidCountDownWidget)?.getWidgetMsgObserver()?.let { observer ->
                    eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, widgetId)
                }
            }
            Selector.id -> {
                (widget as? AgoraTeachAidIClickerWidget)?.getWidgetMsgObserver()?.let { observer ->
                    eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, widgetId)
                }
            }
            Polling.id -> {
                (widget as? AgoraTeachAidVoteWidget)?.getWidgetMsgObserver()?.let { observer ->
                    eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, widgetId)
                }
            }
            AgoraCloudDisk.id -> {
                (widget as? AgoraTeachAidVoteWidget)?.getWidgetMsgObserver()?.let { observer ->
                    eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, widgetId)
                }
            }
        }
        widget?.let { widget ->
            ContextCompat.getMainExecutor(binding.root.context).execute {
                widget.release()
                widget.container?.let { group ->
                    managerWidgetsContainer(binding.root, widgetId, group)
                }
            }
        }
    }

    init {
        AgoraTransportManager.addListener(EVENT_ID_TOOL_COUNTDOWN, this)
        AgoraTransportManager.addListener(EVENT_ID_TOOL_SELECTOR, this)
        AgoraTransportManager.addListener(EVENT_ID_TOOL_POLLING, this)
        AgoraTransportManager.addListener(EVENT_ID_TOOL_CLOUD, this)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
//        binding.fcrDrag.initView(agoraUIProvider)
        eduContext?.roomContext()?.addHandler(roomHandler)
        addAndRemoveActiveObserver()
        eduContext?.widgetContext()?.addWidgetMessageObserver(teachAidWidgetMsgObserver, CountDown.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(teachAidWidgetMsgObserver, Selector.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(teachAidWidgetMsgObserver, Polling.id)
//        eduContext?.widgetContext()?.addWidgetMessageObserver(teachAidWidgetMsgObserver, AgoraCloudDisk.id)

    }

    override fun release() {
        super.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)

        teachAidWidgets.forEach {
            // remove UIDataProviderListener
            when (it.key) {
                CountDown.id -> {
                    (it.value as? AgoraTeachAidCountDownWidget)?.getWidgetMsgObserver()?.let { observer ->
                        eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, CountDown.id)
                    }
                }
                Selector.id -> {
                    (it.value as? AgoraTeachAidIClickerWidget)?.getWidgetMsgObserver()?.let { observer ->
                        eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, Selector.id)
                    }
                }
                Polling.id -> {
                    (it.value as? AgoraTeachAidVoteWidget)?.getWidgetMsgObserver()?.let { observer ->
                        eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, Polling.id)
                    }
                }
//                AgoraCloudDisk.id -> {
//                    (it.value as? AgoraVoteWidget)?.getWidgetMsgObserver()?.let { observer ->
//                        eduContext?.widgetContext()?.removeWidgetMessageObserver(observer, AgoraCloudDisk.id)
//                    }
//                }
            }
            it.value.release()
        }
        teachAidWidgets.clear()
        addAndRemoveActiveObserver(add = false)
        AgoraTransportManager.removeListener(EVENT_ID_TOOL_COUNTDOWN)
        AgoraTransportManager.removeListener(EVENT_ID_TOOL_SELECTOR)
        AgoraTransportManager.removeListener(EVENT_ID_TOOL_POLLING)
        AgoraTransportManager.removeListener(EVENT_ID_TOOL_CLOUD)
        // clear data about position
        changeWidgetPositionBySelf.clear()
        widgetDirectParentLayoutListener.forEach {
            it.value.first.viewTreeObserver.removeOnGlobalLayoutListener(it.value.second)
        }
        widgetDirectParentLayoutListener.clear()
    }

    private fun addAndRemoveActiveObserver(add: Boolean = true) {
        if (add) {
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, CountDown.id)
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, Selector.id)
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, Polling.id)
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraCloudDisk.id)
        } else {
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActiveObserver, CountDown.id)
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActiveObserver, Selector.id)
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActiveObserver, Polling.id)
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActiveObserver, AgoraCloudDisk.id)
        }
    }

    override fun onTransport(event: AgoraTransportEvent) {
        when (event.eventId) {
            EVENT_ID_TOOL_COUNTDOWN -> {
                createWidget(CountDown.id)
            }
            EVENT_ID_TOOL_SELECTOR -> {
                createWidget(Selector.id)
            }
            EVENT_ID_TOOL_POLLING -> {
                createWidget(Polling.id)
            }
            EVENT_ID_TOOL_CLOUD -> {
                handleCloudDiskLifeCycle()//处理云盘点击事件，创建云盘widget
            }
            else -> {
                LogX.e(tag, "receive unexpected event: ${GsonUtil.toJson(event)}")
            }
        }
    }

    /**
     * 管理云盘生命周期
     * 当cloudDiskWidget实例不存在时，直接新建；如果存在则设置widget的container课件(不可见的设置是cloudDiskWidget自身设置的)
     * manager cloudDiskWidget's lifeCycle
     * if the instance of cloudDiskWidget not exists in local, create it; but if it exists, set the container of
     * cloudDiskWidget visibility is VISIBLE
     */
    private fun handleCloudDiskLifeCycle() {
        if (teachAidWidgets.contains(AgoraCloudDisk.id)) {
            LogX.i(tag, "AgoraCloudWidget is exists in local, set container visibility is VISIBLE!")
            teachAidWidgets[AgoraCloudDisk.id]?.container?.visibility = VISIBLE
            return
        }
        createWidget(AgoraCloudDisk.id)
    }

    override fun updateUIForConfig(config: FcrUIConfig) {
    }

    override fun getUIConfig(): FcrUIConfig {
        return getTemplateUIConfig()
    }
}
