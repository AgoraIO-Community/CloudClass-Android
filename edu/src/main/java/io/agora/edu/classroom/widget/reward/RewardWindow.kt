package io.agora.edu.classroom.widget.reward

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.agora.edu.classroom.widget.window.AbstractWindow
import io.agora.edu.R

class RewardWindow : AbstractWindow {
    private val TAG = "RewardWindow"
    private lateinit var rewardImg: AppCompatImageView

    constructor(context: Context) : super(context) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    ) {
        view()
    }

    private fun view() {
        inflate(context, R.layout.reward_window_layout, this)
        rewardImg = findViewById(R.id.reward_Img)
        postDelayed({
            Log.e(TAG, "addView")
            Glide.with(context).asGif()
                    .load(R.drawable.reward_window_reward_anim).listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable?>?, isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            resource?.setLoopCount(1)
                            resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    postDelayed({ (parent as ViewGroup).removeView(this@RewardWindow) }, 50)
                                }
                            })
                            return false
                        }
                    }).into(rewardImg)
        }, 100)
    }
}