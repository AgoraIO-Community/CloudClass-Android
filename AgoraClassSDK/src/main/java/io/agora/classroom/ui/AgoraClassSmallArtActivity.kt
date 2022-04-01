package io.agora.classroom.ui

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.agora.edu.component.teachaids.AgoraCountDownWidget
import com.agora.edu.component.teachaids.AgoraIClickerWidget
import com.google.gson.Gson
import io.agora.agoraclasssdk.databinding.ActivityAgoraClassSmallArtBinding
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoWidget
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.VideoUtils
import io.agora.classroom.common.AgoraEduClassActivity
import io.agora.classroom.presenter.AgoraClassSmallArtPresenter

/**
 * author : wufang
 * date : 2022/2/9
 * description : 美术小班课
 */
class AgoraClassSmallArtActivity : AgoraEduClassActivity() {
    private val TAG = "AgoraClassSmallArt"
    private var agoraClassSmallArtPresenter: AgoraClassSmallArtPresenter? = null
    lateinit var binding: ActivityAgoraClassSmallArtBinding

    private var largeVideoWindowWidget: AgoraBaseWidget? = null
    private var userDetailInfo: AgoraUIUserDetailInfo? = null
    private var lastUserDetailInfo: AgoraUIUserDetailInfo? = null
//    private var largeWindowContainer: RelativeLayout? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            Log.d(TAG, "classroom ${roomInfo.roomUuid} joined success")
//            eduCore()?.eduContextPool()?.extAppContext()?.init(RelativeLayout(this@AgoraClassSmallArtActivity))//todo
            initSystemDevices()

//            val config2 = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.LargeWindow.id)
//            config2?.let {
//                largeVideoWindowWidget = getEduContext()?.widgetContext()?.create(it)
//            }
//            if (largeVideoWindowWidget is AgoraUILargeVideoWidget) {
//                (largeVideoWindowWidget as AgoraUILargeVideoWidget).largeVideoListener = this
//            }
//            largeWindowContainer?.let {
//                largeVideoWindowWidget?.init(it)
//                getUIDataProvider()?.addListener((largeVideoWindowWidget as AgoraUILargeVideoWidget).uiDataProviderListener)
//            }
//            val isLargeWindowOpened = getEduContext()?.widgetContext()?.getWidgetActive(AgoraWidgetDefaultId.LargeWindow.id)
//            isLargeWindowOpened?.let { handleLargeWindowEvent(it) }
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(context = this@AgoraClassSmallArtActivity, text = error.msg)
            Log.e(TAG, "classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            Log.d(TAG, "class state updated: ${state.name}")
        }

//        override fun onLargeVideoShow(streamUuid: String) {
//            if (isLocalStream(streamUuid)) {
//                val configs = VideoUtils().getVideoEditEncoderConfigs()
//                getEduContext()?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
//            }
//        }
//
//        override fun onLargeVideoDismiss(streamUuid: String) {
//            if (isLocalStream(streamUuid)) {
//                val configs = VideoUtils().getSmallVideoEncoderConfigs()
//                getEduContext()?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
//            }
//        }
//
//        override fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String) {
//            val noneView = viewGroup == null
//            if (noneView) {
//                getEduContext()?.mediaContext()?.stopRenderVideo(streamUuid)
//            } else {
//                getEduContext()?.mediaContext()?.startRenderVideo(config, viewGroup!!, streamUuid)
//            }
//        }
    }

    private val smallContainerUserHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(user)
            if (user.role == AgoraEduContextUserRole.Teacher) {
//                teacherDetailInfo = user.copy()
//                notifyVideos()
            } else if (user.role == AgoraEduContextUserRole.Student) {

            }
        }

        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {
            super.onRemoteUserLeft(user, operator, reason)
            if (user.role == AgoraEduContextUserRole.Teacher) {
//                teacherInfo = null
//                notifyVideos()
            } else if (user.role == AgoraEduContextUserRole.Student) {
            }
        }

        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            kickOut()
        }
    }

//    private fun isLocalStream(streamUuid: String): Boolean {
//        getEduContext()?.let { context ->
//            val localUserId = context.userContext()?.getLocalUserInfo()?.userUuid
//            localUserId?.let { userId ->
//                context.streamContext()?.getStreamList(userId)?.forEach { streamInfo ->
//                    if (streamInfo.streamUuid == streamUuid) {
//                        return true
//                    }
//                }
//            }
//        }
//        return false
//    }

