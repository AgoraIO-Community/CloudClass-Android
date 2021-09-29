package io.agora.agoraeducore.core.internal.edu.common.bean.board

import android.os.Parcel
import android.os.Parcelable

object BoardExt {
    const val pdf = "pdf"
    const val pptx = "pptx"
}

class BoardDynamicTaskInfo(
        val ext: String?,
        val resourceName: String?,
        val resourceUuid: String?,
        val taskUuid: String?) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(ext)
        parcel.writeString(resourceName)
        parcel.writeString(resourceUuid)
        parcel.writeString(taskUuid)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BoardDynamicTaskInfo> {
        override fun createFromParcel(parcel: Parcel): BoardDynamicTaskInfo {
            return BoardDynamicTaskInfo(parcel)
        }

        override fun newArray(size: Int): Array<BoardDynamicTaskInfo?> {
            return arrayOfNulls(size)
        }
    }

}