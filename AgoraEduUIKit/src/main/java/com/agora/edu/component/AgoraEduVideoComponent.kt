package com.agora.edu.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.agora.edu.component.helper.FcrClickView
import com.agora.edu.component.teachaids.bean.StaticData
import com.agora.edu.component.teachaids.presenter.FCRLargeWindowManager
import com.agora.edu.component.teachaids.webviewwidget.FcrWidgetDirectParentView
import com.agora.edu.component.view.FcrDragTouchGroupView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Audio
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Video
import io.agora.agoraeducore.core.context.AgoraEduContextSystemDevice
import io.agora.agoraeducore.core.context.AgoraEduContextSystemDevice.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduVideoComponentBinding
import io.agora.agoraeduuikit.impl.video.AgoraEduFloatingControlWindow
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.video.IAgoraOptionListener
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import kotlin.math.abs
import kotlin.math.min

/**
 * 基础视频组件
 * Basic video component
 */
class AgoraEduVideoComponent : AbsAgoraEduComponent, IAgoraOptionListener, FcrDragTouchGroupView.OnDoubleClickListener {

    private val TAG = "AgoraEduVideoComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var videoListener: IAgoraUIVideoListener? = null
    var videoFlatView: AgoraEduFloatingControlWindow? = null
    var localUsrInfo: AgoraEduContextUserInfo? = null
    private var curUserDetailInfo: AgoraUIUserDetailInfo? = null
    var largeWindowOpened: Boolean = false //当前视频组件对应的大窗widget是否打开
    var isLargeWindow: Boolean = false // 当前组件是否是大窗组件还是基础视频窗组件，用于拖拉事件
    private var lastVideoRender: Boolean? = true
    private val binding = AgoraEduVideoComponentBinding.inflate(LayoutInflater.from(context), this, true)
    var videoViewParent: ViewGroup = binding.videoContainer
    private val dash = "-"
    private var defaultPositionPercent = 0.5F //默认位置x，y

    //    private var curZIndex =0 //当前video对应大窗的zIndex
    private var lastSizeWidthPercent = 0f //变成全屏大窗前记录当前大窗的位置百分比
    private var lastSizeHeightPercent = 0f

    //大班课默认宽高
    private var defaultSizeWidthPercent = 0.6F
    private var defaultSizeHeightPercent = 0.5F

    //小班课默认宽高
    private var defaultSizeWidthPercentSmall = 0.2F
    private var defaultSizeHeightPercentSmall = 0.15f

    private var curPositionPercent = 0.5F
    private var curSizeWidthPercent = 0.6F //当前大窗的位置百分比
    private var isLargeWindowFullSize = false //当前大窗是否全屏
    private var lastX: Int = 0//actiondown的时候记录坐标x
    private var lastY: Int = 0
    private var curX: Int = 0
    private var curY: Int = 0
    private var agoraLargeWindowContainer: View? = null
    private var listItemLeft = 0f //用于学生列表中, 当前item的left值

    /**
     * 更新当前用户的音频声音大小
     * refresh curUser audioVolume
     */
    @Volatile
    private var gifRunning = false
    private var isBackgroundGreen = false
    var clickView = FcrClickView()

