package com.agora.edu.component

import android.content.Context
import android.util.AttributeSet
import android.view.*
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.*
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.context.EduContextVideoMode.Single
import io.agora.agoraeducore.core.context.EduContextVideoMode.Companion.fromValue
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduVideoGroupComponentBinding
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraEduVideoGroupComponent : AbsAgoraEduComponent, IAgoraUIVideoListener {
    private val tag = "AgoraEduVideoGroupComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initChildAttributes(context, attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        initChildAttributes(context, attr)
    }

    private val binding = AgoraEduVideoGroupComponentBinding.inflate(LayoutInflater.from(context),
        this, true)
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
            val a = (localIsTeacher||localUserInfo?.role == Observer) && userList.find { it.role == Student } == null//本地是老师或者观众，学生离线
            if (a) {
                secondUserDetailInfo = null
                binding.secondVideo.upsertUserDetailInfo(null) //更新学生窗口UI
            }
            val b = !localIsTeacher && userList.find { it.role == Teacher } == null //本地是学生，老师离线
            if (b) {
                firstUserDetailInfo = null
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

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        AgoraLog?.d(tag, "onUpdateVideo")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Audio)
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        AgoraLog?.d(tag, "onUpdateAudio")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Video)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        val isLocal = isLocalStream(streamUuid)
        if (noneView && isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (noneView && !isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (!noneView && isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid)
        } else if (!noneView && !isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid)
        }
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
}