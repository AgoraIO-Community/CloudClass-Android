package io.agora.agoraeduuikit.impl.whiteboard

import android.os.Parcel
import android.os.Parcelable

class AgoraBoardSceneState(
        val scenes: Array<AgoraBoardScene>,
        val scenePath: String? = null,
        val index: Int = 0
)

class AgoraBoardScene(
        val name: String?,
        val componentsCount: Long,
        val ppt: AgoraBoardPpt?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readLong(),
            parcel.readParcelable(AgoraBoardPpt::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeLong(componentsCount)
        parcel.writeParcelable(ppt, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraBoardScene> {
        override fun createFromParcel(parcel: Parcel): AgoraBoardScene {
            return AgoraBoardScene(parcel)
        }

        override fun newArray(size: Int): Array<AgoraBoardScene?> {
            return arrayOfNulls(size)
        }
    }
}

class AgoraBoardPpt(
        val src: String?,
        val width: Double,
        val height: Double) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readDouble(),
            parcel.readDouble()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(src)
        parcel.writeDouble(width)
        parcel.writeDouble(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraBoardPpt> {
        override fun createFromParcel(parcel: Parcel): AgoraBoardPpt {
            return AgoraBoardPpt(parcel)
        }

        override fun newArray(size: Int): Array<AgoraBoardPpt?> {
            return arrayOfNulls(size)
        }
    }
}

