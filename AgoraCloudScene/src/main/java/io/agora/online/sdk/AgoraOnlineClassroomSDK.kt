package io.agora.online.sdk

import android.content.Context
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.ClassInfoCache
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.registerDefault
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchCallback
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.component.chat.AgoraChatRTMWidget
import io.agora.online.component.chat.AgoraEduEaseChatWidget
import io.agora.online.component.teachaids.AgoraTeachAidCountDownWidget
import io.agora.online.component.teachaids.AgoraTeachAidIClickerWidget
import io.agora.online.component.teachaids.networkdisk.FCRCloudDiskWidget
import io.agora.online.component.teachaids.vote.AgoraTeachAidVoteWidget
import io.agora.online.component.teachaids.webviewwidget.FcrWebViewWidget
import io.agora.online.impl.video.AgoraUILargeVideoWidget
import io.agora.online.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.online.sdk.common.AgoraBaseClassActivity
import io.agora.online.sdk.helper.FCRLauncherManager
import io.agora.online.util.SpUtil
import io.agora.online.widget.FcrWidgetManager.WIDGETS_RTT_ID
import io.agora.online.widget.rtt.FcrRttToolBoxWidget

/**
 * 一键拉起教室
 */
object AgoraOnlineClassroomSDK {
    private const val TAG = "AgoraOnlineClassroomSDK"
    private lateinit var config: AgoraOnlineClassSdkConfig
    var launchConfigList = mutableMapOf<String, AgoraEduLaunchConfig>()
    private var currentLaunchConfig: AgoraEduLaunchConfig? = null

    fun launch(context: Context, launchConfig: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
        launchConfig.isJoinCreateRoomApi = false
        innerLaunch(context, launchConfig, callback)
    }
    fun launch2(
        context: Context,
        launchConfig: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback
    ) {
        innerLaunch(context, launchConfig, callback)
    }

    private fun innerLaunch(context: Context, launchConfig: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
        if (!this::config.isInitialized) {
            LogX.e(
                "$TAG->AgoraClassSdk has not initialized a configuration(not call " +
                        "AgoraClassSdk.setConfig function)"
            )
            return
        }

        LogX.i(
            TAG, "AgoraEduCoreVersion=${io.agora.agoraeducore.BuildConfig.AgoraEduCoreVersion}||" +
                    "AgoraOnlineScene=${io.agora.online.BuildConfig.AgoraCloudScene}"
        )

        LogX.e(TAG, "AgoraClassSdk launch=${launchConfig.roomName}")
        SpUtil.init(context)
        launchConfig.appId = config.appId
        injectRtmMessageWidget(launchConfig)
        replaceClassRoomByServiceType(launchConfig.roomType, launchConfig.serviceType)
        AgoraEduCore.launch(context, launchConfig, callback)
        currentLaunchConfig = launchConfig
        launchConfigList[launchConfig.getLaunchConfigId()] = launchConfig
    }

    /**
     * 退出教室
     */
    fun exit() {
        FCRLauncherManager.notifyAllLauncher()
    }

    /**
     * 退出指定的教室
     */
    fun exit(roomUuid: String) {
        FCRLauncherManager.notifyLauncher(roomUuid)
    }

    fun setConfig(config: AgoraOnlineClassSdkConfig) {
        AgoraOnlineClassroomSDK.config = config
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

        val map = mutableMapOf<String, MutableList<AgoraWidgetConfig>>()
        map[AgoraEduRegion.cn] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.na] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.ap] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.eu] = getWidgetConfigList(appIdExtraInfo)

        registerDefault(map)
    }

    private fun getWidgetConfigList(appIdExtraInfo: MutableMap<String, Any>): MutableList<AgoraWidgetConfig> {
        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(
            AgoraWidgetConfig(
                AgoraEduEaseChatWidget::class.java,
                AgoraWidgetDefaultId.Chat.id,
                extraInfo = appIdExtraInfo
            )
        )
        widgetConfigs.add(AgoraWidgetConfig(AgoraWhiteBoardWidget::class.java, AgoraWidgetDefaultId.WhiteBoard.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraUILargeVideoWidget::class.java, AgoraWidgetDefaultId.LargeWindow.id))
        widgetConfigs.add(AgoraWidgetConfig(FcrWebViewWidget::class.java, AgoraWidgetDefaultId.FcrWebView.id))
        widgetConfigs.add(AgoraWidgetConfig(FcrWebViewWidget::class.java, AgoraWidgetDefaultId.FcrMediaPlayer.id))
        widgetConfigs.add(
            AgoraWidgetConfig(
                AgoraTeachAidCountDownWidget::class.java,
                AgoraWidgetDefaultId.CountDown.id
            )
        )
        widgetConfigs.add(
            AgoraWidgetConfig(
                AgoraTeachAidIClickerWidget::class.java,
                AgoraWidgetDefaultId.Selector.id,
                extraInfo = appIdExtraInfo
            )
        )
        widgetConfigs.add(
            AgoraWidgetConfig(
                AgoraTeachAidVoteWidget::class.java,
                AgoraWidgetDefaultId.Polling.id,
                extraInfo = appIdExtraInfo
            )
        )
        widgetConfigs.add(AgoraWidgetConfig(FCRCloudDiskWidget::class.java, AgoraWidgetDefaultId.AgoraCloudDisk.id))
        widgetConfigs.add(AgoraWidgetConfig(FcrRttToolBoxWidget::class.java, WIDGETS_RTT_ID, extraInfo = mutableMapOf<String, Any>()))
        return widgetConfigs
    }

    private fun addRoomClassTypes() {
        ClassInfoCache.addRoomActivityDefault(RoomType.GROUPING_CLASS.value, AgoraOnlineClassGroupActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, AgoraOnlineClassActivity::class.java)
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

    private fun replaceClassRoomByServiceType(classType: Int, serviceType: AgoraServiceType) {
        // Currently service types only affect large-room classes
//        if (classType == RoomType.LARGE_CLASS.value) {
//            if (serviceType == AgoraServiceType.MixStreamCDN) {
//                replaceClassActivity(classType, AgoraClassLargeMixStreamActivity::class.java)
//            } else if (serviceType == AgoraServiceType.HostingScene) {
//                replaceClassActivity(classType, AgoraClassLargeHostingActivity::class.java)
//            } else if (AgoraServiceType.serviceTypeIsValid(serviceType.value)) {
//                replaceClassActivity(classType, AgoraClassLargeVocationalActivity::class.java)
//            }
//        }
    }

    fun version(): String {
        return AgoraEduSDK.version()
    }

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

class AgoraOnlineClassSdkConfig(var appId: String)
