package io.agora.edu.extensions.widgets

import android.view.ViewGroup

abstract class AbsUiWidget {
    private var id: String = ""
    private var eduContext: io.agora.edu.core.context.EduContextPool? = null
    private var widgetManager: UiWidgetManager? = null
    private var args: Any? = null

    abstract fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int)

    fun setId(id: String) {
        this.id = id
    }

    fun getId(): String {
        return id
    }

    fun setEduContext(eduContext: io.agora.edu.core.context.EduContextPool?) {
        this.eduContext = eduContext
    }

    fun getEduContext() : io.agora.edu.core.context.EduContextPool? {
        return this.eduContext
    }

    fun setWidgetManager(widgetManager: UiWidgetManager?) {
        this.widgetManager = widgetManager
    }

    fun getWidgetManager() : UiWidgetManager? {
        return this.widgetManager
    }

    fun setArguments(args: Any?) {
        this.args = args
    }

    fun getArguments() : Any? {
        return this.args
    }

    protected fun getLogTag(): String {
        return "AgoraWidget_${getId()}"
    }

    protected fun notify(cmd: String, vararg: Any? = null) {
        widgetManager?.notify(getId(), cmd, vararg)
    }

    abstract fun receive(fromCompId: String, cmd: String, vararg: Any? = null)

    abstract fun release()
}