package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.gson.Gson
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.WhiteBoard
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.Chat
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionSignal.UnreadTips
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionPacket
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidget
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidgetListener
import io.agora.agoraeduuikit.impl.container.AbsUIContainer
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.tool.AgoraUIApplianceType
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp
import io.agora.agoraeduuikit.impl.users.RosterType
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardDrawingMemberState
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.impl.whiteboard.bean.*
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.MemberStateChanged
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig
import io.agora.agoraeduuikit.util.PopupAnimationUtil
import io.agora.agoraeduuikit.util.TextPinyinUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Business-related options on the right side of container
 */
class OptionsLayout : LinearLayout, View.OnClickListener {
    var popUpListener: OptionPopupListener? = null
    val tag = "OptionsLayout"

    private var eduContext: EduContextPool? = null

    private var settingItem: DeviceSettingLayoutPopupItem? = null
    private var toolboxItem: ToolBoxLayoutPopupItem? = null
    private var rosterItem: RosterLayoutPopupItem? = null
    private var chatItem: ChatLayoutPopupItem? = null
    var handsUpItem: OptionsLayoutHandsUpItem? = null
    private var boardToolItem: OptionsLayoutWhiteboardItem? = null

    // Current active option layout item that has a popup
    // and the popup shows.
    private var curPopupItem: OptionsLayoutPopupItem? = null
    protected var uiDataProvider: UIDataProvider? = null

    private var role = AgoraEduContextUserRole.Student
    private var studentWavingList = mutableListOf<AgoraUIUserDetailInfo>()
    var mUserList = mutableListOf<AgoraUIUserDetailInfo>()
    var timecount = 0
    private var mThreadExecutor: ExecutorService? = Executors.newSingleThreadExecutor()
    var wavingCountText = TextView(this.context)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        var listener: OptionsLayoutListener? = null
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) { //控制图标闪烁
        override fun handleMessage(msg: Message) {
//            when (msg.what) {
//                1 -> {
//                    showWavingCountText()
//                    if (timecount == 0) {
//                        handsUpItem?.setIconResource(R.drawable.agora_handsup_down_img_art)
//                    } else {
//                        handsUpItem?.setIconResource(R.drawable.agora_handsup_up_img_art)
//                    }
//                }
//                2 -> {
//                    handsUpItem?.setIconResource(R.drawable.agora_handsup_down_img_art)
//                    wavingCountText.visibility = GONE
//                }
//            }
        }
    }

//    private fun showWavingCountText() {
//        wavingCountText.visibility = VISIBLE
//        wavingCountText.text = studentWavingList.size.toString()
//        wavingCountText.textSize = 10f
//        wavingCountText.setTextColor(Color.parseColor("#FFFFFF"))
//        wavingCountText.gravity = Gravity.CENTER
//        wavingCountText.setBackgroundResource(R.drawable.agora_handsup_waving_size_text_bg)
//    }

//    private val userHandler = object : UserHandler() {
//        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
//
//        }
//
//        override fun onRemoteUserLeft(user: AgoraEduContextUserInfo,
//                                      operator: AgoraEduContextUserInfo?,
//                                      reason: EduContextUserLeftReason) {
//
//        }
//
//        override fun onUserHandsWave(user: AgoraEduContextUserInfo, duration: Int) {
//            super.onUserHandsWave(user, duration)
//            if (role == AgoraEduContextUserRole.Teacher) {//老师端 当有人举手时，举手控件闪烁
//                eduContext?.userContext()?.getAllUserList()?.let {
//                    // todo detailInfo to usrInfo
//                }
//            }
//        }
//
//        override fun onUserHandsDown(user: AgoraEduContextUserInfo) {
//            super.onUserHandsDown(user)
//            if (role == AgoraEduContextUserRole.Teacher) {//老师端 当有人取消举手时，举手控件停止闪烁
//                eduContext?.userContext()?.getAllUserList()?.let {
//                    // todo detailInfo to usrInfo
//                }
//            }
//        }
//    }

