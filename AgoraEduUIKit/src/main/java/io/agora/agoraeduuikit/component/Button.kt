package io.agora.agoraeduuikit.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import androidx.appcompat.widget.AppCompatButton

@SuppressLint("ViewConstructor")
class AgoraUIRoundRectButton(context: Context, width: Int, height: Int,
                             colorDefault: Int = Color.TRANSPARENT,
                             colorPressed: Int = Color.TRANSPARENT,
                             colorDisabled: Int = Color.TRANSPARENT,
                             textColorDefault: Int = Color.TRANSPARENT,
                             textColorPressed: Int = Color.TRANSPARENT,
                             textColorDisabled: Int = Color.TRANSPARENT,
                             corner: Int = 0,
                             borderWidth: Int = 0,
                             borderColorDefault: Int = Color.TRANSPARENT,
                             borderColorPressed: Int = Color.TRANSPARENT,
                             borderColorDisabled: Int = Color.TRANSPARENT) : AppCompatButton(context) {

    init {
        val stateDrawable = StateListDrawable()

        val defaultDrawable = GradientDrawable()
        val pressedDrawable = GradientDrawable()
        val disableDrawable = GradientDrawable()

        // If there is no corner set, use the default corner value that
        // is half of the smaller one of width and height
        val cornerValue = if (corner > 0) corner else width.coerceAtMost(height) / 2
        defaultDrawable.cornerRadius = cornerValue.toFloat()
        pressedDrawable.cornerRadius = cornerValue.toFloat()
        disableDrawable.cornerRadius = cornerValue.toFloat()

        defaultDrawable.setColor(colorDefault)

        pressedDrawable.setColor(
                if (colorPressed != Color.TRANSPARENT) colorPressed
                else colorDefault)

        disableDrawable.setColor(
                if (colorDisabled != Color.TRANSPARENT) colorDisabled
                else colorDefault)

        if (borderWidth > 0) {
            defaultDrawable.setStroke(borderWidth, borderColorDefault)
            pressedDrawable.setStroke(borderWidth, borderColorPressed)
            disableDrawable.setStroke(borderWidth, borderColorDisabled)
        }

        stateDrawable.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
        stateDrawable.addState(intArrayOf(-android.R.attr.state_enabled), disableDrawable)
        stateDrawable.addState(intArrayOf(android.R.attr.state_enabled), defaultDrawable)
        this.background = stateDrawable

        if (textColorDefault != Color.TRANSPARENT) {
            val press = if (textColorPressed != Color.TRANSPARENT) textColorPressed else textColorDefault
            val disabled = if (textColorDisabled != Color.TRANSPARENT) textColorDisabled else textColorDefault
            val stateArray = Array(3) { IntArray(1) }
            stateArray[1] = intArrayOf(android.R.attr.state_pressed)
            stateArray[2] = intArrayOf(-android.R.attr.state_enabled)
            stateArray[0] = intArrayOf(android.R.attr.state_enabled)
            val stateTextColor = ColorStateList(
                    stateArray,
                    intArrayOf(
                            press,
                            disabled,
                            textColorDefault)
            )
            this.setTextColor(stateTextColor)
        }
    }

}

class RoundRectButtonBuilder(private val context: Context) {
    private var width = 0
    private var height = 0
    private var corner = 0

    private var bgColorDefault = Color.TRANSPARENT
    private var bgColorPressed = Color.TRANSPARENT
    private var bgColorDisabled = Color.TRANSPARENT
    private var textColorDefault = Color.TRANSPARENT
    private var textColorPressed = Color.TRANSPARENT
    private var textColorDisabled = Color.TRANSPARENT
    private var borderColorDefault = Color.TRANSPARENT
    private var borderColorPressed = Color.TRANSPARENT
    private var borderColorDisabled = Color.TRANSPARENT
    private var borderWidth = 0

    fun width(width: Int): RoundRectButtonBuilder {
        this.width = width
        return this
    }

    fun height(height: Int): RoundRectButtonBuilder {
        this.height = height
        return this
    }

