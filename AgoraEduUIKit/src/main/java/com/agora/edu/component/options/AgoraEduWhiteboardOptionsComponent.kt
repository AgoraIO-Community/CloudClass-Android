package com.agora.edu.component.options

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.whiteboard.AgoraEduApplianceToolsPresenter
import io.agora.agoraeduuikit.databinding.AgoraEduWhiteboardOptionsComponentBinding

/**
 * author : felix
 * date : 2022/2/21
 * description : 白板工具操作
 */
class AgoraEduWhiteboardOptionsComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    val tag = "AgoraEduOptionsComponent"
    lateinit var itemContainer: ViewGroup
    lateinit var uuid: String
    private var binding: AgoraEduWhiteboardOptionsComponentBinding =
        AgoraEduWhiteboardOptionsComponentBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var applianceToolsPresenter: AgoraEduApplianceToolsPresenter
    private var isShowPenTextView = false
    var isShowApplianceView = false
    var boardIconClickListener: IWhiteBoardIconClickListener? = null
    fun initView(uuid: String, itemContainer: ViewGroup, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        this.itemContainer = itemContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)

        applianceToolsPresenter = AgoraEduApplianceToolsPresenter(
            binding,
            itemContainer,
            eduContext,
            uuid,
            agoraUIProvider
        )

        binding.optionItemPenTextG.setOnClickListener {
            // 显示设置画笔和文本
            if (isShowPenTextView) {
                hiddenItem()
                isShowPenTextView = false
            } else {
                applianceToolsPresenter.showPenTextView()
                isShowPenTextView = true
            }
        }

        binding.optionItemAppliance.setOnClickListener {
            // 显示菜单
            if (isShowApplianceView) {
                hiddenItem()
                isShowApplianceView = false
            } else {
                //其他按钮状态更新
                boardIconClickListener?.onWhiteboardIconClicked()
                applianceToolsPresenter.showApplianceView()
                isShowApplianceView = true
            }
        }
    }

    fun resetData(){
        applianceToolsPresenter = AgoraEduApplianceToolsPresenter(
            binding,
            itemContainer,
            eduContext,
            uuid,
            agoraUIProvider
        )
    }

    private fun showItem(item: View?) {
        itemContainer.removeAllViews()
        itemContainer.addView(item)
    }

    private fun hiddenItem() {
        itemContainer.removeAllViews()
    }
}

interface IWhiteBoardIconClickListener {
    fun onWhiteboardIconClicked()
}






















