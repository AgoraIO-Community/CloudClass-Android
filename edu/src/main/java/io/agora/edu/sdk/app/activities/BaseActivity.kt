package io.agora.edu.sdk.app.activities

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import io.agora.edu.core.internal.widget.EyeProtection.EyeProtectionView

abstract class BaseActivity : AppCompatActivity() {
    private var eyeProtectionView: EyeProtectionView? = null

    private fun showEyeProtection() {
        if (eyeProtectionView == null) {
            eyeProtectionView = EyeProtectionView(this)
        }

        eyeProtectionView?.parent?.let {
            addContentView(eyeProtectionView, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT))
        }

        eyeProtectionView?.visibility = View.VISIBLE
    }

    private fun dismissEyeProtection() {
        eyeProtectionView?.visibility = View.GONE
    }
}