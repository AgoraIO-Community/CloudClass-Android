package io.agora.agoraeduuikit.interfaces.protocols

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.UiThread
import io.agora.agoraeduuikit.impl.container.*
import io.agora.agoraeduuikit.impl.container.*
import io.agora.agoraeduuikit.component.toast.AgoraUIToast

interface IAgoraUIContainer {
    fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun layout(): ViewGroup?

    fun showLeave()

    fun kickOut()

    fun showError(error: io.agora.agoraeducore.core.context.EduContextError)

    @UiThread
    fun willLaunchExtApp(appIdentifier: String): Int

    fun showTips(msg: String)

    fun setActivity(activity: Activity)

    fun getActivity(): Activity?

    fun release()
}

object AgoraUIContainer {
    fun create(layout: ViewGroup, left: Int, top: Int,
               width: Int, height: Int, type: AgoraContainerType,
               eduContext: io.agora.agoraeducore.core.context.EduContextPool,
               config: AgoraContainerConfig): IAgoraUIContainer {
        AgoraUIToast.init(layout.context)

        val container = when (type) {
            AgoraContainerType.OneToOne -> {
                AgoraUI1v1Container(eduContext, config)
            }
            AgoraContainerType.SmallClass -> {
                AgoraUISmallClassContainer(eduContext, config)
            }
            AgoraContainerType.SmallClassArt -> {
                AgoraUISmallClassArtContainer(eduContext, config)
            }
            AgoraContainerType.LargeClass -> {
                AgoraUILargeClassContainer(eduContext, config)
            }
            AgoraContainerType.Debug -> {
                AgoraDebugContainer(eduContext, config)
            }
        }

        container.init(layout, left, top, width, height)//layout: contentLayout
        return container
    }
}