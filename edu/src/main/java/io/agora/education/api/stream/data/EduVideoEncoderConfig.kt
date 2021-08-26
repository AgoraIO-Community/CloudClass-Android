package io.agora.education.api.stream.data

import android.os.Parcel
import android.os.Parcelable
import io.agora.rtc.Constants.VIDEO_MIRROR_MODE_AUTO

enum class OrientationMode() : Parcelable {

    ADAPTIVE,
    FIXED_LANDSCAPE,
    FIXED_PORTRAIT;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrientationMode> {
        override fun createFromParcel(parcel: Parcel): OrientationMode {
            return OrientationMode.values()[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<OrientationMode?> {
            return arrayOfNulls(size)
        }
    }
}

enum class DegradationPreference() : Parcelable {
    MAINTAIN_QUALITY,
    MAINTAIN_FRAME_RATE,
    MAINTAIN_BALANCED;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DegradationPreference> {
        override fun createFromParcel(parcel: Parcel): DegradationPreference {
            return DegradationPreference.values()[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<DegradationPreference?> {
            return arrayOfNulls(size)
        }
    }
}

object VideoDimensions {
    val VideoDimensions_640X480 = arrayOf(640, 480)
    val VideoDimensions_320X240 = arrayOf(320, 240)
    val VideoDimensions_360X240 = arrayOf(360, 240)
}

data class EduVideoEncoderConfig(
        var videoDimensionWidth: Int = 360,
        var videoDimensionHeight: Int = 360,
        var frameRate: Int = 15,
        var bitrate: Int = 0,
        var mirrorMode: Int = VIDEO_MIRROR_MODE_AUTO,
        var orientationMode: OrientationMode? = OrientationMode.ADAPTIVE,
        var degradationPreference: DegradationPreference? = DegradationPreference.MAINTAIN_QUALITY
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readParcelable(OrientationMode::class.java.classLoader),
            parcel.readParcelable(DegradationPreference::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(videoDimensionWidth)
        parcel.writeInt(videoDimensionHeight)
        parcel.writeInt(frameRate)
        parcel.writeInt(bitrate)
        parcel.writeParcelable(orientationMode, flags)
        parcel.writeParcelable(degradationPreference, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EduVideoEncoderConfig> {
        override fun createFromParcel(parcel: Parcel): EduVideoEncoderConfig {
            return EduVideoEncoderConfig(parcel)
        }

        override fun newArray(size: Int): Array<EduVideoEncoderConfig?> {
            return arrayOfNulls(size)
        }
    }
}
