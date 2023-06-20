package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.helper.GridSpacingItemDecoration
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduApplicanceComponetBinding
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig

/**
 * author : felix
 * date : 2022/2/16
 * description : 白板教具和教室工具
 */
open class AgoraEduBaseApplianceComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraEduApplicanceComponetBinding =
        AgoraEduApplicanceComponetBinding.inflate(LayoutInflater.from(context), this, true)

    var topListView: RecyclerView = binding.agoraOptionsTopList         //  顶部
    var centerListView: RecyclerView = binding.agoraOptionsCenterList   //  中间
    var bottomListView: RecyclerView = binding.agoraOptionsBottomList   //  底部

    var divider1: View = binding.agoraOptionDivider1
    var divider2: View = binding.agoraOptionDivider2

    protected lateinit var config: AgoraUIDrawingConfig

    val LINE_COUNT = 4
    val LINE_SIZE_COUNT = 5

    init {
        initBase()
    }

    fun setApplianceConfig(config: AgoraUIDrawingConfig) {
        this.config = config
    }

    fun initBase() {
        topListView.layoutManager = GridLayoutManager(context, LINE_COUNT)
        centerListView.layoutManager = GridLayoutManager(context, LINE_COUNT)
        bottomListView.layoutManager = GridLayoutManager(context, LINE_COUNT)

        topListView.addItemDecoration(
            GridSpacingItemDecoration(
                LINE_COUNT,
                resources.getDimensionPixelSize(R.dimen.agora_appliance_item_margin), true
            )
        )
        centerListView.addItemDecoration(
            GridSpacingItemDecoration(
                LINE_COUNT,
                resources.getDimensionPixelSize(R.dimen.agora_appliance_item_margin), true
            )
        )

        bottomListView.addItemDecoration(
            GridSpacingItemDecoration(
                LINE_COUNT,
                resources.getDimensionPixelSize(R.dimen.agora_appliance_item_margin), true
            )
        )
    }
}