//    private fun wavingBtnUpdated(list: MutableList<AgoraUIUserDetailInfo>) {
//        studentWavingList = list
//        // todo get the users waving
//        studentWavingList = studentWavingList.filter { it.isWaving } as MutableList
//
//
//        mThreadExecutor?.execute {
//            while (studentWavingList.size != 0) {
//                var msg = Message()
//                msg.what = 1
//                timecount++
//                timecount %= 2
//                mHandler.sendMessage(msg)
//                try {
//                    sleep(500)
//                } catch (e: Exception) {
//
//                }
//            }
//            var msg2 = Message()
//            msg2.what = 2
//            mHandler.sendMessage(msg2)
//        }
//    }

    fun init(eduContext: EduContextPool?, parent: RelativeLayout, role: AgoraEduContextUserRole,
             width: Int, right: Int, bottom: Int = 0, mode: OptionLayoutMode? = OptionLayoutMode.Joint,
             container: AbsUIContainer, handsUpPopup: AgoraUIHandsUpToastPopUp?) {
        this.eduContext = eduContext
        this.role = role
        orientation = VERTICAL
        uiDataProvider = UIDataProvider(eduContext)
        val isTeacher = role != AgoraEduContextUserRole.Student
        val roomType = eduContext?.roomContext()?.getRoomInfo()?.roomType

        if (roomType != EduContextRoomType.OneToOne) {
            settingItem = DeviceSettingLayoutPopupItem(parent,
                R.drawable.ic_agora_options_item_icon_setting,
                width, right, eduContext, this)
            addPopupButton(settingItem!!, width, R.drawable.agora_option_icon_setting)
        }

        if (isTeacher) {
            ToolBoxLayoutPopupItem(parent,
                R.drawable.ic_agora_options_item_icon_toolbox,
                width, right, eduContext, this).let {
                addPopupButton(it, width, R.drawable.agora_option_icon_toolbox)
                toolboxItem = it
            }
        }

        if (roomType == EduContextRoomType.SmallClass) {
            rosterItem = RosterLayoutPopupItem(parent,
                R.drawable.ic_agora_options_item_icon_roster,
                width, right, eduContext, role, this, uiDataProvider)
            rosterItem?.let {
                addPopupButton(it, width, R.drawable.agora_option_icon_roster)
            }
        }

        if (roomType == EduContextRoomType.SmallClass) {
            chatItem = ChatLayoutPopupItem(parent, container,
                R.drawable.ic_agora_options_item_icon_chat,
                width, right, eduContext, this)
            chatItem?.let {
                addPopupButton(it, width, R.drawable.agora_option_icon_chat)
            }
        }

        if (roomType == EduContextRoomType.SmallClass ||
            roomType == EduContextRoomType.LectureHall) {
            if (isTeacher) {
                handsUpItem = OptionsLayoutHandsUpItem(parent,
                    R.drawable.ic_agora_options_item_icon_hands_wave_teacher,
                    width, role, eduContext, this, handsUpPopup, mUserList)
                handsUpItem?.let {
                    addHandsUpButton(it, width)
                }

                val params = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                wavingCountText.layoutParams = params
                handsUpItem?.addView(wavingCountText)
            } else {
                // A student can have an item that only supports
                // hands wave count down timer
                val item = OptionsLayoutHandsWaveCountDownItem(parent,
                    R.drawable.ic_agora_options_item_icon_hands_wave_student,
                    width, eduContext, right)
                addHandsUpButton(item, width)
            }
        }

        if (mode == OptionLayoutMode.Joint) {
            // Whiteboard tool item is a part of options layout,
            // its right margin and bottom margin is 0
            addWhiteboardToolButton(parent, width, 0, eduContext)
        }

        val params = RelativeLayout.LayoutParams(width, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = bottom
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)
        params.rightMargin = right
        parent.addView(this, params)

        initLayoutHeight(width, role == AgoraEduContextUserRole.Teacher)
    }

    private fun addPopupButton(item: OptionsLayoutPopupItem, width: Int, res: Int) {
        item.setOnClickListener(this)
        addView(item, width, width)
    }

    private fun addHandsUpButton(item: OptionsLayoutItem, width: Int) {
        item.setOnClickListener(this)
        addView(item, width, width)
    }

    private fun addWhiteboardToolButton(parent: RelativeLayout,
                                        size: Int,
                                        popupMargin: Int,
                                        eduContext: EduContextPool?) {
        OptionsLayoutWhiteboardItem(parent, size, popupMargin, eduContext, this).let {
            boardToolItem = it
            it.setOnClickListener(this)
            addView(it, size, size)
        }
    }

    private fun initLayoutHeight(itemSize: Int, granted: Boolean) {
        if (!granted) {
            val measureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            measure(measureSpec, measureSpec)
            val layoutWidth = this.measuredWidth
            val layoutHeight = this.measuredHeight
            val params = this.layoutParams as MarginLayoutParams
            params.width = layoutWidth
            params.height = layoutHeight - itemSize
            this.layoutParams = params
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            checkPopupState(it)
            checkDialogState(it)
        }
    }

    private fun checkPopupState(view: View) {
        when (view) {
            settingItem -> {
                changePopupItemStateAndCallback(settingItem!!)
            }
            toolboxItem -> {
                changePopupItemStateAndCallback(toolboxItem!!)
            }
            rosterItem -> {
                changePopupItemStateAndCallback(rosterItem!!)
            }
            chatItem -> {
                changePopupItemStateAndCallback(chatItem!!)
            }
            handsUpItem -> {
                changePopupItemStateAndCallback(handsUpItem!!)
            }
        }
    }

    private fun checkDialogState(view: View) {
        when (view) {
            boardToolItem -> {
                dismissCurrentPopup()
                boardToolItem!!.toggleDialog()
            }
        }
    }

    fun dismissCurrentPopup() {
        curPopupItem?.let {
            popUpListener?.onPopupDismiss(it.getType())
            it.isActivated = false
            it.dismissPopup()
            curPopupItem = null
        }
    }

    private fun changePopupItemStateAndCallback(item: OptionsLayoutPopupItem) {
        if (item == curPopupItem) {
            popUpListener?.onPopupDismiss(item.getType())
            curPopupItem?.isActivated = false
            curPopupItem?.dismissPopup()
            curPopupItem = null
        } else if (curPopupItem != null) {
            popUpListener?.onPopupDismiss(curPopupItem!!.getType())
            curPopupItem?.dismissPopup()
            curPopupItem?.isActivated = false
            curPopupItem = null
            popUpListener?.onPopupShow(item.getType())
            curPopupItem = item
            curPopupItem?.showPopup()
            curPopupItem?.isActivated = true
        } else {
            popUpListener?.onPopupShow(item.getType())
            curPopupItem = item
            curPopupItem?.showPopup()
            curPopupItem?.isActivated = true
        }
    }

    fun clearCurPopupItem() {
        curPopupItem?.dismissPopup()
        curPopupItem?.isActivated = false
        curPopupItem = null
    }
}

enum class OptionLayoutMode {
    // Only part of the options are inserted into option layout.
    // How to set the option items is determined by business
    // requirements
    Separate,

    // All options are inserted into option layout
    Joint
}

interface OptionPopupListener {
    fun onPopupShow(item: OptionItemType)

    fun onPopupDismiss(item: OptionItemType)
}

enum class OptionItemType {
    Setting, Toolbox, Roster, Chat, HandsUp, Whiteboard
}

@SuppressLint(
    "ViewConstructor",
    "ClickableViewAccessibility")
