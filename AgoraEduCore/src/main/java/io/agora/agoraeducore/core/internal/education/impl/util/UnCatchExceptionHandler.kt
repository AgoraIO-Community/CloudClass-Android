package io.agora.agoraeducore.core.internal.education.impl.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.BusinessException
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.log.UploadManager
import io.agora.agoraeducore.core.internal.log.UploadManager.Params.AndroidException
import io.agora.agoraeducore.core.internal.log.UploadManager.Params.ZIP
import java.io.*
import java.lang.reflect.Field
import kotlin.collections.HashMap


class UnCatchExceptionHandler : Thread.UncaughtExceptionHandler {
    private val tag = "UnCatchExceptionHandler"

    private var context: Context? = null
    private lateinit var logDir: String
    private lateinit var selfPackageName: String
    private var handler: Thread.UncaughtExceptionHandler? = null

    /*保存手机信息和异常信息*/
    private val mMessage: MutableMap<String, String> = HashMap()

    companion object {
        @SuppressLint("StaticFieldLeak")
        private val unCatchExceptionHandler: UnCatchExceptionHandler = UnCatchExceptionHandler()
        const val ANDROID_EXCEPTION = "androidException"
        fun hasException(): Boolean {
            return PreferenceManager.get(ANDROID_EXCEPTION, false)
        }

        fun getExceptionHandler(): UnCatchExceptionHandler {
            return unCatchExceptionHandler
        }
    }

    fun init(mContext: Context, logDir: String, selfPackageName: String) {
        this.context = mContext.applicationContext
        this.logDir = logDir
        this.selfPackageName = selfPackageName
        handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (!handlerException(t, e)) {
            if (handler != null) {
                handler!!.uncaughtException(t, e)
            }
            context = null
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun isInternalException(e: Throwable): Boolean {
        val stackTraces = e.stackTrace
        val first = stackTraces[0].className
        return first.startsWith(selfPackageName)
    }

    /**
     * 是否人为捕获异常
     *
     * @param e Throwable
     * @return true: 已处理 false: 未处理
     */
    private fun handlerException(t: Thread?, e: Throwable?): Boolean {
        e?.let {
            if (!isInternalException(e)) {
                return false
            }
            collectErrorMessages()
            writeErrMsg(e)
            PreferenceManager.put(ANDROID_EXCEPTION, true)
//            uploadLog(t, e)
            return true
        }
        return false
    }

    /**
     * 1.收集错误信息
     */
    private fun collectErrorMessages() {
        val pm: PackageManager = context!!.packageManager
        try {
            val pi: PackageInfo = pm.getPackageInfo(
                    context!!.packageName,
                    PackageManager.GET_ACTIVITIES
            )
            val versionName =
                    if (TextUtils.isEmpty(pi.versionName)) "null" else pi.versionName
            val versionCode = "" + pi.versionCode
            mMessage["versionName"] = versionName
            mMessage["versionCode"] = versionCode
            // 通过反射拿到错误信息
            val fields: Array<Field> = Build::class.java.fields
            if (fields.isNotEmpty()) {
                for (field in fields) {
                    field.isAccessible = true
                    try {
                        mMessage[field.name] = field.get(null).toString()
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * 2.保存错误信息到log文件中
     *
     * @param e Throwable
     */
    private fun writeErrMsg(e: Throwable) {
        val sb = StringBuilder("\n\n")
        for ((key, value) in mMessage) {
            sb.append(key).append("=").append(value).append("\n")
        }
        val writer: Writer = StringWriter()
        val pw = PrintWriter(writer)
        e.printStackTrace(pw)
        var cause = e.cause
        /*循环取出Cause*/
        while (cause != null) {
            cause.printStackTrace(pw)
            cause = e.cause
        }
        pw.close()
        val result: String = writer.toString()
        sb.append(result)
                .append("------Catch Exception------")
        AgoraLog.e(sb.toString())
    }

    fun uploadAndroidException() {
        val uploadParam = UploadManager.UploadParam(BuildConfig.SDK_VERSION, Build.DEVICE,
                Build.VERSION.SDK, ZIP, "Android", AndroidException)
        AgoraLog.i("$tag: Call the uploadLog function to upload logs when handleUnCatchException，parameter->${Gson().toJson(uploadParam)}")
        UploadManager.upload(context!!, Constants.APPID, AgoraEduSDK.logHostUrl(), logDir, uploadParam,
                object : ThrowableCallback<String> {
                    override fun onSuccess(res: String?) {
                        res?.let {
                            AgoraLog.e("$tag: Log uploaded successfully->$res")
                            PreferenceManager.put(ANDROID_EXCEPTION, false)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error.code.let {
                            AgoraLog.e("$tag: Log upload error->code:${error.code}, reason:${
                            error.message ?: throwable?.message
                            }")
                        }
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                })
    }
}
