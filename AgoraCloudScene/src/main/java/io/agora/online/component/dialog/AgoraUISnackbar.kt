package io.agora.online.component.dialog

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineSnackbarLayoutBinding

class AgoraUISnackbar : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)

    private val binding = FcrOnlineSnackbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.fcrSClose.setOnClickListener {
            this.dismiss()
        }
        this.z = Int.MAX_VALUE / 2f // 设置最顶层
    }

    fun attachView(parent: ViewGroup) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        params.gravity = Gravity.CENTER
        params.bottomMargin = context.resources.getDimensionPixelOffset(R.dimen.fcr_snackbar_dis)
        layout.addView(this, params)
        parent.addView(layout)
        //parent.addView(this)
    }

    fun setMessage(message: String) {
        this.binding.fcrSContent.text = message
    }

    fun show() {
        this.visibility = View.VISIBLE
    }

    fun dismiss() {
        this.visibility = View.GONE
    }
}