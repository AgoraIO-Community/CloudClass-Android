package io.agora.education.impl.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import io.agora.education.impl.Constants.Companion.AgoraLog
import java.io.*
import java.lang.reflect.Field
import kotlin.collections.HashMap


class UnCatchExceptionHandler : Thread.UncaughtExceptionHandler {
    private lateinit var context: Context
    private var handler: Thread.UncaughtExceptionHandler? = null

    /*保存手机信息和异常信息*/
    private val mMessage: MutableMap<String, String> = HashMap()

    companion object {
        private val unCatchExceptionHandler: UnCatchExceptionHandler = UnCatchExceptionHandler()

        fun getExceptionHandler(): UnCatchExceptionHandler {
            return unCatchExceptionHandler
        }
    }

    fun init(mContext: Context) {
        this.context = mContext.applicationContext
        handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (!handlerException(e)) {
            if (handler != null) {
                handler!!.uncaughtException(t, e)
            }
        } else {
            try {
                Thread.sleep(1000)
            } catch (e1: InterruptedException) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    /**
     * 是否人为捕获异常
     *
     * @param e Throwable
     * @return true:已处理 false:未处理
     */
    private fun handlerException(e: Throwable?): Boolean {
        if (e == null) {
            return false
        }
        collectErrorMessages()
        writeErrMsg(e)
        return false
    }

    /**
     * 1.收集错误信息
     */
    private fun collectErrorMessages() {
        val pm: PackageManager = context.packageManager
        try {
            val pi: PackageInfo = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_ACTIVITIES
            )
            if (pi != null) {
                val versionName =
                        if (TextUtils.isEmpty(pi.versionName)) "null" else pi.versionName
                val versionCode = "" + pi.versionCode
                mMessage["versionName"] = versionName
                mMessage["versionCode"] = versionCode
            }
            // 通过反射拿到错误信息
            val fields: Array<Field> = Build::class.java.fields
            if (fields != null && fields.isNotEmpty()) {
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
}
