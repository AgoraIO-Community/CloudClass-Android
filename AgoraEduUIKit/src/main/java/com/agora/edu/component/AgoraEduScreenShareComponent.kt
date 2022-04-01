package com.agora.edu.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.cardview.widget.CardView
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.context.EduContextRenderMode
import io.agora.agoraeducore.core.context.EduContextScreenShareState
import io.agora.agoraeduuikit.databinding.AgoraEduScreenShareComponetBinding

/*
* 屏幕分享Component
* */
class AgoraEduScreenShareComponent : AbsAgoraEduComponent, View.OnTouchListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraEduScreenShareComponetBinding = AgoraEduScreenShareComponetBinding.inflate(LayoutInflater.from(context), this, true)
    private val cardView: CardView = binding.cardView

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
//        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_screen_share_view_corner)
//        binding.cardView.radius = radius.toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun updateScreenShareState(state: EduContextScreenShareState, streamUuid: String) {
        uiHandler.post {
            val sharing = state == EduContextScreenShareState.Start
            binding.cardView.visibility = if (sharing) VISIBLE else GONE

            if (sharing) {
                cardView.setOnTouchListener(this)
                eduContext?.mediaContext()?.startRenderVideo(
                    EduContextRenderConfig(renderMode = EduContextRenderMode.FIT),  binding.screenShareContainerLayout, streamUuid
                )
            } else {
                cardView.setOnTouchListener(null)
                eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return true
    }
}