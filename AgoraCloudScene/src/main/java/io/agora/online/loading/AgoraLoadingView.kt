package io.agora.online.loading

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import io.agora.online.databinding.FcrOnlineLoadingViewBinding

/**
 * author : felix
 * date : 2022/3/1
 * description : 加载框,纯View
 */
class AgoraLoadingView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineLoadingViewBinding =
        FcrOnlineLoadingViewBinding.inflate(LayoutInflater.from(context), this, true)

    fun setLoadingMessage(message: String) {
        binding.agoraLoadingText.visibility = View.VISIBLE
        binding.agoraLoadingText.text = message
    }
}