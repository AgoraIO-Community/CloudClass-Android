package io.agora.classroom.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.agora.edu.component.teachaids.bean.StaticData
import com.agora.edu.component.teachaids.presenter.FCRSmallClassVideoPresenter
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassSmallBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.CMD
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.presenter.AgoraClassVideoPresenter

/**
 * author : hefeng
 * date : 2022/1/24
 * description : 小班课（200）
 */
open class AgoraClassSmallActivity : AgoraEduClassActivity() {
    private val TAG = "AgoraClassSmallActivity"
    var agoraClassVideoPresenter: AgoraClassVideoPresenter? = null
    private lateinit var binding: ActivityAgoraClassSmallBinding

    protected val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            AgoraLog?.d("$TAG->classroom ${roomInfo.roomUuid} joined success")
            initSystemDevices()
            val platformEnable = (eduCore()?.eduContextPool()?.roomContext()?.getRoomProperties()
                ?.get(StaticData.platformEnableKey) as? Double)?.toInt() ?: 1 == State.YES.value
            handlerStageStatus(platformEnable)
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onRoomPropertiesUpdated(
            properties: Map<String, Any>, cause: Map<String, Any>?,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onRoomPropertiesUpdated(properties, cause, operator)
            // 解析并判断讲台是否关闭
            cause?.get(CMD)?.let { cmd ->
                if (cmd == StaticData.platformEnableCMD) {
                    val platformEnable = (properties[StaticData.platformEnableKey] as Double).toInt() == State.YES.value
                    handlerStageStatus(platformEnable)
                    //binding.videoLayout.visibility = GONE
                    //binding.agoraLargeWindowContainer.updateContentMargin(false)
                }
            }
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            AgoraLog?.d("$TAG->class state updated: ${state.name}")
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
    fun handlerStageStatus(platformEnable: Boolean) {//platformEnable 1：显示讲台   0：隐藏讲台
        runOnUiThread {
            // 1、讲台区域隐藏
            binding.videoLayout.visibility = if (platformEnable) View.VISIBLE else View.GONE
            // 2、把白板顶上来
            val limitTop = resources.getDimensionPixelSize(R.dimen.agora_small_video_h) / 2
            (binding.agoraEduWhiteboard.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                it.bottomMargin = if (platformEnable) 0 else limitTop
                it.topMargin = if (platformEnable) 0 else limitTop
                binding.agoraEduWhiteboard.layoutParams = it
            }
        }
    }

    fun createJoinRoom() {
        binding.agoraLargeWindowContainer.limitTop = resources.getDimensionPixelSize(R.dimen.agora_small_video_h)
        // 创建了教室对象
        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                // 教室资源加载完成后
                AgoraLog?.d("$TAG->createEduCore success. Ready joinClassRoom")
                joinClassRoom()
                AgoraLog?.d("$TAG->createEduCore success. joinClassRoom success")
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
                binding.teachAidContainer.initView(this)
                binding.agoraClassHead.initView(this)
                binding.agoraClassHead.onExitListener = {
                    onBackPressed()
                }
                binding.agoraClassHead.setTitleToRight()
                binding.agoraEduWhiteboard.initView(uuid, this)
                binding.fcrEduWebView.initView(this)
                binding.agoraLargeWindowContainer.initView(this)
                agoraClassVideoPresenter = AgoraClassVideoPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
                agoraClassVideoPresenter?.initView(getRoomType(), this, uiController)
                binding.agoraLargeWindowContainer.videoPresenter = FCRSmallClassVideoPresenter(
                    binding.videoLayout, binding.agoraClassTeacherVideo,
                    binding.agoraClassUserListVideo, context
                )
                binding.agoraLargeWindowContainer.initView(this)
                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                binding.agoraEduOptions.onExitListener = {
                    onBackPressed()
                }
                launchConfig?.roleType?.let {
                    binding.agoraEduOptions.setShowOption(getRoomType(), it)
                }
            }
            join()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
}