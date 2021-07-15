package io.agora.educontext.context

import io.agora.educontext.WidgetType

interface WidgetContext {
    fun getWidgetProperties(type: WidgetType) : Map<String, Any>?
}