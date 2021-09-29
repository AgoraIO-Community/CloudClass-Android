package io.agora.agoraclasssdk

import android.content.Context
import android.util.Log
import io.agora.agoraclasssdk.app.activities.LargeClassActivity
import io.agora.agoraclasssdk.app.activities.OneToOneClassActivity
import io.agora.agoraclasssdk.app.activities.SmallClassActivity
import io.agora.agoraeduextapp.AgoraExtAppConfiguration
import io.agora.agoraeduextapp.AgoraExtAppEngine
import io.agora.agoraeduuikit.impl.chat.AgoraUIChatWidget
import io.agora.agoraeduuikit.impl.chat.EaseChatWidget
import io.agora.agoraeduwidget.UiWidgetManager.Companion.registerDefaultOnce
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.ClassInfoCache
import io.agora.agoraeducore.core.internal.framework.RoomType
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.sdk.app.activities.BaseClassActivity

object AgoraClassSdk {
    private const val tag = "AgoraClassSdk"
    private lateinit var config: AgoraClassSdkConfig

    init {
        globalInit()
    }

    fun setConfig(config: AgoraClassSdkConfig) {
        AgoraClassSdk.config = config
    }

    private fun globalInit() {
        // Things that the only class sdk instance should do as global initialization
        // 1. register necessary widgets and extension apps
        registerWidgets()

        // 2. register activities for each room type
        addRoomClassTypes()
    }

    private fun registerWidgets() {
        // Register default widgets globally here because we must ensure
        // users call this register method just before they use our edu
        // library and will relief them registering default widgets in their code.
        // Then there will be a chance to replace the widgets of their own.
        // Widget registering will not depend on any other part of classroom
        // mechanism, so we handle it at the beginning of the classroom launch.
        val map = mutableMapOf<String, MutableList<io.agora.agoraeduwidget.UiWidgetConfig>>()
        val cnWidgetConfigs = mutableListOf<io.agora.agoraeduwidget.UiWidgetConfig>()
        cnWidgetConfigs.add(io.agora.agoraeduwidget.UiWidgetConfig(
                io.agora.agoraeduwidget.UiWidgetManager.DefaultWidgetId.Chat.name,
                EaseChatWidget::class.java))
        map[AgoraEduRegion.cn] = cnWidgetConfigs
        val naWidgetConfigs = mutableListOf<io.agora.agoraeduwidget.UiWidgetConfig>()
        naWidgetConfigs.add(io.agora.agoraeduwidget.UiWidgetConfig(
                io.agora.agoraeduwidget.UiWidgetManager.DefaultWidgetId.Chat.name,
                AgoraUIChatWidget::class.java))
        map[AgoraEduRegion.na] = naWidgetConfigs
        val apWidgetConfigs = mutableListOf<io.agora.agoraeduwidget.UiWidgetConfig>()
        apWidgetConfigs.add(io.agora.agoraeduwidget.UiWidgetConfig(
                io.agora.agoraeduwidget.UiWidgetManager.DefaultWidgetId.Chat.name,
                AgoraUIChatWidget::class.java))
        map[AgoraEduRegion.ap] = apWidgetConfigs
        val euWidgetConfigs = mutableListOf<io.agora.agoraeduwidget.UiWidgetConfig>()
        euWidgetConfigs.add(io.agora.agoraeduwidget.UiWidgetConfig(
                io.agora.agoraeduwidget.UiWidgetManager.DefaultWidgetId.Chat.name,
                AgoraUIChatWidget::class.java))
        map[AgoraEduRegion.eu] = euWidgetConfigs
        registerDefaultOnce(map)
    }

    private fun addRoomClassTypes() {
        ClassInfoCache.addRoomActivityDefault(RoomType.ONE_ON_ONE.value, OneToOneClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, SmallClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.LARGE_CLASS.value, LargeClassActivity::class.java)
    }

    /**
     * Replace default activity implementation for a room type if a
     * different activity and UI is used. The activity should be an
     * extension of BaseClassActivity, in order to have classroom
     * capabilities.
     * This replacement is global, and in most cases this should be
     * called only once. Make sure call this method before launch.
     */
    fun replaceClassActivity(classType: Int, activity: Class<out BaseClassActivity>) {
        ClassInfoCache.replaceRoomActivity(classType, activity)
    }

    fun launch(context: Context, config: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
        if (!this::config.isInitialized) {
            Log.e(tag, "AgoraClassSdk has not initialized a configuration")
            return
        }

        AgoraEduCore.setAgoraEduSDKConfig(AgoraEduSDKConfig(AgoraClassSdk.config.appId, 0))
        AgoraEduCore.launch(context, config, callback)
    }

    fun configCourseWare(configs: MutableList<AgoraEduCourseware>) {
        AgoraEduSDK.configCourseWare(configs)
    }

    fun downloadCourseWare(context: Context, listener: AgoraEduCoursewarePreloadListener) {
        AgoraEduSDK.downloadCourseWare(context, listener)
    }

    /**
     * Register custom extension apps.
     * If an extension app's identifier has already registered, it
     * will be ignored.
     */
    fun registerExtensionApp(configs: MutableList<AgoraExtAppConfiguration>) {
        AgoraExtAppEngine.registerExtAppList(configs)
    }
}

class AgoraClassSdkConfig(var appId: String)
