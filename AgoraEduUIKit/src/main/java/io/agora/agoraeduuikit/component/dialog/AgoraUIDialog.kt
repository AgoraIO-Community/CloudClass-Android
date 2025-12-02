package io.agora.agoraeduuikit.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.agoraeduuikit.R

class AgoraUIDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private val title: AppCompatTextView
    private val icon: AppCompatImageView
    private val message: AppCompatTextView
    private val positiveBtn: AppCompatTextView
    private val negativeBtn: AppCompatTextView
    private val closeBtn: View
    private val lineVertical: View

    private var positiveClickListener: View.OnClickListener? = null
    private var negativeClickListener: View.OnClickListener? = null

    init {
        setContentView(R.layout.agora_dialog_layout)
        var window = this.window
        window?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        val layout = findViewById<ConstraintLayout>(R.id.agora_dialog_layout)
        layout.elevation = 10f

        title = findViewById(R.id.agora_dialog_title_text)
        title.visibility = View.GONE
        icon = findViewById(R.id.agora_dialog_icon)
        icon.visibility = View.GONE
        message = findViewById(R.id.agora_dialog_message)
        message.movementMethod = LinkMovementMethod.getInstance()
        message.visibility = View.GONE
        positiveBtn = findViewById(R.id.agora_dialog_positive_button)
        positiveBtn.visibility = View.GONE
        negativeBtn = findViewById(R.id.agora_dialog_negative_button)
        negativeBtn.visibility = View.GONE
        lineVertical = findViewById(R.id.line2)
        lineVertical.visibility = View.GONE
        closeBtn = findViewById(R.id.agora_dialog_close)
        positiveBtn.setOnClickListener {
            this.dismiss()
            positiveClickListener?.onClick(it)
        }
        negativeBtn.setOnClickListener {
            this.dismiss()
            negativeClickListener?.onClick(it)
        }
        closeBtn.setOnClickListener {
            this.dismiss()
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

    fun setShowClose(isShow: Boolean) {
        if (isShow) {
            this.title.setPadding(
                context.resources.getDimensionPixelOffset(R.dimen.fcr_dialog_title_padding2),
                0,
                context.resources.getDimensionPixelOffset(R.dimen.fcr_dialog_title_padding),
                0
            )
        } else {
            this.title.setPadding(
                context.resources.getDimensionPixelOffset(R.dimen.fcr_dialog_title_padding2),
                0,
                context.resources.getDimensionPixelOffset(R.dimen.fcr_dialog_title_padding2),
                0
            )
        }
        this.closeBtn.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setPositiveButtonText(text: String) {
        this.positiveBtn.visibility = View.VISIBLE
        this.positiveBtn.text = text
    }

    fun setPositiveButtonTextColor(color: Int) {
        this.positiveBtn.setTextColor(color)
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
    fun setMessage(messageSpannable: SpannableStringBuilder) {
        this.message.visibility = View.VISIBLE
        this.message.text = messageSpannable
    }
}