package io.agora.agoraeduuikit.whiteboard

import com.google.gson.Gson
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.domain.SDKError
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import org.json.JSONObject

/**
 * author : hefeng
 * date : 2022/6/7
 * description :
 */
class FcrBoardSDKLog : CommonCallback {
    val tag = "WhiteBoardSDK"
    var roomListener: FcrBoardRoomListener? = null

    override fun throwError(args: Any?) {
        LogX.e(tag,"throwError->${GsonUtil.toJson(args!!)}")
        roomListener?.onNetlessLog(tag,"throwError->${GsonUtil.toJson(args!!)}", type = FcrBoardLogType.error)
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        LogX.i(tag,"urlInterrupter->$sourceUrl")
        return null
    }

    override fun onPPTMediaPlay() {
        LogX.i(tag,"onPPTMediaPlay")
    }

    override fun onPPTMediaPause() {
        LogX.i(tag,"onPPTMediaPlay")
    }

    override fun onMessage(`object`: JSONObject?) {
        LogX.e(tag,"onMessage->${Gson().toJson(`object`)}")
    }

    override fun sdkSetupFail(error: SDKError?) {
        LogX.e(tag,"sdkSetupFail->${error?.jsStack}")
    }

    override fun onLogger(info: JSONObject?) {
        super.onLogger(info)
        LogX.i(tag,"onLogger->${info?.toString()}")
        roomListener?.onNetlessLog(tag,"onLogger->${info?.toString()}", type = FcrBoardLogType.error)
    }
}