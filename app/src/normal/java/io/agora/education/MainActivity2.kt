package io.agora.education

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.edu.BuildConfig.APAAS_VERSION
import io.agora.edu.core.internal.base.ToastManager
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.education.api.stream.data.EduVideoEncoderConfig
import io.agora.edu.core.internal.launch.*
import io.agora.edu.core.internal.whiteboard.netless.bean.AgoraBoardFitMode.Retain
import io.agora.edu.extensions.extapp.AgoraExtAppConfiguration
import io.agora.edu.extensions.extapp.AgoraExtAppLayoutParam
import io.agora.edu.extensions.extapp.countdown.CountDownExtApp
import io.agora.edu.sdk.AgoraClassSdk
import io.agora.edu.sdk.AgoraClassSdkConfig
import io.agora.education.config.ConfigData
import io.agora.education.config.ConfigUtil
import io.agora.education.rtmtoken.RtmTokenBuilder
import java.util.regex.Pattern


class MainActivity2 : AppCompatActivity(), View.OnClickListener {
    private val tag = "MainActivity2"
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var icAbout: AppCompatImageView
    private lateinit var edRoomName: AppCompatEditText
    private lateinit var tipsRoomName: AppCompatTextView
    private lateinit var edUserName: AppCompatEditText
    private lateinit var tipsUserName: AppCompatTextView
    private lateinit var roomTypeLayout: RelativeLayout
    private lateinit var tvRoomType: AppCompatTextView
    private lateinit var cardRoomType: CardView
    private lateinit var tvOne2One: AppCompatTextView
    private lateinit var tvSmallClass: AppCompatTextView
    private lateinit var tvLargeClass: AppCompatTextView
    private lateinit var roomRegionLayout: RelativeLayout
    private lateinit var tvRoomRegion: AppCompatTextView
    private lateinit var cardRoomRegion: CardView
    private lateinit var tvCN: AppCompatTextView
    private lateinit var tvNA: AppCompatTextView
    private lateinit var tvEU: AppCompatTextView
    private lateinit var tvAP: AppCompatTextView
    private lateinit var edVideoStreamKey: AppCompatEditText
    private lateinit var edVideoStreamMode: AppCompatTextView
    private lateinit var cardEncryptMode: CardView
    private lateinit var btnJoin: AppCompatButton
    private lateinit var tvFlexibleVersion: AppCompatTextView
    private val pattern = Pattern.compile("[^a-zA-Z0-9]")
    private var roomNameValid = false
    private var userNameNameValid = false
    private var roomTypeValid = false

    private lateinit var rtmToken: String
    private var mDialog: ForbiddenDialog? = null
    private var aboutDialog: AboutDialog? = null

