package io.agora.education.setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import io.agora.education.R
import io.agora.education.utils.AppUtil

class FcrSettingDisclaimersActivity : AppCompatActivity() {
    private val TAG = "DisclaimersActivity"
    private lateinit var rootLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, true)
        setContentView(R.layout.activity_disclaimers)
        rootLayout = findViewById(R.id.root_Layout)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (rootLayout.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
    }
}