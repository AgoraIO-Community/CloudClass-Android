package io.agora.classroom.common

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.agora.edu.component.teachaids.presenter.FCRLargeWindowManager
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.AgoraEduCoreStateListener
import io.agora.agoraeducore.core.AgoraEduCoreStatics
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.launch.AgoraEduUIMode
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager
import io.agora.agoraeducore.core.internal.transport.OnAgoraTransportListener
import io.agora.agoraeducore.density.DensityManager
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.util.MultiLanguageUtil
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.classroom.helper.FCRLauncherListener
import io.agora.classroom.helper.FCRLauncherManager
import io.agora.classroom.presenter.MediaDeviceLifeCycleManager
import java.util.*

abstract class AgoraBaseClassActivity : AppCompatActivity() {
    private companion object {
        const val launchConfig = "LAUNCHCONFIG"
        const val preCheckData = "PRECHECKDATA"
        const val selectFileResultCode = 9998 //云盘选择文件定义code
        const val selectImageResultCode = 9999 //云盘选择图片定义code
    }

    open var TAG = "BaseClassActivity"
    private var preCheckData: RoomPreCheckRes? = null
    private var deviceLifeCycleManager: MediaDeviceLifeCycleManager? = null
    private var eduCore: AgoraEduCore? = null

    protected var launchConfig: AgoraEduLaunchConfig? = null
    protected val uiHandler = Handler(Looper.getMainLooper())
    protected var isRelease = false // 是否释放资源了
    protected var classManager: AgoraEduClassManager? = null
    protected var isSowGroupDialog = false

    lateinit var agoraLoading: AgoraLoadingDialog

    var isJonRoomSuccess = false
    var connState: EduContextConnectionState = EduContextConnectionState.Connected

    protected fun eduCore(): AgoraEduCore? {
        return eduCore
    }

    open fun getEduContext(): EduContextPool? {
        return eduCore?.eduContextPool()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
        initData()
        showLoading()
    }

