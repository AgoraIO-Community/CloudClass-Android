package io.agora.uikit.impl.tool

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.*
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.educontext.context.ExtAppContext
import io.agora.uikit.R
import io.agora.uikit.interfaces.protocols.AgoraUIDrawingConfig

@SuppressLint("InflateParams")
class AgoraUIToolDialog(context: Context,
                        private val type: AgoraUIToolItemType,
                        private val listener: AgoraUIToolDialogListener,
                        private val config: AgoraUIDrawingConfig) : Dialog(context, R.style.agora_dialog) {

    private val width = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_bg_width)
    private val elevation = context.resources.getDimensionPixelOffset(R.dimen.agora_tool_popup_elevation)

    fun show(anchor: View?) {
        init(anchor)
        super.show()
    }

    private fun init(anchor: View?) {
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        when (type) {
            AgoraUIToolItemType.Pen -> {
                initStyleDialog(anchor)
            }
            AgoraUIToolItemType.Color -> {
                initColorDialog(anchor)
            }
            AgoraUIToolItemType.Text -> {
                initFontDialog(anchor)
            }
            else -> return
        }
    }

    private fun initStyleDialog(anchor: View?) {
        val layout = LayoutInflater.from(context).inflate(
                R.layout.agora_tool_popup_pen_layout, null, false)
        setContentView(layout)

        val dialog = layout.findViewById<RelativeLayout>(R.id.agora_tool_popup_layout)
        dialog.clipToOutline = true
        dialog.elevation = elevation.toFloat()

        val recycler = layout.findViewById<RecyclerView>(R.id.agora_tool_style_recycler)
        recycler.layoutManager = GridLayoutManager(context, 4)
        recycler.adapter = StyleAdapter(recycler)

        val height = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_style_bg_height)
        anchor?.let { DialogUtil.adjustPosition(this.window!!, it, width, height) }
    }

    private class StyleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_style_item_icon)
        val dot: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_style_select_dot)
    }

    private inner class StyleAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<StyleViewHolder>() {
        private val iconsRes = arrayOf(
                StyleItem(R.drawable.agora_tool_icon_pen, AgoraUIApplianceType.Pen),
                StyleItem(R.drawable.agora_tool_icon_rect_normal, AgoraUIApplianceType.Rect),
                StyleItem(R.drawable.agora_tool_icon_circle_ring_normal, AgoraUIApplianceType.Circle),
                StyleItem(R.drawable.agora_tool_icon_line_normal, AgoraUIApplianceType.Line))


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
            return StyleViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_tool_popup_style_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
            val itemWidth = recyclerView.width / 4
            var params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
            params.width = itemWidth
            params.height = itemWidth
            val margin = (recyclerView.height - params.height) / 2
            params.topMargin = margin
            params.bottomMargin = margin
            holder.itemView.layoutParams = params

            params = holder.icon.layoutParams as RelativeLayout.LayoutParams
            params.height = (recyclerView.height * 2f / 3).toInt()
            params.width = params.height
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = params

            params = holder.dot.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_BOTTOM, holder.icon.id)

            val pos = holder.adapterPosition
            val item = iconsRes[pos]
            holder.icon.setImageResource(item.res)
            holder.dot.visibility = if (item.type == config.activeAppliance) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                config.activeAppliance = item.type
                notifyDataSetChanged()
                listener.onApplianceSelected(item.type)
            }
        }

        override fun getItemCount(): Int {
            return iconsRes.size
        }
    }

    data class StyleItem(val res: Int, val type: AgoraUIApplianceType)

    private fun initColorDialog(anchor: View?) {
        val layout = LayoutInflater.from(context).inflate(
                R.layout.agora_tool_popup_color_layout, null, false)
        setContentView(layout)

        val dialog = layout.findViewById<RelativeLayout>(R.id.agora_tool_popup_layout)
        dialog.clipToOutline = true
        dialog.elevation = elevation.toFloat()

        val recycler: RecyclerView = layout.findViewById(R.id.agora_tool_popup_color_recycler)
        recycler.layoutManager = GridLayoutManager(context, 4)
        recycler.adapter = ColorAdapter()

        val seekBar: SeekBar = layout.findViewById(R.id.agora_tool_popup_color_progress)
        seekBar.max = 38
        seekBar.progress = config.thick
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val progress = if (it.progress < 1) 1
                    else if (it.progress > 38) 38
                    else it.progress

                    it.progress = progress
                    listener.onThickSelected(progress)
                    config.thick = progress
                }
            }
        })

        val height = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_color_bg_height)
        anchor?.let { DialogUtil.adjustPosition(window!!, it, width, height) }
    }

    private class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_color_item_icon)
    }

    private inner class ColorAdapter : RecyclerView.Adapter<ColorViewHolder>() {
        private val iconColorStrings = context.resources.getStringArray(R.array.agora_tool_color_plate)
        private val colorValues: IntArray = IntArray(iconColorStrings.size)
        private val borderColors = context.resources.getStringArray(R.array.agora_tool_color_plate_border)
        private val selectColors = context.resources.getStringArray(R.array.agora_tool_color_plate_select)
        private val borderWidth = context.resources.getDimensionPixelSize(R.dimen.stroke_small)
        private val iconSize = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_color_plate_icon_size)

        private val icons = ColorOptions.makeColorOptions(iconColorStrings,
                selectColors, borderColors, iconSize, borderWidth)

        init {
            for (i in colorValues.indices) {
                colorValues[i] = Color.parseColor(iconColorStrings[i])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            return ColorViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_tool_popup_color_item_layout, null, false))
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val param = holder.icon.layoutParams as RelativeLayout.LayoutParams
            param.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = param

            val pos = holder.adapterPosition
            holder.icon.setImageDrawable(icons[pos])
            holder.icon.isActivated = (colorValues[pos] == config.color)
            holder.itemView.setOnClickListener {
                config.color = colorValues[pos]
                notifyDataSetChanged()
                listener.onColorSelected(config.color)
            }
        }

        override fun getItemCount(): Int {
            return iconColorStrings.size
        }
    }

    private fun initFontDialog(anchor: View?) {
        val layout = LayoutInflater.from(context).inflate(
                R.layout.agora_tool_popup_font_layout, null, false)
        setContentView(layout)

        val dialog = layout.findViewById<RelativeLayout>(R.id.agora_tool_popup_layout)
        dialog.clipToOutline = true
        dialog.elevation = elevation.toFloat()

        val recycler = layout.findViewById<RecyclerView>(R.id.agora_tool_font_recycler)
        recycler.layoutManager = GridLayoutManager(context, 3)
        recycler.adapter = FontAdapter()
        recycler.addItemDecoration(FontItemDecorator())

        val height = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_font_bg_height)
        anchor?.let { DialogUtil.adjustPosition(window!!, it, width, height) }
    }

    private inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: AppCompatTextView = itemView.findViewById(R.id.agora_tool_font_item)
    }

    private inner class FontAdapter : RecyclerView.Adapter<FontViewHolder>() {
        val fontTexts: Array<String> = context.resources.getStringArray(R.array.agora_tool_fonts)
        val fontSizeValues = context.resources.getIntArray(R.array.agora_tool_font_sizes)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
            return FontViewHolder(LayoutInflater.from(context)
                    .inflate(R.layout.agora_tool_popup_font_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
            val pos = holder.adapterPosition
            holder.text.text = fontTexts[pos]
            holder.itemView.isActivated = (config.fontSize == fontSizeValues[pos])

            holder.itemView.setOnClickListener {
                config.fontSize = fontSizeValues[pos]
                listener.onFontSizeSelected(config.fontSize)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return fontTexts.size
        }
    }

    private inner class FontItemDecorator : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View,
                                    parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            val margin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
            outRect.left = margin
            outRect.right = margin
            outRect.top = margin
            outRect.bottom = margin
        }
    }
}

