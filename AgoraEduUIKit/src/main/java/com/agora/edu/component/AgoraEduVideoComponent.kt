package com.agora.edu.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.view.View.*
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduVideoComponentBinding
import io.agora.agoraeduuikit.impl.video.AgoraEduFloatingControlWindow
import io.agora.agoraeduuikit.impl.video.IAgoraOptionListener2
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.SvgaUtils
import kotlin.math.min
import io.agora.agoraeducore.core.context.AgoraEduContextSystemDevice.CameraFront
import io.agora.agoraeducore.core.context.AgoraEduContextSystemDevice.CameraBack
import io.agora.agoraeducore.core.context.AgoraEduContextSystemDevice.Microphone
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Audio
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Video
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal

/**
 * 基础视频组件
 * Basic video component
 */
class AgoraEduVideoComponent : AbsAgoraEduComponent, OnClickListener, IAgoraOptionListener2 {
    private val tag = "AgoraEduVideoComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var videoListener: IAgoraUIVideoListener? = null
    var m: AgoraEduFloatingControlWindow? = null
    var localUsrInfo: AgoraEduContextUserInfo? = null
    private var curUserDetailInfo: AgoraUIUserDetailInfo? = null
     var largeWindowOpened: Boolean = false
    private var lastVideoRender: Boolean? = true
    private val binding = AgoraEduVideoComponentBinding.inflate(
        LayoutInflater.from(context), this,
        true
    )
    private var userDetailInfo: AgoraUIUserDetailInfo? = null
    private val nameMaxLength = 7
    private val nameSuffix = "."

