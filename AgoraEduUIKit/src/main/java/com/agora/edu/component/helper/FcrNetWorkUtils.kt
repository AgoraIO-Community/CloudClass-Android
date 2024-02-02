package com.agora.edu.component.helper

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.agora.agoraeducore.core.context.EduContextNetworkQuality
import io.agora.agoraeduuikit.R

/**
 * author : felix
 * date : 2023/7/20
 * description :
 */
object FcrNetWorkUtils {

    fun getNetWorkStateDesc(quality: EduContextNetworkQuality): Int {
        return when (quality) {
            EduContextNetworkQuality.Good ->
                R.string.fcr_network_label_network_quality_excellent


            EduContextNetworkQuality.Bad ->
                R.string.fcr_network_label_network_quality_bad

            EduContextNetworkQuality.Down ->
                R.string.fcr_network_label_network_quality_down

            else ->
                R.string.fcr_network_label_network_quality_excellent
        }
    }

    fun getNetWorkStateColor(context: Context, quality: EduContextNetworkQuality): Int {
        return when (quality) {
            EduContextNetworkQuality.Good ->
                ContextCompat.getColor(context, R.color.fcr_network_good)

            EduContextNetworkQuality.Bad ->
                ContextCompat.getColor(context, R.color.fcr_network_bad)

            EduContextNetworkQuality.Down ->
                ContextCompat.getColor(context, R.color.fcr_network_down)

            else ->
                ContextCompat.getColor(context, R.color.fcr_network_good)
        }
    }

    fun getNetworkStateIcon(context: Context, state: EduContextNetworkQuality): Drawable {
        val icon = when (state) {
            EduContextNetworkQuality.Good -> R.drawable.agora_tool_icon_signal_good
            EduContextNetworkQuality.Bad -> R.drawable.agora_tool_icon_signal_bad
            EduContextNetworkQuality.Down -> R.drawable.agora_tool_icon_signal_down
//            EduContextNetworkQuality.Down -> R.drawable.agora_tool_icon_signal_unknown
            else -> R.drawable.agora_tool_icon_signal_good
        }
        return ContextCompat.getDrawable(context, icon)!!
    }
}