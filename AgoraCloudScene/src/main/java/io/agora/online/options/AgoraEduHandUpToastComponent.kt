package io.agora.online.options

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.R

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
    private val layout = LayoutInflater.from(context).inflate(R.layout.fcr_online_handup_toast_dialog_layout2, this, true)


    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }
}






