abstract class OptionsLayoutItem(context: Context?,
                                 private val type: OptionItemType,
                                 private val itemWidth: Int) : RelativeLayout(context) {
    protected val iconViewSizeRatio = 22 / 46f
    protected val iconButtonSizeRatio = 34 / 46f

    private val tag = "OptionsLayoutItem"
    private val defaultIconColor = Color.parseColor("#7B88A0")
    private val defaultIconBgRes = R.drawable.agora_options_icon_bg_default
    private val iconView: AppCompatImageView = AppCompatImageView(getContext())
    private var iconDrawable: VectorDrawableCompat? = null
    private var iconDrawableColor: Int? = null

    private val middleLayer = RelativeLayout(context)
    private val topLayer = RelativeLayout(context)

    private var defaultItemPressedScalingFactor: Float = 1.1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(itemWidth, itemWidth)
    }

    protected fun initIconView() {
        setBackgroundResource(defaultIconBgRes)
        addView(middleLayer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(topLayer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        val iconSize = (itemWidth * iconViewSizeRatio).toInt()
        val param = LayoutParams(iconSize, iconSize)
        param.addRule(CENTER_IN_PARENT, TRUE)
        topLayer.addView(iconView, param)
        iconView.scaleType = ImageView.ScaleType.FIT_XY
    }

    protected fun initTouchListener() {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.scaleX = defaultItemPressedScalingFactor
                    v.scaleY = defaultItemPressedScalingFactor
                    onItemTouchedDown()
                }
                MotionEvent.ACTION_UP -> {
                    v.scaleX = 1f
                    v.scaleY = 1f
                    onItemTouchedUp()
                }
            }

            false
        }
    }

    fun setVectorDrawableRes(res: Int) {
        VectorDrawableCompat.create(resources, res, null)?.let { drawable ->
            if (drawable != iconDrawable) {
                iconDrawable = drawable
                iconView.setImageDrawable(drawable)
                drawable.setTint(iconDrawableColor ?: defaultIconColor)
            }
        } ?: Runnable {
            AgoraLog.w("$tag->setVectorDrawableRes, invalid vector asset resource id: $res")
        }
    }

    fun setDrawableRes(res: Int) {
        iconView.setImageResource(res)
    }

    fun setDrawable(drawable: Drawable?) {
        iconView.setImageDrawable(drawable)
    }

    fun setVectorDrawableColor(color: Int) {
        if (iconDrawableColor != color) {
            iconDrawableColor = color
            iconDrawable?.let { drawable ->
                drawable.setTint(color)
                iconView.setImageDrawable(drawable)
            }
        }
    }

    fun setVectorDrawableDefaultColor() {
        setVectorDrawableColor(defaultIconColor)
    }

    fun setItemPressedScalingFactor(factor: Float) {
        defaultItemPressedScalingFactor = factor
    }

    fun getType(): OptionItemType {
        return type
    }

    /**
     * The icon view resides on the top layer
     */
    fun getIconView(): AppCompatImageView {
        return iconView
    }

    fun getMiddleLayer(): RelativeLayout {
        return middleLayer
    }

    fun getTopLayer(): RelativeLayout {
        return topLayer
    }

    abstract fun onItemTouchedDown()

    abstract fun onItemTouchedUp()
}

/**
 * @param container the parent container of the popup window.
 * Note: it is not the parent of the popup item, which should
 * be the options layout itself.
 */
@SuppressLint("ViewConstructor")
abstract class OptionsLayoutPopupItem(private val container: RelativeLayout,
                                      type: OptionItemType,
                                      private val size: Int,
                                      iconVectorRes: Int,
                                      private val optionLayout: OptionsLayout)
    : OptionsLayoutItem(container.context, type, size) {

    private var popupView: ViewGroup? = null
    private val highlightColor = Color.parseColor("#0073FF")
    private val animationUtil: PopupAnimationUtil = PopupAnimationUtil()
    private var animationPivot: Pair<Float, Float> = Pair(0f, 0f)

    init {
        setMiddleLayerHighlight()
        setVectorDrawableRes(iconVectorRes)
    }

    private fun setMiddleLayerHighlight() {
        getMiddleLayer().let { layer ->
            val icon = AppCompatImageView(this.context)
            val iconSize = (size * iconButtonSizeRatio).toInt()
            val param = LayoutParams(iconSize, iconSize)
            param.addRule(CENTER_IN_PARENT, TRUE)
            layer.addView(icon, param)
            val drawable = createHighlightDrawable(iconSize)
            icon.setImageDrawable(drawable)
            layer.isVisible = false
        }
    }

    private fun createHighlightDrawable(size: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setSize(size, size)
        drawable.setColor(highlightColor)
        return drawable
    }

    open fun showPopup() {
        dismissPopup()
        setVectorDrawableColor(Color.WHITE)
        getMiddleLayer().isVisible = true

        this.post {
            onCreatePopupView().let {
                popupView = it
                val rect = onPopupRect()
                val param = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT)
                param.addRule(ALIGN_PARENT_LEFT, TRUE)
                param.addRule(ALIGN_PARENT_TOP, TRUE)
                param.topMargin = rect.top
                param.leftMargin = rect.left
                param.width = rect.width()
                param.height = rect.height()
                if (it.parent != null) {
                    it.removeAllViews()
                }
                container.addView(it, param)

                animationPivot = getAnimatePivot(rect)
                animationUtil.runShowAnimation(it, animationPivot.first, animationPivot.second)
            }
        }
    }

    fun dismissPopup() {
        onDismissPopupView()
    }

    abstract fun onCreatePopupView(): ViewGroup

    /**
     * @return a pair of coordinates in the parent container,
     * the first of which is the top margin, and the second
     * is the right margin.
     */
    abstract fun onPopupRect(): Rect

    fun getPopupView(): ViewGroup? {
        return popupView
    }

    open fun onDismissPopupView() {
        setVectorDrawableDefaultColor()
        getMiddleLayer().isVisible = false

        this.post {
            popupView?.let { pop ->
                animationUtil.runDismissAnimation(pop,
                    animationPivot.first, animationPivot.second) {
                        pop.parent?.let { parent ->
                            (parent as? ViewGroup)?.removeView(pop)
                        }
                }
            }
            popupView = null
        }
    }

    /**
     * Must be called after the option layout item is
     * displayed and have a measured width and height
     */
    protected fun getRectTop(windowHeight: Int): Int {
        val layoutPos = intArrayOf(0, 0)
        optionLayout.getLocationOnScreen(layoutPos)
        val itemPos = intArrayOf(0, 0)
        this.getLocationOnScreen(itemPos)
        val containerPos = intArrayOf(0, 0)
        container.getLocationOnScreen(containerPos)

        val layoutBottom = layoutPos[1] + optionLayout.height
        val idealItemBottom = itemPos[1] + windowHeight
        return if (idealItemBottom <= layoutBottom) {
            itemPos[1] - containerPos[1]
        } else {
            layoutBottom - windowHeight - containerPos[1]
        }
    }

    override fun onItemTouchedDown() {
        if (popupView != null) {
            return
        }

        setVectorDrawableColor(Color.WHITE)
        getMiddleLayer().isVisible = true
        invalidate()
    }

    override fun onItemTouchedUp() {
        if (popupView != null) {
            return
        }

        setVectorDrawableDefaultColor()
        getMiddleLayer().isVisible = false
        invalidate()
    }

    open fun getAnimatePivot(rect: Rect): Pair<Float, Float> {
        getPopupView()?.let {
            val pivotTop = optionLayout.top + this.top + this.size / 2.toFloat()
            return Pair(rect.width().toFloat(), pivotTop - rect.top)
        }
        return Pair(0f, 0f)
    }
}

