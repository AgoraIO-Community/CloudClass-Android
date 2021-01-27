package io.agora.edu.classroom.widget.toast

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
import io.agora.edu.R

object ClassToastManager {
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

    fun countDownShow(time: Int) {
        val content = getContext().getString(R.string.toast_classtime_until_class_end, time)
        val spannableString = SpannableString(content)
        val start = content.indexOf(time.toString())
        spannableString.setSpan(ForegroundColorSpan(getContext().resources.getColor(R.color.toast_classtime_countdown_time)),
        start, start + time.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val view = LayoutInflater.from(getContext()).inflate(R.layout.toast_classtime_countdown_layout, null)
        val clockImg = view.findViewById<AppCompatImageView>(R.id.clock_Img)
        clockImg.z = 10f
        val textView = view.findViewById<AppCompatTextView>(R.id.content)
        textView.text = spannableString
        val toast = Toast(getContext())
        toast.duration = Toast.LENGTH_LONG
        toast.view = view
        toast.setGravity(Gravity.BOTTOM and Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
    }


    private fun show(text: String, duration: Int) {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.toast_classtime_layout, null)
        val textView = view.findViewById<AppCompatTextView>(R.id.content)
        textView.text = text
        val toast = Toast(getContext())
        toast.duration = duration
        toast.view = view
        toast.setGravity(Gravity.BOTTOM and Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
    }

    fun showShort(text: String) {
        show(text, Toast.LENGTH_SHORT)
    }

    fun showLong(text: String) {
        show(text, Toast.LENGTH_LONG)
    }
}