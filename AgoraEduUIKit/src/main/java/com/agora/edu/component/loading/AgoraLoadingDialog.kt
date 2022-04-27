package com.agora.edu.component.loading

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import io.agora.agoraeduuikit.R

/**
 * author : hefeng
 * date : 2022/3/1
 * description : 加载框
 */
class AgoraLoadingDialog(context: Context) : Dialog(context, R.style.AgoraLoading) {
    lateinit var loadingView: AgoraLoadingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.agora_loading_view_dialog)
        setCanceledOnTouchOutside(false)

        loadingView = findViewById(R.id.agora_loading_view)
    }

    fun setLoadingMessage(message: String) {
        loadingView.setLoadingMessage(message)
    }
}