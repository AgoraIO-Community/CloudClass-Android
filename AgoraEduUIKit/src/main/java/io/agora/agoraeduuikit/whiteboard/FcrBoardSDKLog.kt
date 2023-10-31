package io.agora.agoraeduuikit.whiteboard

import android.util.Log
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.domain.SDKError
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import org.json.JSONObject

/**
 * author : felix
 * date : 2022/6/7
 * description :
 */
class FcrBoardSDKLog : CommonCallback {
    val tag = "WhiteBoardSDK"
    var roomListener: FcrBoardRoomListener? = null
    var isTest = false

    override fun throwError(args: Any?) {
        LogX.e(tag,"throwError->${GsonUtil.toJson(args!!)}")
        roomListener?.onNetlessLog(tag,"throwError->${GsonUtil.toJson(args!!)}", type = FcrBoardLogType.error)
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        if(isTest) {
            Log.i(tag, "urlInterrupter->$sourceUrl")
        }
        return null
    }

    override fun onPPTMediaPlay() {
        if(isTest) {
            Log.i(tag, "onPPTMediaPlay")
        }
    }

    override fun onPPTMediaPause() {
        if(isTest) {
            Log.i(tag, "onPPTMediaPause")
        }
    }

    override fun onMessage(`object`: JSONObject?) {
        if(isTest) {
            Log.e(tag, "onMessage->${GsonUtil.gson.toJson(`object`)}")
        }
    }

    override fun sdkSetupFail(error: SDKError?) {
        LogX.e(tag,"sdkSetupFail->${error?.jsStack}")
    }

    override fun onLogger(info: JSONObject?) {
        super.onLogger(info)
        if (isTest) {
            Log.i(tag, "onLogger->${info?.toString()}")
        }
        roomListener?.onNetlessLog(tag,"onLogger->${info?.toString()}", type = FcrBoardLogType.error)
    }
}