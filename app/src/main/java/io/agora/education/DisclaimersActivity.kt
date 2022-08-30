package io.agora.education

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView

class DisclaimersActivity : AppCompatActivity() {
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