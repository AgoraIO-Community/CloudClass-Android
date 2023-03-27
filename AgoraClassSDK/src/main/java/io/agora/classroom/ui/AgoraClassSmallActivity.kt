package io.agora.classroom.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.agora.edu.component.common.UIUtils
import com.agora.edu.component.helper.RoomPropertiesHelper
import com.agora.edu.component.teachaids.presenter.FCRSmallClassVideoPresenter
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassSmallBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.presenter.AgoraClassVideoPresenter

/**
 * author : hefeng
 * date : 2022/1/24
 * description : 小班课（200）
 */
open class AgoraClassSmallActivity : AgoraEduClassActivity() {
    override var TAG = "AgoraClassSmallActivity"
    var agoraClassVideoPresenter: AgoraClassVideoPresenter? = null
    private lateinit var binding: ActivityAgoraClassSmallBinding

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClassSmallBinding.inflate(layoutInflater)
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
            eduCore()?.eduContextPool()?.widgetContext()?.sendMessageToWidget(packet, AgoraWidgetDefaultId.WhiteBoard.id)
//            if (!isOpenStage) {//关闭讲台，重置大窗容器高度
//                binding.agoraLargeWindowContainer.postDelayed({
//                    binding.agoraLargeWindowContainer.layoutParams.let {
//                        it.width = binding.agoraAreaBoard.width
//                        it.height = binding.agoraAreaBoard.height + binding.agoraAreaVideo.height //改变大窗高度
//                        binding.agoraLargeWindowContainer.layoutParams = it
//                    }
//                }, 150)
//            }
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
                    agoraClassVideoPresenter = AgoraClassVideoPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
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

                binding.teachAidContainer.initView(this)
                binding.fcrEduWebView.initView(this)
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
            binding.agoraWaterMark.startScroll()
            binding.agoraWaterMark.visibility = View.VISIBLE
        } else {
            binding.agoraWaterMark.visibility = View.GONE
        }

        LogX.i(TAG, "watermark = $watermark")
    }
}