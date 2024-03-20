package io.agora.classroom.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.UIUtils
import com.agora.edu.component.helper.RoomPropertiesHelper
import com.agora.edu.component.teachaids.presenter.FCR1V1ClassVideoPresenter
import io.agora.agoraclasssdk.databinding.ActivityAgoraClass1v1Binding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.classroom.common.AgoraEduClassActivity

/**
 * author : felix
 * date : 2022/1/20
 * description : 1v1 教室
 */
class AgoraClass1V1Activity : AgoraEduClassActivity() {
    override var TAG = "AgoraClass1V1Activity"
    lateinit var binding: ActivityAgoraClass1v1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClass1v1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //binding.agoraLargeWindowContainer.limitEnd = resources.getDimensionPixelSize(R.dimen.agora_class_1v1_group_w)
        // 创建了教室对象
        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                // 教室资源加载完成后
                joinClassRoom()
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    ToastManager.showShort(this@AgoraClass1V1Activity,it.msg)
                }
                finish()
            }
        })
    }

    private fun joinClassRoom() {
        ContextCompat.getMainExecutor(this).execute {
            eduCore()?.eduContextPool()?.let { context ->
                context.roomContext()?.addHandler(roomHandler)

                // header
                if (getUIConfig().isHeaderVisible) {
                    binding.agoraClassHead.initView(this)
                    binding.agoraClassHead.onExitListener = {
                        onBackPressed()
                    }
                }

                // whiteboard
                if (getUIConfig().isEngagementVisible) {
                    binding.agoraEduWhiteboard.initView(uuid, this)
                }

                // chat
                if (getUIConfig().isExtensionVisible) {
                    binding.agoraEduTabGroup.initView(binding.root, this)
                }

                binding.teachAidContainer.initView(this)
                binding.fcrEduWebView.initView(this)
                binding.agoraLargeWindowContainer.videoPresenter = FCR1V1ClassVideoPresenter(binding.agoraEduTabGroup)
                binding.agoraLargeWindowContainer.initView(this)


                // options(whiteboard)
                if (getUIConfig().isEngagementVisible) {
                    binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                    launchConfig?.shareUrl?.let {
                        binding.agoraEduOptions.setShareRoomLink(it)
                    }
                    binding.agoraEduOptions.onExitListener = {
                        onBackPressed()
                    }
                    launchConfig?.roleType?.let {
                        binding.agoraEduOptions.setShowOption(RoomType.ONE_ON_ONE, it)
                    }
                }

                UIUtils.setViewVisible(binding.agoraClassHead, getUIConfig().isHeaderVisible)
                UIUtils.setViewVisible(binding.agoraEduWhiteboard, getUIConfig().isEngagementVisible)
                UIUtils.setViewVisible(binding.agoraEduOptions, getUIConfig().isEngagementVisible)
                UIUtils.setViewVisible(binding.agoraEduTabGroup, getUIConfig().isExtensionVisible)
            }
            join()
        }
    }


    override fun onRelease() {
        super.onRelease()
        eduCore()?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
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

    val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            openSystemDevices()
            runOnUiThread {
                handleWaterMark()
            }
        }
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
}