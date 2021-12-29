package io.agora.agoraeduuikit.impl.video

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.EduContextUserDetailInfo
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconAspect
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconWidthRatio
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.isLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMax
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMaxWithLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizePercent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoPlaceHolderImgSizePercent
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.util.AppUtil
import kotlin.math.ceil
import kotlin.math.min


@SuppressLint("ClickableViewAccessibility")
internal class AgoraUILargeVideoArt(
        context: Context,
        parent: ViewGroup,
        left: Float,
        top: Float,
        shadowWidth: Float) : OnClickListener, IAgoraOptionListener {
    private val tag = "AgoraUIVideo"

    private val clickInterval = 500L
    var videoListener: IAgoraUIVideoListener? = null

    private val view: View = LayoutInflater.from(context).inflate(R.layout.agora_large_video_layout_art, parent, false)
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
    private val videoOffLayout: LinearLayout = view.findViewById(R.id.video_off_layout)
    private val videoOffImg: AppCompatImageView = view.findViewById(R.id.video_off_img)
    private val offLineLoadingLayout: LinearLayout = view.findViewById(R.id.offLine_loading_layout)
    private val offLineLoadingImg: AppCompatImageView = view.findViewById(R.id.offLine_loading_img)
    private val noCameraLayout: LinearLayout = view.findViewById(R.id.no_camera_layout)
    private val noCameraImg: AppCompatImageView = view.findViewById(R.id.no_camera_img)
    private val cameraDisableLayout: LinearLayout = view.findViewById(R.id.camera_disable_layout)
    private val handWavingLayout: RelativeLayout = view.findViewById(R.id.hand_waving_layout)
    private val cameraDisableImg: AppCompatImageView = view.findViewById(R.id.camera_disable_img)
    private val handWavingImg: AppCompatImageView = view.findViewById(R.id.hand_waving_img)


    private val audioLayout: LinearLayout = view.findViewById(R.id.audio_Layout)
    private val volumeLayout: LinearLayout = view.findViewById(R.id.volume_Layout)
    private val audioIc: AppCompatImageView = view.findViewById(R.id.audio_ic)
    private val videoNameLayout: RelativeLayout = view.findViewById(R.id.videoName_Layout)

    private val nameText: AppCompatTextView = view.findViewById(R.id.name_Text)

    private var audioVolumeSize: Pair<Int, Int>? = null

    private var userDetailInfo: io.agora.agoraeducore.core.context.EduContextUserDetailInfo? = null//拿到视频区用户具体信息

    var currentTimeMillis: Long = 0

    companion object {
        var userInfoListener: IAgoraUserInfoListener? = null
    }

    init {
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
//                mDialog = userDetailInfo?.let { AgoraUIVideoWindowDialog(it, view.context, this@AgoraUILargeVideoArt) }
//                mDialog?.show(view)
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean { //双击事件
                //列表窗视频恢复 回调当前userDetailInfo给AgoraUISmallClassArtContainer
                userInfoListener?.onUserDoubleClicked(userDetailInfo)
                return super.onDoubleTap(e)
            }
        })
        cardView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        cardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cardView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val tmp = cardView.right - cardView.left
                val width = (tmp * videoPlaceHolderImgSizePercent).toInt()
                videoOffImg.layoutParams.width = width
                offLineLoadingImg.layoutParams.width = width
                noCameraImg.layoutParams.width = width
                cameraDisableImg.layoutParams.width = width
                val maxSize = if (isLargeScreen) videoOptionIconSizeMaxWithLargeScreen else videoOptionIconSizeMax
                val audioSize = min((tmp * videoOptionIconSizePercent).toInt(), maxSize)
                audioLayout.layoutParams.width = audioSize
                audioIc.layoutParams.width = audioSize
                audioIc.layoutParams.height = audioSize
                videoNameLayout.layoutParams.height = audioSize

                nameText.layoutParams.width = (nameText.textSize * 6).toInt()
                val a = audioSize.toFloat() * audioVolumeIconWidthRatio
                val b = a * audioVolumeIconAspect
                audioVolumeSize = Pair(ceil(a.toDouble()).toInt(), ceil(b.toDouble()).toInt())
            }
        })

        audioIc.isEnabled = false
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.audio_ic -> {
                if (AppUtil.isFastClick(clickInterval)) {
                    return
                }
                audioIc.isClickable = false
                userDetailInfo?.let {
                    videoListener?.onUpdateAudio(it.streamUuid, !it.enableAudio)
                }
                audioIc.postDelayed({ audioIc.isClickable = true }, clickInterval)
            }
            R.id.video_ic -> {
                if (AppUtil.isFastClick(clickInterval)) {
                    return
                }
                userDetailInfo?.let {
                    videoListener?.onUpdateVideo(it.streamUuid, !it.enableVideo)
                }
            }
        }
    }

    private fun setTextureViewRound(view: View) {
        val radius: Float = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner).toFloat()
        val textureOutlineProvider = VideoTextureOutlineProvider(radius)
        view.outlineProvider = textureOutlineProvider
        view.clipToOutline = true
    }


    private fun setVideoPlaceHolder(info: EduContextUserDetailInfo) {
        videoContainer.visibility = GONE
        videoOffLayout.visibility = GONE
        offLineLoadingLayout.visibility = GONE
        noCameraLayout.visibility = GONE
        cameraDisableLayout.visibility = GONE
        handWavingLayout.visibility = GONE
        if (!info.onLine) {
            offLineLoadingLayout.visibility = VISIBLE
        } else if (info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Closed) {
            cameraDisableLayout.visibility = VISIBLE
        } else if (info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.UnAvailable) {
            noCameraLayout.visibility = VISIBLE
        } else if (info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available) {
            if (info.enableVideo) {
                videoContainer.visibility = VISIBLE
            } else {
                videoOffLayout.visibility = VISIBLE
            }
        }
    }


    private fun setMicroState(info: EduContextUserDetailInfo) {
        if (!info.onLine || info.microState == io.agora.agoraeducore.core.context.EduContextDeviceState.UnAvailable
                || info.microState == io.agora.agoraeducore.core.context.EduContextDeviceState.Closed) {
            audioIc.isEnabled = false
            audioIc.isSelected = false
            volumeLayout.visibility = GONE
        } else {
            audioIc.isEnabled = true
            audioIc.isSelected = info.enableAudio
            volumeLayout.visibility = if (info.enableAudio) VISIBLE else GONE
        }
    }

    fun upsertUserDetailInfo(info: EduContextUserDetailInfo) {
        AgoraLog.e(tag, "upsertUserDetailInfo->")

        this.view.post {
            if (info.user.role == io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student) {
                audioIc.setOnClickListener(this)
            } else {
            }

            setMicroState(info)
            nameText.text = info.user.userName
            setVideoPlaceHolder(info)

            val currentVideoOpen: Boolean = userDetailInfo?.let {
                it.onLine && it.enableVideo && it.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available
            } ?: false

            val newVideoOpen = info.onLine && info.enableVideo && info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available

            if (!currentVideoOpen && newVideoOpen) {
                videoListener?.onRendererContainer(videoContainer, info.streamUuid)
            } else if (currentVideoOpen && !newVideoOpen) {
                videoListener?.onRendererContainer(null, info.streamUuid)
            } else {
                val parent = if (newVideoOpen) videoContainer else null
                videoListener?.onRendererContainer(parent, info.streamUuid)
            }

            this.userDetailInfo = info
        }
    }


    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        this@AgoraUILargeVideoArt.userDetailInfo = userDetailInfo

        if (visibility == INVISIBLE) {
            this.view.visibility = INVISIBLE
            if (userDetailInfo != null) {
                videoListener?.onRendererContainer(null, userDetailInfo!!.streamUuid)

            }
        } else {

            if (userDetailInfo != null) {
                this.view.visibility = VISIBLE
                nameText.text = userDetailInfo.user.userName

                videoListener?.onRendererContainer(videoContainer, userDetailInfo!!.streamUuid)
            }
        }

    }

    override fun onAudioUpdated(item: EduContextUserDetailInfo, enabled: Boolean) {
    }

    override fun onVideoUpdated(item: EduContextUserDetailInfo, enabled: Boolean) {

    }

    override fun onCohostUpdated(item: EduContextUserDetailInfo, isCoHost: Boolean) {

    }

    override fun onGrantUpdated(item: EduContextUserDetailInfo, hasAccess: Boolean) {

    }

    override fun onRewardUpdated(item: EduContextUserDetailInfo, count: Int) {

    }

}

