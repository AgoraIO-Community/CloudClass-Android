package com.agora.edu.component

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.children
import com.agora.edu.component.common.AbsAgoraEduLinearComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.databinding.AgoraEduCarouselControlComponentBinding

/**
 * author : cjw
 * date : 2022/3/11
 * description : 老师控制上台的轮播组件
 * carousel component for teacher control student staging or not.
 */
class AgoraEduCarouselControlComponent : AbsAgoraEduLinearComponent, View.OnClickListener {
    private val tag = "AgoraEduCarouselControlComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val bind = AgoraEduCarouselControlComponentBinding.inflate(LayoutInflater.from(context))

    private val defaultCarouselInterval = 20
    private val defaultCarouselCount = 6

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            isEnabled = true
            eduContext?.userContext()?.getCoHostCarousel()?.let {
                isActivated = it.state
            }
        }
    }

    init {
        orientation = HORIZONTAL
        val childrens = bind.root.children.toList()
        childrens.forEach {
            bind.root.removeView(it)
            this.addView(it)
        }
        setOnClickListener(this)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.roomContext()?.addHandler(roomHandler)
        isEnabled = false
        if (eduContext?.userContext()?.getLocalUserInfo()?.role != AgoraEduContextUserRole.Teacher) {
            visibility = GONE
        }
    }

    override fun release() {
        super.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
    }

    override fun onClick(v: View?) {
        updateActiveState(!isActivated)
        if (isActivated) {
            startCarousel()
        } else {
            stopCarousel()
        }
    }

    private fun updateActiveState(activated: Boolean) {
        if (Looper.getMainLooper().thread.id == Thread.currentThread().id) {
            isActivated = activated
        } else {
            handler.post {
                isActivated = activated
            }
        }
    }

    private fun startCarousel() {
        eduContext?.userContext()
            ?.startCoHostCarousel(interval = defaultCarouselInterval, count = defaultCarouselCount,
//            ?.startCoHostCarousel(interval = 10, count = 2, type = Sequence, condition = CameraOpened,
                callback = object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                        LogX.i(tag, "startCarousel success")
                    }

                    override fun onFailure(error: EduContextError?) {
                        LogX.e(tag, "startCarousel error: ${error?.let { GsonUtil.toJson(it) }}")
                        // restore image state
                        updateActiveState(false)
                    }
                })
    }

    private fun stopCarousel() {
        eduContext?.userContext()?.stopCoHostCarousel(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                LogX.i(tag, "stopCarousel success")
            }

            override fun onFailure(error: EduContextError?) {
                LogX.e(tag, "stopCarousel error: ${error?.let { GsonUtil.toJson(it) }}")
                // restore image state
                updateActiveState(true)
            }
        })
    }
}