package io.agora.education

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskWidget
import com.agora.edu.component.teachaids.networkdisk.Statics.configKey
import com.agora.edu.component.teachaids.networkdisk.Statics.publicResourceKey
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.BuildConfig.APAAS_VERSION
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.util.PopupAnimationUtil
import io.agora.classroom.sdk.AgoraClassSdkConfig
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.education.config.ConfigData
import io.agora.education.config.ConfigUtil
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity"
    private val REQUEST_CODE_RTC = 101
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var icAbout: AppCompatImageView
    private lateinit var edRoomName: AppCompatEditText
    private lateinit var blRoomName: View
    private lateinit var tipsRoomName: AppCompatTextView
    private lateinit var edUserName: AppCompatEditText
    private lateinit var blUserName: View
    private lateinit var tipsUserName: AppCompatTextView
    private lateinit var roomTypeLayout: RelativeLayout
    private lateinit var roleTypeLayout: RelativeLayout
    private lateinit var icDownUp: AppCompatImageView
    private lateinit var tvRoomType: AppCompatTextView
    private lateinit var tvRoleType: AppCompatTextView
    private lateinit var cardRoomType: CardView
    private lateinit var cardRoleType: CardView
    private lateinit var tvOne2One: AppCompatTextView
    private lateinit var tvSmallClass: AppCompatTextView
    private lateinit var llClassNormal: LinearLayout
    private lateinit var llClassArt: LinearLayout
    private lateinit var tvSmallClassArt: AppCompatTextView
    private lateinit var tvLargeClassArt: AppCompatTextView
    private lateinit var tvLargeClass: AppCompatTextView
    private lateinit var tvRoleStudent: AppCompatTextView
    private lateinit var tvRoleTeacher: AppCompatTextView
    private lateinit var tvRoleAudience: AppCompatTextView
    private lateinit var roomRegionLayout: RelativeLayout
    private lateinit var tvRoomRegion: AppCompatTextView
    private lateinit var cardRoomRegion: CardView
    private lateinit var tvCN: AppCompatTextView
    private lateinit var tvNA: AppCompatTextView
    private lateinit var tvEU: AppCompatTextView
    private lateinit var tvAP: AppCompatTextView
    private lateinit var videoStreamModeLayout: RelativeLayout
    private lateinit var videoStreamKeyLayout: RelativeLayout
    private lateinit var edVideoStreamKey: AppCompatEditText
    private lateinit var edVideoStreamMode: AppCompatTextView
    private lateinit var cardEncryptMode: CardView
    private lateinit var btnJoin: AppCompatButton
    private lateinit var tvFlexibleVersion: AppCompatTextView
    private lateinit var logoView: View
    private val pattern = Pattern.compile("[^a-zA-Z0-9]")
    private val patternUserName = Pattern.compile("[^a-zA-Z0-9\\s\u4e00-\u9fa5]")
    private var roomNameValid = false
    private var userNameNameValid = false
    private var roomTypeValid = false
    private var roleTypeValid = false

    //    private lateinit var rtmToken: String
    private var mDialog: ForbiddenDialog? = null
    private var aboutDialog: AboutDialog? = null

    private var regionStr: String = ""
    private var encryptMode: Int = 0

    private var debugMode: Boolean = false

    private lateinit var tapCount: TapCount
    private var popupAnimationUtil = PopupAnimationUtil()
    private lateinit var agoraLoading: AgoraLoadingDialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            finish()
            return
        }
        if (AppUtil.isTabletDevice(this)) {
            AppUtil.hideStatusBar(window, true)
            setContentView(R.layout.activity_main_tablet)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            AppUtil.hideStatusBar(window, false)
            setContentView(R.layout.activity_main_phone)
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
                roomNameValid = roomNameValid && roomName.length > 5
                tipsRoomName.visibility = if (roomNameValid) GONE else VISIBLE
                blRoomName.isEnabled = roomNameValid
                notifyBtnJoinEnable(true)
            }
        })
        blRoomName = findViewById(R.id.bl_roomName)
        tipsRoomName = findViewById(R.id.tips_roomName)
        blUserName = findViewById(R.id.bl_userName)
        edUserName = findViewById(R.id.ed_userName)
        edUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val userName = edUserName.text.toString()
                userNameNameValid = patternUserName.matcher(userName).replaceAll("").trim() == userName
                userNameNameValid = userNameNameValid && userName.length > 5
                tipsUserName.visibility = if (userNameNameValid) GONE else VISIBLE
                blUserName.isEnabled = userNameNameValid
                notifyBtnJoinEnable(true)
            }
        })
        tipsUserName = findViewById(R.id.tips_userName)
        roomTypeLayout = findViewById(R.id.roomType_Layout)
        roomTypeLayout.setOnClickListener(this)
        icDownUp = findViewById(R.id.ic_down0)
        roleTypeLayout = findViewById(R.id.roleType_Layout)
        roleTypeLayout.setOnClickListener(this)
        tvRoomType = findViewById(R.id.tv_roomType)
        tvRoleType = findViewById(R.id.tv_roleType)
        cardRoomType = findViewById(R.id.card_room_type)
        cardRoleType = findViewById(R.id.card_role_type)
        tvOne2One = findViewById(R.id.tv_one2one)
        tvOne2One.setOnClickListener(this)
        tvSmallClass = findViewById(R.id.tv_small_class)
        tvSmallClass.setOnClickListener(this)
        llClassNormal = findViewById(R.id.class_type_normal)
        llClassNormal.setOnClickListener(this)
        llClassArt = findViewById(R.id.class_type_art)
        llClassArt.setOnClickListener(this)
        tvSmallClassArt = findViewById(R.id.tv_small_class_art)
        tvSmallClassArt.setOnClickListener(this)
        tvLargeClassArt = findViewById(R.id.tv_large_class_art)
        tvLargeClassArt.setOnClickListener(this)
        tvLargeClass = findViewById(R.id.tv_large_class)
        tvLargeClass.setOnClickListener(this)
        tvRoleStudent = findViewById(R.id.tv_role_student)
        tvRoleStudent.setOnClickListener(this)
        tvRoleTeacher = findViewById(R.id.tv_role_teacher)
        tvRoleTeacher.setOnClickListener(this)
        tvRoleAudience = findViewById(R.id.tv_role_audience)
        tvRoleAudience.setOnClickListener(this)
        roomRegionLayout = findViewById(R.id.roomRegion_Layout)
        roomRegionLayout.setOnClickListener(this)
        tvRoomRegion = findViewById(R.id.tv_roomRegion)
        tvRoomRegion.setText(
            if (Locale.getDefault().language == "zh") {
                R.string.cn0
            } else {
                R.string.na0
            }
        )
        cardRoomRegion = findViewById(R.id.card_room_region)
        logoView = findViewById(R.id.logo)
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
        videoStreamModeLayout = findViewById(R.id.videoStream_mode_Layout)
        videoStreamModeLayout.setOnClickListener(this)
        videoStreamModeLayout.visibility = GONE
        videoStreamKeyLayout = findViewById(R.id.videoStream_key_Layout)
        videoStreamKeyLayout.visibility = GONE
        (findViewById<AppCompatTextView>(R.id.none)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.sm4_128_ecb)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_128_gcm)).setOnClickListener(this)
        (findViewById<AppCompatTextView>(R.id.aes_256_gcm)).setOnClickListener(this)

        agoraLoading = AgoraLoadingDialog(this)
        showPrivacyTerms()
        debugLayoutDetect()
        setQATestPage()
    }

    fun setQATestPage() {
        logoView.setOnClickListener(object : View.OnClickListener {
            val COUNTS = 5 //点击次数
            val DURATION = (3 * 1000).toLong() //规定有效时间
            var mHits = LongArray(COUNTS)
            override fun onClick(v: View) {
                /**
                 * 实现双击方法
                 * src 拷贝的源数组
                 * srcPos 从源数组的那个位置开始拷贝.
                 * dst 目标数组
                 * dstPos 从目标数组的那个位子开始写数据
                 * length 拷贝的元素的个数
                 */
                System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
                //实现左移，然后最后一个位置更新距离开机的时间，如果最后一个时间和最开始时间小于DURATION，即连续5次点击
                mHits[mHits.size - 1] = SystemClock.uptimeMillis()
                if (mHits[0] >= SystemClock.uptimeMillis() - DURATION) {
                    startActivity(Intent(this@MainActivity, AgoraQATestActivity::class.java))
                }
            }
        })
    }

    private fun debugLayoutDetect() {
        tapCount = TapCount(this, 10, 2000) {
            debugMode = !debugMode
            debugLayout(debugMode)
        }
        findViewById<View>(R.id.entry_param_state)?.setOnClickListener {
            tapCount.step()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v?.windowToken)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isShouldHideKeyboard(v: View?, event: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.getHeight()
            val right = left + v.getWidth()
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        return false
    }

    private fun hideKeyboard(token: IBinder?) {
        if (token != null) {
            val im: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun debugLayout(isDebugMode: Boolean) {
        val visibility = if (isDebugMode) VISIBLE else GONE
        videoStreamModeLayout.visibility = visibility
        videoStreamKeyLayout.visibility = visibility
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
            REQUEST_CODE_RTC -> start()
            else -> {
            }
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.ic_about -> {
                    if (AppUtil.isTabletDevice(this)) {
                        if (aboutDialog == null) {
                            aboutDialog = AboutDialog(this)
                        }
                        aboutDialog?.show()
                    } else {
                        startActivity(Intent(this, SettingActivity::class.java))
                    }
                }
                R.id.roomType_Layout -> {
                    if (cardRoomType.visibility == GONE) {
                        cardRoomType.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f)
                        cardRoomRegion.visibility = GONE
                        cardRoleType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                        icDownUp.isActivated = true
                        if (!BuildConfig.isArtScene.toBoolean()) {
                            llClassNormal.visibility = VISIBLE
                            llClassArt.visibility = GONE
                        } else {
                            llClassNormal.visibility = GONE
                            llClassArt.visibility = VISIBLE
                        }
                        refreshRoomTypeListColor()

                    } else {
                        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
                            cardRoomType.visibility = GONE
                            icDownUp.isActivated = false
                        }
                    }
                }
                R.id.tv_one2one -> {
                    handleK12RoomTypeClick(R.string.one2one_class)
                }
                R.id.tv_small_class -> {
                    handleK12RoomTypeClick(R.string.small_class)
                }
                R.id.tv_large_class -> {
                    handleK12RoomTypeClick(R.string.large_class)
                }
                R.id.tv_small_class_art -> {
                    handleArtRoomTypeClick(R.string.small_class_art)
                }
                R.id.tv_large_class_art -> {
                    handleArtRoomTypeClick(R.string.large_class_art)
                }
                R.id.roleType_Layout -> {
                    if (cardRoleType.visibility == GONE) {
//                        if (tvRoomType.text.equals(this.getString(R.string.small_class_art))) {
                        cardRoleType.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardRoleType, (cardRoleType.width / 2).toFloat(), 0f)
                        cardRoomType.visibility = GONE
                        cardRoomRegion.visibility = GONE
                        cardEncryptMode.visibility = GONE
//                        } else {
////                            tvRoleType.setText(R.string.role_student)
////                            roleTypeValid = true
//                        }
                        refreshRoleTypeListColor()
                    } else {
                        popupAnimationUtil.runDismissAnimation(cardRoleType, (cardRoleType.width / 2).toFloat(), 0f) {
                            cardRoleType.visibility = GONE
                        }
//                        cardRoleType.visibility = GONE
                    }
                }
                R.id.tv_role_student -> {
                    tvRoleType.setText(R.string.role_student)
                    cardRoleType.visibility = GONE
                    roleTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.tv_role_teacher -> {
                    tvRoleType.setText(R.string.role_teacher)
                    cardRoleType.visibility = GONE
                    roleTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.tv_role_audience -> {
                    tvRoleType.setText(R.string.role_audience)
                    cardRoleType.visibility = GONE
                    roleTypeValid = true
                    notifyBtnJoinEnable(true)
                }
                R.id.roomRegion_Layout -> {
                    if (cardRoomRegion.visibility == GONE) {
                        cardRoomRegion.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardRoomRegion, (cardRoomRegion.width / 2).toFloat(), 0f)
                        cardRoomType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                        cardRoleType.visibility = GONE
                    } else {
                        popupAnimationUtil.runDismissAnimation(
                            cardRoomRegion,
                            (cardRoomRegion.width / 2).toFloat(),
                            0f
                        ) {
                            cardRoomRegion.visibility = GONE
                        }
                    }
                }
                R.id.tv_cn -> {
                    handleRegionClick(R.string.cn0)
                }
                R.id.tv_na -> {
                    handleRegionClick(R.string.na0)
                }
                R.id.tv_eu -> {
                    handleRegionClick(R.string.eu0)
                }
                R.id.tv_ap -> {
                    handleRegionClick(R.string.ap0)
                }
                R.id.videoStream_mode_Layout -> {
                    if (cardEncryptMode.visibility == GONE) {
                        cardEncryptMode.visibility = VISIBLE
                        cardRoomType.visibility = GONE
                        cardRoleType.visibility = GONE
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
                    if (tvRoomType.text.equals(resources.getString(R.string.large_class)) &&
                        tvRoleType.text.equals(resources.getString(R.string.role_teacher))
                    ) {
                        AgoraUIToast.warn(applicationContext, tvRoomType, text = resources.getString(R.string.join_large_for_teacher_fail))
                        return
                    }
                    if (AppUtil.isFastClick()) {
                        return
                    }
                    if (AppUtil.checkAndRequestAppPermission(
                            this, arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA
                            ), REQUEST_CODE_RTC
                        )
                    ) {
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
        val roleTypeStr: String = tvRoleType.text.toString()
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(this, R.string.room_type_should_not_be_empty, Toast.LENGTH_SHORT).show()
            notifyBtnJoinEnable(true)
            return
        }

        agoraLoading.show()

        val roomType = getRoomType(type)
        val roleType = getRoleType(roleTypeStr)
        val roomUuid = roomName.plus(roomType)
//        val userUuid = userName.plus(roleType)
        val userUuid = HashUtil.md5(userName).plus(roleType).lowercase()
        var roomRegion = getRoomRegion(regionStr)

        val appId = EduApplication.getAppId()

        val videoStreamKey: String = edVideoStreamKey.text.toString()

        ConfigUtil.config(ConfigUtil.getRegionUrl(roomRegion), userUuid, object : EduCallback<ConfigData> {
            override fun onSuccess(info: ConfigData?) {
                // Use authentication info from server instead
                info?.let {
                    // Room default duration is 30 minutes
                    val duration = 1800L
//                    val startTime = System.currentTimeMillis()
//                    var videoEncoderConfig = EduContextVideoEncoderConfig(320, 240, 15, 200)

                    val userProperties = mutableMapOf<String, String>()
                    userProperties["avatar"] =
                        "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"
                    val streamState =
                        AgoraEduStreamState(AgoraEduStreamStatus.Enabled.value, AgoraEduStreamStatus.Enabled.value)

                    val config: AgoraEduLaunchConfig =
                        if (videoStreamKey == "" || encryptMode == AgoraEduEncryptMode.NONE.value) {
                            AgoraEduLaunchConfig(
                                userName, userUuid, roomName, roomUuid,
                                roleType, roomType, it.rtmToken, null, duration, roomRegion,
                                null, null, streamState,
                                AgoraEduLatencyLevel.AgoraEduLatencyLevelLow, userProperties, null
                            )
                        } else {
                            AgoraEduLaunchConfig(
                                userName, userUuid, roomName, roomUuid, roleType,
                                roomType, it.rtmToken, null, duration, roomRegion, null,
                                AgoraEduMediaOptions(AgoraEduMediaEncryptionConfigs(videoStreamKey, encryptMode)),
                                streamState, AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
                                userProperties, null
                            )
                        }

//                    if (AgoraEduApiUtils.isAppIdEnable(appId)) {
//                        // 使用配置文件（string_config.xml）的appid
//                        Log.e(TAG, "use appid for string_config.xml")
//                        config.appId = appId
//                    } else {
//                        Log.e(TAG, "use appid for config api")
//                        // 使用config接口的参数
//                        config.appId = it.appId
//                    }
                    config.appId = it.appId

                    // transmit default public courseware
                    configPublicCourseware(config)

                    Log.e(TAG, "appId = ${config.appId}")
                    EduApplication.setAppId(config.appId)
                    AgoraClassroomSDK.setConfig(AgoraClassSdkConfig(config.appId))
                    launch(config)
                }
            }

            override fun onFailure(error: EduError) {
                agoraLoading.dismiss()
                ToastManager.showShort("Request auth data error: region: $roomRegion, user: $userUuid, ${error.msg}")
                notifyBtnJoinEnable(true)
            }
        })
    }

    fun launch(config: AgoraEduLaunchConfig) {
        AgoraClassroomSDK.launch(this, config, AgoraEduLaunchCallback { event ->
            Log.e(TAG, ":launch-课堂状态:" + event.name)

            runOnUiThread {
                agoraLoading.dismiss()
                notifyBtnJoinEnable(true)
            }

            if (event == AgoraEduEvent.AgoraEduEventForbidden) {
                runOnUiThread {
                    mDialog = ForbiddenDialogBuilder(this)
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

    private fun configPublicCourseware(launchConfig: AgoraEduLaunchConfig) {
        val courseware0 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data0)
        val courseware1 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data1)
        val publicCoursewares = ArrayList<AgoraEduCourseware>(2)
        publicCoursewares.add(courseware0)
        publicCoursewares.add(courseware1)
        val cloudDiskExtra = mutableMapOf<String, Any>()
        cloudDiskExtra[publicResourceKey] = publicCoursewares
        cloudDiskExtra[configKey] = Pair(launchConfig.appId, launchConfig.userUuid)
        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(
            AgoraWidgetConfig(widgetClass = FCRCloudDiskWidget::class.java, widgetId = AgoraWidgetDefaultId.AgoraCloudDisk.id,
                extraInfo = cloudDiskExtra)
        )
        launchConfig.widgetConfigs = widgetConfigs
    }

    private fun notifyBtnJoinEnable(enable: Boolean) {
        runOnUiThread {
            btnJoin.isEnabled = enable && roomNameValid && userNameNameValid && roomTypeValid && roleTypeValid
        }
    }

    private fun handleK12RoomTypeClick(strId: Int) {
        tvRoomType.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
            cardRoomType.visibility = GONE
            icDownUp.isActivated = false
        }
        roomTypeValid = true
//        tvRoleType.setText(R.string.role_student)
//        roleTypeValid = true
        notifyBtnJoinEnable(true)
    }

    private fun handleArtRoomTypeClick(strId: Int) {
        tvRoomType.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
            cardRoomType.visibility = GONE
            icDownUp.isActivated = false
        }
        roomTypeValid = true
        notifyBtnJoinEnable(true)
    }

    private fun handleRegionClick(strId: Int) {
        tvRoomRegion.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardRoomRegion, (cardRoomRegion.width / 2).toFloat(), 0f) {
            cardRoomRegion.visibility = GONE
        }
        notifyBtnJoinEnable(true)
        regionStr = tvRoomRegion.text.toString()
    }

    private fun refreshRoomTypeListColor() {
        tvOne2One.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvSmallClass.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvSmallClassArt.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvLargeClassArt.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvLargeClass.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        when (tvRoomType.text) {
            getString(R.string.one2one_class) -> {
                tvOne2One.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.small_class) -> {
                tvSmallClass.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.small_class_art) -> {
                tvSmallClassArt.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class_art) -> {
                tvLargeClassArt.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class) -> {
                tvLargeClass.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            else -> {
            }
        }
    }

    private fun refreshRoleTypeListColor() {
        tvRoleStudent.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvRoleTeacher.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvRoleAudience.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))

        when (tvRoleType.text) {
            getString(R.string.role_student) -> {
                tvRoleStudent.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.role_teacher) -> {
                tvRoleTeacher.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.role_audience) -> {
                tvRoleAudience.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            else -> {
            }
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
            getString(R.string.small_class_art) -> {
                AgoraEduRoomType.AgoraEduRoomTypeSmall.value
            }
            getString(R.string.large_class_art) -> {
                AgoraEduRoomType.AgoraEduRoomTypeLecture.value
            }
            else -> {
                AgoraEduRoomType.AgoraEduRoomTypeLecture.value
            }
        }
    }

    private fun getRoleType(typeName: String): Int {
        return when (typeName) {
            getString(R.string.role_teacher) -> {
                AgoraEduRoleType.AgoraEduRoleTypeTeacher.value
            }
            getString(R.string.role_audience) -> {
                AgoraEduRoleType.AgoraEduRoleTypeObserver.value
            }
            else -> AgoraEduRoleType.AgoraEduRoleTypeStudent.value
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

    override fun onDestroy() {
        super.onDestroy()
        tapCount.reset()
    }
}

class TapCount(
    context: Context,
    private val count: Int,
    private val interval: Long,
    private val onCountDownEnd: Runnable? = null
) {

    private var step: Int = 0
    private val handler = Handler(context.mainLooper)

    private val resetRunnable = Runnable {
        reset()
    }

    @Synchronized
    fun reset() {
        step = 0
        handler.removeCallbacks(resetRunnable)
    }

    @Synchronized
    fun step() {
        step++
        if (step == count) {
            handler.post {
                onCountDownEnd?.run()
                reset()
            }
        } else {
            handler.removeCallbacks(resetRunnable)
            handler.postDelayed(resetRunnable, interval)
        }
    }
}