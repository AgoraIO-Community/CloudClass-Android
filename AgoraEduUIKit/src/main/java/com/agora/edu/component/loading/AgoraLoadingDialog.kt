package com.agora.edu.component.loading

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import io.agora.agoraeduuikit.R


/**
 * author : felix
 * date : 2022/3/1
 * description : 加载框
 */
class AgoraLoadingDialog(context: Context) : Dialog(context, R.style.FCRAgoraLoading) {
    lateinit var loadingView: AgoraLoadingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.agora_loading_view_dialog)
        setCanceledOnTouchOutside(false)
        setCancelable(false)

        loadingView = findViewById(R.id.agora_loading_view)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    fun setLoadingMessage(message: String) {
        loadingView.setLoadingMessage(message)
    }
}