package io.agora.online.component.whiteboard

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.component.whiteboard.data.AgoraEduApplianceData
import io.agora.online.component.whiteboard.data.AgoraEduApplianceData.Companion.getToolResImage
import io.agora.online.component.whiteboard.tool.AgoraEduImageUtils
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduWhiteboardOptionsComponentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardDrawingMemberState
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.online.impl.whiteboard.bean.WhiteboardApplianceType
import io.agora.online.interfaces.protocols.AgoraUIDrawingConfig


/**
 * author : felix
 * date : 2022/2/9
 * description : 白板操作
 */
class AgoraEduApplianceToolsPresenter(
    var binding: FcrOnlineEduWhiteboardOptionsComponentBinding,
    var container: ViewGroup,
    var eduContext: EduContextPool?,
    var uuid: String,
    var agoraUIProvider: IAgoraUIProvider
) {
    val TAG = "AgoraEduWhiteBoardToolsPresenter"
    var context = container.context

    private var config = AgoraUIDrawingConfig()

    private val defaultTintColor = ContextCompat.getColor(context, R.color.agora_def_color)
    private var penTextComponent: AgoraEduPenTextComponent? = null
    private var applianceComponent: AgoraEduApplianceComponent? = null

    private val whiteBoardObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet.signal == AgoraBoardInteractionSignal.MemberStateChanged) {
                val configArgs: AgoraUIDrawingConfig? = Gson().fromJson(
                    packet.body.toString(),
                    AgoraUIDrawingConfig::class.java
                )
                configArgs?.let {
                    setConfigData(it)
                }
            }
        }
    }

    private fun updateBoardMemberState(state: AgoraBoardDrawingMemberState) {
        val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.MemberStateChanged, state)
        eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
    }

    init {
        // 底部居中
        if (container is FrameLayout) {
            val cc: FrameLayout = container as FrameLayout
            val layoutParams = cc.layoutParams as LinearLayoutCompat.LayoutParams
            layoutParams.gravity = Gravity.BOTTOM
            cc.layoutParams = layoutParams
        }

        // 设置数据
        setConfigData(config)

        //eduContext?.widgetContext()?.addWidgetMessageObserver(whiteBoardObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    /**
     * 设置配置数据
     */
    fun setConfigData(config: AgoraUIDrawingConfig) {
        this.config.set(config)

        when (config.activeAppliance) {
            WhiteboardApplianceType.Pen -> {
                switchApplianceOptions(false)
                switchIconOrText(false)
            }

            WhiteboardApplianceType.Text -> {
                switchApplianceOptions(false)
                switchIconOrText(true)
            }

            else -> {
                switchApplianceOptions(true)
            }
        }
    }

    var penShapeType: WhiteboardApplianceType = WhiteboardApplianceType.PenS

    fun showPenTextView() {
        if (penTextComponent == null) {
            penTextComponent = AgoraEduPenTextComponent(context)
            penTextComponent?.onTextListener = object : OnAgoraEduTextListener {
                override fun onTextSizeSelected(size: Int, iconRes: Int) {
                    setConfigData(config)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(textSize = size))
                }

                override fun onTextColorSelected(color: Int, iconRes: Int) {
                    setConfigData(config)
                    updateApplianceImage(color)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(strokeColor = color))
                }
            }

            penTextComponent?.onPenListener = object : OnAgoraEduPenListener {
                override fun onPenShapeSelected(
                    parentType: WhiteboardApplianceType,
                    childShapeType: WhiteboardApplianceType,
                    iconRes: Int
                ) {
                    penShapeType = childShapeType
                    setConfigData(config)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(activeApplianceType = childShapeType))
                }

                override fun onPenThicknessSelected(thick: Int, iconRes: Int) {
                    setConfigData(config)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(strokeWidth = thick))
                }

                override fun onPenColorSelected(color: Int, iconRes: Int) {
                    setConfigData(config)
                    updateApplianceImage(color)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(strokeColor = color))
                }
            }
        }
        penTextComponent?.setApplianceConfig(config)
        if (config.activeAppliance == WhiteboardApplianceType.Text) {
            penTextComponent?.showText()
        } else if (config.activeAppliance == WhiteboardApplianceType.Pen) {
            penTextComponent?.showPen()
        }
        container.removeAllViews()
        container.addView(penTextComponent)
    }

    fun showApplianceView() {
        if (applianceComponent == null) {
            applianceComponent = AgoraEduApplianceComponent(context)
            applianceComponent?.initView(uuid, agoraUIProvider)
            applianceComponent?.onApplianceListener = object : OnAgoraEduApplianceListener {
                override fun onToolsSelected(toolType: WhiteboardApplianceType, iconRes: Int) {
                    dismiss()
                }

                override fun onApplianceSelected(type: WhiteboardApplianceType, iconRes: Int) {
                    when (type) {
                        WhiteboardApplianceType.WB_Clear -> {
                        }
                        WhiteboardApplianceType.WB_Pre -> {
                        }
                        WhiteboardApplianceType.WB_Next -> {
                        }
                        else -> {
                            // 切回画笔,画笔类型和画笔用了一个key
                            penShapeType = WhiteboardApplianceType.PenS
                            config.activeAppliance = type

                            dismiss()
                        }
                    }
                    setConfigData(config)
                    updateBoardMemberState(AgoraBoardDrawingMemberState(activeApplianceType = type))
                }
            }
        }
        applianceComponent?.setApplianceConfig(config)

        // 判断是老师端还是学生端
        if(eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher){
            applianceComponent?.show(true)
        }else{
            applianceComponent?.show(false)
        }

        container.removeAllViews()
        container.addView(applianceComponent)
    }

    fun dismiss() {
        container.removeAllViews()
    }

    /**
     * 切换是几个按钮
     */
    fun switchApplianceOptions(isSingle: Boolean) {
        val layoutParams = binding.optionItemApplianceGroup.layoutParams
        val applianceParams = binding.optionItemAppliance.layoutParams as LinearLayout.LayoutParams

        if (isSingle) {
            binding.optionItemPenTextG.visibility = View.GONE
            binding.optionItemWbLine.visibility = View.GONE
            layoutParams.height = context.resources.getDimensionPixelSize(R.dimen.agora_option_item_w)
            applianceParams.setMargins(0, 0, 0, 0)
        } else {
            binding.optionItemPenTextG.visibility = View.VISIBLE
            binding.optionItemWbLine.visibility = View.VISIBLE
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            applianceParams.setMargins(
                0, 0, 0,
                context.resources.getDimensionPixelSize(R.dimen.agora_option_item_appliance_bottom)
            )
        }
        binding.optionItemApplianceGroup.layoutParams = layoutParams
        binding.optionItemAppliance.layoutParams = applianceParams

        // 设置数据
        agoraUIProvider.getAgoraEduCore()?.config?.roomType?.let {
            val resId = getToolResImage(it, config.activeAppliance)
            setApplianceImage(resId)
        }
    }

    /**
     * 设置底部按钮icon变化
     */
    fun setApplianceImage(resId: Int?) {
        resId?.let {
            if (isNeedUseSetColor()) {
                AgoraEduImageUtils.setImageTintResource(resId, config.color, binding.optionItemAppliance)

            } else {
                AgoraEduImageUtils.setImageTintResource(resId, defaultTintColor, binding.optionItemAppliance)
            }
        }
    }

    /**
     * 画笔和Text需要变更颜色，其他的便会蓝色默认颜色
     */
    fun updateApplianceImage(iconColor: Int) {
        val drawable = binding.optionItemAppliance.drawable
        AgoraEduImageUtils.setImageTintDrawable(drawable, iconColor, binding.optionItemAppliance)
    }

    /**
     * 画笔和文本可以单独设置颜色
     */
    private fun isNeedUseSetColor(): Boolean {
        return when (config.activeAppliance) {
            WhiteboardApplianceType.Text,
            WhiteboardApplianceType.Star,
            WhiteboardApplianceType.Pen,
            WhiteboardApplianceType.PenS,
            WhiteboardApplianceType.Rhombus,
            WhiteboardApplianceType.Line,
            WhiteboardApplianceType.Rect,
            WhiteboardApplianceType.Circle -> true
            else -> false
        }
    }

    fun switchIconOrText(isText: Boolean) {
        if (isText) {
            binding.optionItemWbPenColor.visibility = View.GONE
            // 设置给画笔的大小，依据double了
            binding.optionItemPenText.text = "" + config.fontSize / 2
            binding.optionItemPenText.setBackgroundColor(Color.TRANSPARENT)
        } else {
            binding.optionItemWbPenColor.visibility = View.VISIBLE
            binding.optionItemPenText.text = ""
            binding.optionItemWbPenColor.setBackgroundColor(config.color)
            binding.optionItemPenText.setBackgroundResource(getPenShapeIconRes())
            /*AgoraEduImageUtils.setTextBgTintResource(
                getPenShapeIconRes(),
                config.color,
                binding.optionItemPenText
            )*/
        }
    }

    fun getPenShapeIconRes(): Int {
        var pos = R.drawable.agora_wb_s
        for (value in AgoraEduApplianceData.getListPenShape()) {
            if (penShapeType == value.activeAppliance) {
                pos = value.iconRes
                break
            }
        }
        return pos
    }
}