    fun corner(corner: Int): RoundRectButtonBuilder {
        this.corner = corner
        return this
    }

    fun colorDefault(color: Int): RoundRectButtonBuilder {
        this.bgColorDefault = color
        return this
    }

    fun colorPressed(color: Int): RoundRectButtonBuilder {
        this.bgColorPressed = color
        return this
    }

    fun colorDisabled(color: Int): RoundRectButtonBuilder {
        this.bgColorDisabled = color
        return this
    }

    fun textColorDefault(color: Int): RoundRectButtonBuilder {
        this.textColorDefault = color
        return this
    }

    fun textColorPressed(color: Int): RoundRectButtonBuilder {
        this.textColorPressed = color
        return this
    }

    fun textColorDisabled(color: Int): RoundRectButtonBuilder {
        this.textColorDisabled = color
        return this
    }

    fun borderWidth(width: Int): RoundRectButtonBuilder {
        this.borderWidth = width
        return this
    }

    fun borderColorDefault(color: Int): RoundRectButtonBuilder {
        this.borderColorDefault = color
        return this
    }

    fun borderColorPressed(color: Int): RoundRectButtonBuilder {
        this.borderColorPressed = color
        return this
    }

    fun borderColorDisabled(color: Int): RoundRectButtonBuilder {
        this.borderColorDisabled = color
        return this
    }

    fun build(): AgoraUIRoundRectButton {
        return AgoraUIRoundRectButton(
                context = context,
                width = width,
                height = height,
                colorDefault = bgColorDefault,
                colorPressed = bgColorPressed,
                colorDisabled = bgColorDisabled,
                textColorDefault = textColorDefault,
                textColorPressed = textColorPressed,
                textColorDisabled = textColorDisabled,
                corner = corner,
                borderWidth = borderWidth,
                borderColorDefault = borderColorDefault,
                borderColorPressed = borderColorPressed,
                borderColorDisabled = borderColorDisabled)
    }
}

class RoundRectButtonStateBg(width: Int, height: Int,
                             colorDefault: Int, strokeColorDefault: Int,
                             colorPressed: Int, strokeColorPressed: Int,
                             colorDisabled: Int, strokeColorDisabled: Int,
                             strokeWidth: Int): StateListDrawable() {
    init {
        val default = GradientDrawable()
        default.shape = GradientDrawable.RECTANGLE
        default.setSize(width, height)
        default.cornerRadius = height / 2f
        default.setStroke(strokeWidth, strokeColorDefault)
        default.setColor(colorDefault)

        val pressed = GradientDrawable()
        pressed.shape = GradientDrawable.RECTANGLE
        pressed.setSize(width, height)
        pressed.cornerRadius = height / 2f
        pressed.setStroke(strokeWidth, strokeColorPressed)
        pressed.setColor(colorPressed)

        val disabled = GradientDrawable()
        disabled.shape = GradientDrawable.RECTANGLE
        disabled.setSize(width, height)
        disabled.cornerRadius = height / 2f
        disabled.setStroke(strokeWidth, strokeColorDisabled)
        disabled.setColor(colorDisabled)

        addState(intArrayOf(android.R.attr.state_pressed), pressed)
        addState(intArrayOf(-android.R.attr.state_enabled), disabled)
        addState(intArrayOf(android.R.attr.state_enabled), default)
    }
}

class RectBackgroundBuilder(
        private val width: Int = 0,
        private val height: Int = 0,
        private val color: Int = Color.TRANSPARENT,
        private val strokeWidth: Int = 0,
        private val strokeColor: Int = Color.TRANSPARENT,
        private val corner: Int = 0) {

    fun build(): GradientDrawable {
        val drawable = GradientDrawable()
        if (width > 0 && height > 0) {
            drawable.setSize(width, height)
        }

        drawable.setColor(color)
        drawable.setStroke(strokeWidth, strokeColor)
        drawable.cornerRadius = corner.toFloat()
        return drawable
    }
}