package io.agora.edu.core.internal.launch

import android.os.Parcel
import android.os.Parcelable
import io.agora.edu.core.internal.education.api.stream.data.EduLatencyLevel

enum class AgoraEduLatencyLevel(val value: Int) : Parcelable {
    // 极速直播
    AgoraEduLatencyLevelLow(1),

    // 互动直播
    AgoraEduLatencyLevelUltraLow(2);

    fun convert(): EduLatencyLevel {
        return when (this.value) {
            AgoraEduLatencyLevelLow.value -> EduLatencyLevel.EduLatencyLevelLow
            AgoraEduLatencyLevelUltraLow.value -> EduLatencyLevel.EduLatencyLevelUltraLow
            else -> EduLatencyLevel.EduLatencyLevelUltraLow
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<AgoraEduLatencyLevel?> = object : Parcelable.Creator<AgoraEduLatencyLevel?> {
            override fun createFromParcel(`in`: Parcel): AgoraEduLatencyLevel? {
                return AgoraEduLatencyLevel.values()[`in`.readInt()]
            }

            override fun newArray(size: Int): Array<AgoraEduLatencyLevel?> {
                return arrayOfNulls(size)
            }
        }
    }
}