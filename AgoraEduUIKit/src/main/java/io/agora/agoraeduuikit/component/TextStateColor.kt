package io.agora.agoraeduuikit.component

import android.content.res.ColorStateList
import android.graphics.Color

class TextStateColorBuilder {
    private var colorDefault = Color.TRANSPARENT
    private var colorPressed = Color.TRANSPARENT
    private var colorDisabled = Color.TRANSPARENT

    fun colorDefault(color: Int): TextStateColorBuilder {
        this.colorDefault = color
        return this
    }

    fun colorPressed(color: Int): TextStateColorBuilder {
        this.colorPressed = color
        return this
    }

    fun colorDisabled(color: Int): TextStateColorBuilder {
        this.colorDisabled = color
        return this
    }

    fun build(): ColorStateList {
        return ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_pressed),
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(android.R.attr.state_enabled)),
                intArrayOf(colorDefault,
                        colorPressed,
                        colorDisabled)
        )
    }
}