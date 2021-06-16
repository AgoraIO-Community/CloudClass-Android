package io.agora.uikit.interfaces.protocols

import android.view.ViewGroup
import androidx.annotation.UiThread
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextPool
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.container.*

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

    fun release()
}

object AgoraUIContainer {
    fun create(layout: ViewGroup, left: Int, top: Int,
               width: Int, height: Int, type: AgoraContainerType,
               eduContext: EduContextPool,
               config: AgoraContainerConfig): IAgoraUIContainer {
        AgoraUIToastManager.init(layout.context)

        val container = when (type) {
            AgoraContainerType.OneToOne -> {
                AgoraUI1v1Container(eduContext, config)
            }
            AgoraContainerType.SmallClass -> {
                AgoraUISmallClassContainer(eduContext, config)
            }
            AgoraContainerType.LargeClass -> {
                AgoraUILargeClassContainer(eduContext, config)
            }
            AgoraContainerType.Debug -> {
                AgoraDebugContainer(eduContext, config)
            }
        }

        container.init(layout, left, top, width, height)
        return container
    }
}