package com.agora.edu.component

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeduuikit.databinding.FcrHandWavingLayoutBinding

/**
 * author : cjw
 * date : 2022/4/1
 * description :
 * 挥手组件
 * hand waving component
 */
class FcrHandWavingComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrHandWavingLayoutBinding.inflate(
        LayoutInflater.from(this.context),
        this, true
    )

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }

    fun updateWaveState(waving: Boolean) {
        val runnable = Runnable {
            if (waving) {
                binding.handWavingImg.visibility = VISIBLE
            } else {
                binding.handWavingImg.visibility = GONE
            }
        }
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            runnable.run()
        } else {
            ContextCompat.getMainExecutor(context).execute(runnable)
        }
    }
}