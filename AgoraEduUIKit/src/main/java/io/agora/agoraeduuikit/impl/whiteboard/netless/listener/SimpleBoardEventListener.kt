package io.agora.agoraeduuikit.impl.whiteboard.netless.listener

import com.herewhite.sdk.domain.*
import java.lang.Exception

/**
 * author : hefeng
 * date : 2022/2/22
 * description :
 */
open class SimpleBoardEventListener : BoardEventListener {
    override fun onJoinSuccess(state: GlobalState) {

    }

    override fun onJoinFail(error: SDKError?) {

    }

    override fun onRoomPhaseChanged(phase: RoomPhase) {

    }

    override fun onGlobalStateChanged(state: GlobalState) {

    }

    override fun onSceneStateChanged(state: SceneState) {

    }

    override fun onMemberStateChanged(state: MemberState) {

    }

    override fun onCameraStateChanged(state: CameraState) {

    }

    override fun onDisconnectWithError(e: Exception?) {

    }

    override fun onRoomStateChanged(modifyState: RoomState?) {

    }

    override fun onCanUndoStepsUpdate(canUndoSteps: Long) {

    }

    override fun onCanRedoStepsUpdate(canRedoSteps: Long) {

    }
}