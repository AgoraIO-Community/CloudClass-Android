package io.agora.edu.launch

import android.os.Parcel
import android.os.Parcelable

/**
 * @author cjw
 * @param startTime(Unit is milliseconds)
 * @param duration(Unit is seconds)
 */
class AgoraEduLaunchConfig(val userName: String, val userUuid: String, val roomName: String,
                           val roomUuid: String,
                           val roleType: Int = AgoraEduRoleType.AgoraEduRoleTypeStudent.value,
                           val roomType: Int, val rtmToken: String, val startTime: Long?,
                           val duration: Long?, val region: String, val mediaOptions: AgoraEduMediaOptions?) : Parcelable {
    lateinit var appId: String
    var eyeCare = 0
    lateinit var whiteBoardAppId: String

    private constructor(userName: String, userUuid: String, roomName: String, roomUuid: String,
                        roleType: Int, roomType: Int, rtmToken: String,
                        startTime: Long?, duration: Long?, region: String, appId: String,
                        eyeCare: Int, whiteBoardAppId: String, mediaOptions:AgoraEduMediaOptions?) : this(userName, userUuid, roomName,
            roomUuid, roleType, roomType, rtmToken, startTime, duration, region, mediaOptions) {
        this.appId = appId
        this.eyeCare = eyeCare
        this.whiteBoardAppId = whiteBoardAppId
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
            parcel.readString() ?: AgoraEduRegion.cn,
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readParcelable<AgoraEduMediaOptions>(AgoraEduMediaOptions.javaClass.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userName)
        parcel.writeString(userUuid)
        parcel.writeString(roomName)
        parcel.writeString(roomUuid)
        parcel.writeInt(roleType)
        parcel.writeInt(roomType)
        parcel.writeString(rtmToken)
        startTime?.let { parcel.writeLong(it) }
        duration?.let { parcel.writeLong(it) }
        parcel.writeString(region)
        parcel.writeString(appId)
        parcel.writeInt(eyeCare)
        parcel.writeString(whiteBoardAppId)
        parcel.writeParcelable(mediaOptions, flags)
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