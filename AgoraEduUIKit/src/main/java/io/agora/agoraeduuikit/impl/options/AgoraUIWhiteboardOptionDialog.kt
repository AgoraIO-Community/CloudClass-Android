package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.tool.AgoraUIApplianceType
import io.agora.agoraeduuikit.impl.tool.ColorOptions
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig

class AgoraUIWhiteboardOptionDialog(context: Context,
                                    private val config: AgoraUIDrawingConfig)
    : Dialog(context, R.style.agora_dialog) {

    companion object {
        var listener: AgoraUIWhiteboardOptionListener? = null

        fun hasSubOptions(type: AgoraUIApplianceType): Boolean {
            return type == AgoraUIApplianceType.Pen ||
                    type == AgoraUIApplianceType.Rect ||
                    type == AgoraUIApplianceType.Circle ||
                    type == AgoraUIApplianceType.Line ||
                    type == AgoraUIApplianceType.Text
        }
    }

    internal val applianceSpanCount = 5
    internal val colorSpanCount = 6
    internal val textSizeSpanCount = 4
    internal val thicknessSpanCount = 5

    internal var marginVertical = 0
    internal var marginHorizontal = 0

    private lateinit var divider1: View
    private lateinit var divider2: View
    private lateinit var subItemLayout: RelativeLayout
    private lateinit var colorPlateRecycler: RecyclerView

    private var applianceAdapter: ApplianceItemAdapter? = null

    internal var textAdapter: TextSizeItemAdapter? = null
    internal var thickAdapter: ThicknessItemAdapter? = null

    private var anchor: View? = null
    private var showMargin: Int = 0

    internal val calculator = DialogSizeCalculator()

    /**
     * Show this dialog based on the referencing content layout
     * and the anchor view
     * @param container the actually content container which helps
     * to determine the size of dialog
     * @param anchor the referencing view that helps to
     * determine the position of dialog
     */
    fun show(container: ViewGroup, anchor: View, margin: Int) {
        this.anchor = anchor
        this.showMargin = margin

        setCancelable(true)
        setCanceledOnTouchOutside(true)
        calculator.set(container)
        initView()
        adjustPosition(anchor, margin)
        super.show()
    }

    override fun dismiss() {
        this.anchor = null
        super.dismiss()
    }

    private fun initView() {
        setContentView(R.layout.agora_option_whiteboard_dialog_layout)
        subItemLayout = findViewById(R.id.agora_option_whiteboard_sub_item_layout)
        colorPlateRecycler = findViewById(R.id.agora_option_whiteboard_color_plate_recycler)

        initDividers()
        initColorPlateRecycler()
        initSubRecycler()
        initApplianceRecycler()
        showOrHideSubOptionLayout(hasSubOptions(config.activeAppliance))
    }

    private fun initApplianceRecycler() {
        findViewById<RecyclerView>(R.id.agora_option_whiteboard_appliance_item_recycler)?.let { recycler ->
            recycler.layoutManager = GridLayoutManager(context, applianceSpanCount)
            ApplianceItemAdapter.marginHorizontal = calculator.applianceMarginHorizontal
            ApplianceItemAdapter.marginVertical = calculator.applianceMarginVertical
            ApplianceItemAdapter.itemSize = calculator.applianceItemSize
            ApplianceItemAdapter.itemSpacingHorizontal = calculator.applianceItemSpacingHorizontal
            ApplianceItemAdapter.itemSpacingVertical = calculator.applianceItemSpacingVertical

            applianceAdapter = ApplianceItemAdapter(recycler.context, config, this,
                object : ApplianceItemClickListener {
                    override fun onApplianceClicked(type: AgoraUIApplianceType) {
                        initSubRecycler()
                    }
                })

            recycler.adapter = applianceAdapter
            (recycler.layoutParams as? ViewGroup.MarginLayoutParams)?.let { param ->
                param.topMargin = ApplianceItemAdapter.marginVertical
                param.bottomMargin = ApplianceItemAdapter.marginVertical
                param.leftMargin = ApplianceItemAdapter.marginHorizontal
                param.rightMargin = ApplianceItemAdapter.marginHorizontal
                param.width = ViewGroup.MarginLayoutParams.MATCH_PARENT
                param.height = calculator.applianceItemSize * 2 + calculator.applianceItemSpacingVertical
                recycler.layoutParams = param
            }

            for (i in 0 until recycler.itemDecorationCount) {
                recycler.removeItemDecorationAt(i)
            }

            recycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View,
                    parent: RecyclerView, state: RecyclerView.State) {
                    outRect.bottom = ApplianceItemAdapter.itemSpacingVertical

                    val position = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    val residual = count % applianceSpanCount
                    val lastRowCount = if (residual == 0) applianceSpanCount else residual
                    if (position in count - lastRowCount until count) {
                        outRect.bottom = 0
                    }

                    outRect.left = ((position % applianceSpanCount) *
                        ApplianceItemAdapter.itemSpacingHorizontal
                            / (applianceSpanCount.toFloat())).toInt()
                }
            })

            applianceAdapter?.layoutChangedListener = object : WindowContentLayoutChangedListener {
                override fun onWindowContentLayoutChanged(type: AgoraUIApplianceType, hasSubContent: Boolean) {
                    showOrHideSubOptionLayout(hasSubContent)
                    anchor?.let { adjustPosition(it, showMargin) }
                }
            }
        }
    }

    private fun initColorPlateRecycler() {
        colorPlateRecycler.let { recycler ->
            recycler.visibility = View.VISIBLE
            recycler.layoutManager = AutoMeasureGridLayoutManager(context, colorSpanCount)

            ColorPlateItemAdapter.marginHorizontal = calculator.colorListPaddingHorizontal
            ColorPlateItemAdapter.marginVertical = calculator.colorListPaddingVertical
            ColorPlateItemAdapter.iconSize = calculator.colorItemSize
            ColorPlateItemAdapter.itemSpacingHorizontal = calculator.colorItemSpacingHorizontal
            ColorPlateItemAdapter.itemSpacingVertical = calculator.colorItemSpacingVertical

            recycler.adapter = ColorPlateItemAdapter(context, config, this)
            (recycler.layoutParams as? ViewGroup.MarginLayoutParams)?.let { param ->
                param.topMargin = ColorPlateItemAdapter.marginVertical
                param.bottomMargin = ColorPlateItemAdapter.marginVertical
                param.leftMargin = ColorPlateItemAdapter.marginHorizontal
                param.rightMargin = ColorPlateItemAdapter.marginHorizontal
                param.width = ViewGroup.MarginLayoutParams.MATCH_PARENT
                param.height = ColorPlateItemAdapter.iconSize * 2 +
                    ColorPlateItemAdapter.itemSpacingVertical
                recycler.layoutParams = param
            }

            for (i in 0 until recycler.itemDecorationCount) {
                recycler.removeItemDecorationAt(i)
            }

            recycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View,
                                            parent: RecyclerView, state: RecyclerView.State) {
                    outRect.bottom = ColorPlateItemAdapter.itemSpacingVertical

                    val position = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    val residual = count % colorSpanCount
                    val lastRowCount = if (residual == 0) colorSpanCount else residual
                    if (position in count - 1 - lastRowCount until count) {
                        outRect.bottom = 0
                    }

                    outRect.left = ((position % colorSpanCount) *
                        ColorPlateItemAdapter.itemSpacingHorizontal /
                            (colorSpanCount.toFloat())).toInt()
                }
            })
        }
    }

    private fun hideColorRecycler() {
        colorPlateRecycler.visibility = View.GONE
    }

    private fun initSubRecycler() {
        subItemLayout.visibility = View.VISIBLE
        if (config.activeAppliance == AgoraUIApplianceType.Text) {
            initTextSizeRecycler(subItemLayout)
        } else {
            initThicknessRecycler(subItemLayout)
        }
    }

    private fun hideSubRecycler() {
        subItemLayout.visibility = View.GONE
    }

    private fun showOrHideSubOptionLayout(hasContent: Boolean) {
        if (hasContent) {
            initSubRecycler()
            initColorPlateRecycler()
            showDividers()
        } else {
            hideDividers()
            hideSubRecycler()
            hideColorRecycler()
        }
    }

    private fun initTextSizeRecycler(parent: RelativeLayout) {
        parent.removeAllViews()
        textAdapter = null
        thickAdapter = null

        TextSizeItemAdapter.marginHorizontal = calculator.textSizeListMargin
        TextSizeItemAdapter.itemSpacing = calculator.textSizeItemSpacing
        TextSizeItemAdapter.itemSize = calculator.textItemSize

        val recyclerView = RecyclerView(parent.context)
        val param = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
        param.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
        param.leftMargin = TextSizeItemAdapter.marginHorizontal
        param.rightMargin = TextSizeItemAdapter.marginHorizontal
        parent.addView(recyclerView, param)

        recyclerView.layoutManager = AutoMeasureGridLayoutManager(context, textSizeSpanCount)
        textAdapter = TextSizeItemAdapter(config, this)
        recyclerView.adapter = textAdapter

        for (i in 0 until recyclerView.itemDecorationCount) {
            recyclerView.removeItemDecorationAt(i)
        }

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View,
                parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                outRect.left = ((position % textSizeSpanCount) *
                    TextSizeItemAdapter.itemSpacing / (textSizeSpanCount.toFloat())).toInt()
            }
        })
    }

    private fun initThicknessRecycler(parent: RelativeLayout) {
        parent.removeAllViews()
        textAdapter = null
        thickAdapter = null

        ThicknessItemAdapter.itemSpacing = calculator.thicknessItemSpacing
        ThicknessItemAdapter.marginHorizontal = calculator.thicknessListMargin
        ThicknessItemAdapter.iconSize = calculator.thicknessItemSize

        val recyclerView = RecyclerView(parent.context)
        val param = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
        param.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
        param.leftMargin = ThicknessItemAdapter.marginHorizontal
        param.rightMargin = ThicknessItemAdapter.marginHorizontal
        parent.addView(recyclerView, param)

        recyclerView.layoutManager = AutoMeasureGridLayoutManager(context, thicknessSpanCount)
        thickAdapter = ThicknessItemAdapter(config, this)
        recyclerView.adapter = thickAdapter
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View,
                                        parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                outRect.left = (ThicknessItemAdapter.itemSpacing *
                        position.toFloat() / thicknessSpanCount).toInt()
            }
        })
    }

    private fun initDividers() {
        val margin = (calculator.dialogWidth * 15f / 280).toInt()
        divider1 = findViewById(R.id.agora_option_whiteboard_divider1)
        (divider1.layoutParams as ViewGroup.MarginLayoutParams).let {
            it.leftMargin = margin
            it.rightMargin = margin
            divider1.layoutParams = it
        }

        divider2 = findViewById(R.id.agora_option_whiteboard_divider2)
        (divider2.layoutParams as ViewGroup.MarginLayoutParams).let {
            it.leftMargin = margin
            it.rightMargin = margin
            divider2.layoutParams = it
        }
    }

    private fun showDividers() {
        divider1.visibility = View.VISIBLE
        divider2.visibility = View.VISIBLE
    }

    private fun hideDividers() {
        divider1.visibility = View.GONE
        divider2.visibility = View.GONE
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    /**
     * Adjust current window size to current active appliance type
     */
    private fun adjustPosition(anchor: View, margin: Int) {
        if (hasSubOptions(config.activeAppliance)) {
            adjustPosition(anchor, calculator.dialogWidth,
                calculator.dialogHeight, margin)
        } else {
            adjustPosition(anchor, calculator.dialogWidth,
                calculator.dialogNoSubViewHeight, margin)
        }
    }

    private fun adjustPosition(anchor: View, width: Int, height: Int, margin: Int) {
        val window = window
        val params = window!!.attributes
        hideStatusBar(window)

        findViewById<RelativeLayout>(R.id.agora_option_whiteboard_dialog_border_layout)?.let {
            val p = it.layoutParams as ViewGroup.MarginLayoutParams
            p.width = width
            p.height = height
            it.layoutParams = p
        }

        findViewById<RelativeLayout>(R.id.agora_option_whiteboard_dialog_shadow_bg)?.let {
            it.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            params.width = it.measuredWidth
            params.height = it.measuredHeight
        }

        params.gravity = Gravity.BOTTOM or Gravity.END

        val metric = DisplayMetrics()
        window.windowManager.defaultDisplay.getRealMetrics(metric)
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)

        params.x = metric.widthPixels - anchorLoc[0] + margin
        params.y = metric.heightPixels - (anchorLoc[1] + anchor.height)
        window.attributes = params
    }
}

