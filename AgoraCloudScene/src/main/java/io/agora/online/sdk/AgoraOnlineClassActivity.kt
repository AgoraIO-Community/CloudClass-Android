package io.agora.online.sdk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import io.agora.online.component.common.UIUtils
import io.agora.online.helper.AgoraUIDeviceSetting
import io.agora.online.helper.FcrHandsUpManager
import io.agora.online.helper.RoomPropertiesHelper
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.R
import io.agora.online.component.dialog.AgoraUIDialog
import io.agora.online.component.dialog.AgoraUIDialogBuilder
import io.agora.online.component.teachaids.presenter.FCRSmallClassVideoPresenter
import io.agora.online.component.toast.AgoraUIToast
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.online.sdk.common.AgoraEduClassActivity
import io.agora.online.sdk.presenter.AgoraClassVideoPresenter
import io.agora.online.databinding.ActivityAgoraOnlineClassBinding

/**
 * author : hefeng
 * date : 2022/1/24
 * description : 小班课（200）
 */
open class AgoraOnlineClassActivity : AgoraEduClassActivity() {
    override var TAG = "AgoraOnlineClassActivity"
    var agoraClassVideoPresenter: AgoraClassVideoPresenter? = null
    private lateinit var binding: ActivityAgoraOnlineClassBinding
    var cameraDialog: AgoraUIDialog? = null
    var micDialog: AgoraUIDialog? = null

    protected val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            LogX.i(TAG, "classroom ${roomInfo.roomUuid} joined success")
            openSystemDevices()
            handleStageStatus(RoomPropertiesHelper.isOpenStage(eduCore()))
            handleWaterMark()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            LogX.e(TAG, "classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onRoomPropertiesUpdated(
            properties: Map<String, Any>, cause: Map<String, Any>?,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onRoomPropertiesUpdated(properties, cause, operator)
            // 解析并判断讲台是否关闭
            handleStageStatus(RoomPropertiesHelper.isOpenStage(eduCore()))
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            LogX.d(TAG, "class state updated: ${state.name}")
        }

        override fun onReceiveCustomChannelMessage(customMessage: FcrCustomMessage) {
            super.onReceiveCustomChannelMessage(customMessage)
            switchDevice(customMessage)
        }

