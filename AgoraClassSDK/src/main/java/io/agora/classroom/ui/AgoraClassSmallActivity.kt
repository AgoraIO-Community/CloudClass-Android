package io.agora.classroom.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.agora.edu.component.common.UIUtils
import com.agora.edu.component.teachaids.bean.StaticData
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassSmallBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.CMD
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
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
            openSystemDevices()
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

//    private val streamHandler = object : StreamHandler() {
//        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
//            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
//                if (localUser.userUuid == streamInfo.owner.userUuid) {
//                    localStreamInfo = streamInfo
//                    openSystemDevices()
//                }
//            }
//        }
//
//        override fun onStreamUpdated(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
//            super.onStreamUpdated(streamInfo, operator)
//            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
//                if (localUser.userUuid == streamInfo.owner.userUuid && operator?.role == AgoraEduContextUserRole.Teacher) {
//                    //如果是both或者video，那么现在update的video状态是true
//                    curlocalhasVideo =
//                        streamInfo.streamType == AgoraEduContextMediaStreamType.Video || streamInfo.streamType == AgoraEduContextMediaStreamType.Both
//                    curlocalhasAudio =
//                        streamInfo.streamType == AgoraEduContextMediaStreamType.Audio || streamInfo.streamType == AgoraEduContextMediaStreamType.Both
//                    if (lastLocalhasVideo != curlocalhasVideo) {//如果上一次的视频状态和本次的视频状态不一样
//                        when (streamInfo.streamType) {
//                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.Both -> {//更新视频状态的提示
//                                AgoraUIToast.info(applicationContext, text = String.format(context.getString(R.string.fcr_stream_start_video)))
//                                lastLocalhasVideo = true
//                            }
//                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.None -> {
//                                AgoraUIToast.info(applicationContext, text = String.format(context.getString(R.string.fcr_stream_stop_video)))
//                                lastLocalhasVideo = false
//                            }
//                        }
//                    }
//                    if (lastLocalhasAudio != curlocalhasAudio) {
//                        when (streamInfo.streamType) {
//                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.Both -> {
//                                AgoraUIToast.info(applicationContext, text = String.format(context.getString(R.string.fcr_stream_start_audio)))
//                                lastLocalhasAudio = true
//                            }
//                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.None -> {
//                                AgoraUIToast.info(applicationContext, text = String.format(context.getString(R.string.fcr_stream_stop_audio)))
//                                lastLocalhasAudio = false
//                            }
//                        }
//                    }
//                }
//            }
//            localStreamInfo = streamInfo//保存本地的流信息
//        }
//
//        override fun onStreamLeft(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
//            super.onStreamLeft(streamInfo, operator)
//        }
//    }

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
            binding.agoraAreaVideo.visibility = if (platformEnable) View.VISIBLE else View.GONE
            // 2、设置白板比例
//            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardRatioChange, String())
//            eduCore()?.eduContextPool()?.widgetContext()?.sendMessageToWidget(packet, AgoraWidgetDefaultId.WhiteBoard.id)

            // 2、把白板顶上来
            val limitTop = resources.getDimensionPixelSize(R.dimen.agora_small_video_h) / 2
            (binding.agoraAreaBoard.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                it.bottomMargin = if (platformEnable) 0 else limitTop
                it.topMargin = if (platformEnable) 0 else limitTop
                binding.agoraAreaBoard.layoutParams = it
            }
        }
    }

    fun createJoinRoom() {
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
                    agoraClassVideoPresenter?.initView(getRoomType(), this, uiController)
                }

                // white area
                if (getUIConfig().isEngagementVisible) {
                    // whiteboard
                    binding.agoraEduWhiteboard.initView(uuid, this)

                    // tool bar
                    binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
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