class ApplianceItemAdapter(context: Context,
                           private val config: AgoraUIDrawingConfig,
                           private val dialog: AgoraUIWhiteboardOptionDialog,
                           private val itemClickListener: ApplianceItemClickListener? = null)
    : RecyclerView.Adapter<ApplianceItemViewHolder>() {

    private val iconSizeRatio = 22 / 30f
    private val iconColor = Color.parseColor("#7B88A0")
    private val iconColorSelected = Color.WHITE

    companion object {
        var marginVertical: Int = 0
        var marginHorizontal: Int = 0
        var itemSize: Int = 0
        var itemSpacingHorizontal: Int = 0
        var itemSpacingVertical: Int = 0
    }

    private val iconVectorRes = arrayListOf(
        R.drawable.ic_agora_options_item_icon_move,
        R.drawable.ic_agora_options_item_icon_selection,
        R.drawable.ic_agora_options_item_icon_text,
        R.drawable.ic_agora_options_item_icon_eraser,
//        R.drawable.ic_agora_options_item_icon_laser,
        R.drawable.ic_agora_options_item_icon_pen,
        R.drawable.ic_agora_options_item_icon_line,
        R.drawable.ic_agora_options_item_icon_rect,
        R.drawable.ic_agora_options_item_icon_circle)

    private val iconVectorDrawableList = mutableListOf<VectorDrawableCompat?>()

    private val appliances = arrayListOf(
        AgoraUIApplianceType.Clicker,
        AgoraUIApplianceType.Select,
        AgoraUIApplianceType.Text,
        AgoraUIApplianceType.Eraser,
//        AgoraUIApplianceType.Laser,
        AgoraUIApplianceType.Pen,
        AgoraUIApplianceType.Line,
        AgoraUIApplianceType.Rect,
        AgoraUIApplianceType.Circle)

    private var lastSelected = -1

    internal var layoutChangedListener: WindowContentLayoutChangedListener? = null

    init {
        iconVectorRes.forEach { res ->
            iconVectorDrawableList.add(VectorDrawableCompat
                .create(context.resources, res, null))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceItemViewHolder {
        return ApplianceItemViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.agora_tool_popup_color_item_layout, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ApplianceItemViewHolder, position: Int) {
        holder.itemView.layoutParams?.let {
            it.width = itemSize
            it.height = itemSize
            holder.itemView.layoutParams = it
        }

        (holder.icon.layoutParams as? RelativeLayout.LayoutParams)?.let {
            it.width = (iconSizeRatio * itemSize).toInt()
            it.height = it.width
            it.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = it
        }

        holder.itemView.setOnClickListener {
            val index = holder.absoluteAdapterPosition
            if (index != lastSelected) {
                val lastAppliance = config.activeAppliance
                config.activeAppliance = appliances[index]
                lastSelected = index
                notifyDataSetChanged()
                AgoraUIWhiteboardOptionDialog.listener?.onApplianceSelected(config.activeAppliance)
                itemClickListener?.onApplianceClicked(config.activeAppliance)
                checkLayoutChanged(lastAppliance, config.activeAppliance)

                if (shouldDismissDialog()) {
                    dialog.dismiss()
                }
            }
        }

        val index = holder.absoluteAdapterPosition
        val drawable = iconVectorDrawableList[index]
        if (config.activeAppliance == appliances[index]) {
            drawable?.setTint(iconColorSelected)
            holder.itemView.setBackgroundResource(
                R.drawable.ic_agora_options_whiteboard_appliance_item_bg)
        } else {
            drawable?.setTint(iconColor)
            holder.itemView.background = null
        }
        holder.icon.setImageDrawable(drawable)
    }

    private fun shouldDismissDialog(): Boolean {
        return config.activeAppliance == AgoraUIApplianceType.Clicker ||
                config.activeAppliance == AgoraUIApplianceType.Select ||
                config.activeAppliance == AgoraUIApplianceType.Eraser ||
                config.activeAppliance == AgoraUIApplianceType.Laser
    }

    private fun checkLayoutChanged(before: AgoraUIApplianceType,
                                   after: AgoraUIApplianceType) {

        val hasSubContentBefore = AgoraUIWhiteboardOptionDialog.hasSubOptions(before)
        val hasSubContentAfter = AgoraUIWhiteboardOptionDialog.hasSubOptions(after)
        if (hasSubContentBefore != hasSubContentAfter) {
            layoutChangedListener?.onWindowContentLayoutChanged(after, hasSubContentAfter)
        }
    }

    override fun getItemCount(): Int {
        return iconVectorRes.size
    }
}

class ApplianceItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_color_item_icon)
    init {
        icon.scaleType = ImageView.ScaleType.FIT_XY
    }
}