    private var regionStr: String = ""
    private var encryptMode: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!isTaskRoot) {
            finish()
            return
        }
        if (AppUtil.isTabletDevice(this)) {
            AppUtil.hideStatusBar(window, true)
            setContentView(R.layout.activity_main2_tablet)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            AppUtil.hideStatusBar(window, false)
            setContentView(R.layout.activity_main2_phone)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rootLayout = findViewById(R.id.root_Layout)
        icAbout = findViewById(R.id.ic_about)
        icAbout.setOnClickListener(this)
        val statusBarHeight = AppUtil.getStatusBarHeight(this)
        (icAbout.layoutParams as ViewGroup.MarginLayoutParams).topMargin += statusBarHeight
        edRoomName = findViewById(R.id.ed_roomName)
        edRoomName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val roomName = edRoomName.text.toString()
                roomNameValid = pattern.matcher(roomName).replaceAll("").trim() == roomName
                tipsRoomName.visibility = if (roomNameValid) GONE else VISIBLE
                notifyBtnJoinEnable(true)
            }
        })
        tipsRoomName = findViewById(R.id.tips_roomName)
        edUserName = findViewById(R.id.ed_userName)
        edUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val userName = edUserName.text.toString()
                userNameNameValid = pattern.matcher(userName).replaceAll("").trim() == userName
                tipsUserName.visibility = if (userNameNameValid) GONE else VISIBLE
                notifyBtnJoinEnable(true)
            }
        })
        tipsUserName = findViewById(R.id.tips_userName)
        roomTypeLayout = findViewById(R.id.roomType_Layout)
        roomTypeLayout.setOnClickListener(this)
        tvRoomType = findViewById(R.id.tv_roomType)
        cardRoomType = findViewById(R.id.card_room_type)
        tvOne2One = findViewById(R.id.tv_one2one)
        tvOne2One.setOnClickListener(this)
        tvSmallClass = findViewById(R.id.tv_small_class)
        tvSmallClass.setOnClickListener(this)
        tvLargeClass = findViewById(R.id.tv_large_class)
        tvLargeClass.setOnClickListener(this)
        roomRegionLayout = findViewById(R.id.roomRegion_Layout)
        roomRegionLayout.setOnClickListener(this)
        tvRoomRegion = findViewById(R.id.tv_roomRegion)
        cardRoomRegion = findViewById(R.id.card_room_region)
        tvCN = findViewById(R.id.tv_cn)
        tvCN.setOnClickListener(this)
        tvNA = findViewById(R.id.tv_na)
        tvNA.setOnClickListener(this)
        tvEU = findViewById(R.id.tv_eu)
        tvEU.setOnClickListener(this)
        tvAP = findViewById(R.id.tv_ap)
        tvAP.setOnClickListener(this)
        btnJoin = findViewById(R.id.btn_join)
        btnJoin.setOnClickListener(this)
        tvFlexibleVersion = findViewById(R.id.tv_flexibleVersion)
        tvFlexibleVersion.setOnClickListener(this)
        tvFlexibleVersion.text = String.format(getString(R.string.flexible_version1), APAAS_VERSION)
        edVideoStreamKey = findViewById(R.id.ed_videoStream_key)
        edVideoStreamMode = findViewById(R.id.ed_videoStream_mode)
        cardEncryptMode = findViewById(R.id.card_encrypt_mode)
        (findViewById<RelativeLayout>(R.id.videoStream_mode_Layout)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.none)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_128_xts)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_128_ecb)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_256_xts)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.sm4_128_ecb)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_128_gcm)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_256_gcm)).setOnClickListener(this)
        showPrivacyTerms()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show()
                return
            }
        }
        when (requestCode) {
            AgoraEduSDK.REQUEST_CODE_RTC -> start()
            else -> {
            }
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.tv_flexibleVersion -> {
                    if (AppUtil.isFastClick(300)) {
                        startActivity(Intent(this, QAActivity::class.java))
                    } else {
                    }
                }
                R.id.ic_about -> {
                    if (AppUtil.isTabletDevice(this)) {
                        if (aboutDialog == null) {
                            aboutDialog = AboutDialog(this)
                        }
                        aboutDialog?.show()
                    } else {
                        startActivity(Intent(this, SettingActivity2::class.java))
                    }
                }
                R.id.roomType_Layout -> {
                    if (cardRoomType.visibility == GONE) {
                        cardRoomType.visibility = VISIBLE
                        cardRoomRegion.visibility = GONE
                        cardEncryptMode.visibility = GONE
                    } else {
                        cardRoomType.visibility = GONE
                    }
                }
                R.id.tv_one2one -> {
                    tvRoomType.setText(R.string.one2one_class)
                    cardRoomType.visibility = GONE
                    roomTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.tv_small_class -> {
                    tvRoomType.setText(R.string.small_class)
                    cardRoomType.visibility = GONE
                    roomTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.tv_large_class -> {
                    tvRoomType.setText(R.string.large_class)
                    cardRoomType.visibility = GONE
                    roomTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.roomRegion_Layout -> {
                    if (cardRoomRegion.visibility == GONE) {
                        cardRoomRegion.visibility = VISIBLE
                        cardRoomType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                    } else {
                        cardRoomRegion.visibility = GONE
                    }
                }
                R.id.tv_cn -> {
                    tvRoomRegion.setText(R.string.cn0)
                    cardRoomRegion.visibility = GONE
                    notifyBtnJoinEnable(true)
                    regionStr = tvRoomRegion.text.toString()
                }
                R.id.tv_na -> {
                    tvRoomRegion.setText(R.string.na0)
                    cardRoomRegion.visibility = GONE
                    notifyBtnJoinEnable(true)
                    regionStr = tvRoomRegion.text.toString()
                }
                R.id.tv_eu -> {
                    tvRoomRegion.setText(R.string.eu0)
                    cardRoomRegion.visibility = GONE
                    notifyBtnJoinEnable(true)
                    regionStr = tvRoomRegion.text.toString()
                }
                R.id.tv_ap -> {
                    tvRoomRegion.setText(R.string.ap0)
                    cardRoomRegion.visibility = GONE
                    notifyBtnJoinEnable(true)
                    regionStr = tvRoomRegion.text.toString()
                }
                R.id.videoStream_mode_Layout -> {
                    if (cardEncryptMode.visibility == GONE) {
                        cardEncryptMode.visibility = VISIBLE
                        cardRoomType.visibility = GONE
                        cardRoomRegion.visibility = GONE
                    } else {
                        cardEncryptMode.visibility = GONE
                    }
                }
                R.id.none -> {
                    encryptMode = AgoraEduEncryptMode.NONE.value
                    edVideoStreamMode.text = getString(R.string.none)
                    cardEncryptMode.visibility = GONE
                }
                R.id.aes_128_xts -> {
                    encryptMode = AgoraEduEncryptMode.AES_128_XTS.value
                    edVideoStreamMode.text = getString(R.string.aes_128_xts)
                    cardEncryptMode.visibility = GONE
                }
                R.id.aes_128_ecb -> {
                    encryptMode = AgoraEduEncryptMode.AES_128_ECB.value
                    edVideoStreamMode.text = getString(R.string.aes_128_ecb)
                    cardEncryptMode.visibility = GONE
                }
                R.id.aes_256_xts -> {
                    encryptMode = AgoraEduEncryptMode.AES_256_XTS.value
                    edVideoStreamMode.text = getString(R.string.aes_256_xts)
                    cardEncryptMode.visibility = GONE
                }
                R.id.sm4_128_ecb -> {
                    encryptMode = AgoraEduEncryptMode.SM4_128_ECB.value
                    edVideoStreamMode.text = getString(R.string.sm4_128_ecb)
                    cardEncryptMode.visibility = GONE
                }
                R.id.aes_128_gcm -> {
                    encryptMode = AgoraEduEncryptMode.AES_128_GCM.value
                    edVideoStreamMode.text = getString(R.string.aes_128_gcm)
                    cardEncryptMode.visibility = GONE
                }
                R.id.aes_256_gcm -> {
                    encryptMode = AgoraEduEncryptMode.AES_256_GCM.value
                    edVideoStreamMode.text = getString(R.string.aes_256_gcm)
                    cardEncryptMode.visibility = GONE
                }
                R.id.btn_join -> {
                    if (AppUtil.isFastClick()) {
                        return
                    }
                    if (AppUtil.checkAndRequestAppPermission(this, arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ), AgoraEduSDK.REQUEST_CODE_RTC)) {
                        start()
                    } else {
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun start() {
        notifyBtnJoinEnable(false)

        val roomName: String = edRoomName.text.toString()
        if (TextUtils.isEmpty(roomName)) {
            Toast.makeText(this, R.string.room_name_should_not_be_empty, Toast.LENGTH_SHORT).show()
            notifyBtnJoinEnable(true)
            return
        }

        val userName: String = edUserName.text.toString()
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, R.string.your_name_should_not_be_empty, Toast.LENGTH_SHORT).show()
            notifyBtnJoinEnable(true)
            return
        }

        val type: String = tvRoomType.text.toString()
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(this, R.string.room_type_should_not_be_empty, Toast.LENGTH_SHORT).show()
            notifyBtnJoinEnable(true)
            return
        }

        val roomType = getRoomType(type)
        val roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.value
        val roomUuid = roomName.plus(roomType)
        val userUuid = userName.plus(roleType)
        var roomRegion = getRoomRegion(regionStr)

        // Deprecated, use project defined app id and token
        try {
            // Agora app id, obtained from agora.io console
            val appId = EduApplication.getAppId()
            // Agora certificate, obtained from agora.io console
            val appCertificate = getString(R.string.agora_app_cert)
            // Open-source rtm token generation
            rtmToken = RtmTokenBuilder().buildToken(appId, appCertificate,
                    userUuid, RtmTokenBuilder.Role.Rtm_User, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var videoStreamKey: String = edVideoStreamKey.text.toString()

        ConfigUtil.config(ConfigUtil.getRegionUrl(roomRegion), userUuid, object : EduCallback<ConfigData> {
            override fun onSuccess(res: ConfigData?) {
                // Use authentication info from server instead
                res?.let {
                    // Room default duration is 30 minutes
                    val duration = 1800L
                    val startTime = System.currentTimeMillis()
//                    var videoEncoderConfig = EduVideoEncoderConfig(320, 240, 15, 200)

                    AgoraClassSdk.registerExtensionApp(mutableListOf(
                            AgoraExtAppConfiguration(
                                    "io.agora.countdown",
                                    AgoraExtAppLayoutParam(
                                            AgoraExtAppLayoutParam.wrap,
                                            AgoraExtAppLayoutParam.wrap),
                                    CountDownExtApp::class.java,
                                    "CN",
                                    null
                            )
                    ))

                    val userProperties = mutableMapOf<String, String>()
                    userProperties["avatar"] = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"
                    val streamState = StreamState(videoState = 1, audioState = 1)
                    val config: AgoraEduLaunchConfig
                    if (videoStreamKey == "" || encryptMode == 0) {
                        config = AgoraEduLaunchConfig(userName, userUuid, roomName, roomUuid,
                                roleType, roomType, it.rtmToken, startTime, duration, roomRegion,
                                null, null, Retain, streamState,
                                AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow, null, null)
                    } else {
                        config = AgoraEduLaunchConfig(userName, userUuid, roomName, roomUuid, roleType,
                                roomType, it.rtmToken, startTime, duration, roomRegion, null,
                                AgoraEduMediaOptions(AgoraEduMediaEncryptionConfigs(videoStreamKey, encryptMode)),
                                Retain, streamState, AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
                                userProperties, null)
                    }

                    config.appId = it.appId

                    // Set global app id record for further use
                    EduApplication.setAppId(it.appId)

                    AgoraClassSdk.setConfig(AgoraClassSdkConfig(it.appId, 0))
                    AgoraClassSdk.launch(this@MainActivity2, config, AgoraEduLaunchCallback { event ->
                        Log.e(tag, ":launch-课堂状态:" + event.name)
                        notifyBtnJoinEnable(true)

                        if (event == AgoraEduEvent.AgoraEduEventForbidden) {
                            runOnUiThread {
                                mDialog = ForbiddenDialogBuilder(this@MainActivity2)
                                        .title(resources.getString(R.string.join_forbidden_title))
                                        .message(resources.getString(R.string.join_forbidden_message))
                                        .positiveText(resources.getString(R.string.join_forbidden_button_confirm))
                                        .positiveClick(View.OnClickListener {
                                            if (mDialog != null && mDialog!!.isShowing) {
                                                mDialog!!.dismiss()
                                                mDialog = null
                                            }
                                        })
                                        .build()
                                mDialog?.show()
                            }
                        }
                    })
                }
            }

            override fun onFailure(error: EduError) {
                ToastManager.init(this@MainActivity2)
                ToastManager.showShort("Request auth data error: region: $roomRegion, user: $userUuid, ${error.msg}")
            }
        })
    }

    private fun notifyBtnJoinEnable(enable: Boolean) {
        runOnUiThread {
            btnJoin.isEnabled = enable && roomNameValid && userNameNameValid && roomTypeValid
        }
    }

    private fun getRoomType(typeName: String): Int {
        return when (typeName) {
            getString(R.string.one2one_class) -> {
                AgoraEduRoomType.AgoraEduRoomType1V1.value
            }
            getString(R.string.small_class) -> {
                AgoraEduRoomType.AgoraEduRoomTypeSmall.value
            }
            else -> {
                AgoraEduRoomType.AgoraEduRoomTypeBig.value
            }
        }
    }

    private fun getRoomRegion(region: String): String {
        return when (region) {
            getString(R.string.cn0) -> {
                AgoraEduRegion.cn
            }
            getString(R.string.na0) -> {
                AgoraEduRegion.na
            }
            getString(R.string.eu0) -> {
                AgoraEduRegion.eu
            }
            getString(R.string.ap0) -> {
                AgoraEduRegion.ap
            }
            else -> {
                return AgoraEduRegion.default
            }
        }
    }

    private fun showPrivacyTerms() {
        val key = "PrivacyTerms"
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!sharedPreferences.getBoolean(key, false)) {
            val dialog = PrivacyTermsDialog(this)
            dialog.setPrivacyTermsDialogListener(object : PrivacyTermsDialog.OnPrivacyTermsDialogListener {
                override fun onPositiveClick() {
                    sharedPreferences.edit().putBoolean(key, true).apply()
                    dialog.dismiss()
                }

                override fun onNegativeClick() {
                    dialog.dismiss()
                    finish()
                }
            })
            dialog.show()
        }
    }
}