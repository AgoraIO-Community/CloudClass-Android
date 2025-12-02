package io.agora.online.impl.whiteboard.netless.manager

import android.content.Context
import com.herewhite.sdk.WhiteSdk
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.WindowRegisterAppParams
import io.agora.agoraeducore.core.internal.log.LogX
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * author : felix
 * date : 2022/5/13
 * description :
 */
object BoardUtils {
    /**
     * 支持H5课件
     */
    fun registerTalkative(context: Context, whiteSdk: WhiteSdk) {
        val jsString = getAppJsFromAsserts(context, "app-talkative.010.js")
        val kind = "Talkative"
        val variable = "NetlessAppTalkative.default"
        val params = WindowRegisterAppParams(
            jsString,
            kind,
            variable, emptyMap()
        )
        whiteSdk.registerApp(params, object : Promise<Boolean?> {
            override fun then(result: Boolean?) {
                LogX.e("registerTalkative success = $result")
            }
            override fun catchEx(t: SDKError) {
                LogX.e("registerTalkative error = ${t.message}")
            }
        })
    }

    fun getAppJsFromAsserts(context: Context, path: String): String? {
        var result: String? = null
        try {
            result = getStringFromAsserts(context, path)
        } catch (ignored: IOException) {
            LogX.e("registerTalkative getStringFromAsserts error")
            ignored.printStackTrace()
        }
        return result
    }

    @Throws(IOException::class)
    private fun getStringFromAsserts(context: Context, path: String): String {
        val style = StringBuilder()
        val inputStream: InputStream = context.assets.open(path)
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { br ->
            var str: String?
            while (br.readLine().also { str = it } != null) {
                style.append(str)
            }
        }
        return style.toString()
    }
}