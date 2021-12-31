package io.agora.agoraeduuikit.impl.container

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import io.agora.agoraeduuikit.R

class AgoraDebugContainer(
        eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {

    private val tag = "AgoraDebugContainer"

    private var logTextView: AppCompatTextView? = null

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        LayoutInflater.from(getContext()).inflate(R.layout.debug_container_layout, layout)
        val container = layout.findViewById<FrameLayout>(R.id.container)
        logTextView = layout.findViewById(R.id.log)
    }

    override fun setFullScreen(fullScreen: Boolean) {

    }

    override fun calculateComponentSize() {

    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }
}