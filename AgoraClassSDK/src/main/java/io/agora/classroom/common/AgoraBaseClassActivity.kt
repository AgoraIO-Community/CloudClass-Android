package io.agora.classroom.common

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.agoraclasssdk.R
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.AgoraEduCoreStateListener
import io.agora.agoraeducore.core.AgoraEduCoreStatics
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.data.EduUserRole
import io.agora.agoraeducore.core.internal.launch.AgoraEduLatencyLevel
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.agoraeducore.density.DensityManager
import io.agora.classroom.presenter.MediaDeviceLifeCycleManager

abstract class AgoraBaseClassActivity : AppCompatActivity() {
    private companion object {
        const val tag = "BaseClassActivity"
        const val launchConfig = "LAUNCHCONFIG"
        const val preCheckData = "PRECHECKDATA"
    }

    protected var launchConfig: AgoraEduLaunchConfig? = null
    private var preCheckData: RoomPreCheckRes? = null
    //private lateinit var screenOffReceiver: ScreenOffBroadcastReceiver
    private var deviceLifeCycleManager: MediaDeviceLifeCycleManager? = null
    private var eduCore: AgoraEduCore? = null
    protected lateinit var agoraLoading: AgoraLoadingDialog

    protected fun eduCore(): AgoraEduCore? {
        return eduCore
    }

    open fun getEduContext(): EduContextPool? {
        return eduCore?.eduContextPool()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        initData()
        showLoading()
    }

    private fun initData() {
        agoraLoading = AgoraLoadingDialog(this)
        DensityManager.init(this, 9f / 16, 375)

        launchConfig = intent.getParcelableExtra(Companion.launchConfig)
        preCheckData = intent.getParcelableExtra(Companion.preCheckData)
        registerScreenOffReceiver()
    }

    override fun onResume() {
        super.onResume()
        Constants.AgoraLog?.e("$tag -> onResume")
    }

    override fun onStop() {
        super.onStop()
        Constants.AgoraLog?.e("$tag -> onStop")
    }

    protected open fun onCreateClassJoinMediaOption(): ClassJoinMediaOption {
        // By default, students do not publish media streams.
        // All roles will automatically subscribe any streams that
        // exist in the room
        return ClassJoinMediaOption(true, launchConfig?.roleType != EduUserRole.STUDENT.value)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AgoraEduCoreStatics.selectImageResultCode && resultCode == RESULT_OK) {
            // result of select image
            val intent = Intent()
            intent.action = packageName.plus(resources.getString(R.string.fcr_chat_window_select_image_action))
            intent.putExtra(resources.getString(R.string.fcr_chat_window_select_image_key), data?.data)
            sendBroadcast(intent)
        }
    }

    protected fun createEduCore(callback: EduContextCallback<Unit>) {
        if (launchConfig == null) {
            val msg = "init room fail, launch config is null"
            Constants.AgoraLog?.e("$tag -> $msg")
            callback.onFailure(EduContextError(EduContextErrors.parameterErrCode, msg))
            return
        }

        if (preCheckData == null) {
            val msg = "init room fail, precheck data is null"
            Constants.AgoraLog?.e("$tag -> $msg")
            callback.onFailure(EduContextError(EduContextErrors.parameterErrCode, msg))
            return
        }

        val mediaOption = onCreateClassJoinMediaOption()
        // 创建对象的时候，就自动login
        eduCore = AgoraEduCore(applicationContext,
            AgoraEduCoreConfig(
                appId = launchConfig!!.appId,
                userName = launchConfig!!.userName,
                userUuid = launchConfig!!.userUuid,
                roomName = launchConfig!!.roomName,
                roomUuid = launchConfig!!.roomUuid,
                roleType = launchConfig!!.roleType,
                roomType = launchConfig!!.roomType,
                rtmToken = launchConfig!!.rtmToken,
                startTime = preCheckData!!.startTime,
                duration = preCheckData!!.duration,
                rtcRegion = preCheckData!!.rtcRegion ?: "",
                rtmRegion = preCheckData!!.rtmRegion ?: "",
                mediaOptions = launchConfig!!.mediaOptions,
                userProperties = launchConfig!!.userProperties,
                widgetConfigs = launchConfig!!.widgetConfigs,
                state = preCheckData!!.state,
                closeDelay = preCheckData!!.closeDelay,
                lastMessageId = preCheckData!!.lastMessageId,
                muteChat = preCheckData!!.muteChat,
                videoEncoderConfig = launchConfig!!.videoEncoderConfig,
                agoraEduStreamState = launchConfig!!.agoraEduStreamState,
                latencyLevel = launchConfig!!.latencyLevel ?: AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
                eyeCare = launchConfig!!.eyeCare,
                vendorId = launchConfig!!.vendorId,
                logDir = launchConfig!!.logDir,
                autoSubscribe = mediaOption.autoSubscribe,
                autoPublish = mediaOption.autoPublish,
                needUserListener = true
            ),
            object : AgoraEduCoreStateListener {
                override fun onCreated() {
                    // init mediaDeviceLifeCycleManager
                    deviceLifeCycleManager =
                        MediaDeviceLifeCycleManager(this@AgoraBaseClassActivity, eduCore()?.eduContextPool())
                    callback.onSuccess(Unit)
                    onCreateEduCore(true)
                    dismissLoading()
                }

                override fun onError(error: EduError) {
                    Constants.AgoraLog?.e("$tag -> ${error.msg}")
                    callback.onFailure(EduContextError(error.type, error.msg))
                    onCreateEduCore(false)
                    dismissLoading()
                }
            }
        )
    }

    /**
     * 创建AgoraEduCore
     */
    open fun onCreateEduCore(isSuccess: Boolean) {

    }

    private fun registerScreenOffReceiver() {
        //screenOffReceiver = ScreenOffBroadcastReceiver(this)
        //registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    /**
     * 页面是否销毁
     */
    open fun isDestroyPage(): Boolean {
        return isFinishing || isDestroyed
    }

    override fun getResources(): Resources {
        val resources: Resources = super.getResources()
        DensityManager.setDensity(this, resources)
        return resources
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoading()
        deviceLifeCycleManager?.dispose()
        eduCore()?.release()
        //unregisterReceiver(screenOffReceiver)
    }

    data class ClassJoinMediaOption(
        val autoSubscribe: Boolean,
        val autoPublish: Boolean
    )

    fun showLoading() {
        //Constants.AgoraLog.e("connectionState -> showLoading")
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            agoraLoading.show()
        } else {
            runOnUiThread { agoraLoading.show() }
        }
    }

    fun dismissLoading() {
        //Constants.AgoraLog.e("connectionState -> dismiss")
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            agoraLoading.dismiss()
        } else {
            runOnUiThread { agoraLoading.dismiss() }
        }
    }
}
