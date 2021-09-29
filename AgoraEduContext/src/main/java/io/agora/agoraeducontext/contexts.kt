package io.agora.agoraeducore.core.context

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import io.agora.agoraeducontext.*
import io.agora.agoraeduextapp.AgoraExtAppInfo

abstract class ChatContext : AbsHandlerPool<IChatHandler>() {
    /**
     * @param message the string text message
     * @param timestamp
     * @param callback result of sending this message, the server id and
     * timestamp of this message will be returned if success
     */
    abstract fun sendLocalChannelMessage(message: String, timestamp: Long,
                                         callback: EduContextCallback<EduContextChatItemSendResult>): EduContextChatItem

    /**
     * @param startId the start message id (exclusive) to search the
     * message history from reversely
     */
    abstract fun fetchChannelHistory(startId: String?, count: Int? = 50, callback: EduContextCallback<List<EduContextChatItem>>)

    /**
     * Conversations are currently taken as Q&A sessions,
     * and the messages are sent to the so-call "groups" that
     * are named with the local users' ids internally by aPaaS server.
     * The messages are sent to the group that both the teacher and TA
     * can see and reply
     */
    abstract fun sendConversationMessage(message: String, timestamp: Long,
                                         callback: EduContextCallback<EduContextChatItemSendResult>): EduContextChatItem

    abstract fun fetchConversationHistory(startId: String?, callback: EduContextCallback<List<EduContextChatItem>>)
}

abstract class DeviceContext : io.agora.agoraeducore.core.context.AbsHandlerPool<IDeviceHandler>() {
    abstract fun getDeviceConfig(): EduContextDeviceConfig

    abstract fun setCameraDeviceEnable(enable: Boolean)

    abstract fun switchCameraFacing()

    abstract fun setMicDeviceEnable(enable: Boolean)

    abstract fun setSpeakerEnable(enable: Boolean)

    abstract fun setDeviceLifecycle(lifecycle: EduContextDeviceLifecycle)
}

interface ExtAppContext {
    /**
     * Init ext app context with a container layout to
     * set the ext app window.
     * Must be called before using any of the extension
     * app functions.
     */
    fun init(container: RelativeLayout)

    fun launchExtApp(appIdentifier: String): Int

    fun getRegisteredExtApps(): List<AgoraExtAppInfo>
}

abstract class HandsUpContext : AbsHandlerPool<IHandsUpHandler>() {
    abstract fun performHandsUp(state: EduContextHandsUpState, callback: EduContextCallback<Boolean>? = null)
}

abstract class MediaContext : AbsHandlerPool<IMediaHandler>() {
    abstract fun startPreview(container: ViewGroup)

    abstract fun stopPreview()

    abstract fun openCamera()

    abstract fun closeCamera()

    abstract fun openMicrophone()

    abstract fun closeMicrophone()

    abstract fun publishStream(type: EduContextMediaStreamType)

    abstract fun unPublishStream(type: EduContextMediaStreamType)

    abstract fun setVideoEncoderConfig(videoEncoderConfig: EduContextVideoEncoderConfig)
}

abstract class PrivateChatContext : AbsHandlerPool<IPrivateChatHandler>() {
    abstract fun getLocalUserInfo(): EduContextUserInfo

    abstract fun startPrivateChat(peerId: String, callback: EduContextCallback<EduContextPrivateChatInfo>? = null)

    abstract fun endPrivateChat(callback: EduContextCallback<Boolean>?)
}

abstract class RoomContext : AbsHandlerPool<IRoomHandler>() {
    abstract fun roomInfo(): EduContextRoomInfo

    abstract fun leave(exit: Boolean = true)

    abstract fun uploadLog(quiet: Boolean = false)

    abstract fun updateFlexRoomProps(properties: MutableMap<String, String>, cause: MutableMap<String, String>?)

    abstract fun joinClassroom()
}

abstract class ScreenShareContext : AbsHandlerPool<IScreenShareHandler>() {
    abstract fun setScreenShareState(state: EduContextScreenShareState)

    abstract fun renderScreenShare(container: ViewGroup?, streamUuid: String)
}

abstract class UserContext : AbsHandlerPool<IUserHandler>() {
    abstract fun localUserInfo(): EduContextUserInfo

    abstract fun muteVideo(muted: Boolean)

    abstract fun muteAudio(muted: Boolean)

    abstract fun renderVideo(container: ViewGroup?, streamUuid: String, renderConfig: EduContextRenderConfig)

    abstract fun updateFlexUserProps(userUuid: String, properties: MutableMap<String, String>,
                                     cause: MutableMap<String, String>?)

    abstract fun setVideoEncoderConfig(config: EduContextVideoEncoderConfig)
}

abstract class VideoContext : AbsHandlerPool<IVideoHandler>() {
    abstract fun updateVideo(enabled: Boolean)

    abstract fun updateAudio(enabled: Boolean)

    abstract fun renderVideo(viewGroup: ViewGroup?, streamUuid: String, renderConfig: EduContextRenderConfig)

    abstract fun setVideoEncoderConfig(config: EduContextVideoEncoderConfig)
}

abstract class WhiteboardContext : AbsHandlerPool<IWhiteboardHandler>() {
    abstract fun initWhiteboard(container: ViewGroup)

    abstract fun joinWhiteboard()

    abstract fun isGranted(): Boolean

    abstract fun leave()

    // Drawing configs
    abstract fun selectAppliance(type: WhiteboardApplianceType)

    abstract fun selectColor(color: Int)

    abstract fun selectFontSize(size: Int)

    abstract fun selectThickness(thick: Int)

    abstract fun selectRoster(anchor: View)

    // whiteBoard
    abstract fun setBoardInputEnable(enable: Boolean)

    abstract fun skipDownload(url: String?)

    abstract fun cancelDownload(url: String?)

    abstract fun retryDownload(url: String?)

    // page control
    abstract fun setFullScreen(full: Boolean)

    abstract fun setZoomOut()

    abstract fun setZoomIn()

    abstract fun setPrevPage()

    abstract fun setNextPage()
}

interface WidgetContext {
    fun getWidgetProperties(type: WidgetType): Map<String, Any>?
}

abstract class AbsHandlerPool<T> : IHandlerPool<T> {
    private val handlers: MutableList<T> = mutableListOf()

    override fun addHandler(handler: T?) {
        handler?.let { h ->
            synchronized(this) {
                if (!handlers.contains(h)) {
                    handlers.add(h)
                }
            }
        }
    }

    override fun removeHandler(handler: T?) {
        handler?.let { h ->
            synchronized(this) {
                if (handlers.contains(h)) {
                    handlers.remove(h)
                }
            }
        }
    }

    override fun getHandlers(): List<T>? {
        return handlers.toList()
    }
}

interface EduContextPool {
    fun chatContext(): ChatContext?

    fun handsUpContext(): HandsUpContext?

    fun roomContext(): RoomContext?

    fun mediaContext(): MediaContext?

    fun deviceContext(): DeviceContext?

    fun screenShareContext(): ScreenShareContext?

    fun userContext(): UserContext?

    fun videoContext(): VideoContext?

    fun whiteboardContext(): WhiteboardContext?

    fun privateChatContext(): PrivateChatContext?

    fun extAppContext(): ExtAppContext?

    fun widgetContext(): WidgetContext?
}