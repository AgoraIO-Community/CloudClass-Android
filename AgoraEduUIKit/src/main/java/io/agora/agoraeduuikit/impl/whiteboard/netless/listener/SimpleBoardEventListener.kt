package io.agora.agoraeduuikit.impl.whiteboard.netless.listener

import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import java.lang.Exception

/**
 * author : hefeng
 * date : 2022/2/22
 * description :
 */
open class SimpleBoardEventListener : BoardEventListener {
    val TAG = "AgoraWhiteBoard"

    override fun onJoinSuccess(state: GlobalState) {
        Constants.AgoraLog?.i(TAG + ":onJoinSuccess->" + GsonUtil.gson.toJson(state))
    }

    override fun onJoinFail(error: SDKError?) {

    }

    override fun onRoomPhaseChanged(phase: RoomPhase) {
        Constants.AgoraLog?.i(TAG + ":onRoomPhaseChanged->" + phase.name)
    }

    override fun onGlobalStateChanged(state: GlobalState) {
        Constants.AgoraLog?.d("$TAG:onGlobalStateChanged->${state}")
    }

    override fun onSceneStateChanged(state: SceneState) {

    }

    override fun onPageStateChanged(state: PageState) {
        Constants.AgoraLog?.e("$TAG:onPageStateChanged->${GsonUtil.gson.toJson(state)}")
    }

    override fun onMemberStateChanged(state: MemberState) {
        Constants.AgoraLog?.e("$TAG:onMemberStateChanged->${GsonUtil.gson.toJson(state)}")
    }

    override fun onCameraStateChanged(state: CameraState) {
        Constants.AgoraLog?.i("$TAG:onCameraStateChanged:${GsonUtil.gson.toJson(state)}")
    }

    override fun onDisconnectWithError(e: Exception?) {
        Constants.AgoraLog?.e("$TAG:onDisconnectWithError->${e?.message}")
    }

    override fun onRoomStateChanged(modifyState: RoomState?) {

    }

    override fun onCanUndoStepsUpdate(canUndoSteps: Long) {

    }

    override fun onCanRedoStepsUpdate(canRedoSteps: Long) {

    }
}