@SuppressLint("ViewConstructor")
class DeviceSettingLayoutPopupItem(private val container: RelativeLayout,
                                   iconVectorRes: Int,
                                   private val itemWidth: Int,
                                   private val rightMargin: Int,
                                   private val eduContext: EduContextPool?,
                                   private val optionLayout: OptionsLayout)
    : OptionsLayoutPopupItem(container, OptionItemType.Setting, itemWidth, iconVectorRes, optionLayout) {

    // The fixed width over height ratio of the popup window
    private val ratio = 202 / 240f

    init {
        initIconView()
        initTouchListener()
    }

    override fun onCreatePopupView(): ViewGroup {
        AgoraUIDeviceSettingPopUp(container.context).let {
            it.setEduContextPool(eduContext)
            it.leaveRoomRunnable = Runnable {
                isActivated = false
                optionLayout.clearCurPopupItem()
                OptionsLayout.listener?.onLeave()
            }
            return it
        }
    }

    override fun onPopupRect(): Rect {
        val containerH = container.height
        val baseHeight = if (AgoraUIConfig.isLargeScreen) {
            AgoraUIConfig.baseUIHeightLargeScreen
        } else {
            AgoraUIConfig.baseUIHeightSmallScreen
        }
        val windowHeight = (240 * containerH / baseHeight).toInt()
        val windowWidth = (windowHeight * ratio).toInt()
        val windowTop = getRectTop(windowHeight)
        initView(windowWidth, windowHeight)
        val windowLeft = container.width - itemWidth - rightMargin - windowWidth
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        return Rect(windowLeft, windowTop, windowRight, windowBottom)
    }

    private fun initView(width: Int, height: Int) {
        (getPopupView() as? AgoraUIDeviceSettingPopUp)?.initView(container, width, height)
    }
}

@SuppressLint("ViewConstructor")
class ToolBoxLayoutPopupItem(private val container: RelativeLayout,
                             iconVectorRes: Int,
                             private val itemWidth: Int,
                             private val rightMargin: Int,
                             private val eduContextPool: EduContextPool?,
                             private val optionLayout: OptionsLayout)
    : OptionsLayoutPopupItem(container, OptionItemType.Toolbox, itemWidth, iconVectorRes, optionLayout) {

    // The fixed width over height ratio of the popup window
    private val ratio = 243f / 131f
    private val shadow = 10

    init {
        initIconView()
        initTouchListener()
    }

    override fun onCreatePopupView(): ViewGroup {
        return AgoraUIToolBoxPopUp(container.context).apply {
            this.eduContext = eduContextPool
        }
    }

    override fun onPopupRect(): Rect {
        val containerH = container.height
        if (AgoraUIConfig.isLargeScreen) {
            val right = itemWidth + rightMargin
            val containerPos = intArrayOf(0, 0)
            container.getLocationOnScreen(containerPos)
            val itemPos = intArrayOf(0, 0)
            this.getLocationOnScreen(itemPos)
            val top = itemPos[1] - containerPos[1]
            val height = calculateTabletPopupHeight(containerH)
            val width = (height * ratio).toInt()
            initView(width, height, shadow)
            return Rect(right - width, top, right, top - height)
        } else {
            val margin = (containerH / 5f).toInt()
            val height = containerH - margin * 2
            val width = (height * ratio).toInt()
            val right = itemWidth + rightMargin
            val left = right - width
            initView(width, height, shadow)
            return Rect(left, margin, right, containerH - margin)
        }
    }

    /**
     * @param containerH the height of popup window's container
     */
    private fun calculateTabletPopupHeight(containerH: Int): Int {
        if (containerH <= 800) {
            return (containerH * 2f / 3).toInt()
        } else if (containerH <= 1080) {
            return (containerH * 3f / 5).toInt()
        } else if (containerH <= 1600) {
            return containerH / 3
        } else {
            return 800
        }
    }

    private fun initView(width: Int, height: Int, shadow: Int) {
        (getPopupView() as? AgoraUIToolBoxPopUp)?.initView(container, width, height, shadow)
    }
}

@SuppressLint("ViewConstructor")
class RosterLayoutPopupItem(private val container: RelativeLayout,
                            iconVectorRes: Int,
                            private val itemWidth: Int,
                            private val rightMargin: Int,
                            private val eduContext: EduContextPool?,
                            private val role: AgoraEduContextUserRole,
                            private val optionLayout: OptionsLayout,
                            private val uiDataProvider: UIDataProvider?)
    : OptionsLayoutPopupItem(container, OptionItemType.Roster, itemWidth, iconVectorRes, optionLayout) {

    init {
        initIconView()
        initTouchListener()
    }

    private var userList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var popup: AgoraUIRosterPopUp? = null

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(mUserList: List<AgoraUIUserDetailInfo>) {
            super.onUserListChanged(mUserList)
            userList = mUserList as MutableList<AgoraUIUserDetailInfo>
            onUserListUpdated(userList)
        }
    }

    init {
        uiDataProvider?.addListener(uiDataProviderListener)
        uiDataProvider?.notifyUserListChanged()
    }

    override fun onCreatePopupView(): ViewGroup {
        val view = AgoraUIRosterPopUp(container.context)
        view.setType(RosterType.SmallClass)
        view.setEduContext(eduContext)
        val rect = onPopupRect()
        view.initView(container, rect.width(), rect.height(), role)
        popup = view
        onUserListUpdated(userList)
        return view
    }

    override fun onPopupRect(): Rect {
        val containerH = container.height
        val baseHeight = if (AgoraUIConfig.isLargeScreen) {
            AgoraUIConfig.baseUIHeightLargeScreen
        } else {
            AgoraUIConfig.baseUIHeightSmallScreen
        }
        val windowHeight = (253 * containerH / baseHeight).toInt()
        val windowWidth = getPopupWidth(windowHeight)
        val windowTop = getRectTop(windowHeight)
        val windowLeft = container.width - itemWidth - rightMargin - windowWidth
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        return Rect(windowLeft, windowTop, windowRight, windowBottom)
    }

    private fun getPopupWidth(height: Int): Int {
        return if (role == AgoraEduContextUserRole.Student) {
            (height * 7f / 5).toInt()
        } else {
            (height * 5f / 3).toInt()
        }
    }

    override fun onDismissPopupView() {
        super.onDismissPopupView()
        popup = null
    }

    fun onUserListUpdated(list: MutableList<AgoraUIUserDetailInfo>) {
        userList = sort(list)
        popup?.onUserListUpdated(userList)
    }
}

