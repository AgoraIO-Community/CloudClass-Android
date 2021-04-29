package io.agora.uikit.impl.video

import android.content.Context
import android.graphics.Outline
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import io.agora.educontext.DeviceState
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserRole
import io.agora.uikit.*
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.container.AgoraUIConfig.clickInterval
import io.agora.uikit.impl.container.AgoraUIConfig.isLargeScreen
import io.agora.uikit.impl.container.AgoraUIConfig.videoOptionIconSizeMax
import io.agora.uikit.impl.container.AgoraUIConfig.videoOptionIconSizeMaxWithLargeScreen
import io.agora.uikit.impl.container.AgoraUIConfig.videoOptionIconSizePercent
import io.agora.uikit.impl.container.AgoraUIConfig.videoPlaceHolderImgSizePercent
import kotlin.math.min

internal class AgoraUIVideo(
        context: Context,
        parent: ViewGroup,
        left: Float,
        top: Float,
        shadowWidth: Float) : OnClickListener {
    private val tag = "AgoraUIVideo"

    var videoListener: IAgoraUIVideoListener? = null

    private val view: View = LayoutInflater.from(context).inflate(R.layout.agora_video_layout, parent, false)
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
    private val videoOffLayout: LinearLayout = view.findViewById(R.id.video_off_layout)
    private val videoOffImg: AppCompatImageView = view.findViewById(R.id.video_off_img)
    private val offLineLoadingLayout: LinearLayout = view.findViewById(R.id.offLine_loading_layout)
    private val offLineLoadingImg: AppCompatImageView = view.findViewById(R.id.offLine_loading_img)
    private val noCameraLayout: LinearLayout = view.findViewById(R.id.no_camera_layout)
    private val noCameraImg: AppCompatImageView = view.findViewById(R.id.no_camera_img)
    private val trophyLayout: LinearLayout = view.findViewById(R.id.trophy_Layout)
    private val trophyText: AppCompatTextView = view.findViewById(R.id.trophy_Text)
    private val audioLayout: LinearLayout = view.findViewById(R.id.audio_Layout)
    private val volumeLayout: LinearLayout = view.findViewById(R.id.volume_Layout)
    private val audioIc: AppCompatImageView = view.findViewById(R.id.audio_ic)
    private val videoNameLayout: RelativeLayout = view.findViewById(R.id.videoName_Layout)
    private val videoIc: AppCompatImageView = view.findViewById(R.id.video_ic)
    private val nameText: AppCompatTextView = view.findViewById(R.id.name_Text)
    private val boardGrantedIc: AppCompatImageView = view.findViewById(R.id.boardGranted_ic)

    private var userDetailInfo: EduContextUserDetailInfo? = null

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

        cardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cardView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val tmp = cardView.right - cardView.left
                val width = (tmp * videoPlaceHolderImgSizePercent).toInt()
                videoOffImg.layoutParams.width = width
                offLineLoadingImg.layoutParams.width = width
                noCameraImg.layoutParams.width = width
                val maxSize = if (isLargeScreen) videoOptionIconSizeMaxWithLargeScreen else videoOptionIconSizeMax
                val audioSize = min((tmp * videoOptionIconSizePercent).toInt(), maxSize)
                audioLayout.layoutParams.width = audioSize
                audioIc.layoutParams.width = audioSize
                audioIc.layoutParams.height = audioSize
                videoNameLayout.layoutParams.height = audioSize
                videoIc.layoutParams.width = audioSize
                videoIc.layoutParams.height = audioSize
                boardGrantedIc.layoutParams.width = audioSize
                boardGrantedIc.layoutParams.height = audioSize
                nameText.layoutParams.width = (nameText.textSize * 6).toInt()
            }
        })

        audioIc.isEnabled = false
        videoIc.isEnabled = false
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.audio_ic -> {
                audioIc.isClickable = false
                userDetailInfo?.let {
                    videoListener?.onUpdateAudio(!it.enableAudio)
                }
                audioIc.postDelayed({ audioIc.isClickable = true }, clickInterval)
            }
            R.id.video_ic -> {
                videoIc.isClickable = false
                userDetailInfo?.let {
                    videoListener?.onUpdateVideo(!it.enableVideo)
                }
                videoIc.postDelayed({ videoIc.isClickable = true }, clickInterval)
            }
        }
    }

    private fun setTextureViewRound(view: View) {
        val radius: Float = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner).toFloat()
        val textureOutlineProvider = VideoTextureOutlineProvider(radius)
        view.outlineProvider = textureOutlineProvider
        view.clipToOutline = true
    }

    private fun setCameraState(info: EduContextUserDetailInfo) {
        if (!info.onLine || info.cameraState == DeviceState.UnAvailable
                || info.cameraState == DeviceState.Closed) {
            videoIc.isEnabled = false
        } else {
            videoIc.isEnabled = true
            videoIc.isSelected = info.enableVideo
        }
    }

    private fun setVideoPlaceHolder(info: EduContextUserDetailInfo) {
        videoContainer.visibility = GONE
        videoOffLayout.visibility = GONE
        offLineLoadingLayout.visibility = GONE
        noCameraLayout.visibility = GONE
//        if (!info.onLine || info.cameraState == DeviceState.Closed) {
//            offLineLoadingLayout.visibility = VISIBLE
//        } else if (!info.enableVideo) {
//            videoOffLayout.visibility = VISIBLE
//        } else if (info.enableVideo) {
//            if (info.cameraState == DeviceState.UnAvailable) {
//                noCameraLayout.visibility = VISIBLE
//            } else {
//                videoContainer.visibility = VISIBLE
//            }
//        }
        if (!info.onLine || info.cameraState == DeviceState.Closed) {
            offLineLoadingLayout.visibility = VISIBLE
        } else if (info.cameraState == DeviceState.UnAvailable) {
            noCameraLayout.visibility = VISIBLE
        } else if (info.cameraState == DeviceState.Available) {
            if (info.enableVideo) {
                videoContainer.visibility = VISIBLE
            } else {
                videoOffLayout.visibility = VISIBLE
            }
        }
    }

    private fun setMicroState(info: EduContextUserDetailInfo) {
        if (!info.onLine || info.microState == DeviceState.UnAvailable
                || info.microState == DeviceState.Closed) {
            audioIc.isEnabled = false
            volumeLayout.visibility = GONE
        } else {
            audioIc.isEnabled = true
            audioIc.isSelected = info.enableAudio
            volumeLayout.visibility = if (info.enableAudio) VISIBLE else GONE
        }
    }

    fun upsertUserDetailInfo(info: EduContextUserDetailInfo) {
        Log.e(tag, "upsertUserDetailInfo->")

        this.view.post {
            if (info.user.role == EduContextUserRole.Student) {
                audioIc.setOnClickListener(this)
                videoIc.setOnClickListener(this)
                val reward = info.rewardCount
                if (reward > 0) {
                    trophyLayout.visibility = if (info.coHost) VISIBLE else GONE
                    trophyText.text = String.format(view.context.getString(R.string.agora_video_reward),
                            min(reward, 99))
                    trophyText.text = String.format(view.context.getString(R.string.agora_video_reward), info.rewardCount)
                } else {
                    trophyLayout.visibility = GONE
                }

                boardGrantedIc.visibility = if (info.boardGranted) VISIBLE else INVISIBLE
                setCameraState(info)
                videoIc.visibility = if (info.coHost) GONE else VISIBLE
            } else {
                trophyLayout.visibility = GONE
                videoIc.visibility = GONE
            }

            setMicroState(info)
            nameText.text = info.user.userName
            setVideoPlaceHolder(info)

            val currentVideoOpen: Boolean = userDetailInfo?.let {
                it.onLine && it.enableVideo && it.cameraState == DeviceState.Available
            } ?: false

            val newVideoOpen = info.onLine && info.enableVideo && info.cameraState == DeviceState.Available

            if (!currentVideoOpen && newVideoOpen) {
                videoListener?.onRendererContainer(videoContainer, info.streamUuid)
            } else if (currentVideoOpen && !newVideoOpen) {
                videoListener?.onRendererContainer(null, info.streamUuid)
            }

            this.userDetailInfo = info
        }
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        view.post {
            volumeLayout.removeAllViews()
            var volumeLevel = 0
            if (value > -1) {
                volumeLevel = (value / 36.0f).toInt()
            }
            if (volumeLevel == 0) {
                volumeLevel = 1
            } else if (volumeLevel > 7) {
                volumeLevel = 7
            }
            for (i in 1..(7 - volumeLevel)) {
                val volumeIc = AppCompatImageView(view.context)
                volumeIc.setImageResource(R.drawable.agora_video_ic_volume_off)
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams.topMargin = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_volume_margin_top)
                volumeIc.layoutParams = layoutParams
                volumeLayout.addView(volumeIc)
            }
            for (i in 1..volumeLevel) {
                val volumeIc = AppCompatImageView(view.context)
                volumeIc.setImageResource(R.drawable.agora_video_ic_volume_on)
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams.topMargin = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_volume_margin_top)
                volumeIc.layoutParams = layoutParams
                volumeLayout.addView(volumeIc)
            }
        }
    }

    fun updateMediaMessage(msg: String) {
        AgoraUIToastManager.showShort(msg)
    }

    fun updateReward(reward: Int) {
        trophyText.post {
            if (reward <= 0) {
                trophyLayout.visibility = GONE
            } else {
                trophyText.text = String.format(view.context.getString(R.string.agora_video_reward),
                        min(reward, 99))
            }
        }
    }

    fun updateGrantedStatus(granted: Boolean) {
        boardGrantedIc?.post {
            boardGrantedIc.visibility = if (granted) VISIBLE else INVISIBLE
        }
    }
}

class VideoTextureOutlineProvider(private val mRadius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, mRadius)
    }
}