private object DialogUtil {
    fun adjustPosition(window: Window, anchor: View, width: Int, height: Int) {
        val params = window.attributes
        hideStatusBar(window)

        params.width = width
        params.height = height
        params.gravity = Gravity.TOP or Gravity.START

        val locationsOnScreen = IntArray(2)
        anchor.getLocationOnScreen(locationsOnScreen)
        params.x = locationsOnScreen[0] + anchor.width
        params.y = locationsOnScreen[1] + anchor.height / 2 - height / 2
        window.attributes = params
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}

class AgoraUIExtensionDialog(context: Context,
        private val extAppContext: ExtAppContext,
        private val listener: AgoraUIToolExtAppListener) : Dialog(context, R.style.agora_dialog) {

    private val tag = "AgoraUIExtensionDialog"
    private val width = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_bg_width)
    private val elevation = context.resources.getDimensionPixelOffset(R.dimen.agora_tool_popup_elevation)

    fun show(anchor: View?) {
        init(anchor)
        super.show()
    }

    @SuppressLint("InflateParams")
    private fun init(anchor: View?) {
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        val layout = LayoutInflater.from(context).inflate(
                R.layout.agora_tool_popup_ext_app_layout, null, false)
        setContentView(layout)

        val dialog = layout.findViewById<RelativeLayout>(R.id.agora_tool_popup_layout)
        dialog.clipToOutline = true
        dialog.elevation = elevation.toFloat()

        val recycler = layout.findViewById<RecyclerView>(R.id.agora_tool_popup_ext_app_recycler)
        recycler.layoutManager = GridLayoutManager(context, 3)
        recycler.adapter = ExtensionAppAdapter()

        val height = context.resources.getDimensionPixelSize(R.dimen.agora_tool_popup_ext_app_layout_height)
        anchor?.let { DialogUtil.adjustPosition(window!!, it, width, height) }
    }

    inner class ExtensionAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_popup_ext_app_item_icon)
    }

    inner class ExtensionAppAdapter : RecyclerView.Adapter<ExtensionAppViewHolder>() {
        private val extApps = extAppContext.getRegisteredExtApps()

        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtensionAppViewHolder {
            return ExtensionAppViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_tool_popup_ext_app_item_layout, null, false))
        }

        override fun onBindViewHolder(holder: ExtensionAppViewHolder, position: Int) {
            val pos = holder.adapterPosition
            extApps[pos].let { appInfo ->
                appInfo.imageResource?.let {
                    holder.icon.setImageResource(it)
                }

                holder.itemView.setOnClickListener { view ->
                    listener.onExtAppClicked(view, appInfo.appIdentifier)
                }
            }
        }

        override fun getItemCount(): Int {
            return extApps.size
        }
    }
}

interface AgoraUIToolDialogListener {
    fun onFontSizeSelected(size: Int)
    fun onColorSelected(color: Int)
    fun onThickSelected(thick: Int)
    fun onApplianceSelected(appliance: AgoraUIApplianceType)
}

interface AgoraUIToolExtAppListener {
    fun onExtAppClicked(view: View, identifier: String)
}