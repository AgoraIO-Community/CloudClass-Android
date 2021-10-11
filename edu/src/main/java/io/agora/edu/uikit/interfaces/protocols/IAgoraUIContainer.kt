package io.agora.edu.uikit.interfaces.protocols

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.UiThread
import io.agora.edu.uikit.impl.container.*
import io.agora.edu.uikit.component.toast.AgoraUIToastManager

interface IAgoraUIContainer {
    fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun layout(): ViewGroup?

    fun showLeave()

    fun kickOut()

    fun showError(error: io.agora.edu.core.context.EduContextError)

    @UiThread
    fun willLaunchExtApp(appIdentifier:String):Int

    fun showTips(msg: String)

    fun getWhiteboardContainer(): ViewGroup?

    fun setActivity(activity: Activity)

    fun getActivity(): Activity?

    fun release()
}

object AgoraUIContainer {
    fun create(layout: ViewGroup, left: Int, top: Int,
               width: Int, height: Int, type: AgoraContainerType,
               eduContext: io.agora.edu.core.context.EduContextPool,
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