fun sort(list: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
    var coHosts = mutableListOf<AgoraUIUserDetailInfo>()
    val users = mutableListOf<AgoraUIUserDetailInfo>()
    list.forEach {
        if (it.isCoHost) {
            coHosts.add(it)
        } else {
            users.add(it)
        }
    }
    coHosts = sort2(coHosts)
    val list1 = sort2(users)
    coHosts.addAll(list1)
    return coHosts
}

fun sort2(list: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
    val numList = mutableListOf<AgoraUIUserDetailInfo>()
    val listIterator = list.iterator()
    while (listIterator.hasNext()) {
        val info = listIterator.next()
        val tmp = info.userName[0]
        if (!TextPinyinUtil.isChinaString(tmp.toString()) && tmp.toInt() in 48..57) {
            numList.add(info)
            listIterator.remove()
        }
    }

    numList.sortWith(object : Comparator<AgoraUIUserDetailInfo> {
        override fun compare(o1: AgoraUIUserDetailInfo?, o2: AgoraUIUserDetailInfo?): Int {
            if (o1 == null) {
                return -1
            }
            if (o2 == null) {
                return 1
            }
            return o1.userName.compareTo(o2.userName)
        }
    })

    list.sortWith(object : Comparator<AgoraUIUserDetailInfo> {
        override fun compare(o1: AgoraUIUserDetailInfo?, o2: AgoraUIUserDetailInfo?): Int {
            if (o1 == null) {
                return -1
            }
            if (o2 == null) {
                return 1
            }
            var ch1 = ""
            if (TextPinyinUtil.isChinaString(o1.userName)) {
                TextPinyinUtil.getPinyin(o1.userName).let {
                    ch1 = it
                }
            } else {
                ch1 = o1.userName
            }
            var ch2 = ""
            if (TextPinyinUtil.isChinaString(o2.userName)) {
                TextPinyinUtil.getPinyin(o2.userName).let {
                    ch2 = it
                }
            } else {
                ch2 = o2.userName
            }
            return ch1.compareTo(ch2)
        }
    })
    list.addAll(numList)
    return list
}

/**
 * Chat widget popups have different show/dismiss process
 * than other popup windows
 */
@SuppressLint("ViewConstructor")
class ChatLayoutPopupItem(private val parent: RelativeLayout,
                          private val container: AbsUIContainer,
                          iconVectorRes: Int,
                          private val itemWidth: Int,
                          private val rightMargin: Int,
                          private val eduContext: EduContextPool?,
                          private val optionLayout: OptionsLayout)
    : OptionsLayoutPopupItem(parent, OptionItemType.Chat, itemWidth, iconVectorRes, optionLayout) {

    private val animateUtil: PopupAnimationUtil = PopupAnimationUtil()
    private var pivot: Pair<Float, Float> = Pair(0f, 0f)

    init {
        initIconView()
        initTouchListener()
    }

    private lateinit var window: ChatPopupWidget
    private var layout: ViewGroup? = null
    private var redDot: AppCompatImageView? = null

    private val widgetMessageObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = Gson().fromJson(msg, AgoraChatInteractionPacket::class.java)
            if (packet.signal == UnreadTips && (packet.body as? Boolean) == true) {
                ContextCompat.getMainExecutor(optionLayout.context).execute {
                    if (getMiddleLayer().isVisible) {
                        redDot?.isVisible = false
                    } else {
                        redDot?.isVisible = packet.body
                    }
                }
            }
        }
    }

    init {
        val config = eduContext?.widgetContext()?.getWidgetConfig(Chat.id)
        config?.apply {
            val chat = eduContext?.widgetContext()?.create(config)
            (chat as? ChatPopupWidget)?.let { it ->
                window = it
                window.chatWidgetListener = object : ChatPopupWidgetListener {
                    override fun onChatPopupWidgetClosed() {
                        optionLayout.clearCurPopupItem()
                        this@ChatLayoutPopupItem.isActivated = false
                    }
                }

                try {
                    layout = onCreatePopupView()
                    var muted = false
                    (config.extraInfo as? Map<String, Any>)?.let { map ->
                        (map["muteChat"] as? Double)?.let { mute ->
                            muted = mute.toInt() == 1
                        }
                    }
                    it.setChatMuted(muted)
                } catch (e: RuntimeException) {
                    AgoraLog.e("Init chat layout popup item fails: ${e.message}")
                }
            }

            if (!::window.isInitialized || layout == null) {
                AgoraLog.e("Init chat layout item fails, do you register a chat widget correctly?")
            }
        }
        addUnreadRedDot()
        eduContext?.widgetContext()?.addWidgetMessageObserver(widgetMessageObserver, Chat.id)
    }

    private fun addUnreadRedDot() {
        val parent = getTopLayer()
        redDot = AppCompatImageView(parent.context)
        redDot?.setImageResource(R.drawable.agora_chat_icon_unread)
        val size = parent.context.resources.getDimensionPixelSize(R.dimen.agora_chat_unread_size)
        val layoutParams = LayoutParams(size, size)
        layoutParams.addRule(ALIGN_PARENT_RIGHT)
        layoutParams.addRule(ALIGN_PARENT_END)
        layoutParams.topMargin = parent.context.resources.getDimensionPixelSize(R.dimen.margin_smaller)
        layoutParams.rightMargin = parent.context.resources.getDimensionPixelSize(R.dimen.margin_smaller)
        layoutParams.marginEnd = parent.context.resources.getDimensionPixelSize(R.dimen.margin_smaller)
        redDot?.layoutParams = layoutParams
        parent.addView(redDot)
        redDot?.visibility = GONE
    }

    @Throws(RuntimeException::class)
    override fun onCreatePopupView(): ViewGroup {
        if (::window.isInitialized) {
            window.init(parent, 0, 0, 0, 0)
            // As for it is a popup window, it must have
            // a shadowed background border effect
            window.showShadow(true)

            window.getLayout()?.let {
                return it
            }
            throw RuntimeException("No layout initialized in chat window: id $window")
        } else {
            throw RuntimeException("No layout initialized in chat window: id $window")
        }
    }

    override fun onPopupRect(): Rect {
        val windowBaseHeight = 287
        val ratio: Float
        val containerBaseHeight: Float
        if (AgoraUIConfig.isLargeScreen) {
            containerBaseHeight = AgoraUIConfig.baseUIHeightLargeScreen
            ratio = 340 / 430f
        } else {
            containerBaseHeight = AgoraUIConfig.baseUIHeightSmallScreen
            ratio = 200 / 287f
        }

        val windowHeight = (windowBaseHeight * parent.height / containerBaseHeight).toInt()
        val windowWidth = (windowHeight * ratio).toInt()

        val windowTop = getRectTop(windowHeight)
        val windowLeft = parent.width - itemWidth - rightMargin - windowWidth
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        return Rect(windowLeft, windowTop, windowRight, windowBottom)
    }

    override fun showPopup() {
        redDot?.visibility = GONE
        layout?.let {
            val rect = onPopupRect()
            window.setRect(rect)
            pivot = getAnimatePivot(rect)
            animateUtil.runShowAnimation(it, pivot.first, pivot.second)
        }
        setVectorDrawableColor(Color.WHITE)
        getMiddleLayer().isVisible = true
    }

    override fun onDismissPopupView() {
        // Because of the possible implementations of chat widget,
        // we do not remove the pop window layout from container,
        // but we set the layout to zero size instead.
        layout?.let { layout ->
            animateUtil.runDismissAnimation(layout, pivot.first, pivot.second) {
                window.setRect(Rect(0, 0, 0, 0))
            }
        }
        setVectorDrawableDefaultColor()
        getMiddleLayer().isVisible = false
    }

    override fun getAnimatePivot(rect: Rect): Pair<Float, Float> {
        layout?.let {
            val pivotTop = optionLayout.top + this.top + this.height / 2.toFloat()
            return Pair(rect.width().toFloat(), pivotTop - rect.top)
        }
        return Pair(0f, 0f)
    }
}

