package io.agora.extension

import android.content.Context
import android.view.View

interface IAgoraExtApp {
    fun onExtAppLoaded(context: Context)

    fun onCreateView(content: Context): View

    fun onPropertyUpdated(properties: MutableMap<String, Any>?, cause: MutableMap<String, Any?>?)

    fun onExtAppUnloaded()
}