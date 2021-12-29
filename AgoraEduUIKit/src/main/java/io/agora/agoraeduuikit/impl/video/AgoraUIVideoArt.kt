package io.agora.agoraeduuikit.impl.video

import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Audio
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Video
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
import com.bumptech.glide.Glide
import com.google.gson.Gson
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.context.EduContextUserDetailInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconAspect
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.audioVolumeIconWidthRatio
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.isLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMax
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizeMaxWithLargeScreen
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoOptionIconSizePercent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.videoPlaceHolderImgSizePercent
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.util.AppUtil
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
internal class AgoraUIVideoArt(
        context: Context,
        parent: ViewGroup,
        eduContext: EduContextPool?,
        left: Float,
        top: Float,
        shadowWidth: Float) : OnClickListener, IAgoraOptionListener {
    private val tag = "AgoraUIVideo"

    private val clickInterval = 500L
    var videoListener: IAgoraUIVideoListener? = null
    var mDialog: AgoraUIVideoWindowDialog? = null
    private var eduContext: EduContextPool? = null

    private val view: View = LayoutInflater.from(context).inflate(R.layout.agora_video_layout_art, parent, false)
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
    private val trophyLayout: LinearLayout = view.findViewById(R.id.trophy_Layout)
    private val trophyText: AppCompatTextView = view.findViewById(R.id.trophy_Text)
    private val audioLayout: LinearLayout = view.findViewById(R.id.audio_Layout)
    private val volumeLayout: LinearLayout = view.findViewById(R.id.volume_Layout)
    private val audioIc: AppCompatImageView = view.findViewById(R.id.audio_ic)
    private val videoNameLayout: RelativeLayout = view.findViewById(R.id.videoName_Layout)
    private val videoIc: AppCompatImageView = view.findViewById(R.id.video_ic)
    private val nameText: AppCompatTextView = view.findViewById(R.id.name_Text)
    private val boardGrantedIc: AppCompatImageView = view.findViewById(R.id.boardGranted_ic)
    private var audioVolumeSize: Pair<Int, Int>? = null

    var userDetailInfo: io.agora.agoraeducore.core.context.EduContextUserDetailInfo? = null//拿到视频区用户具体信息

    var currentTimeMillis: Long = 0

    var streamUuid = ""

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
                if (localUsrInfo?.userUuid == userDetailInfo?.user?.userUuid && userDetailInfo?.user?.role == AgoraEduContextUserRole.Teacher ||
                        localUsrInfo?.role == AgoraEduContextUserRole.Teacher && userDetailInfo?.user?.role == AgoraEduContextUserRole.Student ||
                        localUsrInfo?.role == AgoraEduContextUserRole.Student && userDetailInfo?.user?.userUuid == localUsrInfo.userUuid) {
                    mDialog = userDetailInfo?.let { AgoraUIVideoWindowDialog(it, view.context, this@AgoraUIVideoArt, eduContext) }
                    mDialog?.show(view)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean { //双击事件
                // 回调当前userDetailInfo给AgoraUISmallClassArtContainer
//                userInfoListener?.onUserDoubleClicked(userDetailInfo)//开启大窗
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
    }

    private fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
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
                videoIc.isClickable = false
                userDetailInfo?.let {
                    videoListener?.onUpdateVideo(it.streamUuid, !it.enableVideo)
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

    private fun setCameraState(info: io.agora.agoraeducore.core.context.EduContextUserDetailInfo) {
        if (!info.onLine || info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.UnAvailable
                || info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Closed) {
            videoIc.isEnabled = false
            videoIc.isSelected = false
        } else {
            videoIc.isEnabled = true
            videoIc.isSelected = info.enableVideo
        }
    }

    private fun setVideoPlaceHolder(info: io.agora.agoraeducore.core.context.EduContextUserDetailInfo) {
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
            showWaving(info)
        } else if (info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.UnAvailable) {
            noCameraLayout.visibility = VISIBLE
            showWaving(info)
        } else if (info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available) {
            showWaving(info)
            if (info.enableVideo) {
                videoContainer.visibility = VISIBLE
            } else {
                videoOffLayout.visibility = VISIBLE
            }
        }
    }

    private fun showWaving(info: EduContextUserDetailInfo) {
        if (info.isWaving) { // 用户正在挥手状态
            handWavingLayout.visibility = VISIBLE
            handWavingImg.visibility = VISIBLE

            Glide.with(view).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_handsup_waving)
                    .into(handWavingImg)
        } else {
            handWavingLayout.visibility = GONE
            handWavingImg.visibility = GONE
        }
    }

    private fun setMicroState(info: io.agora.agoraeducore.core.context.EduContextUserDetailInfo) {
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

    fun upsertUserDetailInfo(info: io.agora.agoraeducore.core.context.EduContextUserDetailInfo, eduContext: EduContextPool?) {
        AgoraLog.e(tag, "upsertUserDetailInfo->")

        this.view.post {
            if (info.user.role == AgoraEduContextUserRole.Student) {
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
                it.onLine && it.enableVideo && it.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available
            } ?: false

            val newVideoOpen = info.onLine && info.enableVideo && info.cameraState == io.agora.agoraeducore.core.context.EduContextDeviceState.Available///

            if (!currentVideoOpen && newVideoOpen) {
                videoListener?.onRendererContainer(videoContainer, info.streamUuid)
            } else if (currentVideoOpen && !newVideoOpen) {
                videoListener?.onRendererContainer(null, info.streamUuid)
            } else {
                val parent = if (newVideoOpen) videoContainer else null
                videoListener?.onRendererContainer(parent, info.streamUuid)
            }

            if (streamUuid == info.streamUuid) {
                videoListener?.onRendererContainer(null, info.streamUuid)
                val windowPropertiesContext = eduContext!!.windowPropertiesContext()
                windowPropertiesContext?.getHandlers()?.forEach { h ->
                    h.onWindowPropertyUpdated(info.streamUuid)// 让大窗显示，小窗不显示
                }
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
            var width = LinearLayout.LayoutParams.MATCH_PARENT
            var height = LinearLayout.LayoutParams.MATCH_PARENT
            audioVolumeSize?.let {
                width = it.first
                height = it.second
            }
            val topMargin = view.context.resources.getDimensionPixelSize(R.dimen.agora_video_volume_margin_top) * audioVolumeIconWidthRatio
            for (i in 1..(7 - volumeLevel)) {
                val volumeIc = AppCompatImageView(view.context)
                volumeIc.setImageResource(R.drawable.agora_video_ic_volume_off)
                val layoutParams = LinearLayout.LayoutParams(width, height)
                layoutParams.topMargin = floor(topMargin.toDouble()).toInt()
                volumeIc.layoutParams = layoutParams
                volumeLayout.addView(volumeIc)
            }
            for (i in 1..volumeLevel) {
                val volumeIc = AppCompatImageView(view.context)
                volumeIc.setImageResource(R.drawable.agora_video_ic_volume_on)
                val layoutParams = LinearLayout.LayoutParams(width, height)
                layoutParams.topMargin = floor(topMargin.toDouble()).toInt()
                volumeIc.layoutParams = layoutParams
                volumeLayout.addView(volumeIc)
            }
        }
    }

    fun updateMediaMessage(msg: String) {
        AgoraUIToast.info(context = view.context, text = msg)
    }

    fun updateReward(reward: Int = 1) {
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

    override fun onAudioUpdated(item: EduContextUserDetailInfo, enabled: Boolean) {
        AgoraLog.d(tag, "onAudioUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(item.streamUuid).toMutableList(), Audio)

    }

    override fun onVideoUpdated(item: EduContextUserDetailInfo, enabled: Boolean) {
        AgoraLog.d(tag, "onVideoUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(item.streamUuid).toMutableList(), Video)
    }

    override fun onCohostUpdated(item: EduContextUserDetailInfo, isCoHost: Boolean) {
        AgoraLog.d(tag, "onCohostUpdated")
        val userUuid = item.user.userUuid
        if (isCoHost) {
            //eduContext?.userContext()?.addCoHost(userUuid)
        } else {
            //eduContext?.userContext()?.removeCoHost(userUuid)
        }
    }

    override fun onGrantUpdated(item: EduContextUserDetailInfo, hasAccess: Boolean) {
        AgoraLog.d(tag, "onGrantUpdated")
        val data = AgoraBoardGrantData(hasAccess, arrayOf(item.user.userUuid).toMutableList())
        val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
        eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
    }

    override fun onRewardUpdated(item: EduContextUserDetailInfo, count: Int) {
        AgoraLog.d(tag, "onRewardUpdated")
        val userUuid = item.user.userUuid
        //eduContext?.userContext()?.rewardUsers(arrayOf(userUuid).toMutableList(), count)
    }

    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        if (visibility == INVISIBLE) {
            streamUuid = userDetailInfo?.streamUuid!!
            if (userDetailInfo != null) {
                videoListener?.onRendererContainer(null, userDetailInfo!!.streamUuid)
            }
        } else if (visibility == VISIBLE) {
            this.view.visibility = VISIBLE
            if (userDetailInfo != null) {
                videoListener?.onRendererContainer(videoContainer, userDetailInfo!!.streamUuid)
            }
        }
    }
}

interface IAgoraOptionListener {
    fun onAudioUpdated(item: EduContextUserDetailInfo, enabled: Boolean)
    fun onVideoUpdated(item: EduContextUserDetailInfo, enabled: Boolean)
    fun onCohostUpdated(item: EduContextUserDetailInfo, isCoHost: Boolean)
    fun onGrantUpdated(item: EduContextUserDetailInfo, hasAccess: Boolean)
    fun onRewardUpdated(item: EduContextUserDetailInfo, count: Int)

}

interface IAgoraUserInfoListener {
    fun onUserDoubleClicked(userDetailInfo: EduContextUserDetailInfo?)
}
