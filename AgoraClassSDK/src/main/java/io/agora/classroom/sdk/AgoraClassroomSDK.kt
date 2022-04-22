package io.agora.classroom.sdk

import android.content.Context
import com.agora.edu.component.chat.AgoraChatRTMWidget
import com.agora.edu.component.chat.AgoraEduEaseChatWidget
import com.agora.edu.component.teachaids.AgoraCountDownWidget
import com.agora.edu.component.teachaids.AgoraIClickerWidget
import com.agora.edu.component.teachaids.vote.AgoraVoteWidget
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.ClassInfoCache
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.registerDefault
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoWidget
import io.agora.agoraeduuikit.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.classroom.common.AgoraBaseClassActivity
import io.agora.classroom.ui.AgoraClass1V1Activity
import io.agora.classroom.ui.AgoraClassLargeActivity
import io.agora.classroom.ui.AgoraClassSmallActivity
import io.agora.classroom.ui.AgoraClassLargeArtActivity
import io.agora.classroom.ui.AgoraClassSmallArtActivity
import io.agora.classroom.ui.goup.AgoraClassSmallGroupingActivity

/**
 * 一键拉起教室
 */
object AgoraClassroomSDK {
    private const val TAG = "AgoraClassroomSDK"
    private lateinit var config: AgoraClassSdkConfig
    var launchConfigList = mutableMapOf<String, AgoraEduLaunchConfig>()
    private var currentLaunchConfig: AgoraEduLaunchConfig? = null

    fun launch(context: Context, config: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
        if (!this::config.isInitialized) {
            AgoraLog?.e(
                "$TAG->AgoraClassSdk has not initialized a configuration(not call " +
                        "AgoraClassSdk.setConfig function)"
            )
            return
        }
        AgoraLog?.e("$TAG->AgoraClassSdk launch=${config.roomName}")
        injectRtmMessageWidget(config)
        AgoraEduCore.launch(context, config, callback)
        currentLaunchConfig = config
        launchConfigList[config.getLaunchConfigId()] = config
    }

    fun setConfig(config: AgoraClassSdkConfig) {
        AgoraClassroomSDK.config = config
        globalInit()
    }

    fun getCurrentLaunchConfig(): AgoraEduLaunchConfig? {
        return currentLaunchConfig
    }

    fun getLaunchConfig(launchConfigId: String): AgoraEduLaunchConfig? {
        return launchConfigList[launchConfigId]
    }

    fun removeLaunchConfig(launchConfigId: String) {
        launchConfigList.remove(launchConfigId)
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
        val appIdExtraInfo = mutableMapOf<String, Any>()
        if (this::config.isInitialized) {
            appIdExtraInfo["appId"] = config.appId
        }
        // Register default widgets globally here because we must ensure
        // users call this register method just before they use our edu
        // library and will relief them registering default widgets in their code.
        // Then there will be a chance to replace the widgets of their own.
        // Widget registering will not depend on any other part of classroom
        // mechanism, so we handle it at the beginning of the classroom launch.
        val map = mutableMapOf<String, MutableList<AgoraWidgetConfig>>()
        map[AgoraEduRegion.cn] = getCNWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.na] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.ap] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.eu] = getWidgetConfigList(appIdExtraInfo)

        registerDefault(map)
    }

    private fun getCNWidgetConfigList(appIdExtraInfo: MutableMap<String, Any>): MutableList<AgoraWidgetConfig> {
        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(AgoraWidgetConfig(AgoraEduEaseChatWidget::class.java, AgoraWidgetDefaultId.Chat.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(AgoraWhiteBoardWidget::class.java, AgoraWidgetDefaultId.WhiteBoard.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraUILargeVideoWidget::class.java, AgoraWidgetDefaultId.LargeWindow.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraCountDownWidget::class.java, AgoraWidgetDefaultId.CountDown.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraIClickerWidget::class.java, AgoraWidgetDefaultId.Selector.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(AgoraVoteWidget::class.java, AgoraWidgetDefaultId.Polling.id, extraInfo = appIdExtraInfo))
        return widgetConfigs
    }

    private fun getWidgetConfigList(appIdExtraInfo: MutableMap<String, Any>): MutableList<AgoraWidgetConfig> {
        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(AgoraWidgetConfig(AgoraChatRTMWidget::class.java, AgoraWidgetDefaultId.Chat.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(AgoraWhiteBoardWidget::class.java, AgoraWidgetDefaultId.WhiteBoard.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraUILargeVideoWidget::class.java, AgoraWidgetDefaultId.LargeWindow.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraCountDownWidget::class.java, AgoraWidgetDefaultId.CountDown.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraIClickerWidget::class.java, AgoraWidgetDefaultId.Selector.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(AgoraVoteWidget::class.java, AgoraWidgetDefaultId.Polling.id, extraInfo = appIdExtraInfo))
        return widgetConfigs
    }

    private fun addRoomClassTypes() {
        ClassInfoCache.addRoomActivityDefault(RoomType.GROUPING_CLASS.value, AgoraClassSmallGroupingActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.ONE_ON_ONE.value, AgoraClass1V1Activity::class.java)
        if (BuildConfig.isArtScene.toBoolean()) {
            ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, AgoraClassSmallArtActivity::class.java)
        } else {
            ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, AgoraClassSmallActivity::class.java)
        }
        if (BuildConfig.isArtScene.toBoolean()) {
            ClassInfoCache.addRoomActivityDefault(RoomType.LARGE_CLASS.value, AgoraClassLargeArtActivity::class.java)
        } else {
            ClassInfoCache.addRoomActivityDefault(RoomType.LARGE_CLASS.value, AgoraClassLargeActivity::class.java)
        }
    }

    /**
     * Replace default activity implementation for a room type if a
     * different activity and UI is used. The activity should be an
     * extension of BaseClassActivity, in order to have classroom
     * capabilities.
     * This replacement is global, and in most cases this should be
     * called only once. Make sure call this method before launch.
     */
    fun replaceClassActivity(classType: Int, activity: Class<out AgoraBaseClassActivity>) {
        ClassInfoCache.replaceRoomActivity(classType, activity)
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
            it.widgetClass == AgoraChatRTMWidget::class.java
        }?.let { widgetConfig ->
            val newMap = mutableMapOf<String, Any>()
            newMap["appId"] = config.appId
            widgetConfig.extraInfo?.let {
                newMap["default"] = it
            }
            widgetConfig.extraInfo = newMap
        }
    }
}

class AgoraClassSdkConfig(var appId: String)
