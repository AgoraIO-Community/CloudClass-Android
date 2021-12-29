package io.agora.agoraclasssdk.app.activities

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import com.google.gson.Gson
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.edu.classroom.EduDebugMode
import io.agora.agoraeducore.core.internal.edu.classroom.view.ActivityFitLayout
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeduuikit.impl.container.AgoraContainerConfig
import io.agora.agoraeduuikit.impl.container.AgoraContainerType
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIContainer

class LargeClassActivity : BaseClassActivity() {
    private val tag = "LargeClassActivity"

    private val countDown = ClassJoinStateCountDown()

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            Log.d(tag, "classroom ${roomInfo.roomUuid} joined success")
            eduCore()?.eduContextPool()?.extAppContext()?.init(activityLayout!!)
            initSystemDevices()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            Log.e(tag, "classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")
            container?.showError(error)
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            Log.d(tag, "class state updated: ${state.name}")
        }
    }

    override fun onContentViewLayout(): RelativeLayout {
        RelativeLayout(this).let { container ->
            container.setBackgroundColor(resources.getColor(R.color.gray_F9F9FC))
            activityLayout = container
            ActivityFitLayout(this).let {
                contentLayout = it
                it.setBackgroundColor(resources.getColor(R.color.gray_F9F9FC))
                val param = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT)
                param.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                container.addView(it, param)
                setContentView(container)
            }
        }
        return activityLayout!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareUi()
        createEduCore(object : EduContextCallback<Unit> {
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

    private fun prepareUi() {
        activityLayout?.viewTreeObserver?.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (activityLayout!!.width > 0 && activityLayout!!.height > 0) {
                            activityLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
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
                        context.extAppContext()?.init(activityLayout!!)
                        context.roomContext()?.addHandler(roomHandler)

                        createUI(core, contentLayout!!.width, contentLayout!!.height)
                    }

                    join()
                }
            }
        }
    }

    private fun createUI(eduCore: AgoraEduCore, width: Int, height: Int) {
        if (EduDebugMode.useDebugUI) {
            Log.i(tag, "create debug ui container")
            container = AgoraUIContainer.create(contentLayout!!,
                    0, 0, width, height,
                    AgoraContainerType.Debug,
                    eduCore.eduContextPool(),
                    AgoraContainerConfig())
        } else {
            container = AgoraUIContainer.create(contentLayout!!,
                    0, 0, width, height,
                    AgoraContainerType.LargeClass,
                    eduCore.eduContextPool(),
                    AgoraContainerConfig(isGARegion = launchConfig?.isGARegion()
                    ?: false))
        }

        container?.setActivity(this@LargeClassActivity)
    }

    private fun join() {
        // It's application's business to determine whether
        // to join the classroom or whiteboard, and when.
        // Here, as an example, we just join the classroom
        // and whiteboard as soon as edu core is initialized
        eduCore()?.eduContextPool()?.roomContext()?.joinRoom(null)
    }

    override fun onBackPressed() {
        if (!EduDebugMode.useDebugUI) {
            super.onBackPressed()
        } else {
            finish()
        }
    }
}