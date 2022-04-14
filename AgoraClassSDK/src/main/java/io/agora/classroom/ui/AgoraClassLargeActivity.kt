package io.agora.classroom.ui

import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.gson.Gson
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
            AgoraUIToast.error(context = this@AgoraClassLargeActivity, text = error.msg)
            AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")
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
                binding.agoraClassHead.setTitleToRight()
                binding.agoraEduWhiteboard.initView(uuid, this)
                binding.agoraLargeWindowContainer.initView(this)
                binding.agoraClassUserListVideo.classRoomType = RoomType.LARGE_CLASS
                agoraClassVideoPresenter =
                    AgoraClassVideoPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
                agoraClassVideoPresenter?.initView(RoomType.LARGE_CLASS, this, uiController)

                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
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
}