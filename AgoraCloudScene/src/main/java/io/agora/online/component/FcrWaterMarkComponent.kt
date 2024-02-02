package io.agora.online.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.online.databinding.FcrOnlineWaterMarkBinding

/**
 * author : felix
 * date : 2022/10/17
 * description : 水印
 */
class FcrWaterMarkComponent : AbsAgoraEduComponent {

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineWaterMarkBinding.inflate(LayoutInflater.from(this.context), this, true)

    init {
        val nickName = PreferenceManager.get(AppHostUtil.KEY_SP_NICKNAME, "")
        binding.fcrWaterMark.text = nickName
    }

    fun setNickName(nickName: String) {
        binding.fcrWaterMark.text = nickName
    }

    fun startScroll(){
        binding.fcrWaterMark.startScroll()
    }

    fun pauseScroll(){
        binding.fcrWaterMark.pauseScroll()
    }
}