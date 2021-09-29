package io.agora.agoraeduuikit.impl.tool

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import io.agora.agoraeduextapp.AgoraExtAppEngine
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig

class AgoraUIToolItem(
        val type: AgoraUIToolItemType,
        val iconRes: Int,
        val hasPopup: Boolean)

/**
 * Item types existing in the tool list
 */
enum class AgoraUIToolItemType {
    Select, Pen, Rect, Circle, Line, Color, Clicker, Text, Eraser, Toolbox, Roster;
}

/**
 * Whiteboard appliance types
 */
enum class AgoraUIApplianceType {
    Select, Pen, Rect, Circle, Line, Eraser, Text, Clicker;
}

enum class AgoraUIToolType {
    All, Whiteboard
}

object AgoraUIToolItemList {
    val emptyList = mutableListOf<AgoraUIToolItem>()

    //获取白板工具栏
    fun getWhiteboardList(config: AgoraUIDrawingConfig): MutableList<AgoraUIToolItem> {

        var toolItemType = getToolItemType(config.activeAppliance)
        var toolItemRes = judgePicRes(toolItemType)
        //进入房间后，设置初始状态
        when (config.activeAppliance) {
            AgoraUIApplianceType.Select, AgoraUIApplianceType.Eraser,
            AgoraUIApplianceType.Text, AgoraUIApplianceType.Clicker -> {
                toolItemType = getToolItemType(AgoraUIApplianceType.Pen)
                toolItemRes = R.drawable.agora_tool_icon_pen
            }
        }

        return if (AgoraExtAppEngine.getRegisteredExtApps().isNotEmpty()) {
            mutableListOf(
                    AgoraUIToolItem(AgoraUIToolItemType.Clicker, R.drawable.agora_tool_icon_clicker, false),
                    AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
                    AgoraUIToolItem(toolItemType, toolItemRes, true),//change the ui of the toolbar，pen rect ...
                    AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false)
                    // AgoraUIToolItem(AgoraUIToolItemType.Toolbox, R.drawable.agora_tool_icon_toolbox, true)
            )
        } else {
            mutableListOf(
                    AgoraUIToolItem(AgoraUIToolItemType.Clicker, R.drawable.agora_tool_icon_clicker, false),
                    AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
                    AgoraUIToolItem(toolItemType, toolItemRes, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false))
        }
    }

    fun getAllItemList(config: AgoraUIDrawingConfig): MutableList<AgoraUIToolItem> {

        var toolItemType = getToolItemType(config.activeAppliance)
        var toolItemRes = judgePicRes(toolItemType)
        //进入房间后，设置初始状态
        when (config.activeAppliance) {
            AgoraUIApplianceType.Select, AgoraUIApplianceType.Eraser,
            AgoraUIApplianceType.Text, AgoraUIApplianceType.Clicker -> {
                toolItemType = getToolItemType(AgoraUIApplianceType.Pen)
                toolItemRes = R.drawable.agora_tool_icon_pen
            }
        }
        return if (AgoraExtAppEngine.getRegisteredExtApps().isNotEmpty()) {
            mutableListOf(
                    AgoraUIToolItem(AgoraUIToolItemType.Clicker, R.drawable.agora_tool_icon_clicker, false),
                    AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
                    AgoraUIToolItem(toolItemType, toolItemRes, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false),
                    // AgoraUIToolItem(AgoraUIToolItemType.Toolbox, R.drawable.agora_tool_icon_toolbox, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))
        } else {
            mutableListOf(
                    AgoraUIToolItem(AgoraUIToolItemType.Clicker, R.drawable.agora_tool_icon_clicker, false),
                    AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
                    AgoraUIToolItem(toolItemType, toolItemRes, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false),
                    AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))
        }
    }

    //根据type选择icon
    private fun judgePicRes(agoraUIToolItemType: AgoraUIToolItemType): Int {
        return when (agoraUIToolItemType) {
            AgoraUIToolItemType.Pen -> R.drawable.agora_tool_icon_pen
            AgoraUIToolItemType.Rect -> R.drawable.agora_tool_icon_rect
            AgoraUIToolItemType.Circle -> R.drawable.agora_tool_icon_circle_ring
            AgoraUIToolItemType.Line -> R.drawable.agora_tool_icon_line
            //初始状态，返回默认pen；重新登陆的时候，返回默认pen；
            else -> R.drawable.agora_tool_icon_pen
        }
    }

    private fun getToolItemType(appliance: AgoraUIApplianceType): AgoraUIToolItemType {
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

    fun getRosterOnlyList(): MutableList<AgoraUIToolItem> {
        return if (AgoraExtAppEngine.getRegisteredExtApps().isNotEmpty()) {
            mutableListOf(
//                    AgoraUIToolItem(AgoraUIToolItemType.Toolbox, R.drawable.agora_tool_icon_toolbox, true),
                    AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))
        } else {
            mutableListOf(AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))
        }
    }
}

object ColorOptions {
    fun makeColorOptions(colorStrings: Array<String>,
                         selectColors: Array<String>,
                         borderColors: Array<String>,
                         iconSize: Int, borderWidth: Int): List<StateListDrawable> {
        return List(colorStrings.size) { index ->
            makeStateList(
                    makeDrawable(Color.parseColor(colorStrings[index]), iconSize,
                            Color.parseColor(selectColors[index]), borderWidth,
                            Color.WHITE, borderWidth * 2),

                    makeDrawable(Color.parseColor(colorStrings[index]), iconSize,
                            Color.TRANSPARENT, borderWidth,
                            Color.parseColor(borderColors[index]), borderWidth * 2)
            )
        }
    }

    fun makeDrawable(color: Int, size: Int, selectColor: Int, selectWidth: Int,
                     borderColor: Int, borderWidth: Int): LayerDrawable {
        val select = GradientDrawable()
        select.shape = GradientDrawable.OVAL
        select.setSize(size, size)
        select.setColor(selectColor)

        val border = GradientDrawable()
        border.shape = GradientDrawable.OVAL
        border.setSize(size, size)
        border.setColor(borderColor)

        val solid = GradientDrawable()
        solid.shape = GradientDrawable.OVAL
        solid.setSize(size, size)
        solid.setColor(color)

        val drawable = LayerDrawable(arrayOf(select, border, solid))
        drawable.setLayerInset(1, selectWidth, selectWidth, selectWidth, selectWidth)
        drawable.setLayerInset(2, borderWidth, borderWidth, borderWidth, borderWidth)

        return drawable
    }

    private fun makeStateList(active: Drawable, default: Drawable): StateListDrawable {
        val state = StateListDrawable()
        state.addState(intArrayOf(
                android.R.attr.state_activated,
                android.R.attr.state_enabled), active)
        state.addState(intArrayOf(android.R.attr.state_enabled), default)
        return state
    }
}