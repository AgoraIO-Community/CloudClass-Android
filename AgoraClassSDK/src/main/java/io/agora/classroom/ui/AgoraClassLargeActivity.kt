package io.agora.classroom.ui

import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.widget.LinearLayoutCompat
import com.agora.edu.component.teachaids.presenter.FCRLargeClassVideoPresenter
import com.google.gson.Gson
import io.agora.agoraclasssdk.R
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassLargeBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.presenter.AgoraClassVideoPresenter

/**
 * author : hefeng
 * date : 2022/1/24
 * description : 大班课（5000）
 */
class AgoraClassLargeActivity : AgoraEduClassActivity() {
    private val TAG = "AgoraClassLargeActivity"

    lateinit var binding: ActivityAgoraClassLargeBinding
    private var agoraClassVideoPresenter: AgoraClassVideoPresenter? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            AgoraLog?.d("$TAG->classroom ${roomInfo.roomUuid} joined success")
            initSystemDevices()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            AgoraLog?.d("$TAG->class state updated: ${state.name}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClassLargeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.agoraLargeWindowContainer.limitEnd = resources.getDimensionPixelSize(R.dimen.agora_class_teacher_w)
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

                binding.teachAidContainer.initView(this)
                binding.agoraClassHead.initView(this)
                binding.agoraClassHead.onExitListener = {
                    onBackPressed()
                }
                binding.agoraClassHead.setTitleToRight()
                binding.agoraEduWhiteboard.initView(uuid, this)
                binding.agoraLargeWindowContainer.videoPresenter = FCRLargeClassVideoPresenter(
                    binding.agoraClassTeacherVideo, context)
                binding.agoraLargeWindowContainer.initView(this)
//                binding.agoraClassUserListVideo.classRoomType = RoomType.LARGE_CLASS
                agoraClassVideoPresenter = AgoraClassVideoPresenter(binding.agoraClassTeacherVideo)
                agoraClassVideoPresenter?.initView(RoomType.LARGE_CLASS, this, uiController)

                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
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

    fun setOptionItemLayout() {
        // 显示底部对齐
        val p = binding.agoraEduOptionsItemContainer.layoutParams as LinearLayoutCompat.LayoutParams
        p.gravity = Gravity.BOTTOM
        binding.agoraEduOptionsItemContainer.layoutParams = p
    }

    override fun onRelease() {
        super.onRelease()
        eduCore()?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
        agoraClassVideoPresenter?.release()
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