/**
 * Used to indicate the appliance type has changed to
 * outside world.
 */
interface ApplianceItemClickListener {
    fun onApplianceClicked(type: AgoraUIApplianceType)
}

/**
 * Used to indicate that the size of dialog has changed
 * due to content change, used inside the dialog
 */
internal interface WindowContentLayoutChangedListener {
    fun onWindowContentLayoutChanged(type: AgoraUIApplianceType, hasSubContent: Boolean)
}

class TextSizeItemAdapter(private val config: AgoraUIDrawingConfig,
                          private val dialog: AgoraUIWhiteboardOptionDialog) : RecyclerView.Adapter<TextSizeViewHolder>() {

    companion object {
        var marginHorizontal: Int = 0
        var itemSpacing: Int = 0
        var itemSize: Int = 0
    }

    private val textSizes = dialog.context.resources.getIntArray(R.array.agora_board_font_sizes).toList()
    private val iconSizeWeight = arrayListOf(12, 14, 16, 18)
    private val iconSizeBase = 38f
    private var lastSelected = -1

    private val iconColorStrings = dialog.context.resources.getStringArray(R.array.agora_tool_color_plate)

    private val defaultColorUnSelected = Color.parseColor("#7B88A0")
    private val colorSelectedForWhite = Color.parseColor("#E1E1EA")

    private val drawableList = mutableListOf<VectorDrawableCompat?>()
    private val drawableForWhite = VectorDrawableCompat.create(
        dialog.context.resources, R.drawable.ic_agora_options_whiteboard_text_size_item_white, null)

    init {
        for (i in iconColorStrings.indices) {
            drawableList.add(VectorDrawableCompat.create(dialog.context.resources,
                R.drawable.ic_agora_options_whiteboard_text_size_item, null))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextSizeViewHolder {
        return TextSizeViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.agora_tool_popup_color_item_layout, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: TextSizeViewHolder, position: Int) {
        val index = holder.absoluteAdapterPosition
        holder.itemView.layoutParams?.let {
            it.width = itemSize
            it.height = itemSize
            holder.itemView.layoutParams = it
        }

        (holder.icon.layoutParams as? RelativeLayout.LayoutParams)?.let {
            it.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            it.width = (itemSize * iconSizeWeight[index] / iconSizeBase).toInt()
            it.height = it.width
            holder.icon.layoutParams = it
        }

        holder.itemView.setOnClickListener {
            val idx = holder.absoluteAdapterPosition
            if (idx != lastSelected) {
                lastSelected = idx
                config.fontSize = textSizes[idx]
                notifyDataSetChanged()
                AgoraUIWhiteboardOptionDialog.listener?.onTextSizeSelected(config.fontSize) }
        }

        val drawable = if (config.fontSize == textSizes[index]
            && config.color == Color.WHITE) {
            drawableForWhite
        } else {
            drawableList[index]
        }

        val color = if (config.fontSize == textSizes[index]) {
            if (config.color == Color.WHITE) {
                colorSelectedForWhite
            } else {
                config.color
            }
        } else {
            defaultColorUnSelected
        }

        drawable?.setTint(color)
        holder.icon.setImageDrawable(drawable)
    }

    override fun getItemCount(): Int {
       return textSizes.size
    }
}

class TextSizeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_color_item_icon)
    init {
        icon.scaleType = ImageView.ScaleType.FIT_XY
    }
}

