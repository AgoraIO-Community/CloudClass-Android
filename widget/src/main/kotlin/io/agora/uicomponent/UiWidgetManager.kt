package io.agora.uicomponent

import android.content.Context
import android.util.Log
import io.agora.educontext.EduContextPool

class UiWidgetManager {
    private val widgets = mutableMapOf<String, AbsUiWidget>()

    /**
     * Default widget ids that can be used for a specific purpose and
     * already used in Agora aPaaS sdk.
     * However, the id string of a widget can be anything other than
     * what have been defined here.
     */
    enum class DefaultWidgetId {
        Chat
    }

    companion object {
        private const val tag = "UIWidgetManager"
        private val registerMap = mutableMapOf<String, Class<out AbsUiWidget>>()

        /**
         * Register the widgets globally only once.
         * The widgets whose ids are already registered will
         * be IGNORED
         */
        fun registerDefaultOnce(configs: List<UiWidgetConfig>) {
            synchronized(this) {
                configs.forEach { c ->
                    if (!registerMap.contains(c.id)) {
                        Log.i(tag, "UiWidgetManager default widget " +
                                "registered, id=${c.id},class=${c.clz.simpleName}")
                        register(c)
                    } else {
                        Log.i(tag, "UiWidgetManager default widget " +
                                "exists, id=${c.id},class=${c.clz.simpleName}")
                    }
                }
            }
        }

        /**
         * Register widgets by configurations.
         * Any existing widgets with the same id string registered WILL BE
         * replaced
         */
        fun registerAndReplace(configs: List<UiWidgetConfig>) {
            synchronized(this) {
                configs.forEach { c ->
                    Log.i(tag, "UiWidgetManager widget registered" +
                            " or replaced, id=${c.id},class=${c.clz.simpleName}")
                    register(c)
                }
            }
        }

        private fun register(config: UiWidgetConfig) {
            registerMap[config.id] = config.clz
        }

        private fun getWidgetClass(id: String) : Class<out AbsUiWidget>? {
            return registerMap[id]
        }
    }

    fun create(id: String, context: EduContextPool?, args: Any? = null) : AbsUiWidget? {
        val obj = getWidgetClass(id)?.newInstance()
        return obj?.apply {
            this.setId(id)
            this.setEduContext(context)
            this.setWidgetManager(this@UiWidgetManager)
            this.setArguments(args)
        }
    }

    fun addWidget(widget: AbsUiWidget) {
        synchronized(this) {
            if (widgets.containsKey(widget.getId())) {
                Log.d(tag, "add duplicated widget, id=${widget.getId()}")
                return
            }

            widgets.forEach { entry ->
                if (entry.value == widget) {
                    Log.d(tag, "add duplicated widget with id ${widget.getId()}, " +
                            "existing widget id is ${entry.key}")
                    return
                }
            }

            widgets[widget.getId()] = widget
        }
    }

    fun removeWidget(widget: AbsUiWidget) {
        synchronized(this) {
            widgets.remove(widget.getId())
        }
    }

    internal fun notify(fromId: String, cmd: String, vararg : Any? = null) {

    }

    private fun callbackMessages(fromId: String, cmd: String, vararg: Any? = null) {

    }

    fun release() {
        widgets.clear()
    }
}

data class UiWidgetConfig(
        val id: String,
        val clz: Class<out AbsUiWidget>
)