package io.agora.educontext.context

import androidx.annotation.UiThread
import io.agora.educontext.EduContextExtAppInfo

interface ExtAppContext {
    @UiThread
    fun launchExtApp(appIdentifier: String): Int

    fun getRegisteredExtApps(): List<EduContextExtAppInfo>
}