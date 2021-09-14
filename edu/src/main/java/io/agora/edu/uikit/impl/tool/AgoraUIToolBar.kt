package io.agora.edu.uikit.impl.tool

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.util.set
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.edu.R
import io.agora.edu.core.context.WhiteboardApplianceType
import io.agora.edu.uikit.impl.AbsComponent
import io.agora.edu.uikit.handlers.WhiteboardHandler
import io.agora.edu.uikit.interfaces.protocols.AgoraUIDrawingConfig
import io.agora.edu.uikit.util.AppUtil

class AgoraUIToolBar(context: Context,
                     parent: ViewGroup,
                     private val eduContext: io.agora.edu.core.context.EduContextPool?,
                     foldTop: Int = 0, foldLeft: Int = 0,
                     private var foldWidth: Int = 0,
                     private var foldHeight: Int = 0,
                     unfoldTop: Int = 0, unfoldLeft: Int = 0,
                     private var unfoldWidth: Int = 0,
                     private var unfoldHeight: Int = 0,
                     shadowWidth: Int = 0) : AbsComponent(), AgoraUIToolDialogListener {
    private val tag: String = "AgoraUIToolWindow"
    private val interval = 300L
    private val duration = 400L
    private val itemMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
    private val itemHeightRatio = 5f / 6

    private val foldLayout: View = LayoutInflater.from(context).inflate(R.layout.agora_tool_fold_layout, parent, false)
    private val unfoldLayout: View = LayoutInflater.from(context).inflate(R.layout.agora_tool_unfold_layout, parent, false)

    private val toolAdapter: AgoraUIToolItemAdapter
    private val recycler: RecyclerView
    private val unfoldButton: AppCompatImageView
    private val foldButton: AppCompatImageView

    private var extAppDialog: AgoraUIExtensionDialog? = null

    private val config = AgoraUIDrawingConfig()
    private var optionSelected = 0

    // 只记录点击toolBax  roster时上一次选中的item(因为只有这俩的dialog消失时才需要恢复上一次选中的item)
    private var lastOptionSelected = optionSelected
    private var whiteBoardEnabled = false

    private var mDialog: AgoraUIToolDialog? = null

    // only for roster dismiss
    val rosterDismissListener = DialogInterface.OnDismissListener {
        // restore selectedStatus
        notifyOptionSelected()
        setConfig(this.config)
    }

    private var toolBarType = AgoraUIToolType.Whiteboard

    private var toolBarItems = AgoraUIToolItemList.getWhiteboardList(this.config)

    private val whiteboardHandler = object : WhiteboardHandler() {
        override fun onDrawingConfig(config: io.agora.edu.core.context.WhiteboardDrawingConfig) {
            super.onDrawingConfig(config)
            val tmp = AgoraUIDrawingConfig(
                    toAgoraUIAppliance(config.activeAppliance),
                    config.color, config.fontSize, config.thick)
            setConfig(tmp)
        }

        override fun onDrawingEnabled(enabled: Boolean) {
            super.onDrawingEnabled(enabled)
            setWhiteboardFunctionEnabled(enabled)
        }
    }

    init {
        if (foldWidth <= 0) foldWidth = context.resources.getDimensionPixelSize(R.dimen.agora_tool_switch_layout_size)
        if (foldHeight <= 0) foldHeight = context.resources.getDimensionPixelSize(R.dimen.agora_tool_switch_layout_size)

        foldLayout.translationX = -(foldWidth + shadowWidth * 2).toFloat()
        foldLayout.clipToOutline = true
        foldLayout.elevation = shadowWidth.toFloat()
        parent.addView(foldLayout, foldWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        foldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (foldLayout.width > 0 && foldLayout.height > 0) {
                            foldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val params = foldLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.leftMargin = foldLeft
                            params.topMargin = foldTop
                            foldLayout.layoutParams = params
                        }
                    }
                }
        )

        unfoldButton = unfoldLayout.findViewById(R.id.agora_tool_switch)
        foldButton = foldLayout.findViewById(R.id.agora_tool_switch)

        foldLayout.findViewById<ImageView>(R.id.agora_tool_switch).setOnClickListener {
            foldButton.isClickable = false
            unfoldButton.isClickable = false
            it.postDelayed({
                foldButton.isClickable = true
                unfoldButton.isClickable = true
            }, duration * 2)
            foldLayout.animate().setDuration(duration)
                    .setInterpolator(DecelerateInterpolator())
                    .xBy(-(foldLeft + foldWidth + shadowWidth * 2).toFloat())
                    .withEndAction {
                        unfoldLayout.animate().setDuration(duration)
                                .setInterpolator(DecelerateInterpolator())
                                .xBy((unfoldLeft + unfoldWidth + shadowWidth * 2).toFloat())
                    }
        }

        if (unfoldWidth <= 0) unfoldWidth = context.resources.getDimensionPixelSize(R.dimen.agora_tool_layout_default_width)
        if (unfoldHeight <= 0) unfoldHeight = context.resources.getDimensionPixelSize(R.dimen.agora_tool_layout_default_height)

        unfoldLayout.elevation = shadowWidth.toFloat()
        unfoldLayout.outlineProvider = AgoraUIToolUnfoldViewOutline()
        unfoldLayout.clipToOutline = true
        parent.addView(unfoldLayout, unfoldWidth, unfoldHeight)

        unfoldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (foldLayout.width > 0 && foldLayout.height > 0) {
                            unfoldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.leftMargin = unfoldLeft
                            params.topMargin = unfoldTop
                            unfoldLayout.layoutParams = params
                        }
                    }
                }
        )

        unfoldButton.setOnClickListener {
            foldButton.isClickable = false
            unfoldButton.isClickable = false
            it.postDelayed({
                foldButton.isClickable = true
                unfoldButton.isClickable = true
            }, duration * 2)
            unfoldLayout.animate().setDuration(duration)
                    .setInterpolator(DecelerateInterpolator())
                    .xBy(-(unfoldLeft + unfoldWidth + shadowWidth * 2).toFloat())
                    .withEndAction {
                        foldLayout.animate().setDuration(duration)
                                .setInterpolator(DecelerateInterpolator())
                                .xBy((foldLeft + foldWidth + shadowWidth * 2).toFloat())
                    }
        }

        recycler = unfoldLayout.findViewById(R.id.agora_tool_recycler)
        val recyclerParams = recycler.layoutParams as RelativeLayout.LayoutParams
        recyclerParams.height = recyclerHeight()
        recycler.layoutParams = recyclerParams

        toolAdapter = AgoraUIToolItemAdapter(this, recycler)
        recycler.adapter = this.toolAdapter
        recycler.layoutManager = LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false)

        unfoldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (unfoldButton.height > 0 && recycler.height > 0) {
                            unfoldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            adjustToolHeight()
                        }
                    }
                }
        )

        eduContext?.whiteboardContext()?.addHandler(whiteboardHandler)
    }

    private fun recyclerHeight(): Int {
        // recycler item height versus width ratio is 5 / 6
        return (unfoldWidth * toolBarItems.size * itemHeightRatio).toInt()
    }

    private fun adjustToolHeight() {
        var params = unfoldButton.layoutParams as ViewGroup.MarginLayoutParams
        val marginVertical = (unfoldWidth - unfoldButton.width) / 2
        params.topMargin = marginVertical
        params.bottomMargin = marginVertical
        unfoldButton.layoutParams = params

        params = recycler.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = marginVertical

        // The height of recycler view if current list
        // items are all displayed
        val recyclerHeight = recyclerHeight()

        // The total height of tool bar if current
        // list items are all displayed
        var totalHeight = unfoldButton.height + marginVertical * 2 +
                if (recyclerHeight == 0) 0 else (recyclerHeight + marginVertical)

        if (totalHeight > unfoldHeight && recyclerHeight > 0) {
            totalHeight -= (totalHeight - unfoldHeight)
        }

        params.height = totalHeight
        recycler.layoutParams = params

        params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
        params.height = totalHeight
        unfoldLayout.layoutParams = params
    }

    fun setConfig(config: AgoraUIDrawingConfig) {
        this.config.set(config)
        toolAdapter.notifyDataSetChanged()
    }

    private fun notifyOptionSelected() {
        if (whiteBoardEnabled) {
            optionSelected = lastOptionSelected
            if (optionSelected == -1) {
                val itemType = toAgoraUIToolItemType(config.activeAppliance)
                val items = if (!whiteBoardEnabled) {
                    if (toolBarType == AgoraUIToolType.All) {
                        mDialog?.dismiss()
                        AgoraUIToolItemList.getRosterOnlyList()
                    } else {
                        AgoraUIToolItemList.emptyList
                    }
                } else {
                    when (toolBarType) {
                        AgoraUIToolType.All -> AgoraUIToolItemList.getAllItemList(this.config)
                        AgoraUIToolType.Whiteboard -> AgoraUIToolItemList.getWhiteboardList(this.config)
                    }
                }
                items.forEachIndexed { index, item ->
                    if (item.type == itemType) {
                        optionSelected = index
                        return
                    }
                }
                optionSelected = 0
            }
        } else {
            optionSelected = -1
        }
    }

    fun setWhiteboardFunctionEnabled(enabled: Boolean) {
        this.whiteBoardEnabled = enabled
        notifyOptionSelected()
        recycler.post {
            toolBarItems = if (!enabled) {
                if (toolBarType == AgoraUIToolType.All) {
                    mDialog?.dismiss()
                    AgoraUIToolItemList.getRosterOnlyList()
                } else {
                    setSelfVisibility(false)
                    recycler.visibility = GONE
                    AgoraUIToolItemList.emptyList
                }
            } else {
                setSelfVisibility(true)
                recycler.visibility = VISIBLE
                when (toolBarType) {
                    AgoraUIToolType.All -> AgoraUIToolItemList.getAllItemList(this.config)
                    AgoraUIToolType.Whiteboard -> AgoraUIToolItemList.getWhiteboardList(this.config)
                }
            }

            toolAdapter.notifyDataSetChanged()
            adjustToolHeight()
        }
    }

    fun setToolbarType(type: AgoraUIToolType) {
        this.toolBarType = type
        recycler.post {
            toolBarItems = when (type) {
                AgoraUIToolType.All -> AgoraUIToolItemList.getAllItemList(this.config)
                AgoraUIToolType.Whiteboard -> AgoraUIToolItemList.getWhiteboardList(this.config)
            }

            toolAdapter.notifyDataSetChanged()
            adjustToolHeight()
        }
    }

    private fun setSelfVisibility(visible: Boolean) {
        foldLayout.post {
            foldLayout.visibility = if (visible) VISIBLE else GONE
            unfoldLayout.visibility = if (visible) VISIBLE else GONE
        }
    }

    private class AgoraUIToolUnfoldViewOutline : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            view?.let {
                outline?.setRoundRect(0, 0, it.width, it.height, it.width / 2f)
            }
        }
    }

    class AgoraUIToolItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_item_icon)
        val more: AppCompatImageView = itemView.findViewById(R.id.agora_tool_item_more_icon)
    }

    inner class AgoraUIToolItemAdapter(private val window: AgoraUIToolBar,
                                       private val recyclerView: RecyclerView)
        : RecyclerView.Adapter<AgoraUIToolItemViewHolder>() {

        private val colorIconSize = recyclerView.context.resources.getDimensionPixelSize(
                R.dimen.agora_tool_popup_color_plate_icon_size)

        private val colorsIcons: SparseArray<Drawable> = buildColorDrawables(
                recyclerView.context.resources.getStringArray(R.array.agora_tool_color_plate))

        private fun buildColorDrawables(colorStrings: Array<String>): SparseArray<Drawable> {
            val array = SparseArray<Drawable>(colorStrings.size)
            colorStrings.forEach {
                val color: Int = Color.parseColor(it)
                var borderColor = color

                if (color == Color.WHITE) {
                    borderColor = Color.GRAY
                }

                array[color] = ColorOptions.makeDrawable(
                        color, colorIconSize,
                        Color.TRANSPARENT, 14,
                        borderColor, 18)
            }
            return array
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgoraUIToolItemViewHolder {
            return AgoraUIToolItemViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_tool_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: AgoraUIToolItemViewHolder, position: Int) {
            val layoutParam = holder.itemView.layoutParams
            layoutParam.width = recyclerView.width
            layoutParam.height = (layoutParam.width * 5f / 6).toInt()
            holder.itemView.layoutParams = layoutParam

            var params = holder.icon.layoutParams as RelativeLayout.LayoutParams
            params.width = (recyclerView.width / 3f * 2).toInt()
            params.height = params.width
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = params

            params = holder.more.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_BOTTOM, holder.icon.id)
            params.addRule(RelativeLayout.ALIGN_END, holder.icon.id)
            params.addRule(RelativeLayout.ALIGN_RIGHT, holder.icon.id)
            params.bottomMargin = itemMargin
            holder.more.layoutParams = params

            val pos = holder.adapterPosition
            val item = toolBarItems[pos]

            if (item.type == AgoraUIToolItemType.Color) {
                val drawable = colorsIcons[config.color]
                holder.icon.setImageDrawable(drawable)
            } else {
                holder.icon.setImageResource(item.iconRes)
            }

            holder.more.visibility = if (toolBarItems[pos].hasPopup) VISIBLE else GONE

            holder.itemView.isActivated = (pos == optionSelected)
            holder.itemView.setOnClickListener {
                if (AppUtil.isFastClick(interval)) {
                    return@setOnClickListener
                }
                val tmp = optionSelected
                if (toolBarItems[pos].type != AgoraUIToolItemType.Color) {
                    optionSelected = pos
                }
                toolAdapter.notifyDataSetChanged()
                handleClick(holder.itemView, pos, tmp)
            }
        }

        override fun getItemCount(): Int {
            return toolBarItems.size
        }

        //click the item of the toolbar
        private fun handleClick(view: View, newPos: Int, oldPos: Int) {
            // only toolBox/roster dismiss, need restore itemSelectedStatus
            lastOptionSelected = newPos
            val item: AgoraUIToolItem = toolBarItems[newPos]
            when (item.type) {
                AgoraUIToolItemType.Select -> {
                    config.activeAppliance = AgoraUIApplianceType.Select
                    eduContext?.whiteboardContext()?.selectAppliance(WhiteboardApplianceType.Select)
                }

                AgoraUIToolItemType.Pen, AgoraUIToolItemType.Rect,
                AgoraUIToolItemType.Circle, AgoraUIToolItemType.Line,
                -> { //从输入文字，切回画图形，click的时候设置一下whiteboardContext().selectAppliance，切换工具
                    config.activeAppliance = toAgoraUIApplianceType(item.type) //AgoraUIToolItemType to AgoraUIApplianceType
                    eduContext?.whiteboardContext()?.selectAppliance(toWhiteboardApplianceType(config.activeAppliance))

                    getContainer()?.getActivity()?.let {
                        mDialog = AgoraUIToolDialog(it, item.type, this@AgoraUIToolBar, config)
                        mDialog?.show(view)
                    }
                }

                AgoraUIToolItemType.Color -> {
                    getContainer()?.getActivity()?.let {
                        mDialog = AgoraUIToolDialog(it, item.type, this@AgoraUIToolBar, config)
                        mDialog?.show(view)
                    }
                }

                AgoraUIToolItemType.Clicker -> {
                    config.activeAppliance = AgoraUIApplianceType.Clicker
                    eduContext?.whiteboardContext()?.selectAppliance(WhiteboardApplianceType.Clicker)
                }

                AgoraUIToolItemType.Text -> {
                    eduContext?.whiteboardContext()?.selectAppliance(WhiteboardApplianceType.Text)
                    getContainer()?.getActivity()?.let {
                        mDialog = AgoraUIToolDialog(it, item.type, this@AgoraUIToolBar, config)
                        mDialog?.show(view)
                    }
                }

                AgoraUIToolItemType.Eraser -> {
                    config.activeAppliance = AgoraUIApplianceType.Eraser
                    eduContext?.whiteboardContext()?.selectAppliance(WhiteboardApplianceType.Eraser)
                }

                AgoraUIToolItemType.Roster -> {
                    // record old optionSelected, for restore selectedStatus
                    lastOptionSelected = oldPos
                    eduContext?.whiteboardContext()?.selectRoster(view)
                }

                AgoraUIToolItemType.Toolbox -> {
                    // record old optionSelected, for restore selectedStatus
                    lastOptionSelected = oldPos
                    eduContext?.extAppContext()?.let {
                        extAppDialog = AgoraUIExtensionDialog(view.context, it,
                                object : AgoraUIToolExtAppListener {
                                    override fun onExtAppClicked(view: View, identifier: String) {
                                        val result = it.launchExtApp(identifier)
                                        Log.i(tag, "launch ext app $identifier result $result")
                                        extAppDialog?.let { dialog ->
                                            if (dialog.isShowing) {
                                                dialog.dismiss()
                                            }

                                            extAppDialog = null
                                        }
                                    }
                                })
                        extAppDialog?.setOnDismissListener {
                            // restore selectedStatus
                            notifyOptionSelected()
                            setConfig(this@AgoraUIToolBar.config)
                        }
                        extAppDialog?.show(view)
                    }
                }
            }
        }
    }

    override fun onFontSizeSelected(size: Int) {
        eduContext?.whiteboardContext()?.selectFontSize(size)
    }

    override fun onColorSelected(color: Int) {
        eduContext?.whiteboardContext()?.selectColor(color)
        config.color = color
        toolAdapter.notifyDataSetChanged()
    }

    override fun onThickSelected(thick: Int) {
        eduContext?.whiteboardContext()?.selectThickness(thick)
    }

    override fun onApplianceSelected(appliance: AgoraUIApplianceType) {
        config.activeAppliance = appliance
        eduContext?.whiteboardContext()?.selectAppliance(toEduContextAppliance(appliance))
        toolAdapter.notifyDataSetChanged()
    }

    //refresh the toolbar list
    override fun onToolbarItemsUpdated(appliance: AgoraUIApplianceType) {
        toolBarItems = AgoraUIToolItemList.getAllItemList(this.config)
    }

    private fun toEduContextAppliance(appliance: AgoraUIApplianceType): WhiteboardApplianceType {
        return when (appliance) {
            AgoraUIApplianceType.Select -> WhiteboardApplianceType.Select
            AgoraUIApplianceType.Circle -> WhiteboardApplianceType.Circle
            AgoraUIApplianceType.Eraser -> WhiteboardApplianceType.Eraser
            AgoraUIApplianceType.Line -> WhiteboardApplianceType.Line
            AgoraUIApplianceType.Pen -> WhiteboardApplianceType.Pen
            AgoraUIApplianceType.Rect -> WhiteboardApplianceType.Rect
            AgoraUIApplianceType.Text -> WhiteboardApplianceType.Text
            AgoraUIApplianceType.Clicker -> WhiteboardApplianceType.Clicker
        }
    }

    private fun toAgoraUIAppliance(appliance: WhiteboardApplianceType): AgoraUIApplianceType {
        return when (appliance) {
            WhiteboardApplianceType.Select -> AgoraUIApplianceType.Select
            WhiteboardApplianceType.Circle -> AgoraUIApplianceType.Circle
            WhiteboardApplianceType.Eraser -> AgoraUIApplianceType.Eraser
            WhiteboardApplianceType.Line -> AgoraUIApplianceType.Line
            WhiteboardApplianceType.Pen -> AgoraUIApplianceType.Pen
            WhiteboardApplianceType.Rect -> AgoraUIApplianceType.Rect
            WhiteboardApplianceType.Text -> AgoraUIApplianceType.Text
            WhiteboardApplianceType.Clicker -> AgoraUIApplianceType.Clicker
        }
    }

    private fun toWhiteboardApplianceType(appliance: AgoraUIApplianceType): WhiteboardApplianceType {
        return when (appliance) {
            AgoraUIApplianceType.Select -> WhiteboardApplianceType.Select
            AgoraUIApplianceType.Circle -> WhiteboardApplianceType.Circle
            AgoraUIApplianceType.Eraser -> WhiteboardApplianceType.Eraser
            AgoraUIApplianceType.Line -> WhiteboardApplianceType.Line
            AgoraUIApplianceType.Pen -> WhiteboardApplianceType.Pen
            AgoraUIApplianceType.Rect -> WhiteboardApplianceType.Rect
            AgoraUIApplianceType.Text -> WhiteboardApplianceType.Text
            AgoraUIApplianceType.Clicker -> WhiteboardApplianceType.Clicker
        }
    }

    private fun toAgoraUIToolItemType(appliance: AgoraUIApplianceType): AgoraUIToolItemType {
        return when (appliance) {
            AgoraUIApplianceType.Select -> AgoraUIToolItemType.Select
            AgoraUIApplianceType.Pen -> AgoraUIToolItemType.Pen
            AgoraUIApplianceType.Rect -> AgoraUIToolItemType.Rect
            AgoraUIApplianceType.Circle -> AgoraUIToolItemType.Circle
            AgoraUIApplianceType.Line -> AgoraUIToolItemType.Line
            AgoraUIApplianceType.Text -> AgoraUIToolItemType.Text
            AgoraUIApplianceType.Eraser -> AgoraUIToolItemType.Eraser
            AgoraUIApplianceType.Clicker -> AgoraUIToolItemType.Clicker
        }
    }

    private fun toAgoraUIApplianceType(appliance: AgoraUIToolItemType): AgoraUIApplianceType {
        return when (appliance) {
            AgoraUIToolItemType.Select -> AgoraUIApplianceType.Select
            AgoraUIToolItemType.Pen -> AgoraUIApplianceType.Pen
            AgoraUIToolItemType.Rect -> AgoraUIApplianceType.Rect
            AgoraUIToolItemType.Circle -> AgoraUIApplianceType.Circle
            AgoraUIToolItemType.Line -> AgoraUIApplianceType.Line
            AgoraUIToolItemType.Text -> AgoraUIApplianceType.Text
            AgoraUIToolItemType.Eraser -> AgoraUIApplianceType.Eraser
            AgoraUIToolItemType.Clicker -> AgoraUIApplianceType.Clicker
            else -> AgoraUIApplianceType.Pen
        }
    }

    override fun setRect(rect: Rect) {

    }

    fun setVerticalPosition(top: Int, maxHeight: Int) {
        unfoldLayout.post {
            var params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = top
            unfoldLayout.layoutParams = params

            params = foldLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = top
            foldLayout.layoutParams = params

            unfoldHeight = maxHeight
            adjustToolHeight()
        }
    }
}