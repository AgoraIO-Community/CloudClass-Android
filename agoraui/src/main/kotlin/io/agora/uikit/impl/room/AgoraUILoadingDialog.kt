package io.agora.uikit.impl.room

import android.app.Dialog
import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import io.agora.uikit.R

class AgoraUILoadingDialog(context: Context) : Dialog(context, R.style.LoadingDialog) {
    private val tag = "AgoraUILoadingDialog"

    init {
        setContentView(R.layout.agoraui_loading_dialog)
        setCanceledOnTouchOutside(false)
        initView()
    }

    private fun initView() {
        val imgView = findViewById<AppCompatImageView>(R.id.loading_Img)
        Glide.with(context).load(R.drawable.agora_board_loading_img).into(imgView)
    }
}