@SuppressLint("ViewConstructor")
class OptionsLayoutHandsUpItem(private val container: RelativeLayout,
                               iconVectorRes: Int,
                               private val itemWidth: Int,
                               val role: AgoraEduContextUserRole,
                               val eduContext: EduContextPool?,
                               optionsLayout: OptionsLayout,
                               private var handsUpPopup: AgoraUIHandsUpToastPopUp?,
                               var mUserList: List<AgoraUIUserDetailInfo>?)
    : OptionsLayoutPopupItem(container, OptionItemType.HandsUp, itemWidth, iconVectorRes, optionsLayout) {

    private lateinit var handsUpWrapper: AgoraUIHandsUpWrapper
    private var userList: MutableList<AgoraUIUserDetailInfo> = mUserList as MutableList<AgoraUIUserDetailInfo>

    private var handsUpListPopUp: AgoraUIHandsUpListPopUp? = null

    init {
        if (this.role == AgoraEduContextUserRole.Student) {
            this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (width > 0 && height > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)

                        var timerW = (width * 2 / 3f).toInt()
                        timerW = (timerW * 23 / 21f).toInt()
                        val timerH = timerW
                        val timerRight = 9 + (width - timerW) / 2

                        handsUpPopup?.let { popup ->
                            popup.setEduContext(eduContext)
                            if (AgoraUIConfig.isLargeScreen) {
                                popup.initView(container, timerRight + timerW, timerW * 4 + 30)
                            } else {
                                popup.initView(container, timerRight + timerW, timerW * 2 + 12)
                            }
                        }

                        handsUpWrapper = if (AgoraUIConfig.isLargeScreen) {
                            AgoraUIHandsUpWrapper(container, eduContext,
                                this@OptionsLayoutHandsUpItem, timerW, timerH, timerRight - 2, timerW * 4 + 27, handsUpPopup)
                        } else {
                            AgoraUIHandsUpWrapper(container, eduContext,
                                this@OptionsLayoutHandsUpItem, timerW, timerH, timerRight + 2, timerW * 2 + 9, handsUpPopup)
                        }
                    }
                }

            })
        } else {//老师
            onUserListUpdated(userList)//更新用户列表
        }
    }

    override fun onCreatePopupView(): ViewGroup {
        val view = AgoraUIHandsUpListPopUp(container.context, userList)
        view.setEduContext(eduContext)
        handsUpListPopUp = view
        return view
    }


    override fun onPopupRect(): Rect {
        val containerH = container.height

        // For phones, setting window stays at the
        // vertical center of the container, with
        // a margin to the top and bottom
        val margin = (containerH / 16f).toInt()
        val height = containerH - margin * 2
        val width = (height * 1).toInt()
        val right = itemWidth + 1
        val left = right - width
        initView(width, height, 11)
        return Rect(left, margin, right, containerH - margin)

    }

    override fun onDismissPopupView() {
        super.onDismissPopupView()
        handsUpListPopUp = null
    }

    private fun initView(width: Int, height: Int, shadow: Int) {
        val studentWavingList = mutableListOf<AgoraUIUserDetailInfo>()
        userList.forEach { item ->
//            if (item.isWaving) { //筛选出正在举手的用户 //todo
            if (false) { //筛选出正在举手的用户
                studentWavingList.add(item)
            }
        }

        if (this.role == AgoraEduContextUserRole.Teacher) {//角色是老师
            if (studentWavingList.size != 0) {
                (getPopupView() as? AgoraUIHandsUpListPopUp)?.initView(container, role)
            }
        }
    }

    fun onUserListUpdated(list: MutableList<AgoraUIUserDetailInfo>) {
        userList = list
        handsUpListPopUp?.onUserListUpdated(userList)
    }
}

