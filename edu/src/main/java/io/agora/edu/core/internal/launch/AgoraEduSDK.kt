package io.agora.edu.core.internal.launch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import io.agora.edu.BuildConfig
import io.agora.edu.R
import io.agora.edu.core.ClassInfoCache.getRoomActivityDefault
import io.agora.edu.core.ClassInfoCache.getRoomActivityReplaced
import io.agora.edu.core.internal.base.PreferenceManager
import io.agora.edu.core.internal.base.ToastManager
import io.agora.edu.core.internal.base.network.RetrofitManager
import io.agora.edu.core.internal.edu.common.api.BoardPreload
import io.agora.edu.core.internal.edu.common.api.RoomPre
import io.agora.edu.core.internal.edu.common.impl.BoardPreloadImpl
import io.agora.edu.core.internal.edu.common.impl.RoomPreImpl
import io.agora.edu.core.internal.edu.common.listener.BoardPreloadListener
import io.agora.edu.core.internal.education.impl.Constants
import io.agora.edu.core.internal.education.impl.util.UnCatchExceptionHandler.Companion.getExceptionHandler
import io.agora.edu.core.internal.framework.EduManager
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.log.LogManager
import io.agora.edu.core.internal.report.ReportManager.getAPaasReporter
import io.agora.edu.core.internal.report.ReportManager.init
import io.agora.edu.core.internal.report.ReportManager.setJoinRoomInfo
import io.agora.edu.core.internal.report.reporters.APaasReporter
import io.agora.edu.core.internal.server.struct.request.RoomPreCheckReq
import io.agora.edu.core.internal.server.struct.response.EduRemoteConfigRes
import io.agora.edu.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.edu.extensions.extapp.AgoraExtAppConfiguration
import io.agora.edu.extensions.extapp.AgoraExtAppEngine.Companion.registerExtAppList
import io.agora.edu.extensions.widgets.UiWidgetManager.Companion.registerAndReplace
import io.agora.edu.extensions.widgets.UiWidgetManager.Companion.registerDefault
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

object AgoraEduSDK {
    private const val tag = "AgoraEduSDK"
    const val REQUEST_CODE_RTC = 101
    const val REQUEST_CODE_RTE = 909
    const val CODE = "code"
    const val REASON = "reason"
    var defaultCallback = AgoraEduLaunchCallback { state: AgoraEduEvent? -> Constants.AgoraLog.i("$tag->This is the default null implementation!") }
    var agoraEduLaunchCallback = defaultCallback
    private var roomPre: RoomPre? = null
    private var agoraEduSDKConfig: AgoraEduSDKConfig? = null
    private val classRoom = AgoraEduClassRoom()
    const val DYNAMIC_URL = "https://convertcdn.netless.link/dynamicConvert/%s.zip"
    const val DYNAMIC_URL1 = "https://%s/dynamicConvert/%s.zip"
    const val STATIC_URL = "https://convertcdn.netless.link/staticConvert/%s.zip"
    const val PUBLIC_FILE_URL = "https://convertcdn.netless.link/publicFiles.zip"
    var COURSEWARES = Collections.synchronizedList(ArrayList<AgoraEduCourseware>())
    private var baseUrl = BuildConfig.API_BASE_URL
    private var reportUrl = BuildConfig.REPORT_BASE_URL
    private const val reportUrlV2 = BuildConfig.REPORT_BASE_URL_V2
    private var region = AgoraEduRegion.cn
    private const val LOGS_DIR_NAME = "logs"

    @JvmStatic
    fun baseUrl(): String {
        return baseUrl + region.toLowerCase() + File.separator
    }

    fun reportUrl(): String {
        return reportUrl + region.toLowerCase() + File.separator
    }

    fun logHostUrl(): String {
        return BuildConfig.LOG_OSS_CALLBACK_HOST.plus(region.toLowerCase()).plus(File.separator);
    }

    fun reportUrlV2(): String {
        return reportUrlV2
    }

