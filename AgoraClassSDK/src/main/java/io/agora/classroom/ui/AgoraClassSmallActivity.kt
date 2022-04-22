package io.agora.classroom.ui

import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassSmallBinding
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
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
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")
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

    fun createJoinRoom() {
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
                binding.agoraLargeWindowContainer.initView(this)
                agoraClassVideoPresenter = AgoraClassVideoPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
                agoraClassVideoPresenter?.initView(getRoomType(), this, uiController)
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
}