package io.agora.edu.launch

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class AgoraEduReplayConfig(val beginTime: Long, val endTime: Long, val videoUrl: String,
                           val whiteBoardAppId: String, val whiteBoardId: String,
                           val whiteBoardToken: String, val token: String?) : Parcelable {
}