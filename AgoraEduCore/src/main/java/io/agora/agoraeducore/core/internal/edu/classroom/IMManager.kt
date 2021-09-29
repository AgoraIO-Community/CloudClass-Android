package io.agora.agoraeducore.core.internal.edu.classroom

class IMManager {
    companion object {
        const val easeIMWidgetId = "HyphenateChat"
        const val propertiesKey = "im"
        const val hxKey = "huanxin"
    }

    /**
     * Parse Huanxin (Hyphenate) chat properties from "im" part of
     * edu room properties
     */
    fun parseEaseIMProperties(imProperties: Map<String, Any>?) : Map<String, Any>? {
        return imProperties?.get(hxKey) as? Map<String, Any>
    }
}