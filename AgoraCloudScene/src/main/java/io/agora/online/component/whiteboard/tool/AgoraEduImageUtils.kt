package io.agora.online.component.whiteboard.tool

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

/**
 * author : felix
 * date : 2022/2/21
 * description :
 */
class AgoraEduImageUtils {
    companion object {

        fun setImageTintDrawable(iconDrawable: Drawable, iconTintColor: Int, imageView: AppCompatImageView) {
            // 避免影响其他icon
            val tintDrawable = DrawableCompat.wrap(iconDrawable).mutate()
            //DrawableCompat.setTint(tintDrawable, Color.parseColor("#000000"))
            DrawableCompat.setTint(iconDrawable, iconTintColor)
            imageView.setImageDrawable(tintDrawable)
        }

        fun setImageTintResource(iconRes: Int, iconTintColor: Int, imageView: AppCompatImageView) {
            val originalDrawable = ContextCompat.getDrawable(imageView.context, iconRes)
            // 避免影响其他icon
            val tintDrawable = DrawableCompat.wrap(originalDrawable!!).mutate()
            //DrawableCompat.setTint(tintDrawable, Color.parseColor("#000000"))
            DrawableCompat.setTint(tintDrawable, iconTintColor)
            imageView.setImageDrawable(tintDrawable)
        }

        fun setTextBgTintResource(iconRes: Int, iconTintColor: Int, view: TextView) {
            val originalDrawable = ContextCompat.getDrawable(view.context, iconRes)
            // 避免影响其他icon
            val tintDrawable = DrawableCompat.wrap(originalDrawable!!).mutate()
            //DrawableCompat.setTint(tintDrawable, Color.parseColor("#000000"))
            DrawableCompat.setTint(tintDrawable, iconTintColor)
            view.background = tintDrawable
        }
    }
}