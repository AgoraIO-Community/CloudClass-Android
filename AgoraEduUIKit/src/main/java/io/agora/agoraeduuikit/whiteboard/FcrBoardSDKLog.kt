package io.agora.agoraeduuikit.whiteboard

import com.google.gson.Gson
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.domain.SDKError
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
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
        Constants.AgoraLog?.e("$tag:throwError->${GsonUtil.toJson(args!!)}")
        roomListener?.onNetlessLog("$tag:throwError->${GsonUtil.toJson(args!!)}", type = FcrBoardLogType.error)
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        Constants.AgoraLog?.i("$tag:urlInterrupter->$sourceUrl")
        return null
    }

    override fun onPPTMediaPlay() {
        Constants.AgoraLog?.i("$tag:onPPTMediaPlay")
    }

    override fun onPPTMediaPause() {
        Constants.AgoraLog?.i("$tag:onPPTMediaPlay")
    }

    override fun onMessage(`object`: JSONObject?) {
        Constants.AgoraLog?.e("$tag:onMessage->${Gson().toJson(`object`)}")
    }

    override fun sdkSetupFail(error: SDKError?) {
        Constants.AgoraLog?.e("$tag:sdkSetupFail->${error?.jsStack}")
    }

    override fun onLogger(info: JSONObject?) {
        super.onLogger(info)
        Constants.AgoraLog?.i("$tag:onLogger->${info?.toString()}")
        roomListener?.onNetlessLog("$tag:onLogger->${info?.toString()}", type = FcrBoardLogType.error)
    }
}