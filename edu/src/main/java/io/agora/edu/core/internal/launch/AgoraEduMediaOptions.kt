package io.agora.edu.core.internal.launch

import android.os.Parcel
import android.os.Parcelable

data class AgoraEduMediaEncryptionConfigs(
        val encryptionKey: String?,
        val encryptionMode: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt()){

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(encryptionKey)
        parcel.writeInt(encryptionMode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduMediaEncryptionConfigs> {
        override fun createFromParcel(parcel: Parcel): AgoraEduMediaEncryptionConfigs {
            return AgoraEduMediaEncryptionConfigs(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduMediaEncryptionConfigs?> {
            return arrayOfNulls(size)
        }
    }
}

class AgoraEduMediaOptions(val encryptionConfigs: AgoraEduMediaEncryptionConfigs?) : Parcelable {

//    private constructor(encryptionConfigs: AgoraEduMediaEncryptionConfigs?) : this(encryptionConfigs) {
//    }

    constructor(parcel: Parcel) : this(
            parcel.readParcelable<AgoraEduMediaEncryptionConfigs>(AgoraEduMediaEncryptionConfigs.javaClass.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(encryptionConfigs, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduMediaOptions> {
        override fun createFromParcel(parcel: Parcel): AgoraEduMediaOptions {
            return AgoraEduMediaOptions(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduMediaOptions?> {
            return arrayOfNulls(size)
        }
    }
}