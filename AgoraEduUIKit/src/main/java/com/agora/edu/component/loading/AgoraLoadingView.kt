package com.agora.edu.component.loading

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraLoadingViewBinding

/**
 * author : hefeng
 * date : 2022/3/1
 * description : 加载框,纯View
 */
class AgoraLoadingView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraLoadingViewBinding =
        AgoraLoadingViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        Glide.with(context).asGif().skipMemoryCache(true)
            .load(R.drawable.agora_ui_loading_img)
            .listener(object : RequestListener<GifDrawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<GifDrawable?>?, isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable?, model: Any?,
                    target: Target<GifDrawable?>?, dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.setLoopCount(GifDrawable.LOOP_FOREVER)
                    return false
                }
            }).into(binding.agoraLoadingImg)
    }

    fun setLoadingMessage(message: String) {
        binding.agoraLoadingText.visibility = View.VISIBLE
        binding.agoraLoadingText.text = message
    }

//    var rootView: ViewGroup? = null

//    fun show(rootView: ViewGroup) {
//        this.rootView = rootView
//        rootView.addView(this)
//    }
//
//    fun dismiss() {
//        rootView?.removeView(this)
//    }

//    fun show(){
//        visibility = View.VISIBLE
//    }
//
//    fun dismiss(){
//        visibility = View.GONE
//    }
}