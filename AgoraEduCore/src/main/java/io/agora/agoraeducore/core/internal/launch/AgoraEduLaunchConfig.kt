package io.agora.agoraeducore.core.internal.launch

import android.os.Parcel
import android.os.Parcelable
import io.agora.agoraeducore.core.internal.education.api.stream.data.EduVideoEncoderConfig
import io.agora.agoraeducore.core.internal.whiteboard.netless.bean.AgoraBoardFitMode

/**
 * @author cjw
 * @param startTime(Unit is milliseconds)
 * @param duration(Unit is seconds)
 */
class AgoraEduLaunchConfig(val userName: String,
                           val userUuid: String,
                           val roomName: String,
                           val roomUuid: String,
                           val roleType: Int = AgoraEduRoleType.AgoraEduRoleTypeStudent.value,
                           val roomType: Int,
                           val rtmToken: String,
                           val startTime: Long?,
                           val duration: Long?,
                           val region: String,
                           var videoEncoderConfig: EduVideoEncoderConfig? = null,
                           val mediaOptions: AgoraEduMediaOptions?,
                           val boardFitMode: AgoraBoardFitMode,
                           val streamState: StreamState?,
                           val latencyLevel: AgoraEduLatencyLevel? = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
                           val userProperties: MutableMap<String, String>? = null,
                           val widgetConfigs: MutableList<io.agora.agoraeduwidget.UiWidgetConfig>? = null) : Parcelable {

    private constructor(userName: String,
                        userUuid: String,
                        roomName: String,
                        roomUuid: String,
                        roleType: Int,
                        roomType: Int,
                        rtmToken: String,
                        startTime: Long?,
                        duration: Long?,
                        vendorId: Int,
                        region: String,
                        mediaOptions: AgoraEduMediaOptions?,
                        boardFitMode: AgoraBoardFitMode,
                        streamState: StreamState?,
                        latencyLevel: AgoraEduLatencyLevel?,
                        appId: String,
                        whiteBoardAppId: String,
                        videoEncoderConfig: EduVideoEncoderConfig? = null,
                        logDir: String? = null,
                        userProperties: MutableMap<String, String>? = null,
                        widgetConfigs: MutableList<io.agora.agoraeduwidget.UiWidgetConfig>? = null)
            : this(userName, userUuid, roomName, roomUuid, roleType, roomType,
            rtmToken, startTime, duration, region, videoEncoderConfig, mediaOptions, boardFitMode,
            streamState, latencyLevel, userProperties, widgetConfigs) {

        this.appId = appId
        this.whiteBoardAppId = whiteBoardAppId
        this.vendorId = vendorId
        this.videoEncoderConfig = videoEncoderConfig
        this.logDir = logDir
    }

    var appId: String = ""
    var whiteBoardAppId: String = ""
    var vendorId: Int = 0
    var logDir: String? = null
        private set

    internal fun setLogDirPath(path: String) {
        this.logDir = path
    }

    fun isGARegion(): Boolean {
        return region != AgoraEduRegion.cn
    }

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readLong(),
            parcel.readLong(),
            parcel.readInt(),
            parcel.readString() ?: AgoraEduRegion.default,
            parcel.readParcelable<AgoraEduMediaOptions>(AgoraEduMediaOptions.javaClass.classLoader),
            parcel.readParcelable<AgoraBoardFitMode>(AgoraBoardFitMode::class.java.classLoader)
                    ?: AgoraBoardFitMode.Auto,
            parcel.readParcelable(StreamState::class.java.classLoader),
            parcel.readParcelable<AgoraEduLatencyLevel>(AgoraEduLatencyLevel::class.java.classLoader)
                    ?: AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readParcelable(EduVideoEncoderConfig::class.java.classLoader),
            parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userName)
        parcel.writeString(userUuid)
        parcel.writeString(roomName)
        parcel.writeString(roomUuid)
        parcel.writeInt(roleType)
        parcel.writeInt(roomType)
        parcel.writeString(rtmToken)
        parcel.writeLong(startTime ?: -1)
        parcel.writeLong(duration ?: -1)
        parcel.writeInt(vendorId)
        parcel.writeString(region)
        parcel.writeParcelable(mediaOptions, flags)
        parcel.writeParcelable(boardFitMode, flags)
        parcel.writeParcelable(streamState, flags)
        parcel.writeParcelable(latencyLevel, flags)
        parcel.writeString(appId)
        parcel.writeString(whiteBoardAppId)
        parcel.writeParcelable(videoEncoderConfig, flags)
        parcel.writeString(logDir)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduLaunchConfig> {
        override fun createFromParcel(parcel: Parcel): AgoraEduLaunchConfig {
            return AgoraEduLaunchConfig(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduLaunchConfig?> {
            return arrayOfNulls(size)
        }
    }
}