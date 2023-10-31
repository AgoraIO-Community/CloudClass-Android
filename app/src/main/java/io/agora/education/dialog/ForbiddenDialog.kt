package io.agora.education.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.education.R

class ForbiddenDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private val title: AppCompatTextView
    private val icon: AppCompatImageView
    private val message: AppCompatTextView
    private val positiveBtn: AppCompatTextView
    private val negativeBtn: AppCompatTextView
    private val lineVertical: View

    private var positiveClickListener: View.OnClickListener? = null
    private var negativeClickListener: View.OnClickListener? = null

    init {
        setContentView(R.layout.dialog_forbidden_layout)

        val layout = findViewById<ConstraintLayout>(R.id.agora_dialog_layout)
        layout.elevation = 10f

        title = findViewById(R.id.agora_dialog_title_text)
        title.visibility = View.GONE
        icon = findViewById(R.id.agora_dialog_icon)
        icon.visibility = View.GONE
        message = findViewById(R.id.agora_dialog_message)
        message.visibility = View.GONE
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
    }

    fun setTitle(title: String) {
        this.title.visibility = View.VISIBLE
        this.title.text = title
    }

    fun setMessage(message: String) {
        this.message.visibility = View.VISIBLE
        this.message.text = message
    }

    fun setIconResource(iconResource: Int) {
        this.icon.visibility = View.VISIBLE
        this.icon.setImageResource(iconResource)
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
}