package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.GridLayoutManager
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.GridSpacingItemDecoration
import com.agora.edu.component.whiteboard.adpater.*
import com.agora.edu.component.whiteboard.color.AgoraEduWbColorAdapter
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListColor
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListPenShape
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListTextSize
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListThickness
import io.agora.agoraeduuikit.R

/**
 * author : felix
 * date : 2022/2/16
 * description : 笔和文字大小，颜色
 */
class AgoraEduPenTextComponent : AgoraEduBaseApplianceComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private lateinit var shapeToolsAdapter: AgoraEduToolsAdapter<AgoraEduPenShapeInfo>   // 形状
    private lateinit var thicknessSizeToolsAdapter: AgoraEduToolsAdapter<AgoraEduThicknessInfo> // 画笔粗细
    private lateinit var textSizeToolsAdapter: AgoraEduToolsAdapter<AgoraEduTextSizeInfo>  // 文字大小
    private lateinit var colorToolsAdapter: AgoraEduWbColorAdapter<AgoraEduPenColorInfo>     // 颜色

    var onTextListener: OnAgoraEduTextListener? = null
    var onPenListener: OnAgoraEduPenListener? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }

    // 记录位置
    /**
     * 形状的位置
     */
    var shapeSelectIndex = 0

    /**
     * 文本大小
     */
    var textSelectIndex = 2
    fun showPen() {
        divider2.visibility = View.VISIBLE
        bottomListView.visibility = View.VISIBLE

        centerListView.layoutManager = GridLayoutManager(context, LINE_SIZE_COUNT)
        for (i in 0 until centerListView.itemDecorationCount) {
            centerListView.removeItemDecorationAt(i)
        }
        centerListView.addItemDecoration(
            GridSpacingItemDecoration(
                LINE_SIZE_COUNT,
                resources.getDimensionPixelSize(R.dimen.agora_appliance_item_margin), true
            )
        )
        // 通过margin来对齐
        centerListView.setPadding(0,0,resources.getDimensionPixelSize(R.dimen.agora_appliance_pen_m_margin),0)

        // 形状
        shapeToolsAdapter = AgoraEduToolsAdapter(config)
        shapeToolsAdapter.setViewData(getListPenShape())
        shapeToolsAdapter.operationType = 2
        shapeToolsAdapter.selectPosition = shapeSelectIndex
        shapeToolsAdapter.onClickItemListener = { position, info ->
            shapeSelectIndex = position
            // 这里不能覆盖父类型
            //config.activeAppliance = info.activeAppliance
            // 这里是子菜单类型
            onPenListener?.onPenShapeSelected(config.activeAppliance, info.activeAppliance, info.iconRes)
        }
        topListView.adapter = shapeToolsAdapter

        // 粗细，圆形
        thicknessSizeToolsAdapter = AgoraEduToolsAdapter(config)
        thicknessSizeToolsAdapter.operationType = 3
        thicknessSizeToolsAdapter.selectPosition = getPenSize()
        thicknessSizeToolsAdapter.setViewData(getListThickness(context))
        thicknessSizeToolsAdapter.onClickItemListener = { position, info ->
            config.thickSize = info.size
            onPenListener?.onPenThicknessSelected(info.size, info.iconRes)
        }
        centerListView.adapter = thicknessSizeToolsAdapter

        // 颜色
        colorToolsAdapter = AgoraEduWbColorAdapter(config)
        colorToolsAdapter.setViewData(getListColor(context))
        colorToolsAdapter.selectPosition = getPenColorPosition()
        colorToolsAdapter.onClickItemListener = { position, info ->
            config.color = info.iconRes // 颜色存在iconRes
            onPenListener?.onPenColorSelected(info.iconRes, R.drawable.agora_wb_pen)
            // 需要更新：形状，圆圈，小矩形的颜色
            shapeToolsAdapter.notifyDataSetChanged()
            thicknessSizeToolsAdapter.notifyDataSetChanged()
        }
        bottomListView.adapter = colorToolsAdapter
    }

    fun showText() {
        divider2.visibility = View.GONE
        bottomListView.visibility = View.GONE

        centerListView.setPadding(0, 0, 0, 0)
        centerListView.layoutManager = GridLayoutManager(context, LINE_COUNT)
        for (i in 0 until centerListView.itemDecorationCount) {
            centerListView.removeItemDecorationAt(i)
        }
        centerListView.addItemDecoration(
            GridSpacingItemDecoration(
                LINE_COUNT,
                resources.getDimensionPixelSize(R.dimen.agora_appliance_item_margin), true
            )
        )

        textSelectIndex = getTSize()

        // 文字大小 T
        textSizeToolsAdapter = AgoraEduToolsAdapter(config)
        textSizeToolsAdapter.setViewData(getListTextSize(context))
        textSizeToolsAdapter.selectPosition = textSelectIndex
        textSizeToolsAdapter.onClickItemListener = { position, info ->
            textSelectIndex = position
            textSizeToolsAdapter.notifyDataSetChanged()
            config.fontSize = info.size
            onTextListener?.onTextSizeSelected(info.size, R.drawable.agora_wb_text)
        }
        topListView.adapter = textSizeToolsAdapter

        // 颜色
        colorToolsAdapter = AgoraEduWbColorAdapter(config)
        colorToolsAdapter.setViewData(getListColor(context))
        colorToolsAdapter.onClickItemListener = { position, info ->
            config.color = info.iconRes // 颜色存在iconRes
            onTextListener?.onTextColorSelected(info.iconRes, R.drawable.agora_wb_text)
            // 刷新 T 的颜色
            textSizeToolsAdapter.notifyDataSetChanged()
        }
        centerListView.adapter = colorToolsAdapter
    }

    fun getTSize(): Int {
        var pos = 2
        for ((index, value) in getListTextSize(context).withIndex()) {
            if (config.fontSize == value.size) {
                pos = index
                break
            }
        }

        return pos
    }

    /**
     * 选择的位置(可能没有在里面)
     */
    fun getPenSize(): Int {
        var pos = 1
        for ((index, value) in getListThickness(context).withIndex()) {
            if (config.thickSize == value.size) {
                pos = index
                break
            }
        }

        return pos
    }

    /**
     * 选择的位置(可能没有在里面)
     */
    fun getPenColorPosition(): Int {
        var pos = -1
        for ((index, value) in getListColor(context).withIndex()) {
            if (config.color == value.iconRes) {
                pos = index
                break
            }
        }

        return pos
    }
}