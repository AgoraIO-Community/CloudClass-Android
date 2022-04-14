package com.agora.edu.component

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.view.View.*
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduVideoArtComponentBinding
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.video.AgoraEduFloatingControlWindow
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.SvgaUtils
import kotlin.math.min

/**
 * 基础视频组件 Art
 * Basic video component
 */
class AgoraEduVideoArtComponent : AbsAgoraEduComponent, OnClickListener {
    private val tag = "AgoraEduVideoArtComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var m: AgoraEduFloatingControlWindow? = null
    var videoListener: IAgoraUIVideoListener? = null
    private var largeWindowOpened: Boolean = false
    private val binding = AgoraEduVideoArtComponentBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )
    private var audioVolumeSize: Pair<Int, Int>? = null
    private var curUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var lastVideoRender: Boolean? = true
    private var currentWindowUser: AgoraUIUserDetailInfo? = null
    var streamUuid = ""

    companion object {
        var userInfoListener: IAgoraUserInfoListener2? = null
        var largeWindowListener: OnLargeWindowListener? = null
    }

    private val largeWindowObserver = object : AgoraWidgetMessageObserver {//只负责老师窗口的逻辑
        override fun onMessageReceived(msg: String, id: String) {
            if (id == AgoraWidgetDefaultId.LargeWindow.id + "-" + curUserDetailInfo?.streamUuid) {
                val packet = Gson().fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
                if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender) {
                    //大窗停止渲染，小窗恢复渲染
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo显示小窗
                        if (curUserDetailInfo?.userUuid.equals(userDetailInfo.userUuid)) {
                            largeWindowOpened = false
                            upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
                        }
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
                    //大窗开始渲染，小窗显示占位图
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        //拿到userDetailInfo
                        if (curUserDetailInfo?.userUuid.equals(userDetailInfo.userUuid)) {
                            largeWindowOpened = true
                            upsertUserDetailInfo(userDetailInfo, !largeWindowOpened)
                        }
                    } ?: Runnable {
                        Constants.AgoraLog?.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                }
            }
        }
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
//        eduContext?.widgetContext()?.addWidgetMessageObserver(
//            largeWindowObserver, AgoraWidgetDefaultId.LargeWindow.id
//        )
    }

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

        binding.cardView.setOnClickListener(this)
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { //单击事件
                val localUsrInfo = eduContext?.userContext()?.getLocalUserInfo()
                if (localUsrInfo?.userUuid == curUserDetailInfo?.userUuid && curUserDetailInfo?.role == AgoraEduContextUserRole.Teacher ||
                    localUsrInfo?.role == AgoraEduContextUserRole.Teacher && curUserDetailInfo?.role == AgoraEduContextUserRole.Student ||
                    localUsrInfo?.role == AgoraEduContextUserRole.Student && curUserDetailInfo?.userUuid == localUsrInfo.userUuid
                ) {
//                    mDialog = curUserDetailInfo?.let { AgoraUIVideoWindowDialog(it, view.context, this@AgoraUIVideoArt2, eduContext) }
//                    mDialog?.show(view)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean { //双击事件
                // 回调当前userDetailInfo给AgoraUISmallClassArtContainer
                userInfoListener?.onUserDoubleClicked(curUserDetailInfo)//开启大窗
                return super.onDoubleTap(e)
            }
        })
        binding.cardView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        binding.audioIc.isEnabled = false
        binding.videoIc.isEnabled = false
        this.upsertUserDetailInfo(null)
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
        binding.videoIc.isEnabled = isVideoEnable(info)
        binding.videoIc.isSelected = isVideoOpen(info)
        binding.videoIc.visibility = INVISIBLE
    }

    private fun setVideoPlaceHolder(info: AgoraUIUserDetailInfo?) {
        binding.videoContainer.visibility = GONE
        binding.notInLayout.visibility = GONE
        binding.videoOffLayout.visibility = GONE
        binding.offLineLoadingLayout.visibility = GONE
        binding.noCameraLayout.visibility = GONE
        binding.cameraDisableLayout.visibility = GONE
        if (info == null) {
            binding.notInLayout.visibility = VISIBLE
            return
        }
        if (isVideoEnable(info) && isVideoOpen(info)) {
            binding.videoContainer.visibility = VISIBLE
        } else if (!isVideoEnable(info)) {
            binding.cameraDisableLayout.visibility = VISIBLE
        } else {
            binding.videoOffLayout.visibility = VISIBLE
        }
    }

    private fun setMicroState(info: AgoraUIUserDetailInfo?) {
        binding.audioIc.visibility = VISIBLE
        binding.audioIc.isEnabled = isAudioEnable(info)
        binding.audioIc.isSelected = isAudioOpen(info)
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
            if (curVideoShouldRender == false) {// videoRender:  current video item should render
                //large window showed
                binding.audioIc.visibility = GONE
                binding.videoIc.visibility = GONE
                binding.notInLayout.visibility = GONE
                binding.nameText.visibility = GONE
                binding.offLineLoadingLayout.visibility = VISIBLE
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
                binding.nameText.text = info.userName
                updateGrantedStatus(info.whiteBoardGranted)
                // handle streamInfo
                val currentVideoOpen: Boolean = curUserDetailInfo?.let {
                    isVideoEnable(it) && isVideoOpen(it)
                } ?: false
                val newVideoOpen = isVideoEnable(info) && isVideoOpen(info)
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
     * whether audio device is enable
     * */
    private fun isAudioEnable(info: AgoraUIUserDetailInfo?): Boolean {
        if (info == null) {
            return false
        }
        return info.audioSourceState == AgoraEduContextMediaSourceState.Open
    }

    /**
     * whether have permission to send video stream
     * */
    private fun isAudioOpen(info: AgoraUIUserDetailInfo?): Boolean {
        if (info == null) {
            return false
        }
        // whether have permission to send video stream
        return info.hasAudio
    }

    private fun isVideoEnable(info: AgoraUIUserDetailInfo?): Boolean {
        if (info == null) {
            return false
        }
        return info.videoSourceState == AgoraEduContextMediaSourceState.Open
    }

    private fun isVideoOpen(info: AgoraUIUserDetailInfo?): Boolean {
        if (info == null) {
            return false
        }
        // whether have permission to send video stream
        return info.hasVideo
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
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?,
                            target: Target<GifDrawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: GifDrawable?, model: Any?,
                            target: Target<GifDrawable?>?, dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            gifRunning = true
                            val params = binding.audioIc.layoutParams
                            val audioIcId = binding.audioIc.id
                            val isEnabled = binding.audioIc.isEnabled
                            val isSelected = binding.audioIc.isSelected
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
        val svgaUtils = SvgaUtils(context, binding.handWavingImg)
        if (waving) {
            binding.handWavingLayout.visibility = VISIBLE
            binding.handWavingImg.visibility = VISIBLE
            svgaUtils.initAnimator()
            svgaUtils.startAnimator(context?.getString(R.string.fcr_waving_hands))
        } else {
            binding.handWavingLayout.visibility = GONE
            binding.handWavingImg.visibility = GONE
        }
    }

//    fun setVisibility(visibility: Int, curUserDetailInfo: AgoraUIUserDetailInfo?) {
//        if (visibility == INVISIBLE) {
//            streamUuid = curUserDetailInfo?.streamUuid!!
//            if (curUserDetailInfo != null) {
//                //显示占位图即可
//                binding.audioIc.visibility = GONE
//                binding.videoIc.visibility = GONE
//                binding.nameText.visibility = GONE
//                binding.offLineLoadingLayout.visibility = VISIBLE
//            }
//        } else if (visibility == VISIBLE) {
//            this.visibility = VISIBLE
//            if (curUserDetailInfo != null) {
//                this.upsertUserDetailInfo(curUserDetailInfo)
//            }
//        }
//    }

}

interface OnLargeWindowListener {
    fun onLargeWindowInfoChanged(userDetailInfo: AgoraUIUserDetailInfo?)
}


interface IAgoraOptionListener2 {
    fun onAudioUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onVideoUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onCohostUpdated(item: AgoraUIUserDetailInfo, isCoHost: Boolean)
    fun onGrantUpdated(item: AgoraUIUserDetailInfo, hasAccess: Boolean)
    fun onRewardUpdated(item: AgoraUIUserDetailInfo, count: Int)

}

interface IAgoraUserInfoListener2 {
    fun onUserDoubleClicked(userDetailInfo: AgoraUIUserDetailInfo?)
}