class ThicknessItemAdapter(private val config: AgoraUIDrawingConfig,
                           private val dialog: AgoraUIWhiteboardOptionDialog) : RecyclerView.Adapter<ThicknessViewHolder>() {

    companion object {
        var marginHorizontal: Int = 0
        var itemSpacing: Int = 0
        var iconSize: Int = 0
    }

    private val thickValues = dialog.context.resources.getIntArray(R.array.agora_board_tool_thickness).toList()
    private val iconSizes = dialog.context.resources.getIntArray(R.array.agora_board_tool_thickness_icon_sizes).toList()
    private var lastSelected = -1
    private val defaultColor = Color.parseColor("#E1E1EA")

    // When the selected color is white, we need to create a
    // ring icon to indicate the selected state
    private val ringSize = 28
    private val ringWidth = 4
    private val whiteIconSelected = ColorOptions.makeDrawable(
        Color.WHITE, ringSize, defaultColor, ringWidth, Color.WHITE, ringWidth)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThicknessViewHolder {
        return ThicknessViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.agora_tool_popup_color_item_layout, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ThicknessViewHolder, position: Int) {
        holder.itemView.layoutParams?.let {
            val size = iconSize
            it.width = size
            it.height = size
            holder.itemView.layoutParams = it
        }

        val index = holder.absoluteAdapterPosition
        (holder.icon.layoutParams as? RelativeLayout.LayoutParams)?.let {
            it.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            val size = getWeightedIconSize(index)
            it.width = size
            it.height = size
            holder.icon.layoutParams = it
        }

        holder.itemView.setOnClickListener {
            val idx = holder.absoluteAdapterPosition
            if (idx != lastSelected) {
                lastSelected = idx
                config.thick = thickValues[idx]
                notifyDataSetChanged()
                AgoraUIWhiteboardOptionDialog.listener?.onThicknessSelected(config.thick) }
        }

        holder.icon.isActivated = config.thick == thickValues[index]
        setIconImage(holder.icon, holder.icon.isActivated, config.color, defaultColor)
    }

    private fun getWeightedIconSize(index: Int): Int {
        return (iconSizes[index] / 28f * iconSize).toInt()
    }

    private fun setIconImage(icon: AppCompatImageView, activated: Boolean,
                             color: Int, colorDefault: Int) {
        if (activated && color == Color.WHITE) {
            icon.setImageDrawable(whiteIconSelected)
        } else {
            icon.setImageDrawable(ColorOptions.makeCircleDrawable(if (activated) color else colorDefault))
        }
    }

    override fun getItemCount(): Int {
        return dialog.thicknessSpanCount
    }
}

class ThicknessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_color_item_icon)
    init {
        icon.scaleType = ImageView.ScaleType.FIT_XY
    }
}

class ColorPlateItemAdapter(context: Context,
                            private val config: AgoraUIDrawingConfig,
                            private val dialog: AgoraUIWhiteboardOptionDialog)
    : RecyclerView.Adapter<ColorPlateItemViewHolder>() {

    companion object {
        var marginHorizontal: Int = 0
        var marginVertical: Int = 0
        var iconSize: Int = 0
        var itemSpacingHorizontal: Int = 0
        var itemSpacingVertical: Int = 0
    }

    private val iconColorStrings = context.resources.getStringArray(R.array.agora_tool_color_plate)
    private val colorValues: IntArray = IntArray(iconColorStrings.size)
    private val tintForWhite = Color.parseColor("#E1E1EA")

    private val iconVectorDrawable = mutableListOf<VectorDrawableCompat?>()
    private val iconVectorDrawableSelected = mutableListOf<VectorDrawableCompat?>()

    private val vectorResourceSelected = R.drawable.ic_agora_options_whiteboard_color_plate_item_active
    private val vectorResource = R.drawable.ic_agora_options_whiteboard_color_plate_item

    private val iconVectorDrawableWhite = VectorDrawableCompat.create(
        dialog.context.resources, R.drawable.ic_agora_options_whiteboard_color_plate_item_white, null)
    private val iconVectorDrawableWhiteActive = VectorDrawableCompat.create(
        dialog.context.resources, R.drawable.ic_agora_options_whiteboard_color_plate_item_active_white, null)

    private var lastSelected = -1

    init {
        for (i in colorValues.indices) {
            colorValues[i] = Color.parseColor(iconColorStrings[i])
            iconVectorDrawable.add(VectorDrawableCompat.create(
                dialog.context.resources, vectorResource, null))
            iconVectorDrawableSelected.add(VectorDrawableCompat.create(
                dialog.context.resources, vectorResourceSelected, null))
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorPlateItemViewHolder {
        return ColorPlateItemViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.agora_tool_popup_color_item_layout, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ColorPlateItemViewHolder, position: Int) {
        holder.itemView.layoutParams?.let {
            it.width = iconSize
            it.height = iconSize
            holder.itemView.layoutParams = it
        }

        (holder.icon.layoutParams as? RelativeLayout.LayoutParams)?.let {
            it.width = RelativeLayout.LayoutParams.MATCH_PARENT
            it.height = RelativeLayout.LayoutParams.MATCH_PARENT
            holder.icon.layoutParams = it
        }

        holder.itemView.setOnClickListener {
            val index = holder.absoluteAdapterPosition
            if (lastSelected != index) {
                lastSelected = index
                config.color = colorValues[index]
                notifyDataSetChanged()
                notifyColorChanged()
                AgoraUIWhiteboardOptionDialog.listener?.onColorSelected(config.color)
            }
        }

        val index = holder.absoluteAdapterPosition
        val selected = config.color == colorValues[index]
        val drawable = if (index == 0) {
            if (selected) {
                iconVectorDrawableWhiteActive
            } else {
                iconVectorDrawableWhite
            }
        } else {
            if (selected) {
                iconVectorDrawableSelected[index]
            } else {
                iconVectorDrawable[index]
            }
        }

        drawable?.setTint(if (index == 0) {
            tintForWhite
        } else {
            colorValues[index]
        })
        holder.icon.setImageDrawable(drawable)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyColorChanged() {
        dialog.thickAdapter?.notifyDataSetChanged()
        dialog.textAdapter?.notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return iconColorStrings.size
    }
}

class ColorPlateItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_color_item_icon)
    init {
        icon.scaleType = ImageView.ScaleType.FIT_XY
    }
}

interface AgoraUIWhiteboardOptionListener {
    fun onApplianceSelected(type: AgoraUIApplianceType)

    fun onColorSelected(color: Int)

    fun onTextSizeSelected(size: Int)

    fun onThicknessSelected(thick: Int)
}

class AutoMeasureGridLayoutManager(context: Context, spanCount: Int) : GridLayoutManager(context, spanCount) {
    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }
}

