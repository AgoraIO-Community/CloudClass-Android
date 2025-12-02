package io.agora.classroom.ui.large

import android.os.Bundle
import android.view.View
import com.agora.edu.component.helper.RoomPropertiesHelper
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassLargeRecordBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.helper.FcrRecordStreamManager

/**
 * author : felix
 * date : 2022/8/22
 * description : 合流转推 & 伪直播
 */
abstract class AgoraClassLargeRecordActivity : AgoraEduClassActivity() {
    override var TAG = "AgoraClassLargeRecordActivity"
    lateinit var binding: ActivityAgoraClassLargeRecordBinding
    val streamManager = FcrRecordStreamManager()
    var playVideoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAgoraClassLargeRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
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
                context.roomContext()?.addHandler(roomHandler)

                context.userContext()?.getLocalUserInfo()?.let { local ->
                    if (local.role == AgoraEduContextUserRole.Teacher) {
                        // Teacher is not supported in current versions
                    } else if (local.role == AgoraEduContextUserRole.Student) {
                        initStudentView()
                    }
                }
            }
            join()
        }
    }

    private fun initStudentView() {
        binding.agoraClassHead.initView(this)
        binding.agoraClassHead.isEnableRecordView = false
        binding.agoraClassHead.showStandaloneExit(true) { onBackPressed() }
        binding.agoraClassHead.showSettingIcon(false)

        binding.agoraClassChat.rootContainer = binding.root
        binding.agoraClassChat.initView(this)
    }

    abstract fun joinRoomSuccess()

    abstract fun playVideo(isPlay: Boolean)

    open fun onRoomRecordUpdated(state: FcrRecordingState) {}

    open fun onRoomStateUpdated(state: AgoraEduContextClassState) {}

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            LogX.d(TAG, "classroom ${roomInfo.roomUuid} joined success")
            initSystemDevices()
            joinRoomSuccess()
            handleWaterMark()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            LogX.e(TAG, " classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            LogX.d(TAG, "class state updated: ${state.name}")
            onRoomStateUpdated(state)
        }

        override fun onRecordingStateUpdated(state: FcrRecordingState) {
            super.onRecordingStateUpdated(state)
            LogX.d(TAG, "record（onRecordingStateUpdated） state=$state")
            onRoomRecordUpdated(state)
        }
    }

    override fun onRelease() {
        super.onRelease()
        eduCore()?.eduContextPool()?.roomContext()?.removeHandler(roomHandler)
    }

    override fun isAutoSubscribe(): Boolean {
        return false
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