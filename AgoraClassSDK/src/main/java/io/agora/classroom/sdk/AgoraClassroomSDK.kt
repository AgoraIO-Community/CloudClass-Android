package io.agora.classroom.sdk

import android.content.Context
import com.agora.edu.component.chat.AgoraChatRTMWidget
import com.agora.edu.component.chat.AgoraEduEaseChatGroupWidget
import com.agora.edu.component.chat.AgoraEduEaseChatWidget
import com.agora.edu.component.teachaids.AgoraTeachAidCountDownWidget
import com.agora.edu.component.teachaids.AgoraTeachAidIClickerWidget
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskWidget
import com.agora.edu.component.teachaids.vote.AgoraTeachAidVoteWidget
import com.agora.edu.component.teachaids.webviewwidget.FcrWebViewWidget
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.ClassInfoCache
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.registerDefault
import io.agora.agoraeducore.core.internal.framework.proxy.EduRoom
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoWidget
import io.agora.agoraeduuikit.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.classroom.common.AgoraBaseClassActivity
import io.agora.classroom.helper.FCRLauncherManager
import io.agora.classroom.ui.AgoraClass1V1Activity
import io.agora.classroom.ui.AgoraClassLargeActivity
import io.agora.classroom.ui.AgoraClassSmallActivity
import io.agora.classroom.ui.goup.AgoraClassSmallGroupingActivity
import io.agora.classroom.ui.large.AgoraClassLargeHostingActivity
import io.agora.classroom.ui.large.AgoraClassLargeMixStreamActivity
import io.agora.classroom.ui.large.AgoraClassLargeVocationalActivity

/**
 * 一键拉起教室
 */
object AgoraClassroomSDK {
    private const val TAG = "AgoraClassroomSDK"
    private lateinit var config: AgoraClassSdkConfig
    var launchConfigList = mutableMapOf<String, AgoraEduLaunchConfig>()
    private var currentLaunchConfig: AgoraEduLaunchConfig? = null

    fun launch(context: Context, launchConfig: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
        AgoraSDKInitUtils.initSDK(context)

        if (!this::config.isInitialized) {
            LogX.e(
                "$TAG->AgoraClassSdk has not initialized a configuration(not call " +
                    "AgoraClassSdk.setConfig function)"
            )
            return
        }

        LogX.i(
            TAG, "AgoraEduCoreVersion=${io.agora.agoraeducore.BuildConfig.AgoraEduCoreVersion}||" +
                    "AgoraEduUIKitVersion=${io.agora.agoraeduuikit.BuildConfig.AgoraEduUIKitVersion}||" +
                    "AgoraClassSDKVersion=${io.agora.agoraclasssdk.BuildConfig.AgoraClassSDKVersion}"
        )
        LogX.e(TAG, "AgoraClassSdk launch=${launchConfig.roomName}")
        SpUtil.init(context)
        launchConfig.appId = this.config.appId
        injectChatWidget(launchConfig)
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

        val map = mutableMapOf<String, MutableList<AgoraWidgetConfig>>()
        map[AgoraEduRegion.cn] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.na] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.ap] = getWidgetConfigList(appIdExtraInfo)
        map[AgoraEduRegion.eu] = getWidgetConfigList(appIdExtraInfo)

        registerDefault(map)
    }

    private fun getWidgetConfigList(appIdExtraInfo: MutableMap<String, Any>): MutableList<AgoraWidgetConfig> {
        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(AgoraWidgetConfig(AgoraWhiteBoardWidget::class.java, AgoraWidgetDefaultId.WhiteBoard.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraUILargeVideoWidget::class.java, AgoraWidgetDefaultId.LargeWindow.id))
        widgetConfigs.add(AgoraWidgetConfig(FcrWebViewWidget::class.java, AgoraWidgetDefaultId.FcrWebView.id))
        widgetConfigs.add(AgoraWidgetConfig(FcrWebViewWidget::class.java, AgoraWidgetDefaultId.FcrMediaPlayer.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraTeachAidCountDownWidget::class.java, AgoraWidgetDefaultId.CountDown.id))
        widgetConfigs.add(AgoraWidgetConfig(AgoraTeachAidIClickerWidget::class.java, AgoraWidgetDefaultId.Selector.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(AgoraTeachAidVoteWidget::class.java, AgoraWidgetDefaultId.Polling.id, extraInfo = appIdExtraInfo))
        widgetConfigs.add(AgoraWidgetConfig(FCRCloudDiskWidget::class.java, AgoraWidgetDefaultId.AgoraCloudDisk.id))
        return widgetConfigs
    }

    private fun addRoomClassTypes() {
        ClassInfoCache.addRoomActivityDefault(RoomType.GROUPING_CLASS.value, AgoraClassSmallGroupingActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.ONE_ON_ONE.value, AgoraClass1V1Activity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.SMALL_CLASS.value, AgoraClassSmallActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(RoomType.LARGE_CLASS.value, AgoraClassLargeActivity::class.java)
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
        if (classType == RoomType.LARGE_CLASS.value) {
            if (serviceType == AgoraServiceType.MixStreamCDN) {
                replaceClassActivity(classType, AgoraClassLargeMixStreamActivity::class.java)
            } else if (serviceType == AgoraServiceType.HostingScene) {
                replaceClassActivity(classType, AgoraClassLargeHostingActivity::class.java)
            } else if (AgoraServiceType.serviceTypeIsValid(serviceType.value)) {
                replaceClassActivity(classType, AgoraClassLargeVocationalActivity::class.java)
            }
        }
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

    private fun injectChatWidget(config: AgoraEduLaunchConfig) {
        val appIdExtraInfo = mutableMapOf<String, Any>()
        appIdExtraInfo["appId"] = AgoraClassroomSDK.config.appId
        if(config.roomType == RoomType.LARGE_CLASS.value){
            config.widgetConfigs?.add(AgoraWidgetConfig(AgoraEduEaseChatGroupWidget::class.java, AgoraWidgetDefaultId.Chat.id, extraInfo = appIdExtraInfo))
        }else{
            config.widgetConfigs?.add(AgoraWidgetConfig(AgoraEduEaseChatWidget::class.java, AgoraWidgetDefaultId.Chat.id, extraInfo = appIdExtraInfo))
        }
    }
}

class AgoraClassSdkConfig(var appId: String)
