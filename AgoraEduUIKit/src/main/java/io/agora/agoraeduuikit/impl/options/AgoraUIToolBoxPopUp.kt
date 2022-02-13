package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppEngine

class AgoraUIToolBoxPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraUIToolBoxPopUp"

    private val clickInterval = 300L

    private var parent: ViewGroup? = null

    @SuppressLint("InflateParams")
    private val layout = LayoutInflater.from(context).inflate(
            R.layout.agora_toolbox_popup_layout, null, false)

    private val content = layout.findViewById(R.id.root_Layout) as ViewGroup
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recyclerView)
    private val toolBoxItemList = mutableListOf<ToolBoxItem>()
    private val spanCount = 3
    private val space = context.resources.getDimensionPixelSize(R.dimen.divider_thin)

    var eduContext: EduContextPool? = null

    init {
        toolBoxItemList.apply {
            add(ToolBoxItem(ToolBoxItemType.Cloud, R.drawable.agora_toolbox_icon_cloud,
                    context.getString(R.string.agora_toolbox_cloud)))
            val extAppInfos = AgoraExtAppEngine.getRegisteredExtApps()
            extAppInfos.forEach { info ->
                add(ToolBoxItem(ToolBoxItemType.ExtApp, info.imageResource, info.name, info.appIdentifier))
            }
        }
    }

    fun initView(parent: ViewGroup, width: Int, height: Int, shadow: Int) {
        this.parent = parent
        addView(layout, width, height)

        var param = content.layoutParams as MarginLayoutParams
        param.topMargin = shadow
        param.bottomMargin = shadow
        param.leftMargin = shadow
        param.rightMargin = shadow
        param.width = width - shadow * 2
        param.height = height - shadow * 2
        content.layoutParams = param

        // shadow width may be slightly different at
        // different direction, so we reduce the
        // shadow a little bit to avoid sharp edges.
        content.elevation = shadow.toFloat() * 3 / 5
        recyclerView.addItemDecoration(SpaceItemDecoration(space, spanCount,
                context.resources.getColor(R.color.theme_divider_color_gray)))
//                context.resources.getColor(R.color.red)))
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        val itemH = calculateItemHeight(param.height, toolBoxItemList.size)
        recyclerView.adapter = ToolBoxAdapter(toolBoxItemList, itemH, object : RecyclerItemClickListener {
            override fun onItemClick(item: ToolBoxItem) {
                when (item.type) {
                    ToolBoxItemType.Cloud -> {
                        Log.e(tag, "Cloud")
                    }
                    ToolBoxItemType.ExtApp -> {
                        Log.e(tag, "ExtApp")
                        if (item.tag.isNullOrEmpty()) {
                            AgoraLog.e("$tag->ExtApp appIdentifier is empty, please check.")
                        } else {
                            eduContext?.extAppContext()?.launchExtApp(item.tag)
                        }
                    }
                }
            }
        })
    }

    private fun calculateItemHeight(height: Int, count: Int): Int {
        val row = if (count % spanCount == 0) {
            count / spanCount
        } else {
            count / spanCount + 1
        }
        val itemH = (height - space * row) / row
        return itemH
    }

    private class SpaceItemDecoration(val space: Int, val spanCount: Int, dividerColor: Int) :
            RecyclerView.ItemDecoration() {
        private val paint: Paint = Paint()

        init {
            paint.isAntiAlias = true
            paint.color = dividerColor
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDrawOver(c, parent, state)
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val view = parent.getChildAt(i)
                val index = parent.getChildAdapterPosition(view)
                // 竖直
                var verDividerLeft = 0.0f
                //非第一列ItemView才需要绘制left
                if (index % spanCount != 0) {
                    verDividerLeft = (view.left - space).toFloat()
                }
                var verDividerTop: Float = view.top.toFloat()
                var verDividerBottom = view.bottom.toFloat()
                var verDividerRight: Float = view.left.toFloat()
                // 最后一个item但又填不满一行
                if(index == childCount - 1 && childCount % spanCount != 0) {
                    verDividerLeft = (view.right - space).toFloat()
                    verDividerRight = view.right.toFloat()
                    verDividerBottom = (view.bottom + space).toFloat()
                }
                c.drawRect(verDividerLeft, verDividerTop, verDividerRight, verDividerBottom, paint)
                // 横向
                var horDividerLeft = 0.0f
                val horDividerTop: Float = view.bottom.toFloat()
                val horDividerRight: Float = (view.right + space).toFloat()
                var horDividerBottom = (view.bottom + space).toFloat()
                var surplus = if(childCount % spanCount == 0) {
                    3
                } else {
                    childCount % spanCount
                }
                //最后一行ItemView不需要绘制left
                if (index in (childCount - surplus) until childCount) {
                    horDividerBottom = view.bottom.toFloat()
                }
                c.drawRect(horDividerLeft, horDividerTop, horDividerRight, horDividerBottom, paint)
            }
        }
    }

    private data class ToolBoxItem(val type: ToolBoxItemType, val icon: Int, val name: String,
                                   val tag: String? = null)

    private enum class ToolBoxItemType(val value: Int) {
        Cloud(0),
        ExtApp(1)
    }

    private inner class ToolBoxAdapter(val itemList: MutableList<ToolBoxItem>, val itemH: Int,
                                       val listener: RecyclerItemClickListener?) : RecyclerView.Adapter<ToolBoxItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolBoxItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_toolbox_item_layout, null)
            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemH)
            view.layoutParams = layoutParams
            return ToolBoxItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ToolBoxItemViewHolder, position: Int) {
            val item = toolBoxItemList[position]
            val bg = getItemBackground(position)
            holder.bind(item, bg, listener)
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        private fun getItemBackground(pos: Int): Int? {
            if (pos == 0) {
                return R.drawable.agora_toolbox_item_bg_left_top
            } else if (pos == 2) {
                return R.drawable.agora_toolbox_item_bg_right_top
            } else if (pos % spanCount == 0 && itemCount - pos < spanCount + 1) {
                return R.drawable.agora_toolbox_item_bg_left_bottom
            } else if (itemCount % spanCount == 0 && pos == itemCount - 1) {
                return R.drawable.agora_toolbox_item_bg_right_bottom
            }
            return null
        }
    }

    private interface RecyclerItemClickListener {
        fun onItemClick(item: ToolBoxItem)
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class ToolBoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout = itemView.findViewById<LinearLayout>(R.id.root_Layout)
        private val toolIcon = itemView.findViewById<AppCompatImageView>(R.id.toolIcon)
        private val toolName = itemView.findViewById<AppCompatTextView>(R.id.toolName)

        private fun refreshState(activated: Boolean) {
            rootLayout.isActivated = activated
            toolIcon.isActivated = activated
            toolName.isActivated = activated
        }

        fun bind(item: ToolBoxItem, bg: Int?, listener: RecyclerItemClickListener?) {
            bg?.let {
                rootLayout.setBackgroundResource(it)
            }
            toolIcon.setImageResource(item.icon)
            toolName.text = item.name
            rootLayout.setOnTouchListener(object : OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        ACTION_DOWN -> {
                            refreshState(true)
                            return true
                        }
                        ACTION_CANCEL -> {
                            refreshState(false)
                            return true
                        }
                        ACTION_UP -> {
                            refreshState(false)
                            listener?.onItemClick(item)
                            return true
                        }
                    }
                    return true
                }
            })
        }
    }
}