package io.agora.online.view

import android.app.Dialog
import android.content.Context
import io.agora.online.loading.AgoraLoadingView
import io.agora.online.R

/**
 * author : wufang
 * date : 2022/3/11
 * description :进入分组时的等待界面
 */
class AgoraEduFullLoadingDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private var agoraLoadingView: AgoraLoadingView

    init {
        setContentView(R.layout.fcr_online_edu_loading_dialog)
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

