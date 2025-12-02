package com.agora.edu.component.options

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeduuikit.R

/**
 * author : wf
 * date : 2022/2/8 17:58 上午
 * description :
 */
class AgoraEduHandUpToastComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraEduHandUpToastComponent"

    @SuppressLint("InflateParams")
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_handup_toast_dialog_layout2, this, true)


    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }
}






