@SuppressLint("ViewConstructor")
class OptionsLayoutHandsWaveCountDownItem(private val container: RelativeLayout,
                                          private val iconVectorRes: Int,
                                          private val size: Int,
                                          private val eduContext: EduContextPool?,
                                          private val rightMargin: Int)
    : OptionsLayoutItem(container.context, OptionItemType.HandsUp, size),
    AgoraUIHandsWaveCountDownListener {

    private val tag = "OptionsLayoutHandsWaveCountDownItem"
    private val defaultBackground = R.drawable.agora_options_icon_bg
    private val countdownText = AppCompatTextView(this.context)
    private val countdownTextRatio = 13 / 46f
    private val highlightColor = Color.parseColor("#0073FF")
    private val countdownMax = 3
    private val waveHandsTimeoutInSeconds = 3

    init {
        initIconView()
        initTouchListener()
        initCountDown()
        setBackgroundResource(defaultBackground)
        setVectorDrawableRes(iconVectorRes)
        initHandsWaveMiddleLayer()
    }

    override fun onItemTouchedDown() {

    }

    override fun onItemTouchedUp() {

    }

    private fun initCountDown() {
        AgoraUIHandsWaveCountDownWrapper(this,
            container, rightMargin, eduContext, this)
    }

    private fun createHighlightDrawable(size: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setSize(size, size)
        drawable.setColor(highlightColor)
        return drawable
    }

    private fun initHandsWaveMiddleLayer() {
        val highlightSize = (size * iconButtonSizeRatio).toInt()
        val iconView = AppCompatImageView(this.context)
        var param = LayoutParams(highlightSize, highlightSize)
        param.addRule(CENTER_IN_PARENT, TRUE)
        getMiddleLayer().addView(iconView, param)
        val drawable = createHighlightDrawable(highlightSize)
        iconView.setImageDrawable(drawable)

        val textViewSize = (size * iconViewSizeRatio).toInt()
        param = LayoutParams(textViewSize, textViewSize)
        param.addRule(CENTER_IN_PARENT, TRUE)
        countdownText.setTextColor(Color.WHITE)
        countdownText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * countdownTextRatio)
        countdownText.textAlignment = TEXT_ALIGNMENT_GRAVITY
        countdownText.gravity = Gravity.CENTER
        getMiddleLayer().addView(countdownText, param)
        getMiddleLayer().isVisible = false
    }

    private fun showIcon(showed: Boolean) {
        getIconView().isVisible = showed
    }

    override fun onCountDownStart(timeoutInSeconds: Int) {
        AgoraLog.d("$tag->onCountDownStart, timeout $timeoutInSeconds seconds")
        post {
            showIcon(false)
            getMiddleLayer().isVisible = true
            if (timeoutInSeconds in 1..countdownMax) {
                countdownText.text = timeoutInSeconds.toString()
            } else {
                setVectorDrawableRes(iconVectorRes)
            }
        }

        eduContext?.userContext()?.let { context ->
            val localName = context.getLocalUserInfo().userName
            val payload = mutableMapOf<String, Any>()
            payload["userName"] = localName
            context.handsWave(waveHandsTimeoutInSeconds, payload)
        }
    }

    override fun onCountDownTick(secondsToFinish: Int) {
        AgoraLog.d("$tag->onCountDownTick, $secondsToFinish seconds remaining")
        post {
            if (secondsToFinish in 1..countdownMax) {
                countdownText.text = secondsToFinish.toString()
            } else {
                setVectorDrawableRes(iconVectorRes)
            }
        }
    }

    override fun onCountDownEnd() {
        AgoraLog.d("$tag->onCountDownEnd")
        post {
            setBackgroundResource(defaultBackground)
            showIcon(true)
            getMiddleLayer().isVisible = false
        }
    }
}

