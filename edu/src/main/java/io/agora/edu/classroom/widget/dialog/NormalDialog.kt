package io.agora.edu.classroom.widget.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import io.agora.edu.R
import io.agora.edu.classroom.widget.dialog.DialogClickListener

class NormalDialog : DialogFragment, View.OnClickListener {
    private lateinit var tv_dialog_cancel: AppCompatTextView
    private lateinit var tv_dialog_confirm: AppCompatTextView
    private lateinit var close: AppCompatImageView
    private var listener: DialogClickListener? = null
    private var content: String
    private var cancelText: String
    private var confirmText: String
    private var contentImgId: Int

    constructor(content: String, cancelText: String, confirmText: String,
                contentImgId: Int) {
        this.content = content
        this.cancelText = cancelText
        this.confirmText = confirmText
        this.contentImgId = contentImgId
    }

    constructor(content: String, cancelText: String, confirmText: String,
                contentImgId: Int, listener: DialogClickListener?) {
        this.content = content
        this.cancelText = cancelText
        this.confirmText = confirmText
        this.contentImgId = contentImgId
        this.listener = listener
    }

    override fun onCancel(dialog: DialogInterface) {
        if (listener != null) listener!!.onClick(false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.dialog_normal_layout, container, false)
        close = root.findViewById(R.id.close)
        close.setOnClickListener(this)
        tv_dialog_cancel = root.findViewById(R.id.cancel)
        tv_dialog_cancel.setOnClickListener(this)
        tv_dialog_cancel.setText(cancelText)
        tv_dialog_confirm = root.findViewById(R.id.confirm)
        tv_dialog_confirm.setOnClickListener(this)
        tv_dialog_confirm.setText(confirmText)
        (root.findViewById<View>(R.id.contentImg) as AppCompatImageView).setImageResource(contentImgId)
        (root.findViewById<View>(R.id.content) as AppCompatTextView).text = content
        return root
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.close) {
        } else if (id == R.id.cancel) {
            if (listener != null) listener!!.onClick(false)
        } else if (id == R.id.confirm) {
            if (listener != null) listener!!.onClick(true)
        }
        dismiss()
    }

    companion object {
        private const val TAG = "ClassNoEndExitDialog"
    }
}