    @JvmStatic
    fun setParameters(json: String?) {
        try {
            val obj = JSONObject(json)
            if (obj.has("edu.apiUrl")) {
                baseUrl = obj.getString("edu.apiUrl")
            }
            if (obj.has("edu.reportUrl")) {
                reportUrl = obj.getString("edu.reportUrl")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private val classRoomListener: ActivityLifecycleListener = object : ActivityLifecycleListener() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Constants.AgoraLog.i("$tag->classRoomListener:onActivityCreated")
            classRoom.add(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            Constants.AgoraLog.i("$tag->classRoomListener:onActivityDestroyed")
            classRoom.updateState(AgoraEduEvent.AgoraEduEventDestroyed)
        }
    }
    private var boardPreload: BoardPreload? = null

    @JvmStatic
    fun version(): String {
        return EduManager.version()
    }

    fun dispose() {
        COURSEWARES.clear()
        agoraEduSDKConfig = null
        if (boardPreload != null) {
            boardPreload!!.cancelAllPreloadTask()
        }
        agoraEduLaunchCallback = defaultCallback
    }

    fun configCourseWare(coursewares: List<AgoraEduCourseware>) {
        for (courseware in coursewares) {
            if (!COURSEWARES.contains(courseware)) {
                COURSEWARES.add(courseware)
            }
        }
    }

    @Throws(Exception::class)
    fun downloadCourseWare(context: Context, listener: AgoraEduCoursewarePreloadListener?) {
        if (!classRoom.isIdle) {
            // classRoom is running, return
            callbackError(context, "classRoom is running!")
            return
        }
        if (boardPreload == null || !boardPreload!!.isAvailable()) {
            boardPreload = BoardPreloadImpl(context)
        }
        boardPreload!!.preload(PUBLIC_FILE_URL, null)
        for (ware in COURSEWARES) {
            if (!TextUtils.isEmpty(ware.resourceUrl)) {
                boardPreload!!.preload(ware.resourceUrl!!, object : BoardPreloadListener {
                    override fun onStartDownload(url: String) {
                        listener!!.onStartDownload(ware)
                    }

                    override fun onProgress(url: String, progress: Double) {
                        listener!!.onProgress(ware, progress)
                    }

                    override fun onComplete(url: String) {
                        listener!!.onComplete(ware)
                    }

                    override fun onFailed(url: String) {
                        listener!!.onFailed(ware)
                    }
                })
            } else {
                callbackError(context, "resourceUrl is empty!")
                listener!!.onFailed(ware)
            }
        }
    }

    private fun pauseAllCacheTask() {
        if (boardPreload != null && boardPreload!!.isAvailable()) {
            boardPreload!!.cancelAllPreloadTask()
        }
    }

    @JvmStatic
    fun setConfig(agoraEduSDKConfig: AgoraEduSDKConfig) {
        AgoraEduSDK.agoraEduSDKConfig = agoraEduSDKConfig
    }

    @JvmStatic
    fun registerExtApps(apps: MutableList<AgoraExtAppConfiguration>) {
        registerExtAppList(apps)
    }

    private val reporter: APaasReporter
        private get() = getAPaasReporter()

    fun launch(context: Context,
               config: AgoraEduLaunchConfig,
               callback: AgoraEduLaunchCallback): AgoraEduClassRoom? {
        // parameter check
        if (!classRoom.isIdle) {
            val msg = "curState is not AgoraEduEventDestroyed, launch() cannot be called"
            callbackError(context, msg)
        }
        if (agoraEduSDKConfig!!.eyeCare != 0 && agoraEduSDKConfig!!.eyeCare != 1) {
            val msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduSDKConfig.eyeCare is not expected, it must be 0 or 1!")
            callbackError(context, msg)
        }
        if (!AgoraEduRoleType.isValid(config.roleType)) {
            val msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduLaunchConfig.roleType is not expected, it must be 2!")
            callbackError(context, msg)
        }
        if (!AgoraEduRoomType.isValid(config.roomType)) {
            val msg = String.format(context.getString(R.string.parametererror), "The value of " +
                    "AgoraEduLaunchConfig.roomType is not expected, it must be 0 or 4 or 2 !")
            callbackError(context, msg)
        }

        // init logManager
        if (TextUtils.isEmpty(config.logDir)) {
            val tmp = context.cacheDir.absolutePath + File.separator + LOGS_DIR_NAME
            config.setLogDirPath(tmp)
        }
        LogManager.init(config.logDir!!, tag)
        getExceptionHandler().init(context.applicationContext,
                config.logDir!!, "io.agora")
        Constants.AgoraLog = LogManager("SDK")
        Constants.AgoraLog.i("%s: Init LogManager,log path is \$s", tag, config.logDir)

        // set header and add logger
        if (!TextUtils.isEmpty(config.rtmToken)) {
            RetrofitManager.instance().addHeader("x-agora-token", config.rtmToken)
            RetrofitManager.instance().addHeader("x-agora-uid", config.userUuid)
            RetrofitManager.instance().setLogger(object : HttpLoggingInterceptor.Logger {
                override fun log(s: String) {
                    Constants.AgoraLog.i(s)
                }
            })
        }

        // Register user-defined widgets as long as they maintain
        // their widget ids somewhere else.
        // Can replace any widgets that are registered as default above.
        if (config.widgetConfigs != null) {
            registerAndReplace(config.region, config.widgetConfigs)
        } else {
            registerDefault(config.region)
        }

        // before launch, pause All CacheTask
        pauseAllCacheTask()
        // step-0: get agoraEduSDKConfig and to configure
        if (agoraEduSDKConfig == null) {
            Constants.AgoraLog.i("$tag->agoraEduSDKConfig is null!")
            return null
        }

        // get region
        if (!TextUtils.isEmpty(config.region)) {
            region = config.region
        }
        config.appId = agoraEduSDKConfig!!.appId
        config.eyeCare = agoraEduSDKConfig!!.eyeCare

        // init reporter
        init("flexibleClass", "android", config.appId)
        setJoinRoomInfo(config.roomUuid,
                config.userUuid, UUID.randomUUID().toString())
        reporter.reportRoomEntryStart(null)

        // refresh ActivityLifecycleCallbacks
        (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(classRoomListener)
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(classRoomListener)
        agoraEduLaunchCallback = AgoraEduLaunchCallback { state: AgoraEduEvent? ->
            callback.onCallback(state)
            classRoom.updateState(state)
        }

        // init *Manager
        ToastManager.init(context.applicationContext)
        PreferenceManager.init(context.applicationContext)

        // step-1:pull remote config
        roomPre = RoomPreImpl(config.appId, config.roomUuid)
        roomPre?.pullRemoteConfig(object : EduCallback<EduRemoteConfigRes?> {
            override fun onSuccess(res: EduRemoteConfigRes?) {
                val netLessConfig = res!!.netless
                config.whiteBoardAppId = netLessConfig.appId
                config.vendorId = res.vid
                // step-2:check classRoom and init EduManager
                checkAndInit(context, config)
            }

            override fun onFailure(error: EduError) {
                val msg = "pullRemoteConfig failed->code:" + error.type + ",msg:" + error.msg
                callbackError(context, msg)
                reporter.reportRoomEntryEnd("0", error.type.toString() + "", error.httpError.toString() + "", null)
            }
        })
        return classRoom
    }

    private fun checkAndInit(context: Context, config: AgoraEduLaunchConfig) {
        reporter.reportPreCheckStart()
        val req = RoomPreCheckReq(
                config.roomName,
                config.roomType, AgoraEduRoleType.AgoraEduRoleTypeStudent.value.toString(),
                config.startTime,
                config.duration,
                config.userName,
                config.streamState,
                config.userProperties)
        roomPre!!.preCheckClassRoom(config.userUuid, req, object : EduCallback<RoomPreCheckRes?> {
            override fun onSuccess(preCheckRes: RoomPreCheckRes?) {
                assert(preCheckRes != null)
                val intent = createIntent(context, config, preCheckRes!!)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }

            override fun onFailure(error: EduError) {
                var msg = "preCheckClassRoom failed->code:" + error.type + ",msg:" + error.msg
                when (error.type) {
                    RoomPreImpl.ROOMEND -> msg = "Room is End!"
                    RoomPreImpl.ROOMFULL -> msg = context.getString(R.string.room_full)
                    else -> {
                    }
                }
                if (error.type == 30403100) {
                    callbackError(context, AgoraEduEvent.AgoraEduEventForbidden, msg)
                } else {
                    callbackError(context, msg)
                }
                reporter.reportRoomEntryEnd("0", error.type.toString() + "", error.httpError.toString() + "", null)
            }
        })
    }

    private fun createIntent(context: Context, config: AgoraEduLaunchConfig,
                             preCheckRes: RoomPreCheckRes): Intent? {
        val intent = Intent()
        val roomType = config.roomType

        // obtain activity for each room type from registering,
        // and they should be registered in advance
        try {
            var clz: Class<*>? = getRoomActivityReplaced(roomType)
            if (clz == null) {
                clz = getRoomActivityDefault(roomType)
            }
            if (clz != null) {
                intent.setClass(context, clz)
                intent.putExtra("LAUNCHCONFIG", config)
                intent.putExtra("PRECHECKDATA", preCheckRes)
                return intent
            } else {
                Constants.AgoraLog.w("$tag->createIntent: no activity found for room type $roomType")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun callbackError(context: Context, msg: String) {
        callbackError(context, AgoraEduEvent.AgoraEduEventFailed, msg)
    }

    private fun callbackError(context: Context, event: AgoraEduEvent, msg: String) {
        Constants.AgoraLog.e("$tag->$msg")
        agoraEduLaunchCallback.onCallback(event)
        if (context is Activity && event != AgoraEduEvent.AgoraEduEventForbidden) {
            context.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        }
        Constants.AgoraLog.e("$tag->$msg")
    }
}