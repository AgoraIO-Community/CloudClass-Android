package com.agora.edu.component.view

import android.app.Dialog
import android.content.Context
import com.agora.edu.component.loading.AgoraLoadingView
import io.agora.agoraeduuikit.R

/**
 * author : wufang
 * date : 2022/3/11
 * description :进入分组时的等待界面
 */
class AgoraEduFullLoadingDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private var agoraLoadingView: AgoraLoadingView

    init {
        setContentView(R.layout.agora_edu_loading_dialog)
        agoraLoadingView = findViewById(R.id.agora_loading_view)
    }

    fun setContent(message: String) {
        agoraLoadingView.setLoadingMessage(message)
    }

    override fun show() {
        super.show()
        //agoraLoadingView.setLoadingMessage(context.getString(R.string.fcr_edu_enter_group))
    }
}