    fun setLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            window.statusBarColor = Color.parseColor("#00000000")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        LogX.i(TAG, "-> onNewIntent:$this")
    }

    private fun initData() {
        isRelease = false
        agoraLoading = AgoraLoadingDialog(this)
        agoraLoading.show()
        DensityManager.init(this, 9f / 16, 375)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(AgoraActivityLifecycle())
        }

        launchConfig = intent.getSerializableExtra(Companion.launchConfig) as AgoraEduLaunchConfig?
        preCheckData = intent.getParcelableExtra(Companion.preCheckData)

        setDarkMode()

        AgoraTransportManager.addListener(AgoraTransportEventId.EVENT_ID_WHITEBOARD_LOADING, object : OnAgoraTransportListener {
                override fun onTransport(event: AgoraTransportEvent) {
                    // 处理消息
                    if (event.arg2 == true) {
                        showLoading()
                    } else if (connState == EduContextConnectionState.Connected && isJonRoomSuccess) {
                        dismissLoading()
                    }
                }
        })
    }

    fun setDarkMode() {
        if (launchConfig?.uiMode == AgoraEduUIMode.DARK) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AgoraEduCoreStatics.selectImageResultCode && resultCode == RESULT_OK) {
            // result of select image
            val intent = Intent()
            intent.action = packageName.plus(resources.getString(R.string.fcr_chat_window_select_image_action))
            intent.putExtra(resources.getString(R.string.fcr_chat_window_select_image_key), data?.data)
            sendBroadcast(intent)
        }else if (requestCode == selectFileResultCode && resultCode == RESULT_OK) {
            // result of select image
            val intent = Intent()
            intent.action = packageName.plus(resources.getString(R.string.my_clould_select_file_action))
            intent.putExtra(resources.getString(R.string.my_clould_select_image_key), data?.data)
            sendBroadcast(intent)
        }else if (requestCode == selectImageResultCode && resultCode == RESULT_OK) {
            // result of select image
            val intent = Intent()
            intent.action = packageName.plus(resources.getString(R.string.my_clould_select_image_action))
            intent.putExtra(resources.getString(R.string.my_clould_select_image_key), data?.data)
            sendBroadcast(intent)
        }

    }

    protected fun createEduCore(callback: EduContextCallback<Unit>) {
        if (launchConfig == null) {
            val msg = "init room fail, launch config is null"
            LogX.e(TAG, "-> $msg")
            callback.onFailure(EduContextError(EduContextErrors.parameterErrCode, msg))
            return
        }

        if (preCheckData == null) {
            val msg = "init room fail, precheck data is null"
            LogX.e(TAG, "-> $msg")
            callback.onFailure(EduContextError(EduContextErrors.parameterErrCode, msg))
            return
        }

        LogX.e(TAG, "-> EduCore start = ${launchConfig?.fromPage} ||${getRoomType()}")

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
            LogX.i(TAG, "->ready to execute AgoraEduCoreManager.createEduCore ")
            eduCore = AgoraEduCoreManager.createEduCore(launchConfig!!, preCheckData!!)
            isAutoSubscribe()?.let {
                eduCore?.config?.autoSubscribe = it
            }
            LogX.i(TAG, "->createEduCore created $eduCore")
            AgoraEduCoreManager.initEduCore(eduCore!!, applicationContext, object : AgoraEduCoreStateListener {
                override fun onCreated() {
                    LogX.i(TAG, "->initEduCore onCreated")
                    // init mediaDeviceLifeCycleManager
                    deviceLifeCycleManager =
                        MediaDeviceLifeCycleManager(this@AgoraBaseClassActivity, eduCore()?.eduContextPool())
                    classManager = AgoraEduClassManager(this@AgoraBaseClassActivity, eduCore)
                    onCreateEduCore(true)     // uiController，这2个顺序不要乱
                    callback.onSuccess(Unit)  // view init
                    AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
                }

                override fun onError(error: EduError) {
                    LogX.e(TAG, "->initEduCore onError: ${error.msg}")
                    onCreateEduCore(false)
                    callback.onFailure(EduContextError(error.type, error.msg))
                    AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
                    dismissLoading()
                }
            })
        } else {
            LogX.i(TAG, "->eduCore is not null")
            eduCore?.initRoomContext()
            deviceLifeCycleManager = MediaDeviceLifeCycleManager(this@AgoraBaseClassActivity, eduCore()?.eduContextPool())
            classManager = AgoraEduClassManager(this@AgoraBaseClassActivity, eduCore)
            onCreateEduCore(true)     // uiController，这2个顺序不要乱
            callback.onSuccess(Unit)  // view init
            AgoraEduCoreManager.removeEduCoreListener(launchConfig!!.roomUuid)
        }
        LogX.e(TAG, "-> EduCore = $eduCore || ${eduCore?.eduRoom} || ${eduCore?.eduRoom?.eventListener}")
    }

    /**
     * 创建AgoraEduCore
     */
    open fun onCreateEduCore(isSuccess: Boolean) {
        if (isSuccess) {
            LogX.e(TAG, "-> addLauncherListener || ${launchConfig?.roomUuid}")
            launchConfig?.roomUuid?.let {
                FCRLauncherManager.addLauncherListener(it, exitListener)
                FCRLargeWindowManager.clearByRoom(it)
            }
        }
    }

    val exitListener = object : FCRLauncherListener {
        override fun onExit() {
            LogX.e(TAG, "-> addLauncherListener onExit")
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
                LogX.e("roomType is null")
            } else {
                return roomType
            }
        }
        return RoomType.GROUPING_CLASS
    }

    override fun finish() {
        super.finish()
        release()
        LogX.i(TAG, "-> finish:$this")
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
        AgoraTransportManager.removeListener(AgoraTransportEventId.EVENT_ID_WHITEBOARD_LOADING)
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
    open fun setNotShowWhiteLoading() {

    }

    open fun isAutoSubscribe(): Boolean? {
        return null
    }

    override fun attachBaseContext(newBase: Context?) {
        val language: String = SpUtil.getString(this, AppHostUtil.LOCALE_LANGUAGE)
        val country: String = SpUtil.getString(this, AppHostUtil.LOCALE_AREA)
        if (!TextUtils.isEmpty(language) && !TextUtils.isEmpty(country)) {
            val locale = Locale(language, country)
            MultiLanguageUtil.changeAppLanguage(newBase, locale, false)
        }
        super.attachBaseContext(newBase);
    }
}
