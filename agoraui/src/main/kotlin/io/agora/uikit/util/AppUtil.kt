package io.agora.uikit.util

import android.content.Context
import android.media.MediaPlayer

object AppUtil {
    fun playWav(context: Context, wavId: Int) {
        val mediaPlayer = MediaPlayer.create(context.applicationContext, wavId)
        mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
        mediaPlayer.start()
    }
}