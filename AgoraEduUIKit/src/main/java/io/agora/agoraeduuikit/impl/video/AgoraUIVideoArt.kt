package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.opensource.svgaplayer.SVGAImageView
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconAspect
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconWidthRatio
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.isLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMax
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMaxWithLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizePercent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoPlaceHolderImgSizePercent
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.SvgaUtils
import kotlin.math.ceil
import kotlin.math.min


internal class AgoraUIVideoArt2(
    context: Context,
    parent: ViewGroup,
    eduContext: EduContextPool?,
    left: Float,
    top: Float,
    shadowWidth: Float,
    private val curUserInfo: AgoraEduContextUserInfo?) : OnClickListener {
    private val tag = "AgoraUIVideo"

    private val clickInterval = 500L
    var videoListener: IAgoraUIVideoListener? = null
    var mDialog: AgoraUIVideoWindowDialog? = null
    private var eduContext: EduContextPool? = null
    var context: Context? = null

    private val view: View = LayoutInflater.from(context).inflate(R.layout.agora_video_layout, parent, false)
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
    private val notInLayout: LinearLayout = view.findViewById(R.id.not_in_layout)
    private val videoOffLayout: LinearLayout = view.findViewById(R.id.video_off_layout)
    private val videoOffImg: AppCompatImageView = view.findViewById(R.id.video_off_img)
    private val offLineLoadingLayout: LinearLayout = view.findViewById(R.id.offLine_loading_layout)
    private val offLineLoadingImg: AppCompatImageView = view.findViewById(R.id.offLine_loading_img)
    private val noCameraLayout: LinearLayout = view.findViewById(R.id.no_camera_layout)
    private val noCameraImg: AppCompatImageView = view.findViewById(R.id.no_camera_img)
    private val cameraDisableLayout: LinearLayout = view.findViewById(R.id.camera_disable_layout)
    private val cameraDisableImg: AppCompatImageView = view.findViewById(R.id.camera_disable_img)
    private val trophyLayout: LinearLayout = view.findViewById(R.id.trophy_Layout)
    private val trophyText: AppCompatTextView = view.findViewById(R.id.trophy_Text)
    private val audioLayout: LinearLayout = view.findViewById(R.id.audio_Layout)
    private var audioIc: AppCompatImageView = view.findViewById(R.id.audio_ic)
    private val videoNameLayout: RelativeLayout = view.findViewById(R.id.videoName_Layout)
    private val videoIc: AppCompatImageView = view.findViewById(R.id.video_ic)
    private val nameText: AppCompatTextView = view.findViewById(R.id.name_Text)
    private val boardGrantedIc: AppCompatImageView = view.findViewById(R.id.boardGranted_ic)
    private var audioVolumeSize: Pair<Int, Int>? = null
    private val handWavingLayout: RelativeLayout = view.findViewById(R.id.hand_waving_layout)
    private val handWavingImg: SVGAImageView = view.findViewById(R.id.hand_waving_img)
    private var lastVideoRender: Boolean? = true
    private var largeWindowUser: AgoraUIUserDetailInfo? = null
    var userDetailInfo: AgoraUIUserDetailInfo? = null
    var streamUuid = ""

    companion object {
        var userInfoListener: IAgoraUserInfoListener2? = null
        var largeWindowListener: OnLargeWindowListener? = null
    }

    init {
        this.context = context
        view.x = left
        view.y = top
        cardView.z = 0.0f
        cardView.cardElevation = shadowWidth
        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner)
        cardView.radius = radius.toFloat()
        val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
        val margin = (shadowWidth / 1.0f).toInt()
        layoutParams.setMargins(margin, margin, margin, margin)
        parent.addView(view)
        nameText.setShadowLayer(context.resources.getDimensionPixelSize(R.dimen.shadow_width).toFloat(),
            2.0f, 2.0f, context.resources.getColor(R.color.theme_text_color_black))
        setEduContext(eduContext)
        videoContainer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
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
        cardView.setOnClickListener(this)
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { //单击事件
                val localUsrInfo = eduContext?.userContext()?.getLocalUserInfo()
                if (localUsrInfo?.userUuid == userDetailInfo?.userUuid && userDetailInfo?.role == AgoraEduContextUserRole.Teacher ||
                    localUsrInfo?.role == AgoraEduContextUserRole.Teacher && userDetailInfo?.role == AgoraEduContextUserRole.Student ||
                    localUsrInfo?.role == AgoraEduContextUserRole.Student && userDetailInfo?.userUuid == localUsrInfo.userUuid) {
//                    mDialog = userDetailInfo?.let { AgoraUIVideoWindowDialog(it, view.context, this@AgoraUIVideoArt2, eduContext) }
//                    mDialog?.show(view)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean { //双击事件
                // 回调当前userDetailInfo给AgoraUISmallClassArtContainer
                userInfoListener?.onUserDoubleClicked(userDetailInfo)//开启大窗
                return super.onDoubleTap(e)
            }
        })
        cardView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        cardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cardView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val tmpW = cardView.right - cardView.left
                val tmpH = cardView.bottom - cardView.top
                val base = (min(tmpW, tmpH) * videoPlaceHolderImgSizePercent).toInt()
                videoOffImg.layoutParams.width = base
                videoOffImg.layoutParams.height = base
                offLineLoadingImg.layoutParams.width = base
                offLineLoadingImg.layoutParams.height = base
                noCameraImg.layoutParams.width = base
                noCameraImg.layoutParams.height = base
                cameraDisableImg.layoutParams.width = base
                cameraDisableImg.layoutParams.height = base
                val maxSize = if (isLargeScreen) videoOptionIconSizeMaxWithLargeScreen else videoOptionIconSizeMax
                val audioSize = min((tmpW * videoOptionIconSizePercent).toInt(), maxSize)
                audioLayout.layoutParams.width = audioSize
                audioIc.layoutParams.width = audioSize
                audioIc.layoutParams.height = audioSize
                videoNameLayout.layoutParams.height = audioSize
                videoIc.layoutParams.width = audioSize
                videoIc.layoutParams.height = audioSize
                boardGrantedIc.layoutParams.width = audioSize
                boardGrantedIc.layoutParams.height = audioSize
                nameText.layoutParams.width = (nameText.textSize * 6).toInt()
                val a = audioSize.toFloat() * audioVolumeIconWidthRatio
                val b = a * audioVolumeIconAspect
                audioVolumeSize = Pair(ceil(a.toDouble()).toInt(), ceil(b.toDouble()).toInt())
            }
        })
        audioIc.isEnabled = false
        videoIc.isEnabled = false
        upsertUserDetailInfo(null, eduContext)
    }

    private fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
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
        videoIc.visibility = INVISIBLE
        videoIc.isEnabled = isVideoEnable(info)
        videoIc.isSelected = isVideoOpen(info)
    }

    private fun setVideoPlaceHolder(info: AgoraUIUserDetailInfo?) {
        videoContainer.visibility = GONE
        notInLayout.visibility = GONE
        videoOffLayout.visibility = GONE
        offLineLoadingLayout.visibility = GONE
        noCameraLayout.visibility = GONE
        cameraDisableLayout.visibility = GONE
        if (info == null) {
            notInLayout.visibility = VISIBLE
            return
        }
        if (isVideoEnable(info) && isVideoOpen(info)) {
            videoContainer.visibility = VISIBLE
        } else if (!isVideoEnable(info)) {
            cameraDisableLayout.visibility = VISIBLE
        } else {
            videoOffLayout.visibility = VISIBLE
        }
    }

    private fun setMicroState(info: AgoraUIUserDetailInfo?) {
        audioIc.visibility = VISIBLE
        audioIc.isEnabled = isAudioEnable(info)
        audioIc.isSelected = isAudioOpen(info)
    }

    fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo?, eduContext: EduContextPool?, curVideoShouldRender: Boolean? = true) {
        AgoraLog.e("$tag->upsertUserDetailInfo:${Gson().toJson(info)}")
        view.post {
            if (info == userDetailInfo && lastVideoRender == curVideoShouldRender) {
                // double check duplicate data
                AgoraLog.i("$tag->new info is same to old, return")
                return@post
            }
            if (curVideoShouldRender == false) {// videoRender:  current video item should render
                //large window showed
                audioIc.visibility = GONE
                videoIc.visibility = GONE
                notInLayout.visibility = GONE
                nameText.visibility = GONE
                offLineLoadingLayout.visibility = VISIBLE
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
                        trophyLayout.visibility = VISIBLE
                        trophyText.text = String.format(view.context.getString(R.string.agora_video_reward),
                            min(reward, 99))
                        trophyText.text = String.format(view.context.getString(R.string.agora_video_reward), info.reward)
                    } else {
                        trophyLayout.visibility = GONE
                    }
                    videoIc.visibility = GONE
                } else {
                    trophyLayout.visibility = GONE
                    videoIc.visibility = GONE
                }
                nameText.visibility = VISIBLE
                nameText.text = info.userName
                updateGrantedStatus(info.whiteBoardGranted)
                // handle streamInfo
                val currentVideoOpen: Boolean = userDetailInfo?.let {
                    isVideoEnable(it) && isVideoOpen(it)
                } ?: false
                val newVideoOpen = isVideoEnable(info) && isVideoOpen(info)
                if (!currentVideoOpen && newVideoOpen) {
                    videoListener?.onRendererContainer(videoContainer, info.streamUuid)
                } else if (currentVideoOpen && !newVideoOpen) {
                    videoListener?.onRendererContainer(null, info.streamUuid)
                } else {
                    val parent = if (newVideoOpen) videoContainer else null
                    videoListener?.onRendererContainer(parent, info.streamUuid)
                }
            } else {
                nameText.text = ""
                trophyLayout.visibility = GONE
                videoIc.visibility = GONE
                boardGrantedIc.visibility = GONE
                userDetailInfo?.let {
                    videoListener?.onRendererContainer(null, it.streamUuid)
                }
            }


