package io.agora.agoraeduuikit.component.toast

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.agoraeduuikit.R

@SuppressLint("StaticFieldLeak")
object AgoraUIToast {
    private var sContext: Context? = null
    private var sHandler: Handler? = null

    private const val LEVEL_INFO = 1
    private const val LEVEL_WARN = 2
    private const val LEVEL_ERROR = 3

//    private var COLOR_INFO = Color.parseColor("#FAFFFF")
//    private var COLOR_WARN = Color.parseColor("#FFFBF4")
//    private var COLOR_ERROR = Color.parseColor("#FFF2F2")

    private var COLOR_INFO_BORDER = Color.parseColor("#357BF6")
    private var COLOR_WARN_BORDER = Color.parseColor("#F0C996")
    private var COLOR_ERROR_BORDER = Color.parseColor("#F07766")

//    private val iconResInfo = R.drawable.ic_agora_toast_icon_info
//    private val iconResWarn = R.drawable.ic_agora_toast_icon_warn
//    private val iconResError = R.drawable.ic_agora_toast_icon_error

    private val iconResInfo = R.drawable.fcr_gou_alart
    private val iconResWarn = R.drawable.fcr_tanhao_alart
    private val iconResError = R.drawable.fcr_tanhao_alart

    private const val BASE_HEIGHT = 44
    private const val BASE_CORNER = 8f
    private const val BASE_STROKE_WIDTH = 2

    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

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
            val str = if (enable) R.string.fcr_netless_board_granted else R.string.fcr_netless_board_ungranted
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

    fun info(context: Context, anchor: View? = null, textResId: Int, duration: Int = LENGTH_SHORT) {
        showToast(context.applicationContext, LEVEL_INFO, anchor, context.getString(textResId), duration)
    }

    fun info(context: Context, anchor: View? = null, text: String, duration: Int = LENGTH_SHORT) {
        showToast(context.applicationContext, LEVEL_INFO, anchor, text, duration)
    }

    fun warn(context: Context, anchor: View? = null, text: String, duration: Int = LENGTH_LONG) {
        showToast(context.applicationContext, LEVEL_WARN, anchor, text, duration)
    }

    fun error(context: Context, anchor: View? = null, text: String, duration: Int = LENGTH_LONG) {
        showToast(context.applicationContext, LEVEL_ERROR, anchor, text, duration)
    }

    /**
     * Display the toast below the target anchor view
     * @param anchor tells how to position this toast on the screen,
     * if null, display the toast at the system default position
     */
    @SuppressLint("InflateParams")
    private fun showToast(context: Context, level: Int, anchor: View?, text: String, duration: Int) {
        ContextCompat.getMainExecutor(context).execute {
            computeValues()
            val toastLayout = LayoutInflater.from(context).inflate(
                R.layout.agora_toast_layout, null, false)

            toastLayout.findViewById<AppCompatImageView>(R.id.agora_toast_icon)?.let { icon ->
                getToastIconRes(level)?.let { iconRes ->
                    icon.setImageResource(iconRes)
                }
            }

            toastLayout.findViewById<AppCompatTextView>(R.id.agora_toast_message)?.let { msgView ->
                msgView.text = text
            }

            toastLayout.findViewById<RelativeLayout>(R.id.agora_toast_layout)?.let { layout ->
                val screenDensity = context.resources.displayMetrics.density
                val minWidth = (200 * screenDensity).toInt()
                layout.minimumWidth = minWidth
                buildToastBgDrawable(context, level)?.let { drawable ->
                    layout.background = drawable
                }
            }

            val toast = Toast(context.applicationContext)
            toast.view = toastLayout
            toast.duration = duration
            toast.setGravity(Gravity.FILL, 0, 0)
            toast.show()
        }
    }

    private fun computeValues() {

    }

    private fun buildToastBgDrawable(context: Context,level: Int): Drawable? {
        val bgColor: Int
        val borderColor: Int
        when (level) {
            LEVEL_INFO -> {
                bgColor = ContextCompat.getColor(context,R.color.fcr_system_safe_color)
                borderColor = COLOR_INFO_BORDER
            }
            LEVEL_WARN -> {
                bgColor = ContextCompat.getColor(context,R.color.fcr_system_warning_color)
                borderColor = COLOR_WARN_BORDER
            }
            LEVEL_ERROR -> {
                bgColor = ContextCompat.getColor(context,R.color.fcr_system_error_color)
                borderColor = COLOR_ERROR_BORDER
            }
            else -> {
                return null
            }
        }
        return buildToastBgDrawable(bgColor, borderColor)
    }

    private fun buildToastBgDrawable(bgColor: Int, borderColor: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(bgColor)
        drawable.cornerRadius = BASE_CORNER
        //drawable.setStroke(BASE_STROKE_WIDTH, borderColor)
        drawable.setSize(BASE_HEIGHT, BASE_HEIGHT)
        return drawable
    }

    private fun getToastIconRes(level: Int): Int? {
        return when (level) {
            LEVEL_INFO -> iconResInfo
            LEVEL_WARN -> iconResWarn
            LEVEL_ERROR -> iconResError
            else -> null
        }
    }
}