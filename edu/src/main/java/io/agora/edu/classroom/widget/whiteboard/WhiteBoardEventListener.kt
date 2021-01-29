package io.agora.edu.classroom.widget.whiteboard

import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.SceneState

interface WhiteBoardEventListener {
    fun onDisableDeviceInput(disable: Boolean)

    fun onDisableCameraTransform(disable: Boolean)

    fun onSceneStateChanged(state: SceneState?)

    fun onMemberStateChanged(state: MemberState?)
}