package io.agora.education.base

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager.Companion.addListener
import io.agora.agoraeducore.core.internal.transport.OnAgoraTransportListener
import io.agora.agoraeduuikit.util.MultiLanguageUtil
import io.agora.agoraeduuikit.util.SpUtil
import io.agora.education.request.AppUserInfoUtils.logout
import java.util.*

/**
 * author : felix
 * date : 2022/8/16
 * description :
 */
open class BaseActivity : AppCompatActivity() {
    lateinit var loadingView: AgoraLoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingView = AgoraLoadingDialog(this)
        setRefreshTokenListener()
    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let {
            val language: String = SpUtil.getString(newBase, AppHostUtil.LOCALE_LANGUAGE)
            val country: String = SpUtil.getString(newBase, AppHostUtil.LOCALE_AREA)
            if (!TextUtils.isEmpty(language) && !TextUtils.isEmpty(country)) {
                val locale = Locale(language, country)
                MultiLanguageUtil.changeAppLanguage(newBase, locale, false)
            }
        }
        super.attachBaseContext(newBase)
    }

    private fun setRefreshTokenListener() {
        addListener(AgoraTransportEventId.EVENT_ID_REFRESH_TOKEN_ERROR, object : OnAgoraTransportListener {
            override fun onTransport(event: AgoraTransportEvent) {
                LogX.e("BaseActivity", "clear login token")
                logout()
            }
        })
    }
}