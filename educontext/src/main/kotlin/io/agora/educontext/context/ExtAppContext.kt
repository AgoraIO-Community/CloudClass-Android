package io.agora.educontext.context

import androidx.annotation.UiThread
import io.agora.extension.AgoraExtAppInfo

interface ExtAppContext {
    @UiThread
    fun launchExtApp(appIdentifier: String): Int

    fun getRegisteredExtApps(): List<AgoraExtAppInfo>
}