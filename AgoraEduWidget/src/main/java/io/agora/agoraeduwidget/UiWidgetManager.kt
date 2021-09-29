package io.agora.agoraeduwidget

import android.util.Log
import io.agora.agoraeducore.core.context.EduContextPool
import java.util.*

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

        // {region: [{widgetId: widgetClass]...}}
        private val defaultWidgetMap = mutableMapOf<String, MutableList<UiWidgetConfig>>()
        private val registerMap = mutableMapOf<String, Class<out AbsUiWidget>>()

        /**
         * Register the widgets globally only once.
         * The widgets whose ids are already registered will
         * be IGNORED
         */
        fun registerDefaultOnce(configMap: MutableMap<String, MutableList<UiWidgetConfig>>) {
            synchronized(this) {
                configMap.forEach {
                    val existsList = defaultWidgetMap[it.key] ?: mutableListOf()
                    it.value.forEach { item ->
                        if (!isExist(item.id, existsList)) {
                            Log.i(tag, "UiWidgetManager default widget registered, " +
                                    "region=${it.key},id=${item.id},class=${item.clz.simpleName}")
                            existsList.add(item)
                        } else {
                            Log.i(tag, "UiWidgetManager default widget exists, " +
                                    "region=${it.key},id=${item.id},class=${item.clz.simpleName}")
                        }
                    }
                    defaultWidgetMap[it.key] = existsList
                }
            }
        }

        private fun isExist(widgetId: String, data: MutableList<UiWidgetConfig>): Boolean {
            data.forEach {
                if (it.id == widgetId) {
                    return true
                }
            }
            return false
        }

        /**
         * Register widgets by configurations.
         * Any existing widgets with the same id string registered WILL BE
         * replaced
         */
        fun registerAndReplace(region: String, configs: MutableList<UiWidgetConfig>) {
            synchronized(this) {
                val existsList = defaultWidgetMap[region] ?: mutableListOf()
                configs.forEach {
                    existsList.forEach { default ->
                        if (it.id == default.id) {
                            Log.i(tag, "UiWidgetManager widget registered" +
                                    " or replaced, id=${it.id},class=${it.clz.simpleName}")
                            register(it)
                        }
                    }
                }
            }
        }

        /**
         * Register widgets by default configurations.
         */
        fun registerDefault(region: String) {
            synchronized(this) {
                val existsList = defaultWidgetMap[region] ?: mutableListOf()
                existsList.forEach { default ->
                    register(default)
                }
            }
        }

        private fun register(config: UiWidgetConfig) {
            registerMap[config.id] = config.clz
        }

        private fun getWidgetClass(id: String): Class<out AbsUiWidget>? {
            return registerMap[id]
        }
    }

    fun create(id: String, context: EduContextPool?, args: Any? = null): AbsUiWidget? {
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

    internal fun notify(fromId: String, cmd: String, vararg: Any? = null) {

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