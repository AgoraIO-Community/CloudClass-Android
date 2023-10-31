package io.agora.online.sdk.helper

import android.text.TextUtils
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.FcrRecordingState
import java.lang.Exception

/**
 * author : felix
 * date : 2022/8/18
 * description :
 */
class FcrRecordStreamManager {

    fun getRecordState(eduCore: AgoraEduCore?): FcrRecordingState? {
        return eduCore?.eduContextPool()?.roomContext()?.getRecordingState()
    }

    fun getClassState(eduCore: AgoraEduCore?): AgoraEduContextClassState? {
        return eduCore?.eduContextPool()?.roomContext()?.getClassInfo()?.state
    }

    /**
     * 伪直播
     */
    fun getHostingRecordUrl(eduCore: AgoraEduCore?): String? {
        //((eduCore()?.room()?.roomProperties.get("flexProps") as Map<*,*>).get("hostingScene") as Map<*,*>).get("videoURL")
        //((eduCore()?.room()?.roomProperties.get("flexProps") as Map<*,*>).get("hostingScene") as Map<*,*>).get("reserveVideoURL")
        val roomProperties = eduCore?.eduRoom?.roomProperties
        var url: String? = null

        try {
            val hostingScene = (roomProperties?.get("flexProps") as? Map<*,*>)?.get("hostingScene")

            url = (hostingScene as? Map<*,*>)?.get("videoURL") as String?

            if (TextUtils.isEmpty(url)) {
                (hostingScene as? Map<*,*>)?.get("hostingScene") as String?
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return url
    }

    /**
     * 混合CDN
     */
    fun getRecordUrl(eduCore: AgoraEduCore?): String? {
        val roomProperties = eduCore?.eduRoom?.roomProperties
        var url: String? = null

        try {
            val map = ((roomProperties?.get("record") as? Map<*, *>)?.get("streamingUrl") as? Map<*, *>)

            url = map?.get("rtmp") as String?

            if (TextUtils.isEmpty(url)) {
                url = map?.get("flv") as String?
            }

            if (TextUtils.isEmpty(url)) {
                url = map?.get("hls") as String?
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return url
    }
}