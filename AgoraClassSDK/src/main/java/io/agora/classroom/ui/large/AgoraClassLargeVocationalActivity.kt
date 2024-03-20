package io.agora.classroom.ui.large

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import com.agora.edu.component.helper.RoomPropertiesHelper
import com.agora.edu.component.teachaids.presenter.FCRLargeClassVideoPresenter
import io.agora.agoraclasssdk.R
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassLargeVocationalBinding
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.classroom.vocational.AgoraVocationalClassVideoPresenter
import io.agora.classroom.vocational.FCRVocationalLargeWindowContainerComponent

/**
 * author : yanjs
 * date : 2022/05/29
 * description : 职教课
 */
class AgoraClassLargeVocationalActivity : AgoraEduClassActivity() {
    private val tag = "Vocational-Activity"

    companion object {
        const val FusionTimeout: Int = 5
    }

    lateinit var binding: ActivityAgoraClassLargeVocationalBinding
    private var agoraVocationalClassVideoPresenter: AgoraVocationalClassVideoPresenter? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            LogX.d(tag, "classroom ${roomInfo.roomUuid} joined success")
            initSystemDevices()
            agoraVocationalClassVideoPresenter?.addStreamListener()
            handleWaterMark()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            LogX.e(tag, "classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            LogX.d(tag, "class state updated: ${state.name}")
        }
    }

    private val localStreamTypeListener = object :
        FCRVocationalLargeWindowContainerComponent.ILocalStreamTypeListener {
        override fun onLocalStreamTypeChange(isRtc: Boolean) {
            agoraVocationalClassVideoPresenter?.switchTeacherStreamType(isRtc)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClassLargeVocationalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.agoraLargeWindowContainer.limitEnd = resources.getDimensionPixelSize(R.dimen.agora_class_teacher_w)

        // cdn mode hide hands up
        if (AgoraClassroomSDK.getCurrentLaunchConfig()?.serviceType == AgoraServiceType.CDN) {
            binding.agoraEduOptions.visibility = View.GONE
        }

        // 创建了教室对象
        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                // 教室资源加载完成后
                joinClassRoom()
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    ToastManager.showShort(it.msg)
                }
                finish()
            }
        })
    }

    private fun joinClassRoom() {
        runOnUiThread {
            setOptionItemLayout()

            eduCore()?.eduContextPool()?.let { context ->
                context.roomContext()?.addHandler(roomHandler)

                // 投票器/定时器/选择器
                binding.teachAidContainer.initView(this)
                binding.agoraClassHead.initView(this)
                binding.agoraClassHead.onExitListener = {
                    onBackPressed()
                }
                binding.agoraEduWhiteboard.initView(uuid, this)
                // 上台学生
                binding.agoraLargeWindowContainer.videoPresenter = FCRLargeClassVideoPresenter(
                    binding.agoraClassTeacherVideo, context
                )
                binding.agoraLargeWindowContainer.localStreamTypeListener = localStreamTypeListener
                binding.agoraLargeWindowContainer.initView(this)
                binding.agoraLargeWindowContainer.serviceType = AgoraClassroomSDK.getCurrentLaunchConfig()?.serviceType
//                binding.agoraClassUserListVideo.classRoomType = RoomType.LARGE_CLASS
                agoraVocationalClassVideoPresenter = AgoraVocationalClassVideoPresenter(binding.agoraClassTeacherVideo)
                agoraVocationalClassVideoPresenter?.initView(
                    RoomType.LARGE_CLASS,
                    AgoraClassroomSDK.getCurrentLaunchConfig()?.serviceType,
                    this,
                    uiController
                )

                // 举手/采集开关
                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                launchConfig?.shareUrl?.let {
                    binding.agoraEduOptions.setShareRoomLink(it)
                }
                if (AgoraClassroomSDK.getCurrentLaunchConfig()?.serviceType == AgoraServiceType.Fusion) {
                    binding.agoraEduOptions.setHandsupTimeout(FusionTimeout)
                }
                binding.agoraEduOptions.onExitListener = {
                    onBackPressed()
                }
                launchConfig?.roleType?.let {
                    binding.agoraEduOptions.setShowOption(RoomType.LARGE_CLASS, it)
                }
                binding.agoraClassChat.rootContainer = binding.root
                binding.agoraClassChat.initView(this)
                //binding.agoraClassChat.isVisible = true
            }
            join()
        }
    }

    private fun setOptionItemLayout() {
        // 显示底部对齐
        val p = binding.agoraEduOptionsItemContainer.layoutParams as LinearLayoutCompat.LayoutParams
        p.gravity = Gravity.BOTTOM
        binding.agoraEduOptionsItemContainer.layoutParams = p
    }

    override fun onRelease() {
        super.onRelease()
        eduCore()?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
        agoraVocationalClassVideoPresenter?.release()
        binding.agoraEduWhiteboard.release()
        binding.agoraLargeWindowContainer.release()
    }

    override fun hiddenViewLoading() {
        super.hiddenViewLoading()
        if (this::binding.isInitialized) {
            binding.agoraEduWhiteboard.setHiddenLoading()
        }
    }

    override fun getLargeVideoArea(): View {
        return binding.agoraLargeWindowContainer
    }

    fun handleWaterMark() {
        val watermark = RoomPropertiesHelper.isOpenWatermark(eduCore())
        if (watermark) {
            binding.agoraWaterMark.setNickName(launchConfig?.userName ?: "")
            binding.agoraWaterMark.startScroll()
            binding.agoraWaterMark.visibility = View.VISIBLE
        } else {
            binding.agoraWaterMark.visibility = View.GONE
        }

        LogX.i(tag, "watermark = $watermark")
    }
}