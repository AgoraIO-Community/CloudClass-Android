package io.agora.agoraeduuikit.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.agoraeduuikit.R

/**
 * 功能作用：操作提示弹窗
 * 创建人：王亮（Loren）
 * 思路：
 * 方法：
 * 注意：
 * 修改人：
 * 修改时间：
 * 备注：
 *
 * @author 王亮（Loren）
 */
class AgoraUIHintDialog(context: Context) : Dialog(context, R.style.agora_full_screen_dialog) {
    private val title: AppCompatTextView
    private val content: AppCompatTextView
    private val buttonHint: AppCompatTextView
    var buttonClickListener: View.OnClickListener? = null

    init {
        setContentView(R.layout.agora_hint_dialog_layout)
        var window = this.window;
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.decorView?.setBackgroundResource(android.R.color.transparent)
        val layout = findViewById<ConstraintLayout>(R.id.agora_dialog_layout)
        layout.elevation = 10f

        title = findViewById(R.id.agora_dialog_title_text)
        content = findViewById(R.id.agora_dialog_hint_text)
        buttonHint = findViewById(R.id.agora_dialog_positive_button)
        buttonHint.setOnClickListener {
            this.dismiss()
            buttonClickListener?.onClick(it)
        }
    }

    fun setTitle(title: String) {
        this.title.visibility = View.VISIBLE
        this.title.text = title
    }

    fun setContent(text: String) {
        this.content.visibility = View.VISIBLE
        this.content.text = text
    }

    fun setButtonText(text: String) {
        this.buttonHint.visibility = View.VISIBLE
        this.buttonHint.text = text
    }

}