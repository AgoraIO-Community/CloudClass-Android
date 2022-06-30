package io.agora.education

import android.text.TextUtils

/**
 * author : hefeng
 * date : 2022/2/24
 * description :
 */
class AgoraEduApiUtils {
    companion object {
        fun isAppIdEnable(appId: String): Boolean {
            return isAppKeyEnable(appId, "Your AppId")
        }

        fun isAppCertEnable(appCert: String): Boolean {
            return isAppKeyEnable(appCert, "Your AppCertificate")
        }

        fun isAppKeyEnable(value: String, defValue: String): Boolean {
            return !TextUtils.isEmpty(value) && value != defValue
        }
    }
}