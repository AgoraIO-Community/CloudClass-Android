package io.agora.online.component.teachaids.component

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import io.agora.online.component.AgoraEduVideoComponent
import io.agora.online.component.common.AbsAgoraEduConfigComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.helper.AgoraRendererUtils
import io.agora.online.helper.RoomPropertiesHelper
import io.agora.online.view.FcrDragTouchGroupView
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.impl.managers.FCRWidgetSyncFrameObserver
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.R
import io.agora.online.component.toast.AgoraUIToast
import io.agora.online.config.FcrUIConfig
import io.agora.online.impl.video.AgoraUILargeVideoWidget
import io.agora.online.interfaces.listeners.IAgoraUIVideoListener
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.online.provider.UIDataProviderListenerImpl
import kotlin.math.roundToInt

/**
 * author : felix
 * date : 2023/2/2
 * description : 拓展屏，显示本地视频
 * https://confluence.agoralab.co/pages/viewpage.action?pageId=1009189266
 */
abstract class FcrLocalWindowComponent : AbsAgoraEduConfigComponent<FcrUIConfig>,
    AgoraUILargeVideoWidget.IAgoraUILargeVideoListener, FCRWidgetSyncFrameObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    val TAG = "FcrLocalWindowComponent"
    var agoraLargeWindowContainer: View? = null //当前view所在的container
    var videoView: AgoraEduVideoComponent? = null
    var touchView: FcrDragTouchGroupView? = null
    val defaultSizeWidthPercent = 0.2F
    val defaultSizeHeightPercent = 0.15F

    //var snackbar: AgoraUISnackbar? = null
    var myStreamUuid: String? = null
    var myLargeWidgetId: String? = null
    var isShowLocalVideo = false
    var lastCoHostList = ArrayList<AgoraEduContextUserInfo>()

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        agoraLargeWindowContainer = agoraUIProvider.getLargeVideoArea()

        addExternalListener()

        //snackbar = AgoraUISnackbar(context)
        //snackbar?.attachView(getContainerView())
        //snackbar?.dismiss()
    }

    fun addExternalListener() {
        if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActive, AgoraWidgetDefaultId.LargeWindow.id)

            eduCore?.eduContextPool()?.roomContext()?.addHandler(object : RoomHandler() {
                override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
                    super.onJoinRoomSuccess(roomInfo)
                    myStreamUuid = eduCore?.eduContextPool()?.streamContext()?.getMyStreamInfo()?.streamUuid
                    // 记录第一次上台的人
                    lastCoHostList.clear()
                    eduCore?.eduContextPool()?.userContext()?.getCoHostList()?.let {
                        lastCoHostList.addAll(it)
                    }
                    //LogX.e(TAG, "onJoinRoomSuccess myStreamUuid=$myStreamUuid")
                }
            })

            agoraUIProvider.getUIDataProvider()?.addListener(object : UIDataProviderListenerImpl() {
                override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
                    super.onCoHostListChanged(userList)
                    stopUnCoListStream()
                    //LogX.i(TAG, "onCoHostListChanged : ${userList}")
                    //LogX.i(TAG, "CoHostList : ${eduCore?.eduContextPool()?.userContext()?.getCoHostList()}")
                    updateVideoGalleryView(eduCore?.eduContextPool()?.streamContext()?.getMyStreamInfo())
                }
            })
        }
    }

    /**
     * 开启拓展屏，不要订阅台下
     */
    @Synchronized
    fun stopUnCoListStream() {
        if (RoomPropertiesHelper.isOpenExternalScreen(eduCore)) { // 开启了拓展屏，只订阅CoHostList上的人
            val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid ?: ""
            val nowCoHostList = eduCore?.eduContextPool()?.userContext()?.getCoHostList() // 最新台上的人
            val myUuid = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid

            //LogX.e(TAG, "上一次台上的人 = ${lastCoHostList}")
            //LogX.e(TAG, "最新台上的人 = ${nowCoHostList}")
            //LogX.e(TAG, "myUuid = ${myUuid}")

            lastCoHostList.forEach { userInfo ->
                var inCoHost = false
                // 上一次上台的人在不在新的coHost里面
                if (myUuid == userInfo.userUuid) {
                    inCoHost = true
                } else {
                    nowCoHostList?.forEach {
                        if (userInfo.userUuid == it.userUuid) {
                            inCoHost = true
                        }
                    }
                }
                if (!inCoHost) { // 不在台上，取消订阅
                    ContextCompat.getMainExecutor(context).execute {
                        eduCore?.eduContextPool()?.streamContext()?.getStreamList(userInfo.userUuid)
                            ?.forEach { stream ->
                                val streamUuid = stream.streamUuid
                                eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, streamUuid)
                                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
                                LogX.e(TAG, "拓展屏开启，取消订阅不在台上的人 streamUuid = ${streamUuid} || userUuid=${stream.owner.userUuid}")
                            }
                    }
                }
            }

            lastCoHostList.clear()
            if (nowCoHostList != null) {
                lastCoHostList.addAll(nowCoHostList)
            }
        }

        lastCoHostList.clear()
        eduCore?.eduContextPool()?.userContext()?.getCoHostList()?.let {
            lastCoHostList.addAll(it)
        }
    }

    fun updateVideoGalleryView(streamInfo: AgoraEduContextStreamInfo? = null) {
        // 隐藏讲台
        ContextCompat.getMainExecutor(context).execute {
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
                if (RoomPropertiesHelper.isOpenExternalScreen(eduCore)) { // 开启拓展屏
                    if (isShowLocalVideo) { // 如果此时是上台操作，需要移除本地视频
                        if (isCoHost() || isLargeWidget()) {
                            LogX.i(TAG, "讲台区有我或大窗上有我，不显示本地视频")
                            isShowLocalVideo = false
                            removeLocalVideo()
                        } else {
                            LogX.i(TAG, "渲染本地视频 streamInfo=$streamInfo")
                            upsertUserDetailInfo(streamInfo)
                        }
                        return@execute
                    }
                    //snackbar?.show()

                    if (isCoHost() || isLargeWidget()) { // 1. 讲台区有我或大窗上有我，啥也不用处理，老师端会创建到大窗显示
                        LogX.i(TAG, "讲台区有我或大窗上有我，不显示本地视频")

                        isShowLocalVideo = false
                        //snackbar?.dismiss()
                        removeLocalVideo()
                    } else {  // 2. 如果不在讲台区域或者在大窗无我，开启拓展屏，右下角显示我的视频
                        AgoraUIToast.info(context, text = String.format(context.getString(R.string.fcr_exteral_video_tips)))
                        LogX.i(TAG, "拓展屏已经开启，显示本地视频")
                        removeLocalVideo()

                        isShowLocalVideo = true
                        showLocalVideo(getContainerView())
                        upsertUserDetailInfo(streamInfo)
                    }
                } else {
                    //LogX.i(TAG, "没有开启拓展屏")

                    isShowLocalVideo = false
                    //snackbar?.dismiss()
                    removeLocalVideo()
                }
            }
        }
    }

    fun upsertUserDetailInfo(streamInfo: AgoraEduContextStreamInfo?) {
        streamInfo?.let { // 3. 渲染本地数据并且依据接口数据来控制是否发流
            LogX.i(TAG, "3. 渲染本地数据并且依据接口数据来控制是否发流 ${streamInfo.owner.userUuid} || ${eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid}")
            if (streamInfo.owner.userUuid == eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid) {
                LogX.i(TAG, "拓展屏，streamInfo=${streamInfo}")
                videoView?.upsertUserDetailInfo(convertStreamInfo(streamInfo))
            }
        }
    }

    /**
     * 我是否在讲台区域
     */
    fun isCoHost(): Boolean {
        var isCoHost = false
        val userUuid = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid
        val list = eduCore?.eduContextPool()?.userContext()?.getCoHostList()
        if (list?.find { it.userUuid == userUuid } != null) {
            isCoHost = true
            LogX.i(TAG, "讲台区有我")
        }
        return isCoHost
    }

    /**
     * 我是否在大窗区域
     */
    fun isLargeWidget(): Boolean {
        var isLargeWidget = false
        eduContext?.widgetContext()?.getAllWidgetActive()?.forEach { entry ->
            if (entry.key.startsWith("streamWindow")) {
                if (entry.value) {
                    ContextCompat.getMainExecutor(context).execute {
                        // widgetId：streamWindow-1869983176
                        val streamUuid = entry.key.split("-")
                        if (streamUuid[1] == myStreamUuid) {
                            isLargeWidget = true
                            LogX.i(TAG, "大窗上有我")
                        }
                    }
                }
            }
        }
        return isLargeWidget
    }

    fun showLocalVideo(viewGroup: ViewGroup) {
        viewGroup.addView(getLocalVideo(), getLocalVideoViewSize(viewGroup))
    }

    fun removeLocalVideo() {
        videoView?.upsertUserDetailInfo(null)
        videoView?.videoListener = null
        touchView?.removeAllViews()
        getContainerView().removeView(touchView)
        //LogX.e(TAG, "removeLocalVideo=$touchView")
    }

    fun convertStreamInfo(streamInfo: AgoraEduContextStreamInfo): AgoraUIUserDetailInfo? {
        val userInfo = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()
        userInfo?.let {
            val hasVideo = (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Camera)
                    && (streamInfo.videoSourceState == AgoraEduContextMediaSourceState.Open)

            val hasAudio = (streamInfo.audioSourceType == AgoraEduContextAudioSourceType.Mic)
                    && (streamInfo.audioSourceState == AgoraEduContextMediaSourceState.Open)

            //val hasAudio = false

            return AgoraUIUserDetailInfo(
                userInfo.userUuid,
                userInfo.userName,
                AgoraEduContextUserRole.Student,
                false,
                0,
                false,
                true,
                hasAudio,
                hasVideo,
                streamInfo.streamUuid,
                streamInfo.streamName,
                streamInfo.streamType,
                streamInfo.audioSourceType,
                streamInfo.videoSourceType,
                streamInfo.audioSourceState,
                streamInfo.videoSourceState
            )
        }
        return null
    }

    fun getLocalVideo(): View {
        videoView = AgoraEduVideoComponent(context)
        videoView?.initView(agoraUIProvider)
        videoView?.videoListener = object : IAgoraUIVideoListener {
            override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
                AgoraRendererUtils.onRendererContainer(eduCore, viewGroup, info, false)
            }
        }
        //videoView?.upsertUserDetailInfo(getLocalStreamInfo())
        touchView = FcrDragTouchGroupView(context)
        touchView?.setDragRange(agoraLargeWindowContainer?.width ?: 0, agoraLargeWindowContainer?.height ?: 0)
        touchView?.setEnableDrag(false)
        touchView?.z = Int.MAX_VALUE / 4f // 设置最顶层

        touchView?.addView(videoView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        LogX.e(TAG,"addLocalVideo=$touchView")
        return touchView!!
    }

    private fun getLocalVideoViewSize(allWidgetsContainer: ViewGroup): LayoutParams {
        val allWidgetsContainerWidth = allWidgetsContainer.width
        val allWidgetsContainerHeight = allWidgetsContainer.height
        //val width = allWidgetsContainerWidth.toFloat() * defaultSizeWidthPercent
        //val height = allWidgetsContainerHeight.toFloat() * defaultSizeHeightPercent
        val width = context.resources.getDimension(R.dimen.fcr_local_video_w)
        val height = context.resources.getDimension(R.dimen.fcr_local_video_h)
        val params = LayoutParams(width.roundToInt(), height.roundToInt())
        params.gravity = Gravity.RIGHT or Gravity.BOTTOM
        //params.leftMargin = (0.9f * (allWidgetsContainerWidth - width)).roundToInt()
        //params.topMargin = (0.8f * (allWidgetsContainerHeight - height)).roundToInt()
        params.rightMargin = context.resources.getDimensionPixelOffset(R.dimen.fcr_local_video_right)
        params.bottomMargin = context.resources.getDimensionPixelOffset(R.dimen.fcr_local_video_bottom)
        return params
    }

    private val widgetActive = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
                if (widgetId.startsWith("streamWindow")) {
                    val streamUuid = widgetId.split("-")
                    if (streamUuid[1] == myStreamUuid) {
                        myLargeWidgetId = widgetId
                    }
                }

                ContextCompat.getMainExecutor(context).execute {
                    // 在拓展屏中，点击上台，显示大窗，隐藏右下角视频窗
                    if (myLargeWidgetId == widgetId && RoomPropertiesHelper.isOpenExternalScreen(eduCore)) {
                        LogX.i(TAG, "在拓展屏中，点击上台，显示大窗，隐藏右下角视频窗")
                        isShowLocalVideo = false
                        //snackbar?.dismiss()
                        removeLocalVideo()
                    }
                }
            }
        }

        override fun onWidgetInActive(widgetId: String) {

        }
    }

    override fun release() {
        super.release()
        eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActive, AgoraWidgetDefaultId.LargeWindow.id)
    }

    abstract fun getContainerView(): ViewGroup
}