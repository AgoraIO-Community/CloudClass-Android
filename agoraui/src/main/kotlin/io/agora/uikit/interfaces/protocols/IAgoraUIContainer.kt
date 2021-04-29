package io.agora.uikit.interfaces.protocols

import android.view.ViewGroup
import androidx.annotation.UiThread
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextPool
import io.agora.uikit.impl.container.AgoraUI1v1Container
import io.agora.uikit.impl.container.AgoraUISmallClassContainer
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.container.AgoraUILargeClassContainer

interface IAgoraUIContainer {
    fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun layout(): ViewGroup?

    fun showLeave()

    fun kickOut()

    fun showError(error: EduContextError)

    @UiThread
    fun willLaunchExtApp(appIdentifier:String):Int

    fun showTips(msg: String)
}

object AgoraUIContainer {
    fun create(layout: ViewGroup, left: Int, top: Int,
               width: Int, height: Int, type: AgoraContainerType,
               eduContext: EduContextPool): IAgoraUIContainer {
        AgoraUIToastManager.init(layout.context)

        val container = when (type) {
            AgoraContainerType.OneToOne -> {
                AgoraUI1v1Container(eduContext)
            }
            AgoraContainerType.SmallClass -> {
                AgoraUISmallClassContainer(eduContext)
            }
            AgoraContainerType.LargeClass -> {
                AgoraUILargeClassContainer(eduContext)
            }
        }

        container.init(layout, left, top, width, height)
        return container
    }
}