    var gestureDetector: GestureDetectorCompat = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            LogX.e(TAG, "child onSingleTapConfirmed")
            handleClick()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            LogX.e(TAG, "child onDoubleTap")
            handleDoubleClick()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    })

    val onTouchListener: OnTouchListener = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            //LogX.e(TAG, "child onTouch，isLargeWindow=$isLargeWindow")

            if (isLargeWindow) {
                return onTouchEvent(event)
            } else {
                onMyTouchEvent(event)
                return gestureDetector.onTouchEvent(event)
            }
        }
    }

    init {
        binding.nameText.setShadowLayer(
            context.resources.getDimensionPixelSize(R.dimen.shadow_width).toFloat(),
            2.0f, 2.0f, context.resources.getColor(R.color.fcr_text_level1_color)
        )
        //初始化音频图标状态
        binding.audioIc.isEnabled = false
        binding.videoIc.isEnabled = false
        binding.audioIcContainer.visibility = GONE
        binding.audioIc.visibility = GONE
        upsertUserDetailInfo(null)
        binding.cardView.setOnTouchListener(onTouchListener)
        clickView.listener = object :FcrDragTouchGroupView.OnDoubleClickListener{
            override fun onDoubleClick() {
                handleDoubleClick()
            }

            override fun onClick() {
                handleClick()
            }
        }
    }

    private val largeWindowObserver = object : AgoraWidgetMessageObserver {
        //只负责老师窗口的逻辑
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.gson.fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
            if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender) {
                //大窗停止渲染，小窗恢复渲染
                val bodyStr = GsonUtil.toJson(packet.body)
                (GsonUtil.gson.fromJson(bodyStr, AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                    //拿到userDetailInfo显示小窗
                    largeWindowOpened = false
                    upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
                } ?: Runnable {
                    LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                }
            } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
                val bodyStr = GsonUtil.toJson(packet.body)
                //大窗开始渲染，小窗显示占位图
                (GsonUtil.gson.fromJson(bodyStr, AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                    //拿到userDetailInfo
                    largeWindowOpened = true
                    upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
                } ?: Runnable {
                    LogX.e(TAG, "${packet.signal}, packet.body convert failed")
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        agoraLargeWindowContainer = agoraUIProvider.getLargeVideoArea()
        localUsrInfo = eduContext?.userContext()?.getLocalUserInfo()
    }

    fun handleClick() {
        // only teacher can trigger floatingWindow
        if (localUsrInfo?.role != Teacher
            || eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.ONE_ON_ONE
            && curUserDetailInfo?.role == Teacher
        ) {
            //如果不是老师，或者是1v1的老师 直接返回
            return
        }
        videoFlatView = curUserDetailInfo?.let {
            AgoraEduFloatingControlWindow(it, context, this@AgoraEduVideoComponent, eduContext)
        }
        videoFlatView?.show(binding.root)
    }

    fun handleDoubleClick() {
        if (localUsrInfo?.role == Teacher) {
            if (curUserDetailInfo == null) {
                return
            }
            //set 大窗widget active
            val extraProperties: MutableMap<String, Any> = mutableMapOf()
//                extraProperties["zIndex"] = curMaxZIndex
            extraProperties[StaticData.extraUserUuidKey] = curUserDetailInfo?.userUuid!!
            //打开大窗widget
            eduContext?.roomContext()?.getRoomInfo()?.roomUuid?.let {
                isLargeWindow = FCRLargeWindowManager.isLargeWindow(it, curUserDetailInfo?.streamUuid!!)
            }
            if (!isLargeWindow) {//当前组件不属于大窗的话
                eduContext?.widgetContext()?.setWidgetActive(
                    widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                    ownerUserUuid = curUserDetailInfo?.userUuid,
                    roomProperties = extraProperties,
                    syncFrame = AgoraWidgetFrame(
                        defaultPositionPercent,
                        defaultPositionPercent,
                        defaultSizeWidthPercent,
                        defaultSizeHeightPercent
                    )
                )
            } else {//大窗已经打开的情况
                if (!isLargeWindowFullSize) {
                    lastSizeWidthPercent = ((this.parent.parent as? FcrWidgetDirectParentView)?.left ?: 0) * 1.0f
                    lastSizeHeightPercent = ((this.parent.parent as? FcrWidgetDirectParentView)?.top ?: 0) * 1.0f

                    if (agoraLargeWindowContainer!!.width != width) {
                        lastSizeWidthPercent /= (agoraLargeWindowContainer!!.width - width)
                    }
                    if (agoraLargeWindowContainer!!.height != height) {
                        lastSizeHeightPercent /= (agoraLargeWindowContainer!!.height - height)
                    }
                    //从大窗变成全屏大窗
                    eduContext?.widgetContext()?.setWidgetActive(
                        widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                        ownerUserUuid = curUserDetailInfo?.userUuid,
                        roomProperties = extraProperties,
                        syncFrame = AgoraWidgetFrame(0f, 0f, 1f, 1f)
                    )
                    isLargeWindowFullSize = true
                } else {
                    //全屏大窗变成默认宽高大窗
                    isLargeWindowFullSize = false
                    //大窗的宽
                    var largeWindowWidth = defaultSizeWidthPercent * agoraLargeWindowContainer!!.width
                    //大窗的高
                    var largeWindowHeight = largeWindowWidth * 0.62f
                    if (curUserDetailInfo?.role == Teacher && eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS
                        || eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.ONE_ON_ONE
                    ) {
                        eduContext?.widgetContext()?.setWidgetActive(
                            widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                            ownerUserUuid = curUserDetailInfo?.userUuid,
                            roomProperties = extraProperties,
                            syncFrame = AgoraWidgetFrame(
                                lastSizeWidthPercent,
                                lastSizeHeightPercent,
                                largeWindowWidth / agoraLargeWindowContainer!!.width,
                                largeWindowHeight / agoraLargeWindowContainer!!.height
                            )
                        )
                    } else {//小班课中的大窗默认大小；大班课中学生的大窗默认大小
                        //大窗的宽
                        largeWindowWidth = defaultSizeWidthPercentSmall * agoraLargeWindowContainer!!.width
                        //大窗的高
                        largeWindowHeight = largeWindowWidth * 0.62f
                        eduContext?.widgetContext()?.setWidgetActive(
                            widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                            ownerUserUuid = curUserDetailInfo?.userUuid,
                            roomProperties = extraProperties,
                            syncFrame = AgoraWidgetFrame(
                                lastSizeWidthPercent,
                                lastSizeHeightPercent,
                                largeWindowWidth / agoraLargeWindowContainer!!.width,
                                largeWindowHeight / agoraLargeWindowContainer!!.height
                            )
                        )
                    }
                }
            }

        }
    }

    fun onMyTouchEvent(event: MotionEvent): Boolean {
        updateLargeWindowData()
        //LogX.e(TAG, "child onTouchEvent isLargeWindow = ${isLargeWindow}")
        if (isLargeWindow) {
            return false
        }
        clickView.onTouchEvent(this, event)
        curX = event.x.toInt()
        curY = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                LogX.i(TAG, "child onTouchEvent = ACTION_DOWN")
            }
            MotionEvent.ACTION_MOVE -> {
                LogX.i(TAG, "child onTouchEvent = ACTION_MOVE :  ${event.x}")
            }

            MotionEvent.ACTION_UP -> {
                LogX.i(TAG, "child onTouchEvent = ACTION_UP")
                // only teacher can trigger floatingWindow
                if (eduContext?.userContext()?.getLocalUserInfo()?.role == Teacher) {
                    when (eduContext?.roomContext()?.getRoomInfo()?.roomType) {
                        RoomType.SMALL_CLASS -> {
                            LogX.e(TAG, "child onTouchEvent event.y: ${event.y} agora_small_video_h: ${
                                    resources.getDimension(R.dimen.agora_small_video_h)}")

                            if (event.y > resources.getDimension(R.dimen.agora_small_video_h)*2) {
                                //如果大窗没有打开过，则发送widget active 打开大窗
                                eduContext?.roomContext()?.getRoomInfo()?.roomUuid?.let {
                                    isLargeWindow = FCRLargeWindowManager.isLargeWindow(it, curUserDetailInfo?.streamUuid!!)//判断当前组件是否属于大窗
                                }
                                if (!isLargeWindow) {//如果不是大窗，则打开大窗widget
                                    LogX.e(TAG, "child onTouchEvent 打开大窗 x = ${event.x}")
                                    //大窗的宽
                                    val largeWindowWidth = defaultSizeWidthPercentSmall * agoraLargeWindowContainer!!.width
                                    //大窗的高
                                    val largeWindowHeight = largeWindowWidth * 0.62f
                                    val medWidth = agoraLargeWindowContainer!!.width - largeWindowWidth
                                    val medHeight = agoraLargeWindowContainer!!.height - largeWindowHeight
                                    // widget的左边界的值除以widget有效移动范围宽(medWidth)
                                    //当前view 距离父布局的距离（left） - 大窗宽的一半 - event.x 绝对值
                                    var curPositionPercentX = 0f
                                    // widget的上边界的值除以widget有效移动范围高(medHeight)
                                    var curPositionPercentY = (event.y - largeWindowHeight) / medHeight
                                    if (event.x < 0) {
                                        //往当前view左边滑动
                                        if (curUserDetailInfo?.role == Student) {
                                            //移动讲台区域学生视频窗
                                            LogX.e(TAG, "child onTouchEvent 打开大窗left = $listItemLeft event.x = ${event.x}  largeWindowWidth/2: ${largeWindowWidth / 2}")
                                            curPositionPercentX = (listItemLeft + event.x - largeWindowWidth / 2) / medWidth
                                        } else {
                                            //移动讲台区域老师视频窗
                                            curPositionPercentX = (left - largeWindowWidth / 2 - abs(event.x)) / medWidth
                                        }
                                    } else {
                                        //往当前view右边滑动
                                        LogX.e(
                                            TAG,
                                            "child onTouchEvent 打开大窗right = ${right - width} event.x = ${event.x}  largeWindowWidth/2: ${largeWindowWidth / 2}"
                                        )
                                        if (curUserDetailInfo?.role == Student) {
                                            LogX.e(
                                                TAG,
                                                "child onTouchEvent 打开大窗right = $listItemLeft event.x = ${event.x}  largeWindowWidth/2: ${largeWindowWidth / 2}"
                                            )
                                            curPositionPercentX = (event.x + largeWindowWidth / 2) / medWidth
                                        } else {
                                            curPositionPercentX = (left + event.x - largeWindowWidth / 2) / medWidth
                                        }
                                    }
                                    //x轴滑动到最左边的情况
                                    if (curPositionPercentX < 0) {
                                        curPositionPercentX = 0f
                                    }
                                    if (curPositionPercentX > 1) {
                                        curPositionPercentX = (agoraLargeWindowContainer!!.width - largeWindowWidth) / medWidth
                                    }
                                    //y轴移动到大窗容器最下面附近的时候
                                    if (event.y - y + largeWindowHeight / 2 > y + agoraLargeWindowContainer!!.height) {
                                        curPositionPercentY =
                                            (agoraLargeWindowContainer!!.height - largeWindowHeight) / medHeight
                                    }
                                    //准备发送widget active消息
                                    val extraProperties: MutableMap<String, Any> = mutableMapOf()
                                    curMaxZIndex += 1
                                    extraProperties["zIndex"] = curMaxZIndex
                                    extraProperties[StaticData.extraUserUuidKey] = curUserDetailInfo?.userUuid!!
                                    eduContext?.widgetContext()?.setWidgetActive(
                                        widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                                        ownerUserUuid = curUserDetailInfo?.userUuid,
                                        roomProperties = extraProperties,
                                        syncFrame = AgoraWidgetFrame(
                                            curPositionPercentX,
                                            curPositionPercentY,
                                            largeWindowWidth / agoraLargeWindowContainer!!.width,
                                            largeWindowHeight / agoraLargeWindowContainer!!.height
                                        )
                                    )
                                }
                            }
                        }
                        RoomType.LARGE_CLASS, RoomType.ONE_ON_ONE -> {
                            if (event.x <= 0 && !isLargeWindow) {  //如果大窗没有打开过，则发送widget active 打开大窗
                                LogX.e(TAG, "child onTouchEvent 打开大窗 x = ${event.x}")

                                val largeWindowWidth = defaultSizeWidthPercent * agoraLargeWindowContainer!!.width
                                val largeWindowHeight =
                                    defaultSizeHeightPercent * agoraLargeWindowContainer!!.height
                                val medWidth = agoraLargeWindowContainer!!.width - largeWindowWidth
                                val medHeight = agoraLargeWindowContainer!!.height - largeWindowHeight
                                // widget的左边界的值除以widget有效移动范围宽(medWidth)
                                var curPositionPercentX =
                                    (agoraLargeWindowContainer!!.width - largeWindowWidth / 2 - abs(event.x)) / medWidth
                                var curPositionPercentY = (event.y - largeWindowHeight / 2) / medHeight
                                //x轴滑动到最左边的情况
                                if (curPositionPercentX < 0) {
                                    curPositionPercentX = 0f
                                }
                                //x轴滑动到接近最右边边的情况:滑动到的位置(大窗容器宽度-x偏移的绝对值)+一半的宽度>大窗容器宽度
                                if (agoraLargeWindowContainer!!.width - largeWindowWidth / 2 - abs(event.x) + largeWindowWidth > agoraLargeWindowContainer!!.width) {
                                    curPositionPercentX =
                                        (agoraLargeWindowContainer!!.width - largeWindowWidth) / medWidth
                                }
                                //y轴滑动到大窗容器最上边的情况
                                if (curPositionPercentY < 0) {
                                    curPositionPercentY = 0f
                                }
                                //y轴移动到大窗容器最下面的情况
                                if (curPositionPercentY > 1) {
                                    curPositionPercentY =
                                        (agoraLargeWindowContainer!!.height - largeWindowHeight) / medHeight
                                }
                                val extraProperties: MutableMap<String, Any> = mutableMapOf()
                                curMaxZIndex += 1
                                extraProperties["zIndex"] = curMaxZIndex
                                if (curUserDetailInfo != null) {
                                    extraProperties[StaticData.extraUserUuidKey] = curUserDetailInfo?.userUuid!!
                                    eduContext?.widgetContext()?.setWidgetActive(
                                        widgetId = AgoraWidgetDefaultId.LargeWindow.id + dash + curUserDetailInfo?.streamUuid,
                                        ownerUserUuid = curUserDetailInfo?.userUuid,
                                        roomProperties = extraProperties,
                                        syncFrame = AgoraWidgetFrame(
                                            curPositionPercentX,
                                            curPositionPercentY,
                                            defaultSizeWidthPercent,
                                            defaultSizeHeightPercent
                                        )
                                    )
                                }
                                return false
                            }
                        }
                        else -> {}
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                LogX.e(TAG, "child onTouchEvent = ACTION_CANCEL")
            }
            else -> {
            }
        }
        // 默认消费事件，如果是大窗，有父布局，就不消费，父布局传递点击和双击事件
        return true
    }

    private fun setTextureViewRound(view: View) {
        val radius: Float = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner).toFloat()
        val textureOutlineProvider = VideoTextureOutlineProvider(radius)
        view.outlineProvider = textureOutlineProvider
        view.clipToOutline = true
    }

    //设置学生列表中 item的left值
    fun setVideoItemLeft(leftValue: Float) {
        listItemLeft = leftValue
    }

    private fun setCameraState(info: AgoraUIUserDetailInfo?) {
        binding.videoIc.isEnabled = info?.isVideoEnable() ?: false
        binding.videoIc.isSelected = info?.hasVideo ?: false
    }

    private fun setVideoPlaceHolder(info: AgoraUIUserDetailInfo?) {
        binding.cardView.visibility = visibility
        binding.fcrNotVideoPlaceholderLayout.visibility = View.GONE

        binding.videoContainer.visibility = GONE
        if (info == null) {
            setVideoImgPlaceHolder(1)
        } else if (info.isVideoEnable() && info.hasVideo) {
            binding.videoContainer.visibility = VISIBLE
        } else if (!info.isVideoEnable()) {
            setVideoImgPlaceHolder(6)
        } else {
            setVideoImgPlaceHolder(3)
        }
    }

    fun setVideoImgPlaceHolder(level: Int) {
        binding.fcrNotVideoPlaceholderLayout.visibility = View.VISIBLE
        binding.fcrVideoPlaceholder.setImageLevel(level)
    }

    private fun setMicroState(info: AgoraUIUserDetailInfo?) {
        binding.audioIcContainer.visibility = VISIBLE
        binding.audioIc.visibility = VISIBLE
        binding.audioIc.isEnabled = info?.isAudioEnable() ?: false
        binding.audioIc.isSelected = info?.hasAudio ?: false
        if (!binding.audioIc.isEnabled || !binding.audioIc.isSelected) { //如果音频关闭，绿框消失
            postDelayed({ binding.cardView.setBackgroundResource(R.drawable.fcr_video_item_bg) }, 1000)
            isBackgroundGreen = false
        }
    }

    fun setPublishAudioVideo(info: AgoraUIUserDetailInfo?) {
        info?.streamUuid?.let {
            if (isLocalStream(info.streamUuid)) {
                LogX.e(TAG, "upsertUserDetailInfo audio = ${!info.hasAudio} || video = ${!info.hasVideo}")
                eduCore?.eduContextPool()?.streamContext()
                    ?.muteLocalStream(info.streamUuid, !info.hasAudio, !info.hasVideo)
            }
        }
    }

    fun isLocalStream(streamUuid: String): Boolean {
        val localUserUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid
        val info = eduContext?.streamContext()?.getAllStreamList()?.find {
            it.streamUuid == streamUuid && it.owner.userUuid == localUserUuid
        }
        return info != null
    }


    private fun fillName(name: String) {
        binding.nameText.text = name
    }

    fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo?, curVideoShouldRender: Boolean? = true) {
        eduContext?.widgetContext()?.addWidgetMessageObserver(
            largeWindowObserver, AgoraWidgetDefaultId.LargeWindow.id + "-" + info?.streamUuid
        )
        LogX.e(TAG, "upsertUserDetailInfo:${info.toString()}")
        ContextCompat.getMainExecutor(context).execute {
            /*if (info == curUserDetailInfo && lastVideoRender == curVideoShouldRender) {
                // double check duplicate data
                LogX.i(TAG, "new info is same to old, return")
                return@execute
            }*/
            LogX.i(TAG, "upsertUserDetailInfo curVideoShouldRender=$curVideoShouldRender || largeWindowOpened=$largeWindowOpened")
            //当前小窗不应该渲染流
            if ((curVideoShouldRender == false || largeWindowOpened) && !isLargeWindow) {// curVideoShouldRender:  current video item should render
                //large window showed
                //当前组件不属于大窗才会走这里
                binding.audioIcContainer.visibility = GONE
                binding.audioIc.visibility = GONE
                binding.videoIc.visibility = GONE
                binding.nameText.visibility = GONE
                binding.videoContainer.visibility = GONE
                setVideoImgPlaceHolder(2)
                binding.cardView.visibility = GONE // 拖拉下来，不要显示
                lastVideoRender = curVideoShouldRender
                return@execute
            }
            binding.cardView.visibility = VISIBLE // 显示
            setCameraState(info)
            setMicroState(info)
            setVideoPlaceHolder(info)
            if (info != null) {
                // handle userInfo
                if (info.role == AgoraEduContextUserRole.Student) {
                    val reward = info.reward
                    if (reward >= 0) {
                        binding.trophyLayout.visibility = VISIBLE
                        binding.trophyText.text = String.format(
                            context.getString(R.string.fcr_agora_video_reward),
                            min(reward, 99)
                        )
                        binding.trophyText.text =
                            String.format(context.getString(R.string.fcr_agora_video_reward), info.reward)
                    } else {
                        binding.trophyLayout.visibility = GONE
                    }
                    binding.videoIc.visibility = GONE
                } else {
                    binding.trophyLayout.visibility = GONE
                    binding.videoIc.visibility = GONE
                }
                binding.nameText.visibility = VISIBLE
                fillName(info.userName)
                if (info.role != Teacher) {
                    updateGrantedStatus(info.whiteBoardGranted)
                }

                if (info.isVideoEnable()) {
                    videoListener?.onRendererContainer(binding.videoContainer, info)
                } else {
                    videoListener?.onRendererContainer(null, info)
                }
            } else {
                binding.nameText.text = ""
                binding.trophyLayout.visibility = GONE
                binding.videoIc.visibility = GONE
                binding.boardGrantedIc.visibility = GONE
                curUserDetailInfo?.let {
                    videoListener?.onRendererContainer(null, it)
                }
            }
            this.curUserDetailInfo = info?.copy()
            this.lastVideoRender = curVideoShouldRender
            setPublishAudioVideo(info)
            updateLargeWindowData()
        }
    }

    fun updateLargeWindowData() {
        eduContext?.roomContext()?.getRoomInfo()?.roomUuid?.let { roomUuid ->
            curUserDetailInfo?.streamUuid?.let { streamUuid ->
                isLargeWindow = FCRLargeWindowManager.isLargeWindow(roomUuid, streamUuid)
            }
        }
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        if (context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        if (binding.audioIc.isEnabled && binding.audioIc.isSelected && value > 0 && !gifRunning) {
            ContextCompat.getMainExecutor(context).execute {
                //如果value超过50，显示绿框
                //LogX.d("updateAudioVolumeIndication value = $value")
                if (value > 40) { //value>40
                    //LogX.d("updateAudioVolumeIndication value = $value open green")
                    isBackgroundGreen = true
                    binding.cardView.setBackgroundResource(R.drawable.fcr_video_container_bg)
                } else {
                    if (isBackgroundGreen) {//防止上次设置了false 再次进来后多次发送postDelayed消息
                        //LogX.d("updateAudioVolumeIndication value = $value close green isBackgroundGreen = $isBackgroundGreen")
                        postDelayed({ binding.cardView.setBackgroundResource(R.drawable.fcr_video_item_bg) }, 1000)
                        isBackgroundGreen = false
                    }
                }
                Glide.with(this).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_video_ic_audio_on)
                    .placeholder(R.drawable.agora_video_ic_audio_on)
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<GifDrawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: GifDrawable?, model: Any?, target: Target<GifDrawable?>?,
                            dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            gifRunning = true
                            resource?.setLoopCount(1)
                            resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    binding.audioIc.setImageResource(R.drawable.agora_video_ic_audio_bg)
                                    gifRunning = false
                                }
                            })
                            return false
                        }
                    }).into(binding.audioIc)
            }
        }
    }

    private fun updateGrantedStatus(granted: Boolean) {
        binding.boardGrantedIc.post {
            binding.boardGrantedIc.visibility = if (granted) VISIBLE else INVISIBLE
        }
    }

    /**
     * 播放举手动画
     * play wave animation
     */
    fun updateWaveState(userUuid: String, waving: Boolean) {
        if (curUserDetailInfo?.userUuid == userUuid) {
            val runnable = Runnable {
                binding.handWavingComponent.visibility = VISIBLE
                binding.handWavingComponent.updateWaveState(waving)
            }
            if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
                runnable.run()
            } else {
                ContextCompat.getMainExecutor(context).execute(runnable)
            }
        }
    }

    override fun onAudioUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean) {
        LogX.d("onAudioUpdated")
        switchMedia(item, enabled, Microphone)
    }

    override fun onVideoUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean) {
        LogX.d("onVideoUpdated")
        val device = if (AgoraUIDeviceSetting.isFrontCamera()) {
            CameraFront
        } else {
            CameraBack
        }
        switchMedia(item, enabled, device)
    }

    override fun onCohostUpdated(item: AgoraUIUserDetailInfo, isCoHost: Boolean) {
        LogX.d(TAG, "onCohostUpdated-item:${GsonUtil.toJson(item)}, isCoHost:$isCoHost")
        if (isCoHost) {
            // unreachable
            eduContext?.userContext()?.addCoHost(item.userUuid)
        } else {
            if (item.role == Teacher) {
                // remove all cohost from stage
                eduContext?.userContext()?.getCoHostList()?.forEach {   //找到properties中对应的用户
                    eduContext?.streamContext()?.getStreamList(it.userUuid)?.forEach { eduStreamInfo -> //让该用户中的所有流大窗关闭
                        //找到当前当前台上用户的streamUuid
                        eduContext?.widgetContext()?.setWidgetInActive(
                            AgoraWidgetDefaultId.LargeWindow.id + dash + eduStreamInfo?.streamUuid,
                            true
                        )
                    }
                }
                eduContext?.userContext()?.removeAllCoHosts()
            } else {
                eduContext?.userContext()?.removeCoHost(item.userUuid)
            }
        }

    }

    override fun onGrantUpdated(item: AgoraUIUserDetailInfo, hasAccess: Boolean) {
        LogX.d("onGrantUpdated")
        val data = AgoraBoardGrantData(hasAccess, arrayOf(item.userUuid).toMutableList())
        val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
        eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
    }

    override fun onRewardUpdated(item: AgoraUIUserDetailInfo, count: Int) {
        LogX.d("onRewardUpdated")
        eduContext?.userContext()?.rewardUsers(arrayOf(item.userUuid).toMutableList(), count)
    }

    private fun switchMedia(item: AgoraUIUserDetailInfo, enabled: Boolean, device: AgoraEduContextSystemDevice) {
        localUsrInfo?.userUuid?.let {
            if (it == item.userUuid) {
                if (enabled) {
                    eduContext?.mediaContext()?.openSystemDevice(device)
                } else {
                    eduContext?.mediaContext()?.closeSystemDevice(device)
                }
            } else {
                val streamType = if (device == Microphone) Audio else Video
                eduContext?.streamContext()?.updateStreams(arrayListOf(item.streamUuid), enabled, streamType)

//                if (enabled) {
//                    eduContext?.streamContext()?.publishStreams(arrayListOf(item.streamUuid), streamType)
//                } else {
//                    eduContext?.streamContext()?.muteStreams(arrayListOf(item.streamUuid).toMutableList(), streamType)
//                }
            }
        }
    }

    fun getViewPosition(streamUuid: String): Rect? {
        curUserDetailInfo?.let {
            if (streamUuid == it.streamUuid) {
                return Rect(left, top, right, bottom)
            }
        }
        return null
    }

    override fun onDoubleClick() {
        LogX.e(TAG, "onDoubleClick")
        handleDoubleClick()
    }

    override fun onClick() {
        LogX.e(TAG, "onClick")
        handleClick()
    }
}

class VideoTextureOutlineProvider(private val mRadius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, mRadius)
    }
}