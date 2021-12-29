package io.agora.agoraclasssdk

import android.content.Context
import io.agora.agoraclasssdk.app.activities.*
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.ClassInfoCache
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.registerDefaultOnce
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppConfiguration
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppEngine
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.chat.EaseChatWidgetPopup
import io.agora.agoraeduuikit.impl.chat.rtm.AgoraChatWidgetPopup
import io.agora.agoraeduuikit.impl.whiteboard.AgoraWhiteBoardWidget

object AgoraClassSdk {
    private const val tag = "AgoraClassSdk"
    private lateinit var config: AgoraClassSdkConfig

    fun setConfig(config: AgoraClassSdkConfig) {
        AgoraClassSdk.config = config
        globalInit()
    }

    private fun globalInit() {
        // Things that the only class sdk instance should do as global initialization
        // 1. register necessary widgets and extension apps
        registerWidgets()

        // 2. register activities for each room type
        addRoomClassTypes()
    }

    private fun registerWidgets() {
        // Extra app id for agora chat temporarily
        val agoraChatExtraInfo = mutableMapOf<String, Any>()
        if (this::config.isInitialized) {
            agoraChatExtraInfo["appId"] = config.appId
        }
        // Register default widgets globally here because we must ensure
        // users call this register method just before they use our edu
        // library and will relief them registering default widgets in their code.
        // Then there will be a chance to replace the widgets of their own.
        // Widget registering will not depend on any other part of classroom
        // mechanism, so we handle it at the beginning of the classroom launch.
        val map = mutableMapOf<String, MutableList<AgoraWidgetConfig>>()
        val cnWidgetConfigs = mutableListOf<AgoraWidgetConfig>()
        cnWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = EaseChatWidgetPopup::class.java,
            widgetId = AgoraWidgetDefaultId.Chat.id))
        cnWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraWhiteBoardWidget::class.java,
            widgetId = AgoraWidgetDefaultId.WhiteBoard.id))
        map[AgoraEduRegion.cn] = cnWidgetConfigs

        val naWidgetConfigs = mutableListOf<AgoraWidgetConfig>()
        naWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraChatWidgetPopup::class.java,
            widgetId = AgoraWidgetDefaultId.Chat.id,
            extraInfo = agoraChatExtraInfo))
        naWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraWhiteBoardWidget::class.java,
            widgetId = AgoraWidgetDefaultId.WhiteBoard.id))
        map[AgoraEduRegion.na] = naWidgetConfigs

        val apWidgetConfigs = mutableListOf<AgoraWidgetConfig>()
        apWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraChatWidgetPopup::class.java,
            widgetId = AgoraWidgetDefaultId.Chat.id,
            extraInfo = agoraChatExtraInfo))
        apWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraWhiteBoardWidget::class.java,
            widgetId = AgoraWidgetDefaultId.WhiteBoard.id))
        map[AgoraEduRegion.ap] = apWidgetConfigs

        val euWidgetConfigs = mutableListOf<AgoraWidgetConfig>()
        euWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraChatWidgetPopup::class.java,
            widgetId = AgoraWidgetDefaultId.Chat.id,
            extraInfo = agoraChatExtraInfo))
        euWidgetConfigs.add(AgoraWidgetConfig(
            widgetClass = AgoraWhiteBoardWidget::class.java,
            widgetId = AgoraWidgetDefaultId.WhiteBoard.id))
        map[AgoraEduRegion.eu] = euWidgetConfigs
        registerDefaultOnce(map)
    }

    private fun addRoomClassTypes() {
        ClassInfoCache.addRoomActivityDefault(RoomType.ONE_ON_ONE.value, OneToOneClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, SmallClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS_ART.value, SmallClassArtActivity::class.java)
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
            AgoraLog.e("$tag->AgoraClassSdk has not initialized a configuration(not call " +
                "AgoraClassSdk.setConfig function)")
            return
        }

        injectRtmMessageWidget(config)
        AgoraEduCore.setAgoraEduSDKConfig(AgoraEduSDKConfig(AgoraClassSdk.config.appId, 0))
        AgoraEduCore.launch(context, config, callback)
    }

    fun version(): String {
        return AgoraEduSDK.version()
    }

    // Workaround for rtm message widget to store back-end app id
    // for message services.
    // If user has set a pre-defined configuration for rtm message
    // widget, we package the config to a new map, which info is
    // stored into a key named "default"
    // If user has not set a configuration for rtm message widget,
    // nothing is done
    private fun injectRtmMessageWidget(config: AgoraEduLaunchConfig) {
        config.widgetConfigs?.find { it ->
            it.widgetClass == AgoraChatWidgetPopup::class.java
        } ?.let { widgetConfig ->
            val newMap = mutableMapOf<String, Any>()
            newMap["appId"] = config.appId
            widgetConfig.extraInfo?.let {
                newMap["default"] = it
            }
            widgetConfig.extraInfo = newMap
        }
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