@SuppressLint("ViewConstructor")
class OptionsLayoutWhiteboardItem(private val container: RelativeLayout,
                                  private val size: Int,
                                  private val rightMargin: Int,
                                  val eduContext: EduContextPool?,
                                  val optionsLayout: OptionsLayout)
    : OptionsLayoutItem(container.context, OptionItemType.Whiteboard, size) {
    private val scaleFactor = 1.1f

    private var dialog: AgoraUIWhiteboardOptionDialog? = null
    private var config = AgoraUIDrawingConfig()

    private val applianceIconResource = mutableMapOf<AgoraUIApplianceType, Int>()

    private val defaultWhiteTintColor = Color.parseColor("#E1E1EA")

    private var curGranted = false

    private val whiteboardDialogListener = object : AgoraUIWhiteboardOptionListener {
        override fun onApplianceSelected(type: AgoraUIApplianceType) {
            setConfigIcon(type)
            setConfigIconColor()
            val applianceType = toWhiteboardApplianceType(type)
            updateBoardMemberState(AgoraBoardDrawingMemberState(activeApplianceType = applianceType))
        }

        override fun onColorSelected(color: Int) {
            config.color = color
            setConfigIcon(config.activeAppliance)
            setConfigIconColor()
            updateBoardMemberState(AgoraBoardDrawingMemberState(strokeColor = color))
        }

        override fun onTextSizeSelected(size: Int) {
            updateBoardMemberState(AgoraBoardDrawingMemberState(textSize = size))
        }

        override fun onThicknessSelected(thick: Int) {
            updateBoardMemberState(AgoraBoardDrawingMemberState(strokeWidth = thick))
        }
    }

    private val whiteBoardObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = Gson().fromJson(msg, AgoraBoardInteractionPacket::class.java)
            when (packet.signal) {
                AgoraBoardInteractionSignal.BoardGrantDataChanged -> {
                    eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                         if (localUser.role == AgoraEduContextUserRole.Student) {
                             val granted = (packet.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                             if (granted != curGranted) {
                                 this@OptionsLayoutWhiteboardItem.post {
                                     runScrollAnimation(granted, optionsLayout.height, size)
                                 }
                             }
                             curGranted = granted
                         }
                    }
                }
                MemberStateChanged -> {
                    val configArgs: AgoraUIDrawingConfig? = Gson().fromJson(packet.body.toString(),
                        AgoraUIDrawingConfig::class.java)
                    configArgs?.let {
                        initConfig(it)
                    }
                }
            }
        }

        private fun runScrollAnimation(granted: Boolean, heightBefore: Int, diffY: Int) {
            optionsLayout.animate().cancel()
            optionsLayout.animate().setDuration(200).setUpdateListener {
                val fraction = it.animatedFraction
                val alpha: Float
                val scale: Float
                val height: Int

                val changedHeight = fraction * diffY
                if (granted) {
                    height = (heightBefore + changedHeight).toInt()
                    if (changedHeight >= this@OptionsLayoutWhiteboardItem.size / 2) {
                        alpha = (fraction - 0.5f) / 0.5f
                        scale = (fraction - 0.5f) / 0.5f
                    } else {
                        alpha = 0f
                        scale = 0f
                    }
                } else {
                    height = (heightBefore - changedHeight).toInt()
                    if (changedHeight <= this@OptionsLayoutWhiteboardItem.size / 2) {
                        alpha = (1 - fraction * 2)
                        scale = (1 - fraction * 2)
                    } else {
                        alpha = 0f
                        scale = 0f
                    }
                }

                this@OptionsLayoutWhiteboardItem.alpha = alpha
                this@OptionsLayoutWhiteboardItem.scaleX = scale
                this@OptionsLayoutWhiteboardItem.scaleY = scale

                val params = optionsLayout.layoutParams as MarginLayoutParams
                params.height = height
                optionsLayout.layoutParams = params
            }.start()
        }
    }

    init {
        initIconView()
        initTouchListener()
    }

    private fun updateBoardMemberState(state: AgoraBoardDrawingMemberState) {
        val packet = AgoraBoardInteractionPacket(MemberStateChanged, state)
        eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), WhiteBoard.id)
    }

    private fun toWhiteboardApplianceType(type: AgoraUIApplianceType): WhiteboardApplianceType {
        return when (type) {
            AgoraUIApplianceType.Select -> WhiteboardApplianceType.Select
            AgoraUIApplianceType.Text -> WhiteboardApplianceType.Text
            AgoraUIApplianceType.Pen -> WhiteboardApplianceType.Pen
            AgoraUIApplianceType.Line -> WhiteboardApplianceType.Line
            AgoraUIApplianceType.Rect -> WhiteboardApplianceType.Rect
            AgoraUIApplianceType.Circle -> WhiteboardApplianceType.Circle
            AgoraUIApplianceType.Eraser -> WhiteboardApplianceType.Eraser
            AgoraUIApplianceType.Clicker -> WhiteboardApplianceType.Clicker
            AgoraUIApplianceType.Laser -> WhiteboardApplianceType.Laser
        }
    }

    init {
        replaceDefaultBackground()
        initApplianceIconResource()
        setConfigIcon(config.activeAppliance)
        addTouchScaling()
        AgoraUIWhiteboardOptionDialog.listener = whiteboardDialogListener
        eduContext?.widgetContext()?.addWidgetMessageObserver(whiteBoardObserver, WhiteBoard.id)
    }

    // This item does not need the pressed background
    // color as the default item does
    private fun replaceDefaultBackground() {
        setBackgroundResource(R.drawable.agora_options_icon_bg_default)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTouchScaling() {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.scaleX = scaleFactor
                    v.scaleY = scaleFactor
                }
                MotionEvent.ACTION_UP -> {
                    v.scaleX = 1f
                    v.scaleY = 1f
                }
            }

            false
        }
    }

    /**
     * Option layout needs its own drawing config initial values
     */
    private fun initConfig(config: AgoraUIDrawingConfig) {
        this.config = config
    }

    private fun setConfigIcon(activeAppliance: AgoraUIApplianceType) {
        applianceIconResource[activeAppliance]?.let { setVectorDrawableRes(it) }
    }

    private fun needChangeColor(appliance: AgoraUIApplianceType): Boolean {
        return when (appliance) {
            AgoraUIApplianceType.Text,
            AgoraUIApplianceType.Pen,
            AgoraUIApplianceType.Line,
            AgoraUIApplianceType.Rect,
            AgoraUIApplianceType.Circle -> true
            else -> false
        }
    }

    private fun setConfigIconColor() {
        applianceIconResource[config.activeAppliance]?.let {
            if (needChangeColor(config.activeAppliance)) {
                setVectorDrawableColor(
                    if (config.color == Color.WHITE) {
                        defaultWhiteTintColor
                    } else {
                        config.color
                    })
            } else {
                setVectorDrawableDefaultColor()
            }
        }
    }

    private fun initApplianceIconResource() {
        applianceIconResource[AgoraUIApplianceType.Clicker] =
            R.drawable.ic_agora_options_item_icon_move
        applianceIconResource[AgoraUIApplianceType.Select] =
            R.drawable.ic_agora_options_item_icon_selection
        applianceIconResource[AgoraUIApplianceType.Text] =
            R.drawable.ic_agora_options_item_icon_text
        applianceIconResource[AgoraUIApplianceType.Eraser] =
            R.drawable.ic_agora_options_item_icon_eraser
        applianceIconResource[AgoraUIApplianceType.Laser] =
            R.drawable.ic_agora_options_item_icon_laser
        applianceIconResource[AgoraUIApplianceType.Pen] =
            R.drawable.ic_agora_options_item_icon_pen
        applianceIconResource[AgoraUIApplianceType.Line] =
            R.drawable.ic_agora_options_item_icon_line
        applianceIconResource[AgoraUIApplianceType.Rect] =
            R.drawable.ic_agora_options_item_icon_rect
        applianceIconResource[AgoraUIApplianceType.Circle] =
            R.drawable.ic_agora_options_item_icon_circle
    }

    fun toggleDialog() {
        if (dialog?.isShowing == true) {
            dismiss()
        } else {
            showDialog()
        }
    }

    fun showDialog() {
        if (dialog?.isShowing == true) {
            return
        }

        dialog = AgoraUIWhiteboardOptionDialog(container.context, config)
        dialog?.show(container, this, rightMargin)
    }

    fun dismiss() {
        if (dialog != null && dialog?.isShowing == true) {
            dialog?.dismiss()
            dialog = null
        }
    }

    override fun onItemTouchedDown() {

    }

    override fun onItemTouchedUp() {

    }
}

interface OptionsLayoutListener {
    fun onLeave()

    fun onKickout(userId: String, userName: String)
}