internal class DialogSizeCalculator {
    // Base height of all ui element on the designs
    var baseHeight: Int = 0
    var dialogHeight: Int = 0
    var dialogWidth: Int = 0
    var applianceMarginVertical: Int = 0
    var applianceMarginHorizontal: Int = 0
    var applianceItemSize: Int = 0
    var applianceItemSpacingVertical: Int = 0
    var applianceItemSpacingHorizontal: Int = 0

    var colorListPaddingHorizontal: Int = 0
    var colorListPaddingVertical: Int = 0
    var colorItemSpacingVertical: Int = 0
    var colorItemSpacingHorizontal: Int = 0
    var colorItemSize: Int = 0

    var thicknessItemSize: Int = 0
    var thicknessListMargin: Int = 0
    var thicknessItemSpacing: Int = 0

    var textItemSize: Int = 0
    var textSizeListMargin: Int = 0
    var textSizeItemSpacing: Int = 0

    // The height of dialog when the sub options
    // view is not visible
    var dialogNoSubViewHeight: Int = 0

    fun set(container: ViewGroup) {
        if (AgoraUIConfig.isLargeScreen) {
            baseHeight = AgoraUIConfig.baseUIHeightLargeScreen.toInt()
            dialogHeight = (container.height * 280 / baseHeight.toFloat()).toInt()
            dialogWidth = (dialogHeight.toFloat() * 258 / 280).toInt()
            dialogNoSubViewHeight = (dialogHeight.toFloat() * 123 / 280).toInt()
            applianceMarginVertical = (container.height * 18 / baseHeight.toFloat()).toInt()
            applianceMarginHorizontal = (container.height * 14 / baseHeight.toFloat()).toInt()
            applianceItemSize = (container.height * 35 / baseHeight.toFloat()).toInt()
            applianceItemSpacingHorizontal = applianceMarginHorizontal
            applianceItemSpacingVertical = applianceMarginVertical

            // all values are adjusted by a factor of 280 / 310
            colorListPaddingHorizontal = (container.height * 18f / baseHeight * 280 / 310).toInt()
            colorListPaddingVertical = (container.height * 20f / baseHeight * 280 / 310).toInt()
            colorItemSpacingVertical = (container.height * 15f / baseHeight * 280 / 310).toInt()
            colorItemSpacingHorizontal = (container.height * 15f / baseHeight * 280 / 310).toInt()
            colorItemSize = (container.height * 28f / baseHeight * 280 / 310).toInt()

            thicknessItemSize = (container.height * 28f / baseHeight * 280 / 310).toInt()
            thicknessListMargin = (container.height * 20f / baseHeight * 280 / 310).toInt()
            thicknessItemSpacing = (container.height * 25f / baseHeight * 280 / 310).toInt()

            textItemSize = (container.height * 38f / baseHeight * 280 / 310).toInt()
            textSizeListMargin = (container.height * 22f / baseHeight * 280 / 310).toInt()
            textSizeItemSpacing = (container.height * 28f / baseHeight * 280 / 310).toInt()
        } else {
            baseHeight = AgoraUIConfig.baseUIHeightSmallScreen.toInt()
            dialogHeight = (container.height * (258 / baseHeight.toFloat())).toInt()
            dialogWidth = (dialogHeight.toFloat() * 240 / 258).toInt()
            dialogNoSubViewHeight = (dialogHeight.toFloat() * 105 / 258).toInt()
            applianceMarginVertical = (container.height * 15 / baseHeight.toFloat()).toInt()
            applianceMarginHorizontal = (container.height * 15 / baseHeight.toFloat()).toInt()
            applianceItemSize = (container.height * 30 / baseHeight.toFloat()).toInt()
            applianceItemSpacingHorizontal = applianceMarginHorizontal
            applianceItemSpacingVertical = applianceMarginVertical

            colorListPaddingHorizontal = (container.height * 11f / baseHeight).toInt()
            colorListPaddingVertical = (container.height * 15f / baseHeight).toInt()
            colorItemSpacingVertical = (container.height * 15f / baseHeight).toInt()
            colorItemSpacingHorizontal = (container.height * 10f / baseHeight).toInt()
            colorItemSize = (container.height * 28f / baseHeight).toInt()

            thicknessItemSize = (container.height * 28f / baseHeight).toInt()
            thicknessListMargin = (container.height * 20f / baseHeight).toInt()
            thicknessItemSpacing = (container.height * 15f / baseHeight).toInt()

            textItemSize = (container.height * 38f / baseHeight).toInt()
            textSizeListMargin = (container.height * 22f / baseHeight).toInt()
            textSizeItemSpacing = (container.height * 28f / baseHeight).toInt()
        }
    }
}