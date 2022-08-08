package io.agora.classroom.common

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.agora.edu.component.teachaids.presenter.FCRLargeWindowManager
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreStateListener
import io.agora.agoraeducore.core.AgoraEduCoreStatics
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.agoraeducore.density.DensityManager
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.helper.FCRLauncherListener
import io.agora.classroom.helper.FCRLauncherManager
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
    lateinit var agoraLoading: AgoraLoadingDialog
    protected val uiHandler = Handler(Looper.getMainLooper())
    protected var isRelease = false // 是否释放资源了
    protected var classManager: AgoraEduClassManager? = null
    protected var isSowGroupDialog = false

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Constants.AgoraLog?.i("$tag -> onNewIntent:$this")
    }

    private fun initData() {
        isRelease = false
        agoraLoading = AgoraLoadingDialog(this)
        DensityManager.init(this, 9f / 16, 375)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(AgoraActivityLifecycle())
        }

        launchConfig = intent.getSerializableExtra(Companion.launchConfig) as AgoraEduLaunchConfig?
        preCheckData = intent.getParcelableExtra(Companion.preCheckData)

        registerScreenOffReceiver()
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

        Constants.AgoraLog?.e("$tag -> EduCore start = ${launchConfig?.fromPage} ||${getRoomType()}")

        // 从分组返回，分组返回大教室
        if (launchConfig?.fromPage == FCRGroupClassUtils.SUBROOM_PAGE &&
            getRoomType() != RoomType.GROUPING_CLASS
        ) {
            FCRGroupClassUtils.mainRoomInfo?.roomUuid?.let {
                eduCore = AgoraEduCoreManager.getEduCore(it)
                eduCore?.resetRoomJoined()
                eduCore?.setOnlyJoinRtcChannel(true)

                eduCore?.eduContextPool()?.roomContext()?.clear()
                eduCore?.eduContextPool()?.userContext()?.clear()
                eduCore?.eduContextPool()?.groupContext()?.clear()
                eduCore?.eduContextPool()?.mediaContext()?.clear()
                eduCore?.eduContextPool()?.streamContext()?.clear()
                eduCore?.eduContextPool()?.windowPropertiesContext()?.clear()
            }
        }

        if (eduCore == null) {
            Constants.AgoraLog?.i("$tag ->ready to execute AgoraEduCoreManager.createEduCore ")
            eduCore = AgoraEduCoreManager.createEduCore(launchConfig!!, preCheckData!!)
            Constants.AgoraLog?.i("$tag ->createEduCore created $eduCore")
            AgoraEduCoreManager.initEduCore(eduCore!!, applicationContext, object : AgoraEduCoreStateListener {
                override fun onCreated() {
                    Constants.AgoraLog?.i("$tag ->initEduCore onCreated")
                    // init mediaDeviceLifeCycleManager
                    deviceLifeCycleManager = MediaDeviceLifeCycleManager(this@AgoraBaseClassActivity, eduCore()?.eduContextPool())
                    classManager = AgoraEduClassManager(this@AgoraBaseClassActivity, eduCore)
                    onCreateEduCore(true)     // uiController，这2个顺序不要乱
                    callback.onSuccess(Unit)  // view init
                    AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
                }

                override fun onError(error: EduError) {
                    Constants.AgoraLog?.e("$tag ->initEduCore onError: ${error.msg}")
                    onCreateEduCore(false)
                    callback.onFailure(EduContextError(error.type, error.msg))
                    AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
                    dismissLoading()
                }
            })
        } else {
            Constants.AgoraLog?.i("$tag ->eduCore is not null")
            deviceLifeCycleManager = MediaDeviceLifeCycleManager(this@AgoraBaseClassActivity, eduCore()?.eduContextPool())
            classManager = AgoraEduClassManager(this@AgoraBaseClassActivity, eduCore)
            onCreateEduCore(true)     // uiController，这2个顺序不要乱
            callback.onSuccess(Unit)  // view init
            AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
        }
        Constants.AgoraLog?.e("$tag -> EduCore = $eduCore || ${eduCore?.eduRoom} || ${eduCore?.eduRoom?.eventListener}")
    }

    /**
     * 创建AgoraEduCore
     */
    open fun onCreateEduCore(isSuccess: Boolean) {
        if (isSuccess) {
            Constants.AgoraLog?.e("$tag -> addLauncherListener || ${launchConfig?.roomUuid}")
            launchConfig?.roomUuid?.let {
                FCRLauncherManager.addLauncherListener(it, exitListener)
                FCRLargeWindowManager.clearByRoom(it)
            }
        }
    }

    val exitListener = object : FCRLauncherListener {
        override fun onExit() {
            Constants.AgoraLog?.e("$tag -> addLauncherListener onExit")
            exit()
        }
    }

    fun exit() {
        eduCore?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                FCRHandlerManager.roomHandlerMap.forEach {
                    if (roomUuid == it.key) {
                        it.value.onRoomStateUpdated(AgoraEduContextUserLeaveReason.NORMAL)
                    }
                }

                isRelease = true
                finish()
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    AgoraUIToast.error(applicationContext, text = error.msg)
                }
            }
        })
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

    open fun getRoomType(): RoomType {
        launchConfig?.let {
            val roomType = RoomType.getRoomType(it.roomType)
            if (roomType == null) {
                Constants.AgoraLog?.e("roomType is null")
            } else {
                return roomType
            }
        }
        return RoomType.GROUPING_CLASS
    }

    override fun finish() {
        super.finish()
        release()
        Constants.AgoraLog?.i("$tag -> finish:$this")
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    /**
     * 大教室: release
     * 大教室 -> 分组：leaveRtcChannel
     * 分组 -> 大房间：leaveRoom & releaseRTC & releaseData
     * 分组 -> 分组：leaveRoom & releaseRTC & releaseData
     */
    fun releaseRTC() {
        eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid?.let {
            eduCore()?.releaseRTC(it)
        }
    }

    fun release() {
        if (!isRelease) {
            releaseData()
            eduCore()?.release()
            FCRGroupClassUtils.release()
            //unregisterReceiver(screenOffReceiver)
        }
    }

    fun releaseData() {
        isRelease = true
        dismissLoading()
        onRelease()
        deviceLifeCycleManager?.dispose()
        FCRLauncherManager.removeLauncherListener(exitListener)
    }

    /**
     * 释放资源
     */
    open fun onRelease() {

    }

    fun showLoading() {
        if (isSowGroupDialog) {
            return
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            agoraLoading.show()
        } else {
            uiHandler.postAtFrontOfQueue { agoraLoading.show() }
        }
        hiddenViewLoading()
    }

    fun dismissLoading() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            agoraLoading.dismiss()
        } else {
            uiHandler.postAtFrontOfQueue { agoraLoading.dismiss() }
        }
    }

    /**
     * 隐藏其他loading
     */
    open fun hiddenViewLoading() {

    }

    /**
     * 不要显示白板loading，当前分组重试加入
     */
    open fun setNotShowWhiteLoading(){

    }
}
