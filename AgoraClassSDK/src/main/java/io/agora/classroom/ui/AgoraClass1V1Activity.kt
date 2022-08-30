package io.agora.classroom.ui

import android.os.Bundle
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.agora.edu.component.teachaids.presenter.FCR1V1ClassVideoPresenter
import io.agora.agoraclasssdk.R
import io.agora.agoraclasssdk.databinding.ActivityAgoraClass1v1Binding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.classroom.common.AgoraEduClassActivity

/**
 * author : hefeng
 * date : 2022/1/20
 * description : 1v1 教室
 */
class AgoraClass1V1Activity : AgoraEduClassActivity() {
    private val TAG = "AgoraClass1V1Activity"
    lateinit var binding: ActivityAgoraClass1v1Binding

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                if (localUser.userUuid == streamInfo.owner.userUuid) {
                    openSystemDevices()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClass1v1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.agoraLargeWindowContainer.limitEnd = resources.getDimensionPixelSize(R.dimen.agora_class_1v1_group_w)
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
            eduCore()?.eduContextPool()?.let { context ->
                context.streamContext()?.addHandler(streamHandler)

                binding.teachAidContainer.initView(this)
                binding.agoraClassHead.initView(this)
                binding.agoraClassHead.onExitListener = {
                    onBackPressed()
                }
                binding.agoraEduWhiteboard.initView(uuid, this)
                binding.fcrEduWebView.initView(this)
                binding.agoraEduTabGroup.initView(binding.root, this)
                binding.agoraLargeWindowContainer.videoPresenter = FCR1V1ClassVideoPresenter(
                    binding.agoraEduTabGroup
                )
                binding.agoraLargeWindowContainer.initView(this)
                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                binding.agoraEduOptions.onExitListener = {
                    onBackPressed()
                }
                launchConfig?.roleType?.let {
                    binding.agoraEduOptions.setShowOption(RoomType.ONE_ON_ONE, it)
                }
            }
            join()
        }
    }

    private fun openSystemDevices() { // 打开语音，摄像头，麦克风
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Speaker)
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(
            if (AgoraUIDeviceSetting.isFrontCamera()) {
                AgoraEduContextSystemDevice.CameraFront
            } else {
                AgoraEduContextSystemDevice.CameraBack
            }
        )
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
    }

    override fun onRelease() {
        super.onRelease()
        eduCore()?.eduContextPool()?.streamContext()?.removeHandler(streamHandler)
        binding.agoraEduWhiteboard.release()
        binding.agoraLargeWindowContainer.release()
    }

    override fun hiddenViewLoading() {
        super.hiddenViewLoading()
        if (this::binding.isInitialized) {
            binding.agoraEduWhiteboard.setHiddenLoading()
        }
    }
}