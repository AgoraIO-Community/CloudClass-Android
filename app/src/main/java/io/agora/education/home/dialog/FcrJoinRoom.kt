package io.agora.education.home.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskWidget
import com.agora.edu.component.teachaids.networkdisk.Statics
import io.agora.agoraeducore.core.context.EduContextVideoEncoderConfig
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.classroom.helper.FcrStreamParameters
import io.agora.classroom.sdk.AgoraClassSdkConfig
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.data.DefaultPublicCoursewareJson
import io.agora.education.dialog.ForbiddenDialog
import io.agora.education.dialog.ForbiddenDialogBuilder
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrCreateRoomProperties
import io.agora.education.request.bean.FcrJoinRoomReq
import io.agora.education.request.bean.FcrJoinRoomRes
import java.io.File

/**
 * author : hefeng
 * date : 2022/9/23
 * description :
 */
class FcrJoinRoom(var context: Context) {
    var forbiddenDialog: ForbiddenDialog? = null

    /**
     * 第一个参数：加入教室是否成功
     * 第二个参数：是否需要隐藏加入教室Dialog
     * 第三个参数：加入教室失败原因
     */
    var setJoinRoomListener: ((Boolean, Boolean, String) -> Unit)? = null
    val TAG = "FcrJoinRoom"

    fun joinRoom(roomId: String, roleType: Int, userName: String) {
        if (roleType == AgoraEduRoleType.AgoraEduRoleTypeStudent.value || roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
            val req = FcrJoinRoomReq(roomId, roleType)
            val call = AppRetrofitManager.instance().getService(AppService::class.java)
                .joinRoom(AppUserInfoUtils.getCompanyId(), req)
            AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrJoinRoomRes>>() {
                override fun onSuccess(result: HttpBaseRes<FcrJoinRoomRes>?) {
                    result?.data?.let {
                        if ((it.roomDetail?.roomProperties?.serviceType == AgoraServiceType.Fusion.value
                                || it.roomDetail?.roomProperties?.serviceType == AgoraServiceType.HostingScene.value)
                            && (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value)
                        ) {
                            setJoinRoomListener?.invoke(false, false, context.getString(R.string.fcr_joinroom_tips_cdn_character))
                            ToastManager.showShort(context.getString(R.string.fcr_joinroom_tips_cdn_character))
                        } else {
                            launchRoomData(it, userName, roleType)
                        }
                    }
                }

                override fun onError(httpCode: Int, code: Int, message: String?) {
                    if (code == 1101021) {
                        ToastManager.showShort(context.getString(R.string.fcr_joinroom_tips_emptyid))
                    } else {
                        super.onError(httpCode, code, message)
                    }
                    setJoinRoomListener?.invoke(false, false, message ?: "")
                }
            })
        } else {
            ToastManager.showShort("roleType is $roleType (error)")
            setJoinRoomListener?.invoke(false, false, "roleType is $roleType (error)")
        }
    }

    fun launchRoomData(info: FcrJoinRoomRes, userName: String, roleType: Int) {
        val isNightMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false) // 暗黑模式
        val userName = userName
        val userUuid = AppUserInfoUtils.getCompanyId()
        val roomName = info.roomDetail.roomName
        val roomType = info.roomDetail.roomType
        val roomUuid = info.roomDetail.roomId // 房间号
        val duration = 1800L // 30 minutes x
        val roomRegion = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        val startTime = info.roomDetail.startTime // x

        val userProperties = mutableMapOf<String, String>()
        userProperties["avatar"] = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"

        val config = AgoraEduLaunchConfig(
            userName,
            userUuid,
            roomName,
            roomUuid,
            roleType,
            roomType,
            info.token,
            startTime,
            duration
        )

        config.appId = info.appId //setConfigPublicCourseware需要用到appid
        // 可选参数：区域
        config.region = roomRegion
        // 可选参数：用户参数
        config.userProperties = userProperties
        config.videoEncoderConfig = EduContextVideoEncoderConfig(
            FcrStreamParameters.HeightStream.width,
            FcrStreamParameters.HeightStream.height,
            FcrStreamParameters.HeightStream.frameRate,
            FcrStreamParameters.HeightStream.bitRate
        )
        config.uiMode = if (isNightMode) AgoraEduUIMode.DARK else AgoraEduUIMode.LIGHT
        // 可选参数：分享链接
        config.shareUrl = info.roomDetail.getShareLink(context)

        setConfigPublicCourseware(config)  // 测试数据
        setLectureType(info.roomDetail?.roomProperties, config)

        AgoraClassroomSDK.setConfig(AgoraClassSdkConfig(info.appId))

        launchRoom(config)
    }

    fun launchRoom(config: AgoraEduLaunchConfig) {
        AgoraClassroomSDK.launch(context, config, AgoraEduLaunchCallback { event ->
            LogX.e(TAG, ":launch-课堂状态:" + event.name)

            ContextCompat.getMainExecutor(context).execute {
                if (event == AgoraEduEvent.AgoraEduEventForbidden) {
                    setJoinRoomListener?.invoke(false, true, context.resources.getString(R.string.join_forbidden_message))
                    forbiddenDialog = ForbiddenDialogBuilder(context)
                        .title(context.resources.getString(R.string.join_forbidden_title))
                        .message(context.resources.getString(R.string.join_forbidden_message))
                        .positiveText(context.resources.getString(R.string.join_forbidden_button_confirm))
                        .positiveClick {
                            if (forbiddenDialog != null && forbiddenDialog?.isShowing == true) {
                                forbiddenDialog?.dismiss()
                                forbiddenDialog = null
                            }
                        }
                        .build()
                    forbiddenDialog?.show()
                } else {
                    setJoinRoomListener?.invoke(true, true, "success")
                }
            }
        })
    }

    /**
     * 设置大班课类型
     */
    fun setLectureType(roomProperties: FcrCreateRoomProperties? = null, config: AgoraEduLaunchConfig) {
        roomProperties?.let {
            if (roomProperties.serviceType == AgoraServiceType.LiveStandard.value){
                // 极速直播
                config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
            }else{
                // 互动直播
                config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
            }

            if (roomProperties.serviceType != AgoraServiceType.LivePremium.value) {
                config.serviceType = AgoraServiceType.fromValue(roomProperties.serviceType)
            }
        }
    }

    /**
     * 带入课件
     */
    fun setConfigPublicCourseware(launchConfig: AgoraEduLaunchConfig) {
        val courseware0 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data0)
        val courseware1 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data1)

        val publicCoursewares = ArrayList<AgoraEduCourseware>(2)
        publicCoursewares.add(courseware0)
        publicCoursewares.add(courseware1)

        val cloudDiskExtra = mutableMapOf<String, Any>()
        cloudDiskExtra[Statics.publicResourceKey] = publicCoursewares
        cloudDiskExtra[Statics.configKey] = Pair(launchConfig.appId, launchConfig.userUuid)

        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(AgoraWidgetConfig(FCRCloudDiskWidget::class.java, AgoraWidgetDefaultId.AgoraCloudDisk.id, extraInfo = cloudDiskExtra))
        launchConfig.widgetConfigs = widgetConfigs
    }
}