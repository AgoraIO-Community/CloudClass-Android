package io.agora.classroom.common

import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduRewardWindow
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialog
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.classroom.ui.AgoraClassUIController
import java.util.*

/**
 * 教室基类
 */
abstract class AgoraEduClassActivity : AgoraBaseClassActivity(), IAgoraUIProvider {
    private var destroyClassDialog: AgoraUIDialog? = null
    protected var uiController: AgoraClassUIController = AgoraClassUIController()

    /**
     * 页面ID
     */
    protected var uuid: String = UUID.randomUUID().toString()
    private var rewardWindow: AgoraEduRewardWindow? = null

    override fun onCreateEduCore(isSuccess: Boolean) {
        uiController.init(eduCore())
        setEduCoreListener()
    }

    override fun getAgoraEduCore(): AgoraEduCore? {
        return eduCore()
    }

    override fun getUIDataProvider(): UIDataProvider? {
        return uiController.uiDataProvider
    }

    private fun setEduCoreListener() {
        getEduContext()?.monitorContext()?.addHandler(object : IMonitorHandler {
            override fun onLocalNetworkQualityUpdated(quality: EduContextNetworkQuality) {

            }

            override fun onLocalConnectionUpdated(state: EduContextConnectionState) {
                updateConnectionState(state)
            }
        })

        getEduContext()?.roomContext()?.addHandler(object : RoomHandler() {
            override fun onRoomClosed() {
                super.onRoomClosed()
                destroyClassDialog()
            }
        })

        getEduContext()?.userContext()?.addHandler(object : UserHandler() {
            override fun onLocalUserKickedOut() {
                super.onLocalUserKickedOut()
                kickOut()
            }

            // play reward animation
            override fun onUserRewarded(
                user: AgoraEduContextUserInfo,
                rewardCount: Int,
                operator: AgoraEduContextUserInfo?
            ) {
                super.onUserRewarded(user, rewardCount, operator)
                ContextCompat.getMainExecutor(this@AgoraEduClassActivity).execute {
                    if (rewardWindow == null) {
                        rewardWindow = AgoraEduRewardWindow(this@AgoraEduClassActivity)
                    }
                    if (rewardWindow?.isShowing == false) {
                        rewardWindow?.show()
                    }
                }
            }
        })
    }

    /**
     * 踢出教室
     */
    fun kickOut() {
        ContextCompat.getMainExecutor(this).execute {
            if (!isDestroyPage()) {
                AgoraUIDialogBuilder(this)
                    .title(resources.getString(R.string.fcr_user_local_kick_out_notice))
                    .message(resources.getString(R.string.fcr_user_local_kick_out))
                    .positiveText(resources.getString(R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        finish()
                    }
                    .build()
                    .show()
                getEduContext()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                    }

                    override fun onFailure(error: EduContextError?) {
                        error?.let {
                            AgoraUIToast.error(context = this@AgoraEduClassActivity, text = error.msg)
                        }
                    }
                })
            }
        }
    }

    /**
     * 课程结束
     */
    private fun destroyClassDialog() {
        if (destroyClassDialog != null && destroyClassDialog!!.isShowing) {
            return
        }

        ContextCompat.getMainExecutor(this).execute {
            if (!isDestroyPage()) {
                destroyClassDialog = AgoraUIDialogBuilder(this)
                    .title(resources.getString(io.agora.agoraeduuikit.R.string.fcr_user_dialog_class_destroy_title))
                    .message(resources.getString(io.agora.agoraeduuikit.R.string.fcr_room_class_over))
                    .positiveText(resources.getString(io.agora.agoraeduuikit.R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        finish()
                    }
                    .build()
                destroyClassDialog?.show()
                getEduContext()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                    }

                    override fun onFailure(error: EduContextError?) {
                        error?.let {
                            AgoraUIToast.error(context = this@AgoraEduClassActivity, text = error.msg)
                        }
                    }
                })
            }
        }
    }

    /**
     * 多设备进教室，退出教室
     */
    fun updateConnectionState(connectionState: EduContextConnectionState) {
        //Constants.AgoraLog.e("connectionState -> $connectionState")

        if (connectionState == EduContextConnectionState.Reconnecting || connectionState == EduContextConnectionState.Connecting) {
            showLoading()  // 重连
        } else {
            dismissLoading()
        }

        if (connectionState == EduContextConnectionState.Aborted) {
            AgoraUIToast.error(
                context = this,
                text = resources.getString(io.agora.agoraeduuikit.R.string.fcr_monitor_login_remote_device)
            )

            eduCore()?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                override fun onSuccess(target: Unit?) {
                    finish()
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
        }
    }


    /**
     * Default setting of system devices as below:
     * - system speaker is turned on
     * - system camera is turned on for teachers, and turned off for students
     * - system microphone is turned on for teachers, and turned off for students
     * Note, this method is valid to use only after joining the classroom
     * successfully.
     */
    protected fun initSystemDevices() { // // 打开语音，摄像头，麦克风
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Speaker)
        eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let {
            if (it.role == AgoraEduContextUserRole.Teacher) {
                eduCore()?.eduContextPool()?.mediaContext()?.apply {
                    openSystemDevice(
                        if (AgoraUIDeviceSetting.isFrontCamera()) {
                            AgoraEduContextSystemDevice.CameraFront
                        } else {
                            AgoraEduContextSystemDevice.CameraBack
                        }
                    )
                    openSystemDevice(AgoraEduContextSystemDevice.Microphone)
                }
            } else {
                eduCore()?.eduContextPool()?.mediaContext()?.apply {
                    closeSystemDevice(AgoraEduContextSystemDevice.CameraFront)
                    closeSystemDevice(AgoraEduContextSystemDevice.CameraBack)
                    closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
                }
            }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        showLeaveDialog()
    }

    /**
     * 退出教室
     */
    fun showLeaveDialog() {
        AgoraUIDialogBuilder(this)
            .title(resources.getString(R.string.fcr_room_class_leave_class_title))
            .message(resources.getString(R.string.fcr_room_exit_warning))
            .negativeText(resources.getString(R.string.fcr_user_kick_out_cancel))
            .positiveText(resources.getString(R.string.fcr_user_kick_out_submit))
            .positiveClick {
                // 确保能退出页面
                if (getEduContext() == null || getEduContext()?.roomContext() == null) {
                    finish()
                }
                getEduContext()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                        finish()
                    }

                    override fun onFailure(error: EduContextError?) {
                        error?.let {
                            AgoraUIToast.error(context = this@AgoraEduClassActivity, text = error.msg)
                        }
                    }
                })
            }
            .build()
            .show()
    }

    protected fun join() {
        eduCore()?.eduContextPool()?.roomContext()?.joinRoom(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardWindow = null
    }
}
