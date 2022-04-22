package com.agora.edu.component

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.FcrHandWavingLayoutBinding
import io.agora.agoraeduuikit.util.SvgaUtils

/**
 * author : cjw
 * date : 2022/4/1
 * description :
 * 挥手组件
 * hand waving component
 */
class FCRHandWavingComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrHandWavingLayoutBinding.inflate(
        LayoutInflater.from(this.context),
        this, true
    )
    private val svgaUtils = SvgaUtils(context, binding.handWavingImg)

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        svgaUtils.stopSVGA()
    }

    fun updateWaveState(waving: Boolean) {
        val runnable = Runnable {
            if (waving) {
                binding.handWavingImg.visibility = VISIBLE
                svgaUtils.initAnimator()
                svgaUtils.startAnimator(context?.getString(R.string.fcr_waving_hands))
            } else {
                binding.handWavingImg.visibility = GONE
                svgaUtils.stopSVGA()
            }
        }
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            runnable.run()
        } else {
            ContextCompat.getMainExecutor(context).execute(runnable)
        }
    }
}