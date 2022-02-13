package io.agora.agoraeduuikit.impl.tool

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Whiteboard appliance types
 */
enum class AgoraUIApplianceType {
    Select, Pen, Rect, Circle, Line, Eraser, Text, Clicker, Laser;
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

    fun makeCircleDrawable(color: Int, size: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            this.setColor(color)
            size?.let { this.setSize(it, it) }
        }
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