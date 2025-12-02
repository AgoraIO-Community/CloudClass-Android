package com.agora.edu.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.agora.edu.component.common.AbsAgoraEduComponent
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeduuikit.databinding.FcrWaterMarkBinding

/**
 * author : felix
 * date : 2022/10/17
 * description : 水印
 */
class FcrWaterMarkComponent : AbsAgoraEduComponent {

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrWaterMarkBinding.inflate(LayoutInflater.from(this.context), this, true)

    init {
        val nickName = PreferenceManager.get(AppHostUtil.KEY_SP_NICKNAME, "")
        binding.fcrWaterMark.text = nickName
    }

    fun setNickName(nickName: String) {
        binding.fcrWaterMark.text = nickName
    }

    fun startScroll() {
        binding.fcrWaterMark.startScroll()
    }

    fun pauseScroll() {
        binding.fcrWaterMark.pauseScroll()
    }
}