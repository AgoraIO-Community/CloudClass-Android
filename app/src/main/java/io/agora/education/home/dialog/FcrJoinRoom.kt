package io.agora.education.home.dialog

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskWidget
import com.agora.edu.component.teachaids.networkdisk.Statics
import io.agora.agoraeducore.core.context.EduContextVideoEncoderConfig
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.bean.FcrSceneType
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.utils.SkinUtils
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.classroom.helper.FcrStreamParameters
import io.agora.classroom.sdk.AgoraClassSdkConfig
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.config.ConfigData
import io.agora.education.config.ConfigUtil
import io.agora.education.data.DefaultPublicCoursewareJson
import io.agora.education.dialog.ForbiddenDialog
import io.agora.education.dialog.ForbiddenDialogBuilder
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrCreateRoomProperties
import io.agora.education.request.bean.FcrCreateRoomReq
import io.agora.education.request.bean.FcrCreateRoomRes
import io.agora.education.request.bean.FcrJoinRoomReq
import io.agora.education.request.bean.FcrJoinRoomRes
import io.agora.education.request.bean.FcrRoomDetail
import io.agora.education.utils.HashUtil
import io.agora.online.sdk.AgoraOnlineClassSdkConfig
import io.agora.online.sdk.AgoraOnlineClassroomSDK

