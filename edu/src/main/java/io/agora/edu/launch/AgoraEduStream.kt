package io.agora.edu.launch

import android.os.Parcel
import android.os.Parcelable

class AgoraEduStream(
        var videoState:Int,
        var audioState:Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt()) {
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduStream> {
        override fun createFromParcel(parcel: Parcel): AgoraEduStream {
            return AgoraEduStream(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduStream?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(videoState)
        dest?.writeInt(audioState)
    }
}