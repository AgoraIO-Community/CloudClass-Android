package com.agora.edu.component

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast

/**
 * author : cjw
 * date : 2022/3/1
 * description :
 */
class AgoraEduRewardWindow(context: Context, var userName: String) : Dialog(context, R.style.agora_full_screen_dialog) {
    private var rewardImg: AppCompatImageView
    private var mediaPlayer: MediaPlayer? = null

    init {
        setContentView(R.layout.agora_edu_reward_window)
        val window = this.window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        rewardImg = findViewById(R.id.reward_Img)
    }

    override fun show() {
        super.show()
        AgoraUIToast.info(
            context, text = String.format(
                context.resources.getString(R.string.fcr_user_congratulation),
                userName
            )
        )
        Glide.with(rewardImg.context).asGif().skipMemoryCache(true)
            .load(R.drawable.agora_reward_anim)
            .listener(object : RequestListener<GifDrawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<GifDrawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable?, model: Any?,
                    target: Target<GifDrawable?>?, dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.setLoopCount(1)
                    resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            dismiss()
                        }
                    })
                    return false
                }
            }).into(rewardImg)
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer.create(rewardImg.context.applicationContext, R.raw.agora_reward_sound)
        mediaPlayer!!.setOnCompletionListener {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        this.mediaPlayer!!.start()
    }
}

