package io.agora.uikit.component.toast

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.uikit.R

@SuppressLint("StaticFieldLeak")
internal object AgoraUIToastManager {
    private var sContext: Context? = null
    private var sHandler: Handler? = null

    fun init(context: Context) {
        if (sContext == null) {
            sContext = context.applicationContext
        }
        if (sHandler == null) {
            sHandler = Handler()
        }
    }

    @Throws(IllegalStateException::class)
    private fun getContext(): Context {
        checkNotNull(sContext) { "ToastManager is not initialized. Please call init() before use!" }
        return sContext!!
    }

    fun whiteBoardPermissionTips(enable: Boolean) {
        sHandler?.post {
            val str = if (enable) R.string.agora_toast_board_permission_true else R.string.agora_toast_board_permission_false
            val content = getContext().getString(str)
            val view = LayoutInflater.from(getContext()).inflate(R.layout.agora_toast_board_permission_layout, null)
            val img = view.findViewById<AppCompatImageView>(R.id.img)
            img.isEnabled = enable
            img.z = 10f
            val textView = view.findViewById<AppCompatTextView>(R.id.content)
            textView.text = content
            val toast = Toast(getContext())
            toast.duration = Toast.LENGTH_LONG
            toast.view = view
            toast.setGravity(Gravity.BOTTOM and Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }

    private fun show(text: String, duration: Int) {
        sHandler?.post {
            val view = LayoutInflater.from(getContext()).inflate(R.layout.agora_toast_layout, null)
            val textView = view.findViewById<AppCompatTextView>(R.id.content)
            textView.text = text
            val toast = Toast(getContext())
            toast.duration = duration
            toast.view = view
            toast.setGravity(Gravity.BOTTOM and Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }

    fun showShort(text: String) {
        show(text, Toast.LENGTH_SHORT)
    }

    fun showLong(text: String) {
        show(text, Toast.LENGTH_LONG)
    }
}