    init {
        binding.cardView.z = 0.0f
        binding.cardView.cardElevation = 1f
        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner)
        binding.cardView.radius = radius.toFloat()
        binding.nameText.setShadowLayer(
            context.resources.getDimensionPixelSize(R.dimen.shadow_width).toFloat(),
            2.0f, 2.0f, context.resources.getColor(R.color.theme_text_color_black)
        )
        binding.videoContainer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parentView: View?, child: View?) {
                child?.let {
                    if (child is TextureView || child is SurfaceView) {
                        setTextureViewRound(child)
                    }
                }
            }

            override fun onChildViewRemoved(p0: View?, p1: View?) {
            }
        })
        binding.audioIc.isEnabled = false
        binding.videoIc.isEnabled = false
        upsertUserDetailInfo(null)
    }

    private val largeWindowObserver = object : AgoraWidgetMessageObserver {
        //只负责老师窗口的逻辑
        override fun onMessageReceived(msg: String, id: String) {
//            if (id == AgoraWidgetDefaultId.LargeWindow.id + "-" + curUserDetailInfo?.streamUuid) {
                val packet = Gson().fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
                if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender) {
                    //大窗停止渲染，小窗恢复渲染
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo显示小窗
//                        if (curUserDetailInfo?.userUuid.equals(userDetailInfo.userUuid)) {
                            largeWindowOpened = false
                            upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
//                        }
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
                    //大窗开始渲染，小窗显示占位图
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo
//                        if (curUserDetailInfo?.userUuid.equals(userDetailInfo.userUuid)) {
                        largeWindowOpened = true
                        upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
//                        }
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                }
//            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        localUsrInfo = eduContext?.userContext()?.getLocalUserInfo()
        binding.cardView.setOnClickListener(this)
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // only teacher can trigger floatingWindow
                if (localUsrInfo?.role != AgoraEduContextUserRole.Teacher) {
                    return super.onSingleTapConfirmed(e)
                }
                m = userDetailInfo?.let {
                    AgoraEduFloatingControlWindow(it, context, this@AgoraEduVideoComponent, eduContext)
                }
                m?.show(binding.root)
                return super.onSingleTapConfirmed(e)
            }
        })
        binding.cardView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    override fun onClick(view: View?) {
    }

    private fun setTextureViewRound(view: View) {
        val radius: Float = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner).toFloat()
        val textureOutlineProvider = VideoTextureOutlineProvider(radius)
        view.outlineProvider = textureOutlineProvider
        view.clipToOutline = true
    }

    private fun setCameraState(info: AgoraUIUserDetailInfo?) {
        binding.videoIc.isEnabled = info?.isVideoEnable() ?: false
        binding.videoIc.isSelected = info?.hasVideo ?: false
    }

    private fun setVideoPlaceHolder(info: AgoraUIUserDetailInfo?) {
        binding.videoContainer.visibility = GONE
        binding.notInLayout.visibility = GONE
        binding.videoInLargeWindowLayout.visibility = GONE
        binding.videoOffLayout.visibility = GONE
        binding.offLineLoadingLayout.visibility = GONE
        binding.noCameraLayout.visibility = GONE
        binding.cameraDisableLayout.visibility = GONE
        if (info == null) {
            binding.notInLayout.visibility = VISIBLE
            return
        }
        if (info.isVideoEnable() && info.hasVideo) {
            binding.videoContainer.visibility = VISIBLE
        } else if (!info.isVideoEnable()) {
            binding.cameraDisableLayout.visibility = VISIBLE
        } else {
            binding.videoOffLayout.visibility = VISIBLE
        }
    }

    private fun setMicroState(info: AgoraUIUserDetailInfo?) {
        binding.audioIc.visibility = VISIBLE
        binding.audioIc.isEnabled = info?.isAudioEnable() ?: false
        binding.audioIc.isSelected = info?.hasAudio ?: false
    }

    private fun fillName(name: String) {
//        binding.nameText.text = if (name.length > nameMaxLength) {
//            name.substring(0, nameMaxLength).plus(nameSuffix)
//        } else {
//            name
//        }
        binding.nameText.text = name
    }

    /**
     * 更新当前用户信息
     * refresh curUser Info
     * @param info
     */
    fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo?, curVideoShouldRender: Boolean? = true) {
        eduContext?.widgetContext()?.addWidgetMessageObserver(
            largeWindowObserver, AgoraWidgetDefaultId.LargeWindow.id + "-" + info?.streamUuid
        )
        AgoraLog?.e("$tag->upsertUserDetailInfo:${Gson().toJson(info)}")
        this.post {
            if (info == curUserDetailInfo && lastVideoRender == curVideoShouldRender) {
                // double check duplicate data
                AgoraLog?.i("$tag->new info is same to old, return")
                return@post
            }

            //当前小窗不应该渲染流
            if (curVideoShouldRender == false) {// curVideoShouldRender:  current video item should render
                //large window showed
                binding.audioIc.visibility = GONE
                binding.videoIc.visibility = GONE
                binding.nameText.visibility = GONE
                binding.notInLayout.visibility = GONE
                binding.videoInLargeWindowLayout.visibility = VISIBLE
                lastVideoRender = curVideoShouldRender
                return@post
            }
            setCameraState(info)
            setMicroState(info)
            setVideoPlaceHolder(info)

            if (info != null) {
                // handle userInfo
                if (info.role == AgoraEduContextUserRole.Student) {
                    val reward = info.reward
                    if (reward > 0) {
                        binding.trophyLayout.visibility = VISIBLE
                        binding.trophyText.text = String.format(
                            context.getString(R.string.fcr_agora_video_reward),
                            min(reward, 99)
                        )
                        binding.trophyText.text = String.format(context.getString(R.string.fcr_agora_video_reward), info.reward)
                    } else {
                        binding.trophyLayout.visibility = GONE
                    }
                    binding.videoIc.visibility = GONE
                } else {
                    binding.trophyLayout.visibility = GONE
                    binding.videoIc.visibility = GONE
                }
                binding.nameText.visibility = VISIBLE
                fillName(info.userName)
                updateGrantedStatus(info.whiteBoardGranted)
                // handle streamInfo
                val currentVideoOpen: Boolean = curUserDetailInfo?.let {
                    it.isVideoEnable() && info.hasVideo
                } ?: false
                val newVideoOpen = info.isVideoEnable() && info.hasVideo
                if (!currentVideoOpen && newVideoOpen) {
                    videoListener?.onRendererContainer(binding.videoContainer, info.streamUuid)
                } else if (currentVideoOpen && !newVideoOpen) {
                    videoListener?.onRendererContainer(null, info.streamUuid)
                } else {
                    val parent = if (newVideoOpen) binding.videoContainer else null
                    videoListener?.onRendererContainer(parent, info.streamUuid)
                }
            } else {
                binding.nameText.text = ""
                binding.trophyLayout.visibility = GONE
                binding.videoIc.visibility = GONE
                binding.boardGrantedIc.visibility = GONE
                curUserDetailInfo?.let {
                    videoListener?.onRendererContainer(null, it.streamUuid)
                }
            }
            this.curUserDetailInfo = info?.copy()
            this.lastVideoRender = curVideoShouldRender
        }
    }

    /**
     * 更新当前用户的音频声音大小
     * refresh curUser audioVolume
     */
    @Volatile
    private var gifRunning = false
    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        if (binding.audioIc.isEnabled && binding.audioIc.isSelected && value > 0 && !gifRunning) {
            this.post {
                Glide.with(this).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_video_ic_audio_on)
                    .placeholder(R.drawable.agora_video_ic_audio_on)
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<GifDrawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: GifDrawable?, model: Any?, target: Target<GifDrawable?>?,
                            dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            gifRunning = true
                            resource?.setLoopCount(1)
                            resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    binding.audioIc.setImageResource(R.drawable.agora_video_ic_audio_bg)
                                    gifRunning = false
                                }
                            })
                            return false
                        }
                    }).into(binding.audioIc)
            }
        }
    }

    private fun updateGrantedStatus(granted: Boolean) {
        binding.boardGrantedIc.post {
            binding.boardGrantedIc.visibility = if (granted) VISIBLE else INVISIBLE
        }
    }

    /**
     * 播放举手动画
     * play wave animation
     */
    fun updateWaveState(waving: Boolean) {
        val runnable = Runnable {
            binding.handWavingComponent.visibility = VISIBLE
            binding.handWavingComponent.updateWaveState(waving)
        }
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            runnable.run()
        } else {
            ContextCompat.getMainExecutor(context).execute(runnable)
        }
    }

    // implement IAgoraOptionListener2
    override fun onAudioUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean) {
        AgoraLog?.d(tag, "onAudioUpdated")
        switchMedia(item, enabled, Microphone)
    }

    override fun onVideoUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean) {
        AgoraLog?.d(tag, "onVideoUpdated")
        val device = if (AgoraUIDeviceSetting.isFrontCamera()) {
            CameraFront
        } else {
            CameraBack
        }
        switchMedia(item, enabled, device)
    }

    override fun onCohostUpdated(item: AgoraUIUserDetailInfo, isCoHost: Boolean) {
        AgoraLog?.d("$tag->onCohostUpdated-item:${GsonUtil.toJson(item)}, isCoHost:$isCoHost")
        if (isCoHost) {
            // unreachable
            eduContext?.userContext()?.addCoHost(item.userUuid)
        } else {
            if (item.role == Teacher) {
                // remove all cohost from stage
                eduContext?.userContext()?.removeAllCoHosts()
            } else {
                eduContext?.userContext()?.removeCoHost(item.userUuid)
            }
        }

    }

    override fun onGrantUpdated(item: AgoraUIUserDetailInfo, hasAccess: Boolean) {
        AgoraLog?.d(tag, "onGrantUpdated")
        val data = AgoraBoardGrantData(hasAccess, arrayOf(item.userUuid).toMutableList())
        val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
        eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)

    }

    override fun onRewardUpdated(item: AgoraUIUserDetailInfo, count: Int) {
        AgoraLog?.d(tag, "onRewardUpdated")
        eduContext?.userContext()?.rewardUsers(arrayOf(item.userUuid).toMutableList(), count)
    }

    private fun switchMedia(item: AgoraUIUserDetailInfo, enabled: Boolean, device: AgoraEduContextSystemDevice) {
        localUsrInfo?.userUuid?.let {
            if (it == item.userUuid) {
                if (enabled) {
                    eduContext?.mediaContext()?.openSystemDevice(device)
                } else {
                    eduContext?.mediaContext()?.closeSystemDevice(device)
                }
            } else {
                val streamType = if (device == Microphone) Audio else Video
                if (enabled) {
                    eduContext?.streamContext()?.publishStreams(arrayListOf(item.streamUuid), streamType)
                } else {
                    eduContext?.streamContext()?.muteStreams(arrayListOf(item.streamUuid).toMutableList(), streamType)

                }
            }
        }
    }
}

class VideoTextureOutlineProvider(private val mRadius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, mRadius)
    }
}