package io.agora.edu.core.internal.launch

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StreamState (
        var videoState:Int,
        var audioState:Int
):Parcelable