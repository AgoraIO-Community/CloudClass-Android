package io.agora.online.component.dialog

import android.content.Context
import android.view.View

class AgoraUIDialogBuilder(private val context: Context) {
    private var title: String? = null
    private var message: String? = null
    private var positiveText: String? = null
    private var negativeText: String? = null
    private var iconResource: Int? = null
    private var positiveListener: View.OnClickListener? = null
    private var negativeListener: View.OnClickListener? = null
    private var mCancelable: Boolean? = null

    fun setCanceledOnTouchOutside(cancel: Boolean): AgoraUIDialogBuilder {
        this.mCancelable = cancel
        return this
    }

    fun title(title: String): AgoraUIDialogBuilder {
        this.title = title
        return this
    }

    fun message(message: String): AgoraUIDialogBuilder {
        this.message = message
        return this
    }

    fun positiveText(text: String): AgoraUIDialogBuilder {
        this.positiveText = text
        return this
    }

    fun positiveClick(listener: View.OnClickListener): AgoraUIDialogBuilder {
        this.positiveListener = listener
        return this
    }

    fun negativeText(text: String): AgoraUIDialogBuilder {
        this.negativeText = text
        return this
    }

    fun negativeClick(listener: View.OnClickListener): AgoraUIDialogBuilder {
        this.negativeListener = listener
        return this
    }

    fun image(resource: Int): AgoraUIDialogBuilder {
        this.iconResource = resource
        return this
    }

    fun build(): AgoraUIDialog {
        val dialog = AgoraUIDialog(context)
        title?.let { dialog.setTitle(it) }
        message?.let { dialog.setMessage(it) }
        positiveText?.let { dialog.setPositiveButtonText(it) }
        positiveListener?.let { dialog.setPositiveClick(it) }
        negativeText?.let { dialog.setNegativeButtonText(it) }
        negativeListener?.let { dialog.setNegativeClick(it) }
        mCancelable?.let {
            dialog.setCanceledOnTouchOutside(it)
            dialog.setCancelable(it)
        }
        return dialog
    }
}

class AgoraUICustomDialogBuilder(private val context: Context) {
    private var title: String? = null
    private var positiveText: String? = null
    private var negativeText: String? = null
    private var positiveListener: View.OnClickListener? = null
    private var negativeListener: View.OnClickListener? = null
    private var customView: View? = null
    private var gravity: Int? = null
    private var mCancelable: Boolean? = null

    fun setCanceledOnTouchOutside(cancel: Boolean): AgoraUICustomDialogBuilder {
        this.mCancelable = cancel
        return this
    }

    fun title(title: String): AgoraUICustomDialogBuilder {
        this.title = title
        return this
    }

    fun positiveText(text: String): AgoraUICustomDialogBuilder {
        this.positiveText = text
        return this
    }

    fun positiveClick(listener: View.OnClickListener): AgoraUICustomDialogBuilder {
        this.positiveListener = listener
        return this
    }

    fun negativeText(text: String): AgoraUICustomDialogBuilder {
        this.negativeText = text
        return this
    }

    fun negativeClick(listener: View.OnClickListener): AgoraUICustomDialogBuilder {
        this.negativeListener = listener
        return this
    }

    fun setCustomView(view: View): AgoraUICustomDialogBuilder {
        this.customView = view
        return this
    }

    fun setCustomView(view: View, gravity: Int): AgoraUICustomDialogBuilder {
        this.customView = view
        this.gravity = gravity
        return this
    }

    fun build(): AgoraUICustomDialog {
        val dialog = AgoraUICustomDialog(context)
        title?.let { dialog.setTitle(it) }
        positiveText?.let { dialog.setPositiveButtonText(it) }
        positiveListener?.let { dialog.setPositiveClick(it) }
        negativeText?.let { dialog.setNegativeButtonText(it) }
        negativeListener?.let { dialog.setNegativeClick(it) }
        if (customView != null && gravity != null) {
            dialog.setCustomView(customView!!, gravity!!)
        } else {
            customView?.let { dialog.setCustomView(it) }
        }
        mCancelable?.let {
            dialog.setCanceledOnTouchOutside(it)
            dialog.setCancelable(it)
        }
        return dialog
    }
}