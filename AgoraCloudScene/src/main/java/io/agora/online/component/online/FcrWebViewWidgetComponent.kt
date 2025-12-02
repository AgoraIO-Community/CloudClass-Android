package io.agora.online.component.online

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.widget.FcrWidgetManager
import io.agora.online.widget.bean.FcrWebViewData
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessage
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.R
import io.agora.online.component.teachaids.webviewwidget.FcrWebViewWidget
import io.agora.online.databinding.FcrOnlineWidgetWebviewComponentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.contains
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableMapOf
import kotlin.collections.set


/**
 * author : felix
 * date : 2023/6/14
 * description : widget container component
 */
class FcrWebViewWidgetComponent : FcrBaseWidgetComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineWidgetWebviewComponentBinding.inflate(LayoutInflater.from(context), this, true)
    private var zIndex = 0f
    private var TAG = "WidgetContainerComponent"
    private val widgetList = LinkedHashMap<String, FcrWebViewData>()
    private val titleAdapter = TitleAdapter()
    private val contentAdapter = ContentAdapter()
    private var isFullScreen = false

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.widgetContext()
            ?.addWidgetMessageObserver(cloudDiskWidgetObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        initView()
    }

    fun initView() {
        binding.fcrDrag.initView(agoraUIProvider)
        binding.fcrDrag.setOnDragTouchListener(onDragClickListener)

        binding.fcrTitleTab.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.fcrTitleTab.adapter = titleAdapter
        titleAdapter.onClickTabListener = {
            binding.fcrContent.currentItem = it
        }

        binding.fcrContent.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.fcrContent.adapter = contentAdapter
        binding.fcrContent.isUserInputEnabled = false
        binding.fcrContent.offscreenPageLimit = Int.MAX_VALUE / 2

        binding.btnHidden.setOnClickListener {
            widgetListener?.onWidgetUpdate(false, AgoraWidgetDefaultId.FcrWebView.id, widgetList.size)
        }

        binding.btnFullSize.setOnClickListener {
            binding.fcrDrag.resetPosition()
            if (isFullScreen) {
                binding.btnFullSize.setImageResource(R.drawable.fcr_web_view_zoom2)
                val p = binding.fcrDrag.layoutParams
                p.width = context.resources.getDimensionPixelOffset(R.dimen.fcr_wv_width)
                p.height = context.resources.getDimensionPixelOffset(R.dimen.fcr_wv_height)
                binding.fcrDrag.layoutParams = p
            } else {
                binding.btnFullSize.setImageResource(R.drawable.fcr_web_view_zoom)
                val p = binding.fcrDrag.layoutParams
                p.width = ViewGroup.LayoutParams.MATCH_PARENT
                p.height = ViewGroup.LayoutParams.MATCH_PARENT
                binding.fcrDrag.layoutParams = p
            }
            isFullScreen = !isFullScreen
        }

        binding.btnRefresh.setOnClickListener {
            contentAdapter.refreshCurrent(binding.fcrContent.currentItem)
        }
    }

    override fun getRegisterWidgetIds(): List<String> {
        return listOf(AgoraWidgetDefaultId.FcrWebView.id, AgoraWidgetDefaultId.FcrMediaPlayer.id)
    }

    fun updateView() {
        if (widgetList.isNotEmpty()) {
            binding.rootLayout.visibility = View.VISIBLE
        } else {
            binding.rootLayout.visibility = View.GONE
        }
        val list = ArrayList<FcrWebViewData>()
        widgetList.forEach {
            list.add(it.value)
        }

        widgetListener?.onWidgetUpdate(true, AgoraWidgetDefaultId.FcrWebView.id, list.size)

        val selectPosition = list.size - 1

        titleAdapter.setData(list, selectPosition)
        contentAdapter.setData(list)
        binding.fcrContent.post { binding.fcrContent.currentItem = selectPosition }
    }

    override fun createWidget(widgetId: String) {
        LogX.e(TAG, "createWidget : $widgetId")

        if (widgetsMap.contains(widgetId)) { // widgetId：streamWindow-resourceUuid
            LogX.w(TAG, "'$widgetId' is already created")
            return
        }

        if (FcrWidgetManager.isWebViewWidget(widgetId)) {
            val str = widgetId.split(FcrWidgetManager.WIDGET_ID_DASH)
            val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(str[0])
            widgetConfig?.let { config ->
                config.widgetId = widgetId

                val widget = eduContext?.widgetContext()?.create(config)

                val widgetDirectParent = FrameLayout(context)
                widget?.init(widgetDirectParent)
                widget?.let {
                    widgetsMap[widgetId] = widget
                    val data = FcrWebViewData()
                    data.widget = widget
                    data.widgetId = widgetId
                    data.title = FcrWebViewWidget.getTitle(widget.widgetInfo)
                    data.itemView = widgetDirectParent
                    widgetList.put(widgetId, data)
                    updateView()
                }
            }
        }
    }

    override fun removeWidget(widgetId: String) {
        super.removeWidget(widgetId)
        LogX.e(TAG, "removeWidget : $widgetId")
        widgetList.remove(widgetId)
        updateView()
    }

    override fun release() {
        super.release()
        eduContext?.widgetContext()
            ?.removeWidgetMessageObserver(cloudDiskWidgetObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    override var roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            ContextCompat.getMainExecutor(context).execute {
                eduContext?.widgetContext()?.getAllWidgetActive()?.forEach { entry ->
                    if (FcrWidgetManager.isWebViewWidget(entry.key)) {
                        if (entry.value) {
                            createWidget(entry.key)
                        }
                    }
                }
            }
        }
    }

    internal class TitleAdapter : RecyclerView.Adapter<TitleAdapter.TitleHolder>() {
        var listData = ArrayList<FcrWebViewData>()
        var onClickTabListener: ((Int) -> Unit)? = null
        var selectPosition = 0

        fun setData(list: List<FcrWebViewData>, selectPosition: Int) {
            listData.clear()
            listData.addAll(list)
            this.selectPosition = selectPosition
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleHolder {
            return TitleHolder(LayoutInflater.from(parent.context).inflate(R.layout.fcr_online_webview_title, parent, false))
        }

        override fun getItemCount(): Int {
            return listData.size
        }

        override fun onBindViewHolder(holder: TitleHolder, position: Int) {
            holder.bindView(listData[position], selectPosition == position)
            holder.itemView.setOnClickListener {
                onClickTabListener?.invoke(position)
                selectPosition = position
                notifyDataSetChanged()
            }
        }

        internal class TitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bindView(info: FcrWebViewData, isSelect: Boolean) {
                itemView.findViewById<TextView>(R.id.fcr_file_name).text = info.title
                if (isSelect) {
                    itemView.findViewById<View>(R.id.fcr_file_item).setBackgroundColor(Color.WHITE)
                    itemView.findViewById<TextView>(R.id.fcr_file_name)
                        .setTextColor(itemView.resources.getColor(R.color.fcr_text_level1_color))
                } else {
                    itemView.findViewById<View>(R.id.fcr_file_item).background = null
                    itemView.findViewById<TextView>(R.id.fcr_file_name)
                        .setTextColor(itemView.resources.getColor(R.color.fcr_text_level2_color))
                }
            }
        }
    }


    internal class ContentAdapter : RecyclerView.Adapter<ContentAdapter.ContentHolder>() {
        var listData = ArrayList<FcrWebViewData>()

        fun setData(list: List<FcrWebViewData>) {
            listData.clear()
            listData.addAll(list)
            notifyDataSetChanged()
        }

        fun refreshCurrent(position: Int) {
            val message = AgoraWidgetMessage()
            message.action = 1
            listData.get(position).widget.sendWidgetMessage(message)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentHolder {
            return ContentHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.fcr_online_webview_content_item, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return listData.size
        }

        override fun onBindViewHolder(holder: ContentHolder, position: Int) {
            holder.bindView(listData.get(position))
        }

        internal class ContentHolder(var rootView: View) : RecyclerView.ViewHolder(rootView) {
            fun bindView(info: FcrWebViewData) {
                rootView.findViewById<ViewGroup>(R.id.fcr_content_item).removeAllViews()
                (info.itemView.parent as? ViewGroup)?.removeAllViews()
                rootView.findViewById<ViewGroup>(R.id.fcr_content_item).addView(info.itemView)
            }
        }
    }

    private val defaultPositionPercent = 0.5F
    private val defaultSizeWidthPercent = 0.54F
    private val defaultSizeHeightPercent = 0.71F

    val cloudDiskWidgetObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet.signal == AgoraBoardInteractionSignal.LoadAlfFile) { //打开alf文件
                packet?.let {
                    //设置active状态，是否关闭当前widget： 0关闭 1激活
                    val bodyStr = GsonUtil.gson.toJson(packet.body)
                    val data = GsonUtil.gson.fromJson(bodyStr, AgoraEduCourseware::class.java)
                    curMaxZIndex += 1 //老师端设置widget的active状态
                    val extraProperties: MutableMap<String, Any> = mutableMapOf()
                    extraProperties[FcrWidgetManager.WIDGET_WEBVIEW_RUL] = data?.resourceUrl ?: ""
                    extraProperties[FcrWidgetManager.WIDGET_Z_INDEX] = curMaxZIndex
                    //打开云盘中的alf课件 setWidgetActive 本地会收到active消息，打开webview widget
                    eduContext?.widgetContext()?.setWidgetActive(
                        widgetId = FcrWidgetManager.WIDGET_WEBVIEW.plus(FcrWidgetManager.WIDGET_ID_DASH)
                            .plus(data.resourceUuid),
                        ownerUserUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid,
                        roomProperties = extraProperties,
                        syncFrame = AgoraWidgetFrame(
                            defaultPositionPercent,
                            defaultPositionPercent,
                            defaultSizeWidthPercent,
                            defaultSizeHeightPercent
                        )
                    )
                }
            }
        }
    }
}