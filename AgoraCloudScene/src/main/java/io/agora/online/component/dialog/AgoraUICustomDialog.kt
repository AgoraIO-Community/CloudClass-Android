package io.agora.online.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.online.R

class AgoraUICustomDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private val title: AppCompatTextView
    private val positiveBtn: AppCompatTextView
    private val negativeBtn: AppCompatTextView
    private val lineVertical: View
    private val customLayout: RelativeLayout

    private var positiveClickListener: View.OnClickListener? = null
    private var negativeClickListener: View.OnClickListener? = null

    init {
        setContentView(R.layout.fcr_online_custom_dialog_layout)
        var window = this.window;
        window?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        val layout = findViewById<ConstraintLayout>(R.id.agora_dialog_layout)
        layout.elevation = 10f

        title = findViewById(R.id.agora_dialog_title_text)
        title.visibility = View.GONE
        positiveBtn = findViewById(R.id.agora_dialog_positive_button)
        positiveBtn.visibility = View.GONE
        negativeBtn = findViewById(R.id.agora_dialog_negative_button)
        negativeBtn.visibility = View.GONE
        lineVertical = findViewById(R.id.line2)
        lineVertical.visibility = View.GONE
        positiveBtn.setOnClickListener {
            this.dismiss()
            positiveClickListener?.onClick(it)
        }
        negativeBtn.setOnClickListener {
            this.dismiss()
            negativeClickListener?.onClick(it)
        }
        customLayout = findViewById(R.id.agora_dialog_custom_layout)
    }

    fun setTitle(title: String) {
        this.title.visibility = View.VISIBLE
        this.title.text = title
    }

    fun setPositiveButtonText(text: String) {
        this.positiveBtn.visibility = View.VISIBLE
        this.positiveBtn.text = text
    }

    fun setPositiveClick(listener: View.OnClickListener) {
        positiveClickListener = listener
    }

    fun setNegativeButtonText(text: String) {
        this.lineVertical.visibility = View.VISIBLE
        this.negativeBtn.visibility = View.VISIBLE
        this.negativeBtn.text = text
    }

    fun setNegativeClick(listener: View.OnClickListener) {
        negativeClickListener = listener
    }

    fun setCustomView(view: View) {
        customLayout.removeAllViews()
        customLayout.gravity = Gravity.CENTER
        customLayout.addView(view,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
    }

    fun setCustomView(view: View,gravity: Int) {
        customLayout.removeAllViews()
        customLayout.gravity = gravity
        customLayout.addView(view,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
    }
}