//    private val largeWindowActivateObserver = object : AgoraWidgetActiveObserver {
//        override fun onWidgetActive(widgetId: String) {
//            if (widgetId == AgoraWidgetDefaultId.LargeWindow.id) {
//                handleLargeWindowEvent(true)
//            }
//        }
//
//        override fun onWidgetInActive(widgetId: String) {
//            if (widgetId == AgoraWidgetDefaultId.LargeWindow.id) {
//                handleLargeWindowEvent(false)
//            }
//        }
//    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgoraClassSmallArtBinding.inflate(layoutInflater)
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
            eduCore()?.eduContextPool()?.let { context ->
                context.roomContext()?.addHandler(roomHandler)
                binding.agoraLargeWindowContainer.initView(this)
                binding.agoraClassHead.initView(this)
                binding.agoraClassHead.setTitleToRight()
                binding.agoraEduWhiteboard.initView(uuid, this)

                agoraClassSmallArtPresenter = AgoraClassSmallArtPresenter(binding.agoraClassTeacherVideo, binding.agoraClassUserListVideo)
                binding.agoraEduOptions.itemContainer = binding.agoraEduOptionsItemContainer
                agoraClassSmallArtPresenter?.initView(RoomType.SMALL_CLASS, this, uiController)
                binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                launchConfig?.roleType?.let {
                    binding.agoraEduOptions.setShowOption(RoomType.SMALL_CLASS, it)
                }
            }
            join()
            //大窗Container
//            largeWindowContainer = RelativeLayout(this.applicationContext)
//            val param = ConstraintLayout.LayoutParams(
//                binding.agoraEduWhiteboard.measuredWidth / 2,
//                binding.agoraEduWhiteboard.measuredHeight * 2 / 3
//            )
//            var rootContainer = binding.agoraContainerSmallArt
//
//            param.startToStart = rootContainer.id
//            param.topToTop = rootContainer.id
//            param.endToEnd = rootContainer.id
//            param.bottomToBottom = rootContainer.id
//            rootContainer.addView(largeWindowContainer, param)
        }
//        getEduContext()?.widgetContext()?.addWidgetActiveObserver(
//            largeWindowActivateObserver, AgoraWidgetDefaultId.LargeWindow.id
//        )
    }


//    private fun handleLargeWindowEvent(active: Boolean) {
//        largeVideoWindowWidget?.widgetInfo?.let { widgetInfo ->
//            widgetInfo.roomProperties?.let { properties ->
//                (properties["userUuid"] as? String)?.let { userId ->
//                    // Edu context api does not provide an API to
//                    // obtain the info of a certain single user
//                    getEduContext()?.let { context ->
//                        context.userContext()?.let { userContext ->
//                            userContext.getAllUserList().find { eduUserInfo ->
//                                eduUserInfo.userUuid == userId
//                            }?.let { userInfo ->
//                                (properties["streamUuid"] as? String).let { streamId ->
//                                    context.streamContext()?.getStreamList(userInfo.userUuid)?.find { eduStreamInfo ->
//                                        eduStreamInfo.streamUuid == streamId
//                                    }?.let { streamInfo ->
//                                        sendToLargeWindow(active, userInfo, streamInfo)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

//    private fun sendToLargeWindow(
//        active: Boolean,
//        userInfo: AgoraEduContextUserInfo,
//        streamInfo: AgoraEduContextStreamInfo
//    ) {
//        buildLargeWindowUserInfoData(userInfo, streamInfo)?.let {
//            val signal = if (active) {
//                AgoraLargeWindowInteractionSignal.LargeWindowShowed
//            } else {
//                AgoraLargeWindowInteractionSignal.LargeWindowClosed
//            }
//
//            val packet = AgoraLargeWindowInteractionPacket(signal, it)
//            getEduContext()?.widgetContext()?.sendMessageToWidget(
//                Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id
//            )
//            lastUserDetailInfo = userDetailInfo
//        }
//    }

//    private fun buildLargeWindowUserInfoData(
//        userInfo: AgoraEduContextUserInfo,
//        streamInfo: AgoraEduContextStreamInfo
//    ): AgoraUIUserDetailInfo? {
//        val localVideoState: AgoraEduContextDeviceState2?
//        val localAudioState: AgoraEduContextDeviceState2?
//        if (userInfo.userUuid == getEduContext()?.userContext()?.getLocalUserInfo()?.userUuid) {
//            localVideoState = getUIDataProvider()?.localVideoState
//            localAudioState = getUIDataProvider()?.localAudioState
//        } else {
//            localVideoState = null
//            localAudioState = null
//        }
//
//        return getUIDataProvider()?.toAgoraUserDetailInfo(
//            userInfo,
//            true, streamInfo, localAudioState, localVideoState
//        )
//    }
}