package io.agora.online.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import io.agora.online.R


object AppUtil {
    /**
     * 防止按钮连续点击
     */
    private var lastClickTime: Long = 0

    @Synchronized
    fun isFastClick(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < 700) {
            return true
        }
        lastClickTime = time
        return false
    }

    @Synchronized
    fun isFastClick(interval: Long): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < interval) {
            return true
        }
        lastClickTime = time
        return false
    }

    fun isVisibleToUser(view: View, rootParentId: Int): Boolean {
        var visible = view.visibility
        while (true) {
            val parent = view.parent
            if (parent !is ViewGroup) {
                return false
            }
            if (parent.id != rootParentId) {
                visible = parent.visibility
            } else {
                break
            }
        }
        return visible == VISIBLE
    }

    fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun extractInitials(name: String): String {
        val initials = StringBuilder()
        val parts = name.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val multiLanguages =
            name.matches(Regex("\\p{L}+")) // TODO(HAI_GUO)MUST include language detector
        for (part in parts) {
            if (multiLanguages) { // Check if the part contains only letters
                initials.append(Character.toUpperCase(part[0]))
                break
            } else {
                initials.append(
                    Character.toUpperCase(
                        part.substring(
                            0,
                            1.coerceAtMost(part.length)
                        )[0]
                    )
                )
            }
            initials.append(" ")
        }
        return initials.toString().trim { it <= ' ' }
    }

    private val colorList =
        arrayOf(
            R.color.theme_text_color_orange_red,
            R.color.theme_text_color_gray,
            R.color.theme_blue_light,
            R.color.fcr_purple_500,
            R.color.theme_blue_gray
        )

    fun generateRoundBitmap(context: Context, name: String, size: Float): Bitmap? {
        // Create a mutable bitmap with the desired size and an alpha channel
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)

        // Create a canvas to draw on the bitmap
        val canvas = Canvas(bitmap)

        // Clear the canvas with a transparent color
        canvas.drawColor(
            Color.TRANSPARENT, PorterDuff.Mode.CLEAR
        )

        // Set up the paint for drawing the circle
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        circlePaint.color = context.getColor(
//            colorList[Random(System.currentTimeMillis()).nextFloat().times(colorList.size).toInt()]
//        )

        circlePaint.color = context.resources.getColor(R.color.theme_blue_light)

        // Calculate the radius of the circle
        val radius = size / 2f

        // Draw the circle on the canvas
        canvas.drawCircle(radius, radius, radius, circlePaint)

        // Set up the paint for drawing text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = size / 2
        textPaint.textAlign = Paint.Align.CENTER

        // Calculate the coordinates for drawing text in the center of the bitmap
        val x = size / 2f
        val y = (size - textPaint.descent() - textPaint.ascent()) / 2f

        // Draw the name text on the canvas
        canvas.drawText(name, x, y, textPaint)
        return bitmap
    }

    fun generateRoundRectBitmap2(context: Context, name: String?, size: Float): Bitmap? {
        // Create a mutable bitmap with the desired size and an alpha channel
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)

        // Create a canvas to draw on the bitmap
        val canvas = Canvas(bitmap)

        // Clear the canvas with a transparent color
        canvas.drawColor(
            context.getColor(R.color.theme_text_color_orange_red), PorterDuff.Mode.SRC
        )

        // Calculate the radius of the circle
        val radius = size / 2f

        // Set up the paint for drawing text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = size / 3
        textPaint.textAlign = Paint.Align.CENTER

        // Calculate the coordinates for drawing text in the center of the bitmap
        val x = size / 2f
        val y = (size - textPaint.descent() - textPaint.ascent()) / 2f

        // Create a rounded rectangle shape
        val rect = RectF(0f, 0f, size, size)
        val path = Path()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)

        // Set the shape as a clipping path on the canvas
        canvas.clipPath(path)
        // Draw the name text on the canvas
        canvas.drawText(name!!, x, y, textPaint)
        return bitmap
    }
}