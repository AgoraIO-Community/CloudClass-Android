package io.agora.edu.core.internal.launch

import android.os.Parcel
import android.os.Parcelable
import io.agora.edu.core.internal.edu.common.bean.board.sceneppt.SceneInfo

data class AgoraEduCourseware(
        val resourceName: String?,
        val resourceUuid: String?,
        val scenePath: String?,
        val scenes: List<SceneInfo>?,
        val resourceUrl: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.createTypedArrayList(SceneInfo.CREATOR),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(resourceName)
        parcel.writeString(resourceUuid)
        parcel.writeString(scenePath)
        parcel.writeTypedList(scenes)
        parcel.writeString(resourceUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduCourseware> {
        override fun createFromParcel(parcel: Parcel): AgoraEduCourseware {
            return AgoraEduCourseware(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduCourseware?> {
            return arrayOfNulls(size)
        }
    }

}