//            if (streamUuid == info?.streamUuid) {
//                videoListener?.onRendererContainer(null, info.streamUuid)
//                val windowPropertiesContext = eduContext!!.windowPropertiesContext()
//                windowPropertiesContext?.getHandlers()?.forEach { h ->
//                    h.onWindowPropertyUpdated(info.streamUuid)// 让大窗显示，小窗不显示
//                }
//            }
            this.userDetailInfo = info?.copy()
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

    @Volatile
    private var gifRunning = false
    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        if (audioIc.isEnabled && audioIc.isSelected && value > 0 && !gifRunning) {
            view.post {
                Glide.with(view).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_video_ic_audio_on)
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?,
                                                  target: Target<GifDrawable?>?,
                                                  isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(resource: GifDrawable?, model: Any?,
                                                     target: Target<GifDrawable?>?, dataSource: DataSource?,
                                                     isFirstResource: Boolean): Boolean {
                            gifRunning = true
                            val params = audioIc.layoutParams
                            val audioIcId = audioIc.id
                            val isEnabled = audioIc.isEnabled
                            val isSelected = audioIc.isSelected
                            resource?.setLoopCount(1)
                            resource?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    audioIc.setImageResource(R.drawable.agora_video_ic_audio_bg)
                                    gifRunning = false
                                }
                            })
                            return false
                        }
                    }).into(audioIc)
            }
        }
    }

    private fun updateGrantedStatus(granted: Boolean) {
        boardGrantedIc.post {
            boardGrantedIc.visibility = if (granted) VISIBLE else INVISIBLE
        }
    }

    fun updateWaveState(waving: Boolean) {
        val svgaUtils = SvgaUtils(context, handWavingImg)
        if (waving) {
            handWavingLayout.visibility = VISIBLE
            handWavingImg.visibility = VISIBLE
            svgaUtils.initAnimator()
            svgaUtils.startAnimator(context?.getString(R.string.waving_hands))
        } else {
            handWavingLayout.visibility = GONE
            handWavingImg.visibility = GONE
        }
    }

    fun setVisibility(visibility: Int, userDetailInfo: AgoraUIUserDetailInfo?) {
        if (visibility == INVISIBLE) {
            streamUuid = userDetailInfo?.streamUuid!!
            if (userDetailInfo != null) {
                //显示占位图即可
                audioIc.visibility = GONE
                videoIc.visibility = GONE
                nameText.visibility = GONE
                offLineLoadingLayout.visibility = VISIBLE
            }
        } else if (visibility == VISIBLE) {
            this.view.visibility = VISIBLE
            if (userDetailInfo != null) {
                upsertUserDetailInfo(userDetailInfo, eduContext)
            }
        }
    }
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


//class VideoTextureOutlineProvider(private val mRadius: Float) : ViewOutlineProvider() {
//    override fun getOutline(view: View, outline: Outline) {
//        outline.setRoundRect(0, 0, view.width, view.height, mRadius)
//    }
//}