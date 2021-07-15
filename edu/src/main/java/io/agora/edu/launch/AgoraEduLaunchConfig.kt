package io.agora.edu.launch

import android.os.Parcel
import android.os.Parcelable
import io.agora.whiteboard.netless.bean.AgoraBoardFitMode
import io.agora.uicomponent.UiWidgetConfig

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
                           val boardRegion: String,
                           val stream: AgoraEduStream?,
                           val boardFitMode: AgoraBoardFitMode,
                           val userProperties: MutableMap<String, String>? = null,
                           val widgetConfigs: MutableList<UiWidgetConfig>? = null) : Parcelable {

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
                        boardRegion: String,
                        stream: AgoraEduStream?,
                        boardFitMode: AgoraBoardFitMode,
                        appId: String,
                        eyeCare: Int,
                        whiteBoardAppId: String,
                        userProperties: MutableMap<String, String>? = null,
                        widgetConfigs: MutableList<UiWidgetConfig>? = null)
            : this(userName, userUuid, roomName, roomUuid, roleType, roomType,
            rtmToken, startTime, duration, boardRegion, stream, boardFitMode, userProperties, widgetConfigs) {

        this.appId = appId
        this.eyeCare = eyeCare
        this.whiteBoardAppId = whiteBoardAppId
        this.vendorId = vendorId
    }

    var appId: String = ""
    var eyeCare = 0
    var whiteBoardAppId: String = ""
    var vendorId: Int = 0

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
            parcel.readString() ?: "",
            parcel.readParcelable<AgoraEduStream>(AgoraEduStream::class.java.classLoader),
            parcel.readParcelable<AgoraBoardFitMode>(AgoraBoardFitMode::class.java.classLoader)
                    ?: AgoraBoardFitMode.Auto,
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readString() ?: ""
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
        parcel.writeString(boardRegion)
        parcel.writeParcelable(stream, flags)
        parcel.writeParcelable(boardFitMode, flags)
        parcel.writeString(appId)
        parcel.writeInt(eyeCare)
        parcel.writeString(whiteBoardAppId)
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