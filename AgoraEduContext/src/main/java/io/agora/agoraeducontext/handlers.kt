package io.agora.agoraeducore.core.context

import android.content.Context
import android.view.View
import android.view.ViewGroup
import io.agora.agoraeducontext.*

interface IChatHandler {
    fun onReceiveMessage(item: EduContextChatItem)

    fun onReceiveChatHistory(history: List<EduContextChatItem>)

    fun onReceiveConversationMessage(item: EduContextChatItem)

    fun onReceiveConversationHistory(history: List<EduContextChatItem>)

    fun onChatAllowed(allowed: Boolean)

    /**
     * @param allowed
     * @param userInfo related user info
     * @param operator who caused this change
     * @param local whether the related user is local user
     */
    fun onChatAllowed(allowed: Boolean, userInfo: EduContextUserInfo, operator: EduContextUserInfo?, local: Boolean)

    fun onChatTips(tip: String)
}

interface IDeviceHandler {
    fun onCameraDeviceEnableChanged(enabled: Boolean)

    fun onCameraFacingChanged(facing: EduContextCameraFacing)

    fun onMicDeviceEnabledChanged(enabled: Boolean)

    fun onSpeakerEnabledChanged(enabled: Boolean)

    fun onDeviceTips(tips: String)
}

interface IHandsUpHandler {
    fun onHandsUpEnabled(enabled: Boolean)

    fun onHandsUpStateUpdated(state: EduContextHandsUpState, coHost: Boolean)

    fun onHandsUpStateResultUpdated(error: EduContextError?)

    fun onHandsUpTips(tips: String)
}

interface IMediaHandler {

}

interface IPrivateChatHandler {
    fun onPrivateChatStarted(info: EduContextPrivateChatInfo)

    fun onPrivateChatEnded()
}

interface IRoomHandler {
    fun onClassroomName(name: String)

    fun onClassState(state: EduContextClassState)

    fun onClassTime(time: String)

    fun onNetworkStateChanged(state: EduContextNetworkState)

    fun onLogUploaded(logData: String)

    fun onConnectionStateChanged(state: EduContextConnectionState)

    fun onClassTip(tip: String)

    fun onFlexRoomPropsInitialized(properties: MutableMap<String, Any>)

    /** @param properties all custom props
     * @param  operator this is null when server update props */
    fun onFlexRoomPropsChanged(changedProperties: MutableMap<String, Any>,
                               properties: MutableMap<String, Any>,
                               cause: MutableMap<String, Any>?, operator: EduContextUserInfo?)

    fun onError(error: EduContextError)

    fun onClassroomJoinSuccess(roomUuid: String, timestamp: Long)

    fun onClassroomJoinFail(roomUuid: String, code: Int?, msg: String?, timestamp: Long)

    fun onClassroomLeft(roomUuid: String, timestamp: Long, exit: Boolean = true)
}

interface IUserHandler {
    fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>)

    fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>)

    fun onUserReward(userInfo: EduContextUserInfo)

    fun onKickOut()

    fun onVolumeUpdated(volume: Int, streamUuid: String)

    fun onUserTip(tip: String)

    fun onRoster(context: Context, anchor: View, type: Int?)

    /** @param  operator this is null when server update props */
    fun onFlexUserPropsChanged(changedProperties: MutableMap<String, Any>,
                               properties: MutableMap<String, Any>,
                               cause: MutableMap<String, Any>?,
                               fromUser: EduContextUserDetailInfo,
                               operator: EduContextUserInfo?)
}

interface IScreenShareHandler {
    // only control the render of screenShare
    fun onScreenShareStateUpdated(state: EduContextScreenShareState, streamUuid: String)

    // only control the display and hide of screenShare
    fun onSelectScreenShare(select: Boolean)

    fun onScreenShareTip(tips: String)
}

interface IVideoHandler {
    fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo)

    fun onVolumeUpdated(volume: Int, streamUuid: String)

    fun onMessageUpdated(msg: String)
}

interface IWhiteboardHandler {
    fun onWhiteboardJoinSuccess(config: WhiteboardDrawingConfig)

    fun onWhiteboardJoinFail(msg: String)

    fun onWhiteboardLeft(boardId: String, timestamp: Long)

    /**
     * Gets the parent container of the whiteboard
     * */
    fun getBoardContainer(): ViewGroup?

    /**
     * Called when the whiteboard drawing config is to be set,
     * like pencil shapes, color, or font size
     */
    fun onDrawingConfig(config: WhiteboardDrawingConfig)

    /**
     * Called when the change of whiteboard drawing config
     * is enabled or disabled.
     */
    fun onDrawingEnabled(enabled: Boolean)

    /**
     * Set current page number and page count for current ppt
     */
    fun onPageNo(no: Int, count: Int)

    /**
     * Set if the page control and tool bar is allowed
     */
    fun onPagingEnabled(enabled: Boolean)

    /**
     * Set if the zooming of whiteboard is enabled
     */
    fun onZoomEnabled(zoomOutEnabled: Boolean?, zoomInEnabled: Boolean?)

    /**
     * Set if whiteboard is allowed to be made full screen
     */
    fun onFullScreenEnabled(enabled: Boolean)

    /**
     * Called whether the whiteboard is made full screen or not
     */
    fun onFullScreenChanged(isFullScreen: Boolean)

    /**
     * Called when the interaction with whiteboard is enabled
     * or disabled, including paging, zooming and resizing
     * (full screen)
     */
    fun onInteractionEnabled(enabled: Boolean)

    fun onBoardPhaseChanged(phase: EduBoardRoomPhase)

    fun onDownloadProgress(url: String, progress: Float)

    fun onDownloadTimeout(url: String)

    fun onDownloadCompleted(url: String)

    fun onDownloadError(url: String)

    fun onDownloadCanceled(url: String)

    /**
     * Called when the whiteboard authorization is granted or not
     */
    fun onPermissionGranted(granted: Boolean)
}

interface IHandlerPool<T> {
    fun addHandler(handler: T?)

    fun removeHandler(handler: T?)

    fun getHandlers(): List<T>?
}