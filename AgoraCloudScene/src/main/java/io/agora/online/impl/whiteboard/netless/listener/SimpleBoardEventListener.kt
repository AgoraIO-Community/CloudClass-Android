package io.agora.online.impl.whiteboard.netless.listener

import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import java.lang.Exception

/**
 * author : felix
 * date : 2022/2/22
 * description :
 */
open class SimpleBoardEventListener : BoardEventListener {
    val TAG = "AgoraWhiteBoard"

    override fun onJoinSuccess(state: GlobalState) {
        LogX.i(TAG, ":onJoinSuccess->" + GsonUtil.gson.toJson(state))
    }

    override fun onJoinFail(error: SDKError?) {

    }

    override fun onRoomPhaseChanged(phase: RoomPhase) {
        LogX.i(TAG, ":onRoomPhaseChanged->" + phase.name)
    }

    override fun onGlobalStateChanged(state: GlobalState) {
        LogX.d(TAG, "onGlobalStateChanged->${state}")
    }

    override fun onSceneStateChanged(state: SceneState) {

    }

    override fun onPageStateChanged(state: PageState) {
        LogX.i(TAG, "onPageStateChanged->${GsonUtil.gson.toJson(state)}")
    }

    override fun onMemberStateChanged(state: MemberState) {
        LogX.i(TAG, "onMemberStateChanged->${GsonUtil.gson.toJson(state)}")
    }

    override fun onCameraStateChanged(state: CameraState) {
        LogX.i(TAG, "onCameraStateChanged:${GsonUtil.gson.toJson(state)}")
    }

    override fun onDisconnectWithError(e: Exception?) {
        LogX.e(TAG, "onDisconnectWithError->${e?.message}")
    }

    override fun onRoomStateChanged(modifyState: RoomState?) {

    }

    override fun onCanUndoStepsUpdate(canUndoSteps: Long) {

    }

    override fun onCanRedoStepsUpdate(canRedoSteps: Long) {

    }
}