/**
 * author : felix
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

    fun joinQueryRoom(roomId: String, roleType: Int, userName: String) {
        queryRoom(roomId, roleType, userName)
    }

    /**
     * 由于通用场景不支持老师端，所以先查询房间信息
     */
    fun queryRoom(roomId: String, roleType: Int, userName: String) {
        val call = AppRetrofitManager.instance().getService(AppService::class.java).getRoomInfoForVisitor(roomId)
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrRoomDetail>>() {
            override fun onSuccess(result: HttpBaseRes<FcrRoomDetail>?) {
                if (result?.data?.sceneType == FcrSceneType.CLOUD_CLASS.value && roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
                    ToastManager.showShort(context.getString(R.string.fcr_login_free_tips_app_support_role))
                    setJoinRoomListener?.invoke(
                        false, false, context.getString(R.string.fcr_login_free_tips_app_support_role)
                    )
                } else {
                    joinRoom(roomId, roleType, userName)
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                setJoinRoomListener?.invoke(false, false, message ?: "")
            }
        })
    }

    fun joinRoom(roomId: String, roleType: Int, userName: String) {
        if (roleType == AgoraEduRoleType.AgoraEduRoleTypeStudent.value || roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value || roleType == AgoraEduRoleType.AgoraEduRoleTypeObserver.value) {
            // 加入房间接口 userUuid 和 lunch 房间的 userUuid要一样，否则RTM无法登录
            val userUuid = getUserUuid(userName, roleType)
            val req = FcrJoinRoomReq(roomId, roleType, userUuid, userName)

            val call = if (AppUserInfoUtils.isLogin()) {
                AppRetrofitManager.instance().getService(AppService::class.java)
                    .joinRoom(AppUserInfoUtils.getCompanyId(), req)
            } else {
                AppRetrofitManager.instance().getService(AppService::class.java).joinRoomForVisitor(req)
            }

            AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrJoinRoomRes>>() {
                override fun onSuccess(result: HttpBaseRes<FcrJoinRoomRes>?) {
                    result?.data?.let {
                        val serviceType = it.roomDetail?.roomProperties?.serviceType
                        if ((serviceType == AgoraServiceType.Fusion.value || serviceType == AgoraServiceType.HostingScene.value) &&
                            (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value)
                        ) {
                            setJoinRoomListener?.invoke(
                                false,
                                false,
                                context.getString(R.string.fcr_joinroom_tips_cdn_character)
                            )
                            ToastManager.showShort(context.getString(R.string.fcr_joinroom_tips_cdn_character))
                        } else {
                            launchRoomData(it, it.roomDetail.userName ?: userName, userUuid, roleType)
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

    /**
     * 免登录：（不互踢）
     * Web： md5(`${userName}-${role}`)
     * iOS：md5(`${userName}_${role}`)
     * Android：md5(`${userName}${role}`)
     *
     *  登录：（互踢）
     * ”${companyId}_$(role)“
     */
    fun getUserUuid(userName: String, roleType: Int): String {
        return if (AppUserInfoUtils.isLogin()) {
            AppUserInfoUtils.getCompanyId() + "_" + roleType
        } else {
            //HashUtil.md5(userName).plus(roleType).lowercase()
            HashUtil.md5(userName + roleType).lowercase()
        }
    }

    fun launchRoomData(info: FcrJoinRoomRes, userName: String, userUuid: String, roleType: Int) {
        val isNightMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false) // 暗黑模式
        //val userUuid = getUserUuid(userName, roleType)
        val roomName = info.roomDetail.roomName
        val roomType = info.roomDetail.getRoomType()
        val roomUuid = info.roomDetail.roomId // 房间号
        val duration = 1800L // 30 minutes x
        val roomRegion = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        val startTime = info.roomDetail.startTime // x

        val userProperties = mutableMapOf<String, String>()
        userProperties["avatar"] = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"

        val config = AgoraEduLaunchConfig(
            userName, userUuid, roomName, roomUuid, roleType, roomType, info.token, startTime, duration
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
        config.shareUrl = info.roomDetail.getShareLink(context, userName)

        setConfigPublicCourseware(config)  // 测试数据
        setLectureType(info.roomDetail?.roomProperties, config)

        // 暗黑模式，建议这个写在Application中，避免页面重启
        if (isNightMode) {
            SkinUtils.setNightMode(true)
        } else {
            SkinUtils.setNightMode(false)
        }

        if (info.roomDetail.sceneType == FcrSceneType.CLOUD_CLASS.value) {
            if (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
                ToastManager.showShort(context.getString(R.string.fcr_login_free_tips_app_support_role))
                setJoinRoomListener?.invoke(
                    false, false, context.getString(R.string.fcr_login_free_tips_app_support_role)
                )
            } else {
                launchOnlineRoom(config)
            }
        } else {
            launchRoom(config)
        }
    }

    /**
     * 免登录：创建房间并进入房间
     */
    fun createJoinRoom(userName: String, roomName: String, roleType: Int, roomType: Int) {
        val startTime = System.currentTimeMillis()
        // Room default duration is 30 minutes
        val createRoomReq = FcrCreateRoomReq(roomName, roomType, startTime, startTime + 1800 * 1000)
        createRoomReq.sceneType = roomType
        createRoomReq.roomProperties = FcrCreateRoomProperties()

        val call =
            if (AppUserInfoUtils.isLogin()) {
                AppRetrofitManager.instance().getService(AppService::class.java)
                    .createRoom(AppUserInfoUtils.getCompanyId(), createRoomReq)
            } else {
                AppRetrofitManager.instance().getService(AppService::class.java)
                    .createRoomForVisitor(createRoomReq)
            }

        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrCreateRoomRes>>() {
            override fun onSuccess(result: HttpBaseRes<FcrCreateRoomRes>?) {
                val roomId = result?.data?.roomId
                joinRoom(roomId ?: "", roleType, userName)
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                super.onError(httpCode, code, message)
                setJoinRoomListener?.invoke(false, false, message ?: "")
            }
        })
    }

    fun launchRoom(userName: String, roomName: String, roleType: Int, roomType: Int) {
        val roomUuid = HashUtil.md5(roomName).plus(roomType).lowercase()
        val userUuid = HashUtil.md5(userName).plus(roleType).lowercase()
        val roomRegion = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)

        Log.e(TAG, "launchRoom => roomUuid= $roomUuid || userUuid=$userUuid || roomRegion=$roomRegion")

        ConfigUtil.getV3Config(AppHostUtil.getAppHostUrl(roomRegion),
            roomUuid,
            roleType,
            userUuid,
            object : EduCallback<ConfigData> {
                override fun onSuccess(info: ConfigData?) {
                    // Use authentication info from server instead
                    info?.let {
                        // Room default duration is 30 minutes
                        val duration = 1800L

                        val userProperties = mutableMapOf<String, String>()
                        userProperties["avatar"] =
                            "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"

                        val config = AgoraEduLaunchConfig(
                            userName, userUuid, roomName, roomUuid, roleType, roomType, it.token, null, duration
                        )
                        config.appId = it.appId
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
                        // 互动直播比极速直播更快，延迟少
                        config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
                        setConfigPublicCourseware(config)  // 测试数据

                        // 测试：暗黑模式
                        val isNightMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false)
                        config.uiMode = if (isNightMode) AgoraEduUIMode.DARK else AgoraEduUIMode.LIGHT

                        // 暗黑模式，建议这个写在Application中，避免页面重启
                        if (isNightMode) {
                            SkinUtils.setNightMode(true)
                        } else {
                            SkinUtils.setNightMode(false)
                        }

                        launchRoom(config)
                        Log.e(TAG, "appId = ${config.appId}")
                    }
                }

                override fun onFailure(error: EduError) {
                    ToastManager.showShort("Get Token error: ${error.msg} ( ${error.type})")
                }
            })
    }

    /**
     * 教育场景
     */
    fun launchRoom(config: AgoraEduLaunchConfig) {
        AgoraClassroomSDK.setConfig(AgoraClassSdkConfig(config.appId))
        AgoraClassroomSDK.launch(context, config, AgoraEduLaunchCallback { event ->
            LogX.e(TAG, ":launch-课堂状态:" + event.name)

            ContextCompat.getMainExecutor(context).execute {
                if (event == AgoraEduEvent.AgoraEduEventForbidden) {
                    setJoinRoomListener?.invoke(
                        false, true, context.resources.getString(R.string.join_forbidden_message)
                    )
                    forbiddenDialog =
                        ForbiddenDialogBuilder(context).title(context.resources.getString(R.string.join_forbidden_title))
                            .message(context.resources.getString(R.string.join_forbidden_message))
                            .positiveText(context.resources.getString(R.string.join_forbidden_button_confirm))
                            .positiveClick {
                                if (forbiddenDialog != null && forbiddenDialog?.isShowing == true) {
                                    forbiddenDialog?.dismiss()
                                    forbiddenDialog = null
                                }
                            }.build()
                    forbiddenDialog?.show()
                } else {
                    setJoinRoomListener?.invoke(true, true, "success")
                }
            }
        })
    }

    /**
     * 云课堂场景
     */
    fun launchOnlineRoom(config: AgoraEduLaunchConfig) {
        AgoraOnlineClassroomSDK.setConfig(AgoraOnlineClassSdkConfig(config.appId))
        AgoraOnlineClassroomSDK.launch(context, config, AgoraEduLaunchCallback { event ->
            LogX.e(TAG, ":launch-课堂状态:" + event.name)

            ContextCompat.getMainExecutor(context).execute {
                if (event == AgoraEduEvent.AgoraEduEventForbidden) {
                    setJoinRoomListener?.invoke(
                        false, true, context.resources.getString(R.string.join_forbidden_message)
                    )
                    forbiddenDialog =
                        ForbiddenDialogBuilder(context).title(context.resources.getString(R.string.join_forbidden_title))
                            .message(context.resources.getString(R.string.join_forbidden_message))
                            .positiveText(context.resources.getString(R.string.join_forbidden_button_confirm))
                            .positiveClick {
                                if (forbiddenDialog != null && forbiddenDialog?.isShowing == true) {
                                    forbiddenDialog?.dismiss()
                                    forbiddenDialog = null
                                }
                            }.build()
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
            if (roomProperties.serviceType == AgoraServiceType.LiveStandard.value) {
                // 极速直播
                config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
            } else {
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
        widgetConfigs.add(
            AgoraWidgetConfig(
                FCRCloudDiskWidget::class.java, AgoraWidgetDefaultId.AgoraCloudDisk.id, extraInfo = cloudDiskExtra
            )
        )
        launchConfig.widgetConfigs = widgetConfigs
    }
}