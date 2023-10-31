package io.agora.online.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.*
import io.agora.online.databinding.FcrOnlineEduScreenShareComponetBinding

/*
* 屏幕分享Component
* */
class AgoraEduScreenShareComponent : AbsAgoraEduComponent, View.OnTouchListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineEduScreenShareComponetBinding =
        FcrOnlineEduScreenShareComponetBinding.inflate(LayoutInflater.from(context), this, true)
    private val cardView: CardView = binding.cardView

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }

    fun updateScreenShareState(state: EduContextScreenShareState, info: AgoraEduContextStreamInfo) {
        ContextCompat.getMainExecutor(context).execute {
            val sharing = state == EduContextScreenShareState.Start
            binding.cardView.visibility = if (sharing) VISIBLE else GONE

            val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid

            if (sharing) {
                if (info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
                    cardView.setOnTouchListener(this)
                    eduContext?.mediaContext()?.startRenderVideo(
                        EduContextRenderConfig(renderMode = EduContextRenderMode.FIT),
                        binding.screenShareContainerLayout,
                        info.streamUuid
                    )
                } else {
                    cardView.setOnTouchListener(null)
                    eduContext?.mediaContext()?.stopRenderVideo(info.streamUuid)
                }

                roomUuid?.let {
                    if (info.audioSourceState == AgoraEduContextMediaSourceState.Open) {
                        eduCore?.eduContextPool()?.mediaContext()?.startPlayAudio(roomUuid, info.streamUuid)
                    } else {
                        eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, info.streamUuid)
                    }
                }
            } else {
                cardView.setOnTouchListener(null)
                eduContext?.mediaContext()?.stopRenderVideo(info.streamUuid)
                eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid!!, info.streamUuid)
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return true
    }
}