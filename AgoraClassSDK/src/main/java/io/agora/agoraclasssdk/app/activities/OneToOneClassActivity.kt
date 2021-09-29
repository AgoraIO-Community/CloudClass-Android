package io.agora.agoraclasssdk.app.activities

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import io.agora.agoraclasssdk.R
import io.agora.agoraeducontext.*
import io.agora.agoraeducontext.handlerimpl.RoomHandler
import io.agora.agoraeducontext.handlerimpl.WhiteboardHandler
import io.agora.agoraeduuikit.impl.chat.tabs.ChatTabConfig
import io.agora.agoraeduuikit.impl.chat.tabs.TabType
import io.agora.agoraeduuikit.impl.container.AgoraContainerConfig
import io.agora.agoraeduuikit.impl.container.AgoraContainerType
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIContainer
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.sdk.app.activities.BaseClassActivity
import io.agora.agoraeducore.sdk.app.activities.ClassJoinStateCountDown

class OneToOneClassActivity : BaseClassActivity() {
    private val tag = "OneToOneClassActivity"

    private val countDown = ClassJoinStateCountDown()

    private val roomHandler = object : RoomHandler() {
        override fun onConnectionStateChanged(state: EduContextConnectionState) {
            Log.d(tag, "connection state changed: ${state.name}")
        }

        override fun onClassroomJoinSuccess(roomUuid: String, timestamp: Long) {
            Log.d(tag, "classroom $roomUuid joined success")
            eduCore()?.eduContextPool()?.extAppContext()?.init(contentLayout!!)
        }

        override fun onClassroomJoinFail(roomUuid: String, code: Int?, msg: String?, timestamp: Long) {
            Log.e(tag, "classroom $roomUuid joined fail")
            container?.showError(EduContextError(code ?: -1, msg ?: ""))
        }

        override fun onClassroomLeft(roomUuid: String, timestamp: Long, exit: Boolean) {
            Log.d(tag, "classroom left, room id $roomUuid, ts $timestamp")
            if(exit) {
                finish()
            }
        }
    }

    private val whiteboardHandler = object : WhiteboardHandler() {
        override fun onWhiteboardJoinSuccess(config: WhiteboardDrawingConfig) {
            Log.d(tag, "whiteboard join success")
        }

        override fun onWhiteboardJoinFail(msg: String) {
            ContextCompat.getMainExecutor(this@OneToOneClassActivity).execute {
                container?.showError(EduContextError(-1, msg))
            }
        }

        override fun onWhiteboardLeft(boardId: String, timestamp: Long) {
            Log.d(tag, "whiteboard left, board id $boardId, ts $timestamp")
        }
    }

    override fun onContentViewLayout(): RelativeLayout {
        contentLayout = RelativeLayout(this)
        return contentLayout!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareUi()
        createEduCore(autoSubscribe = true, autoPublish = true, callback = object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                countDown.countdownRoomInit()
                checkReady()
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    ToastManager.showShort(it.msg)
                }
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        eduCore()?.eduContextPool()?.deviceContext()?.setDeviceLifecycle(EduContextDeviceLifecycle.Resume)
    }

    override fun onStop() {
        super.onStop()
        eduCore()?.eduContextPool()?.deviceContext()?.setDeviceLifecycle(EduContextDeviceLifecycle.Stop)
    }

    private fun prepareUi() {
        contentLayout?.viewTreeObserver?.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (contentLayout!!.width > 0 && contentLayout!!.height > 0) {
                            contentLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            countDown.countdownUiReady()
                            checkReady()
                        }
                    }
                })
    }

    private fun checkReady() {
        if (countDown.isDone()) {
            runOnUiThread {
                eduCore()?.let { core ->
                    core.eduContextPool().let { context ->
                        context.extAppContext()?.init(contentLayout!!)
                        context.roomContext()?.addHandler(roomHandler)
                        context.whiteboardContext()?.addHandler(whiteboardHandler)

                        createUI(core, contentLayout!!.width, contentLayout!!.height)
                        container?.let { container ->
                            container.getWhiteboardContainer()?.let { parent ->
                                context.whiteboardContext()?.initWhiteboard(parent)
                            }
                        }
                    }

                    join()
                }
            }
        }
    }

    private fun createUI(eduCore: AgoraEduCore, width: Int, height: Int) {
        container = AgoraUIContainer.create(contentLayout!!,
                0, 0, width, height,
                AgoraContainerType.OneToOne,
                eduCore.eduContextPool(),
                AgoraContainerConfig(chatTabConfigs =
                listOf(ChatTabConfig(getString(
                        R.string.agora_chat_tab_message),
                        TabType.Public, null)
                )))
        container?.setActivity(this@OneToOneClassActivity)
    }

    private fun join() {
        // It's application's business to determine whether
        // to join the classroom or whiteboard, and when.
        // Here, as an example, we just join the classroom
        // and whiteboard as soon as edu core is initialized
        eduCore()?.eduContextPool()?.roomContext()?.joinClassroom()
        eduCore()?.eduContextPool()?.whiteboardContext()?.joinWhiteboard()
    }
}