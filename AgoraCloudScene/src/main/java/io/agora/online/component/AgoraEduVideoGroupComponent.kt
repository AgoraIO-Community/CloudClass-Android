package io.agora.online.component

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.helper.AgoraRendererUtils
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.*
import io.agora.agoraeducore.core.context.EduContextVideoMode.Companion.fromValue
import io.agora.agoraeducore.core.context.EduContextVideoMode.Single
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduVideoGroupComponentBinding
import io.agora.online.interfaces.listeners.IAgoraUIVideoListener
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.online.provider.UIDataProviderListenerImpl

class AgoraEduVideoGroupComponent : AbsAgoraEduComponent, IAgoraUIVideoListener {
    private val tag = "AgoraEduVideoGroupComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initChildAttributes(context, attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        initChildAttributes(context, attr)
    }

    private val binding = FcrOnlineEduVideoGroupComponentBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )
    private var videoMode = Single
    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var firstUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var secondUserDetailInfo: AgoraUIUserDetailInfo? = null

    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            val localIsTeacher = localUserInfo?.role == Teacher
            userList.forEach {
                if (it.role == Teacher) {
                    if (firstUserDetailInfo == it) {
                        return@forEach
                    }
                    firstUserDetailInfo = it
                    if (!binding.firstVideo.largeWindowOpened) {//老师大窗没有打开，更新老师视频窗口
                        binding.firstVideo.upsertUserDetailInfo(it)
                    }
                } else if (it.role == Student) {
                    // check duplicate data
                    if (secondUserDetailInfo == it) {
                        return@forEach
                    }
                    secondUserDetailInfo = it
                    binding.secondVideo.upsertUserDetailInfo(it)
                }
            }
            val a = (localIsTeacher || localUserInfo?.role == Observer) && userList.find { it.role == Student } == null//本地是老师或者观众，学生离线
            if (a) {
                secondUserDetailInfo = null
                binding.secondVideo.largeWindowOpened = false
                binding.secondVideo.upsertUserDetailInfo(null) //更新学生窗口UI
            }
            val b = !localIsTeacher && userList.find { it.role == Teacher } == null //本地是学生，老师离线
            if (b) {
                firstUserDetailInfo = null
                binding.firstVideo.largeWindowOpened = false
                binding.firstVideo.upsertUserDetailInfo(null)
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == secondUserDetailInfo?.streamUuid || streamUuid == "0") {
                binding.secondVideo.updateAudioVolumeIndication(volume, streamUuid)
            } else if (streamUuid == firstUserDetailInfo?.streamUuid) {
                binding.firstVideo.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.firstVideo.initView(agoraUIProvider)
        binding.secondVideo.initView(agoraUIProvider)
        localUserInfo = eduContext?.userContext()?.getLocalUserInfo()
    }

    private fun initChildAttributes(context: Context, attr: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attr, R.styleable.AgoraEduVideoGroupComponent)
        videoMode = fromValue(typedArray.getInt(R.styleable.AgoraEduVideoGroupComponent_videoMode, Single.value))
        typedArray.recycle()
        binding.firstVideo.videoListener = this
        if (videoMode == Single) {
            binding.rootLayout.removeView(binding.secondVideo)
        } else {
            binding.secondVideo.videoListener = this
        }
    }


    override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        AgoraRendererUtils.onRendererContainer(eduCore, viewGroup, info, isLocalStream(info.streamUuid))
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        return when {
            firstUserDetailInfo?.streamUuid == streamUuid -> {
                firstUserDetailInfo?.userUuid == localUserInfo?.userUuid && !localUserInfo?.userUuid.isNullOrEmpty()
            }
            secondUserDetailInfo?.streamUuid == streamUuid -> {
                secondUserDetailInfo?.userUuid == localUserInfo?.userUuid && !localUserInfo?.userUuid.isNullOrEmpty()
            }
            else -> {
                false
            }
        }
    }

    fun show(show: Boolean) {
        this.post {
            binding.rootLayout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun getVideoPosition(streamUuid: String): Rect? {
        if (firstUserDetailInfo?.streamUuid == streamUuid) {
            return binding.firstVideo.getViewPosition(streamUuid)
        } else if (secondUserDetailInfo?.streamUuid == streamUuid) {
            return binding.secondVideo.getViewPosition(streamUuid)
        }
        return null
    }
}