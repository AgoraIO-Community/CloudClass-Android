package io.agora.education.dialog

import android.content.Context
import android.view.View

class ForbiddenDialogBuilder(private val context: Context) {
    private var title: String? = null
    private var message: String? = null
    private var positiveText: String? = null
    private var negativeText: String? = null
    private var iconResource: Int? = null
    private var positiveListener: View.OnClickListener? = null
    private var negativeListener: View.OnClickListener? = null

    fun title(title: String): ForbiddenDialogBuilder {
        this.title = title
        return this
    }

    fun message(message: String): ForbiddenDialogBuilder {
        this.message = message
        return this
    }

    fun positiveText(text: String): ForbiddenDialogBuilder {
        this.positiveText = text
        return this
    }

    fun positiveClick(listener: View.OnClickListener): ForbiddenDialogBuilder {
        this.positiveListener = listener
        return this
    }

    fun negativeText(text: String): ForbiddenDialogBuilder {
        this.negativeText = text
        return this
    }

    fun negativeClick(listener: View.OnClickListener): ForbiddenDialogBuilder {
        this.negativeListener = listener
        return this
    }

    fun image(resource: Int): ForbiddenDialogBuilder {
        this.iconResource = resource
        return this
    }

    fun build(): ForbiddenDialog {
        val dialog = ForbiddenDialog(context)
        title?.let { dialog.setTitle(it) }
        message?.let { dialog.setMessage(it) }
        positiveText?.let { dialog.setPositiveButtonText(it) }
        positiveListener?.let { dialog.setPositiveClick(it) }
        negativeText?.let { dialog.setNegativeButtonText(it) }
        negativeListener?.let { dialog.setNegativeClick(it) }
        return dialog
    }
}