        override fun onReceiveCustomPeerMessage(customMessage: FcrCustomMessage) {
            super.onReceiveCustomPeerMessage(customMessage)
            switchDevice(customMessage)
        }
    }

    fun switchDevice(customMessage: FcrCustomMessage){
        ContextCompat.getMainExecutor(context).execute {
            val cmd = customMessage.payload.data[FcrHandsUpManager.CMD]
            if (cmd?.equals(FcrHandsUpManager.DEVICE_SWITCH) == true || cmd?.equals(FcrHandsUpManager.DEVICE_SWITCH_BATCH) == true) {
                val data = customMessage.payload.data[FcrHandsUpManager.DATA] as? Map<String, Any>
                val state = ("" + data?.get("deviceState")).toDouble().toInt() // 0.close, 1.open
                val activated = (state == 1)
                val deviceType = ("" + data?.get("deviceType")).toDouble().toInt() // 1.camera, 2.mic

                if (deviceType == 1) {
                    val device = if (AgoraUIDeviceSetting.isFrontCamera()) {
                        AgoraEduContextSystemDevice.CameraFront
                    } else {
                        AgoraEduContextSystemDevice.CameraBack
                    }

                    if (!activated) {
                        getEduContext()?.mediaContext()?.closeSystemDevice(device)
                    } else {
                        FcrHandsUpManager.getDeviceState(eduCore(), device) { isDeviceOpen ->
                            if (!isDeviceOpen) {
                                if (cameraDialog?.isShowing == true) {
                                    return@getDeviceState
                                }
                                cameraDialog = AgoraUIDialogBuilder(this@AgoraOnlineClassActivity)
                                    .title(context.resources.getString(R.string.fcr_switch_tips_teacher_start_video_title))
                                    .message(context.resources.getString(R.string.fcr_switch_tips_teacher_start_video_content))
                                    .negativeText(context.resources.getString(R.string.fcr_switch_tips_button_refuse))
                                    .positiveText(context.resources.getString(R.string.fcr_switch_tips_button_agree))
                                    .positiveClick {
                                        getEduContext()?.mediaContext()?.openSystemDevice(device)
                                    }
                                    .build()
                                cameraDialog?.show()
                            } else {
                                getEduContext()?.mediaContext()?.openSystemDevice(device)
                            }
                        }
                    }
                } else if (deviceType == 2) {
                    if (!activated) {
                        getEduContext()?.mediaContext()?.closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
                    } else {
                        FcrHandsUpManager.getDeviceState(
                            eduCore(),
                            AgoraEduContextSystemDevice.Microphone
                        ) { isDeviceOpen ->
                            if (!isDeviceOpen) {
                                if (micDialog?.isShowing == true) {
                                    return@getDeviceState
                                }
                                micDialog = AgoraUIDialogBuilder(this@AgoraOnlineClassActivity)
                                    .title(context.resources.getString(R.string.fcr_switch_tips_teacher_unmute_title))
                                    .message(context.resources.getString(R.string.fcr_switch_tips_teacher_unmute_content))
                                    .negativeText(context.resources.getString(R.string.fcr_switch_tips_button_refuse))
                                    .positiveText(context.resources.getString(R.string.fcr_switch_tips_button_agree))
                                    .positiveClick {
                                        getEduContext()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
                                    }
                                    .build()
                                micDialog?.show()
                            } else {
                                getEduContext()?.mediaContext()
                                    ?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraOnlineClassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createJoinRoom()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        dismissFullDialog()
    }

    //处理讲台的状态，隐藏还是打开
    fun handleStageStatus(isOpenStage: Boolean) {//platformEnable 1：显示讲台   0：隐藏讲台
        LogX.i(TAG, "isOpenStage=$isOpenStage")

        runOnUiThread {
            // 1、讲台区域隐藏
            binding.agoraAreaVideo.visibility = if (isOpenStage) View.VISIBLE else View.GONE
            // 2、设置白板比例
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardRatioChange, String())
            eduCore()?.eduContextPool()?.widgetContext()
                ?.sendMessageToWidget(packet, AgoraWidgetDefaultId.WhiteBoard.id)
        }
    }

    fun createJoinRoom() {
        // 创建了教室对象
        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                // 教室资源加载完成后
                LogX.d(TAG, "createEduCore success. Ready joinClassRoom")
                joinClassRoom()
                LogX.d(TAG, "createEduCore success. joinClassRoom success")
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    ToastManager.showShort(it.msg)
                }
                finish()
            }
        })
    }

    open fun joinClassRoom() {
        runOnUiThread {
            eduCore()?.eduContextPool()?.let { context ->
                context.roomContext()?.addHandler(roomHandler)
                FcrHandsUpManager.init(eduCore())

                // header area
                if (getUIConfig().isHeaderVisible) {
                    binding.agoraClassHead.initView(this)
                    binding.agoraClassHead.onExitListener = {
                        onBackPressed()
                    }
                    binding.agoraClassHead.setTitleToRight()
                }

                // video area
                if (getUIConfig().isStageVisible) {
                    agoraClassVideoPresenter =
                        AgoraClassVideoPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
                    binding.agoraLargeWindowContainer.videoPresenter = FCRSmallClassVideoPresenter(
                        binding.agoraAreaVideo, binding.agoraClassTeacherVideo,
                        binding.agoraClassUserListVideo, context
                    )
                    agoraClassVideoPresenter?.initView(getRoomType(), this, uiController)
                }

                // white area
                if (getUIConfig().isEngagementVisible) {
                    // whiteboard
                    binding.agoraEduWhiteboard.initView(uuid, this)

                    // tool bar
                    binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                    launchConfig?.shareUrl?.let {
                        binding.agoraEduOptions.setShareRoomLink(it)
                    }
                    binding.agoraEduOptions.onExitListener = {
                        onBackPressed()
                    }
                    launchConfig?.roleType?.let {
                        binding.agoraEduOptions.setShowOption(getRoomType(), it)
                    }
                }

                //binding.teachAidContainer.initView(this)
                binding.fcrEduWidgetView.initView(this)
                binding.agoraLargeWindowContainer.initView(this)

                UIUtils.setViewVisible(binding.agoraClassHead, getUIConfig().isHeaderVisible)
                UIUtils.setViewVisible(binding.agoraClassTeacherVideo, getUIConfig().isStageVisible)
                UIUtils.setViewVisible(binding.agoraClassUserListVideo, getUIConfig().isStageVisible)
                UIUtils.setViewVisible(binding.agoraEduWhiteboard, getUIConfig().isEngagementVisible)
                UIUtils.setViewVisible(binding.agoraEduOptions, getUIConfig().isEngagementVisible)
            }
            join()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.agoraWaterMark.pauseScroll()
        eduCore()?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
    }

    override fun onRelease() {
        super.onRelease()
        agoraClassVideoPresenter?.release()
        binding.agoraEduOptions.release()
        binding.agoraEduWhiteboard.release()
        binding.agoraLargeWindowContainer.release()
        binding.fcrEduWidgetView.release()
        FcrHandsUpManager.release(eduCore())
        //binding.root.removeAllViews()
    }

    override fun hiddenViewLoading() {
        super.hiddenViewLoading()
        if (this::binding.isInitialized) {
            binding.agoraEduWhiteboard.setHiddenLoading()
        }
    }

    override fun setNotShowWhiteLoading() {
        super.setNotShowWhiteLoading()
        if (this::binding.isInitialized) {
            binding.agoraEduWhiteboard.setNotShowWhiteLoading()
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

        LogX.i(TAG, "watermark = $watermark")
    }

    override fun cancelHandsUp() {
        super.cancelHandsUp()
        binding.agoraEduOptions.cancelHandsUp()
    }
}