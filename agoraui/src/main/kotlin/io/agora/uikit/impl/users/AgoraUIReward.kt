package io.agora.uikit.impl.users

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.agora.uikit.R
import io.agora.uikit.impl.AbsComponent

class AgoraUIReward(
        context: Context,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int
) : AbsComponent() {
    private val tag = "AgoraUIReward"

    private val contentView = LayoutInflater.from(context).inflate(R.layout.agora_reward_layout, parent, false)
    private val rewardImg: AppCompatImageView = contentView.findViewById(R.id.reward_Img)
    private var mediaPlayer: MediaPlayer? = null

    init {
        parent.addView(contentView, width, height)

        val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.width = width
        params.height = height
        params.topMargin = top
        params.leftMargin = left
        contentView.layoutParams = params
        contentView.visibility = GONE
        contentView.z = Float.MAX_VALUE
    }

    fun show() {
        contentView.post {
            contentView.visibility = VISIBLE
            Glide.with(contentView.context).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_reward_anim)
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?,
                                                  target: Target<GifDrawable?>?,
                                                  isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(resource: GifDrawable?, model: Any?,
                                                     target: Target<GifDrawable?>?, dataSource: DataSource?,
                                                     isFirstResource: Boolean): Boolean {
                            resource?.setLoopCount(1)
                            resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    contentView.visibility = GONE
                                }
                            })
                            return false
                        }
                    }).into(rewardImg)
        }

        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer.create(contentView.context.applicationContext, R.raw.agora_reward_sound)
        mediaPlayer!!.setOnCompletionListener {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        this.mediaPlayer!!.start()
    }

    fun isShowing(): Boolean {
        return contentView.visibility == VISIBLE
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }
}