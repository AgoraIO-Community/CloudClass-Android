package io.agora.education.setting

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
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskWidget
import com.agora.edu.component.teachaids.networkdisk.Statics
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.BuildConfig.APAAS_VERSION
import io.agora.agoraeducore.core.context.EduContextVideoEncoderConfig
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.core.utils.SkinUtils
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetConfig
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.config.template.FcrSceneTypeObject
import io.agora.agoraeduuikit.util.PopupAnimationUtil
import io.agora.classroom.helper.FcrStreamParameters
import io.agora.classroom.sdk.AgoraClassSdkConfig
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.education.R
import io.agora.education.base.BaseActivity
import io.agora.education.dialog.ForbiddenDialog
import io.agora.education.dialog.ForbiddenDialogBuilder
import io.agora.education.config.ConfigData
import io.agora.education.config.ConfigUtil
import io.agora.education.data.DefaultPublicCoursewareJson
import io.agora.education.utils.AppUtil
import io.agora.education.utils.HashUtil
import io.agora.online.sdk.AgoraOnlineClassSdkConfig
import io.agora.online.sdk.AgoraOnlineClassroomSDK
import java.util.*
import java.util.regex.Pattern

/**
 * 免登录，进入教室入口
 */
class FcrMainActivity : BaseActivity(), View.OnClickListener {
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
    private lateinit var tvCloudClass: AppCompatTextView
    private lateinit var tvLargeClassVocational: AppCompatTextView
    private lateinit var tvRoleStudent: AppCompatTextView
    private lateinit var tvRoleTeacher: AppCompatTextView
    private lateinit var tvRoleAudience: AppCompatTextView
    private lateinit var icRoleDownUp: AppCompatImageView
    private lateinit var videoStreamModeLayout: RelativeLayout
    private lateinit var videoStreamKeyLayout: RelativeLayout
    private lateinit var edVideoStreamKey: AppCompatEditText
    private lateinit var edVideoStreamMode: AppCompatTextView
    private lateinit var cardEncryptMode: CardView
    private lateinit var btnJoin: AppCompatButton
    private lateinit var tvFlexibleVersion: AppCompatTextView
    private lateinit var logoView: View
    private lateinit var serviceTypeLayout: RelativeLayout
    private lateinit var cardServiceType: CardView
    private lateinit var icServiceDownUp: AppCompatImageView
    private lateinit var tvServiceType: AppCompatTextView
    private lateinit var tvServiceLivePremium: AppCompatTextView
    private lateinit var tvServiceLiveStandard: AppCompatTextView
    private lateinit var tvServiceCdn: AppCompatTextView
    private lateinit var tvServiceFusion: AppCompatTextView
    private lateinit var tvServiceScreenShare: AppCompatTextView
    private lateinit var tvServiceHostingScene: AppCompatTextView
    private val pattern = Pattern.compile("[^a-zA-Z0-9]")
    private val patternUserName = Pattern.compile("[^a-zA-Z0-9\\s\u4e00-\u9fa5]")
    private var roomNameValid = false
    private var userNameNameValid = false
    private var roomTypeValid = false
    private var roleTypeValid = false
    private var serviceTypeValid = false

    //    private lateinit var rtmToken: String
    private var mDialog: ForbiddenDialog? = null
    private var regionStr: String = ""
    private var encryptMode: Int = 0
    private var debugMode: Boolean = false
    private var tapCount: TapCount? = null
    private var popupAnimationUtil = PopupAnimationUtil()
    private lateinit var agoraLoading: AgoraLoadingDialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        regionStr = PreferenceManager.get(io.agora.education.config.AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)

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
                userNameNameValid = patternUserName.matcher(userName).replaceAll("").trim() == userName.trim()
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
        tvCloudClass = findViewById(R.id.tv_cloud_class)
        tvCloudClass.setOnClickListener(this)
        tvLargeClassVocational = findViewById(R.id.tv_large_class_vocational)
        tvLargeClassVocational.setOnClickListener(this)

        tvRoleStudent = findViewById(R.id.tv_role_student)
        tvRoleStudent.setOnClickListener(this)
        tvRoleTeacher = findViewById(R.id.tv_role_teacher)
        tvRoleTeacher.setOnClickListener(this)
        tvRoleAudience = findViewById(R.id.tv_role_audience)
        tvRoleAudience.setOnClickListener(this)
        icRoleDownUp = findViewById(R.id.ic_down9)
        logoView = findViewById(R.id.logo)
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

        serviceTypeLayout = findViewById(R.id.serviceType_Layout)
        serviceTypeLayout.setOnClickListener(this)
        tvServiceType = findViewById(R.id.tv_serviceType)
        icServiceDownUp = findViewById(R.id.ic_serivce_down)

        cardServiceType = findViewById(R.id.card_service_type)
        tvServiceLivePremium = findViewById(R.id.tv_service_live_premium)
        tvServiceLivePremium.setOnClickListener(this)
        tvServiceLiveStandard = findViewById(R.id.tv_service_live_standard)
        tvServiceLiveStandard.setOnClickListener(this)
        tvServiceCdn = findViewById(R.id.tv_service_cdn)
        tvServiceCdn.setOnClickListener(this)
        tvServiceFusion = findViewById(R.id.tv_service_fusion)
        tvServiceFusion.setOnClickListener(this)
        tvServiceScreenShare = findViewById(R.id.tv_service_screen_share)
        tvServiceScreenShare.setOnClickListener(this)
        tvServiceHostingScene = findViewById(R.id.tv_service_hosting_scene)
        tvServiceHostingScene.setOnClickListener(this)

        agoraLoading = AgoraLoadingDialog(this)
        //showPrivacyTerms()
        debugLayoutDetect()
        setQATestPage()

        if (BuildConfig.DEBUG) {
            edRoomName.setText("apaas230704")
            edUserName.setText("student123")
        }
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
                    startActivity(Intent(this@FcrMainActivity, FcrSettingTestActivity::class.java))
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
            tapCount?.step()
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

    var sceneType = FcrSceneTypeObject.FcrInnerSceneType.Small

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.ic_about -> {
//                    if (AppUtil.isTabletDevice(this)) {
//                        if (aboutDialog == null) {
//                            aboutDialog = AboutDialog(this)
//                        }
//                        aboutDialog?.show()
//                    } else {
//                        startActivity(Intent(this, SettingPageActivity::class.java))
//                    }
                    startActivity(Intent(this, FcrSettingActivity::class.java))

                }
                R.id.roomType_Layout -> {
                    if (cardRoomType.visibility == GONE) {
                        cardRoomType.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f)
                        cardRoleType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                        cardServiceType.visibility = GONE
                        icDownUp.isActivated = true
                        llClassNormal.visibility = VISIBLE
                        val sceneTypeArr = FcrSceneTypeObject.getSceneType()
                        sceneTypeArr.forEach { type ->
                            if (type == FcrSceneTypeObject.FcrInnerSceneType.OneToOne) {
                                tvOne2One.visibility = VISIBLE
                            }
                            if (type == FcrSceneTypeObject.FcrInnerSceneType.Small) {
                                tvSmallClass.visibility = VISIBLE
                            }
                            if (type == FcrSceneTypeObject.FcrInnerSceneType.Lecture) {
                                tvLargeClass.visibility = VISIBLE
                            }

                            if (type == FcrSceneTypeObject.FcrInnerSceneType.CloudClass) {
                                tvCloudClass.visibility = VISIBLE
                            }

                            if (type == FcrSceneTypeObject.FcrInnerSceneType.Vocational) {
                                tvLargeClassVocational.visibility = VISIBLE
                            }
                        }
                        llClassArt.visibility = GONE
                        refreshRoomTypeListColor()

                    } else {
                        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
                            cardRoomType.visibility = GONE
                            icDownUp.isActivated = false
                        }
                    }
                }
                R.id.tv_one2one -> {
                    sceneType = FcrSceneTypeObject.FcrInnerSceneType.OneToOne
                    handleK12RoomTypeClick(R.string.one2one_class)
                }
                R.id.tv_small_class -> {
                    sceneType = FcrSceneTypeObject.FcrInnerSceneType.Small
                    handleK12RoomTypeClick(R.string.small_class)
                }
                R.id.tv_cloud_class -> {
                    sceneType = FcrSceneTypeObject.FcrInnerSceneType.CloudClass
                    handleK12RoomTypeClick(R.string.fcr_login_free_class_mode_option_cloud_class)
                }
                R.id.tv_large_class -> {
                    sceneType = FcrSceneTypeObject.FcrInnerSceneType.Lecture
                    handleK12RoomTypeClick(R.string.large_class)
                }
                R.id.tv_small_class_art -> {
                    handleArtRoomTypeClick(R.string.small_class_art)
                }
                R.id.tv_large_class_art -> {
                    handleArtRoomTypeClick(R.string.large_class_art)
                }
                R.id.tv_large_class_vocational -> {
                    sceneType = FcrSceneTypeObject.FcrInnerSceneType.Vocational
                    handleVocationalRoomTypeClick(R.string.large_class_vocational)
                }
                R.id.roleType_Layout -> {
                    if (cardRoleType.visibility == GONE) {
//                        if (tvRoomType.text.equals(this.getString(R.string.small_class_art))) {
                        cardRoleType.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardRoleType, (cardRoleType.width / 2).toFloat(), 0f)
                        cardRoomType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                        cardServiceType.visibility = GONE
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
                R.id.videoStream_mode_Layout -> {
                    if (cardEncryptMode.visibility == GONE) {
                        cardEncryptMode.visibility = VISIBLE
                        cardRoomType.visibility = GONE
                        cardRoleType.visibility = GONE
                        cardServiceType.visibility = GONE
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
                R.id.serviceType_Layout -> {
                    if (cardServiceType.visibility == GONE) {
                        cardServiceType.visibility = VISIBLE
                        popupAnimationUtil.runShowAnimation(cardServiceType, (cardServiceType.width / 2).toFloat(), 0f)
                        icServiceDownUp.isActivated = true
                        cardRoomType.visibility = GONE
                        cardRoleType.visibility = GONE
                        cardEncryptMode.visibility = GONE
                        refreshServiceTypeListColor()
                    } else {
                        popupAnimationUtil.runDismissAnimation(
                            cardServiceType,
                            (cardServiceType.width / 2).toFloat(),
                            0f
                        ) {
                            cardServiceType.visibility = GONE
                            icServiceDownUp.isActivated = false
                        }
                    }
                }
                R.id.tv_service_live_premium -> {
                    handleServiceTypeClick(R.string.service_live_premium)
                }
                R.id.tv_service_live_standard -> {
                    handleServiceTypeClick(R.string.service_live_standard)
                }
                R.id.tv_service_cdn -> {
                    handleServiceTypeClick(R.string.service_cdn)
                }
                R.id.tv_service_fusion -> {
                    handleServiceTypeClick(R.string.service_fusion)
                }
                R.id.tv_service_screen_share -> {
                    handleServiceTypeClick(R.string.service_screen_share)
                }
                R.id.tv_service_hosting_scene -> {
                    handleServiceTypeClick(R.string.service_hosting_scene)
                }
                R.id.btn_join -> {
//                    if (tvRoomType.text.equals(resources.getString(R.string.large_class)) &&
//                        tvRoleType.text.equals(resources.getString(R.string.role_teacher))
//                    ) {
//                        AgoraUIToast.warn(
//                            applicationContext,
//                            tvRoomType,
//                            text = resources.getString(R.string.join_large_for_teacher_fail)
//                        )
//                        return
//                    }
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
        regionStr = PreferenceManager.get(io.agora.education.config.AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
//        if (!TokenUtils.isExistLoginToken()) {
//            loadLoginPage()
//            return
//        }

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
//        val roomUuid = roomName.plus(roomType)
        val roomUuid = HashUtil.md5(roomName).plus(roomType).lowercase()
        val userUuid = HashUtil.md5(userName).plus(roleType).lowercase()
        val roomRegion = getRoomRegion(regionStr)
        val videoStreamKey: String = edVideoStreamKey.text.toString()

        ConfigUtil.getV3Config(
            AppHostUtil.getAppHostUrl(roomRegion),
            roomUuid,
            roleType,
            userUuid,
            object : EduCallback<ConfigData> {
                override fun onSuccess(info: ConfigData?) {
                    // Use authentication info from server instead
                    info?.let {
                        // Room default duration is 30 minutes
                        val duration = 1800L

                        val userProperties = mutableMapOf<String, String>()
                        userProperties["avatar"] =
                            "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image9.png"

                        val config = AgoraEduLaunchConfig(
                            userName,
                            userUuid,
                            roomName,
                            roomUuid,
                            roleType,
                            roomType,
                            it.token,
                            null,
                            duration
                        )
                        // 可选参数：加密方式
                        if (!TextUtils.isEmpty(videoStreamKey) && encryptMode != AgoraEduEncryptMode.NONE.value) {
                            config.mediaOptions =
                                AgoraEduMediaOptions(AgoraEduMediaEncryptionConfigs(videoStreamKey, encryptMode))
                        }
                        config.appId = it.appId
                        // 可选参数：区域
                        config.region = roomRegion
                        // 可选参数：用户参数
                        config.userProperties = userProperties
                        config.videoEncoderConfig = EduContextVideoEncoderConfig(
                            FcrStreamParameters.HeightStream.width,
                            FcrStreamParameters.HeightStream.height,
                            FcrStreamParameters.HeightStream.frameRate,
                            FcrStreamParameters.HeightStream.bitRate
                        )
                        // 互动直播比极速直播更快，延迟少
                        config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow

                        testData(config) // 测试，忽略
                        setConfigPublicCourseware(config)  // 测试数据
                        configVocational(config)        // 测试数据

                        // 测试：暗黑模式
                        val isNightMode = PreferenceManager.get(Constants.KEY_SP_NIGHT, false)
                        config.uiMode = if (isNightMode) AgoraEduUIMode.DARK else AgoraEduUIMode.LIGHT

                        // 暗黑模式，建议这个写在Application中，避免页面重启
                        if (isNightMode) {
                            SkinUtils.setNightMode(true)
                        } else {
                            SkinUtils.setNightMode(false)
                        }


                        if (sceneType == FcrSceneTypeObject.FcrInnerSceneType.CloudClass) {
                            AgoraOnlineClassroomSDK.setConfig(AgoraOnlineClassSdkConfig(it.appId))
                            launchCloud(config)
                        } else {
                            AgoraClassroomSDK.setConfig(AgoraClassSdkConfig(it.appId))
                            launch(config)
                        }
                        Log.e(TAG, "appId = ${config.appId}")
                    }
                }

                override fun onFailure(error: EduError) {
                    agoraLoading.dismiss()
                    ToastManager.showShort(this@FcrMainActivity, "Get Token error: ${error.msg} ( ${error.type})")
                    notifyBtnJoinEnable(true)
                }
            })
    }

    fun launchCloud(config: AgoraEduLaunchConfig) {
        AgoraOnlineClassroomSDK.launch(this, config, AgoraEduLaunchCallback { event ->
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

    fun testData(config: AgoraEduLaunchConfig) {
        val isFastRoom = PreferenceManager.get(Constants.KEY_SP_FAST, false)
        if (isFastRoom) {
            config.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
        }
    }

    /**
     * 职教数据
     */
    private fun configVocational(launchConfig: AgoraEduLaunchConfig) {
        if (tvRoomType.text == getString(R.string.large_class_vocational)) {
            when (tvServiceType.text) {
                getString(R.string.service_live_premium) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
                    //launchConfig.serviceType = AgoraServiceType.LivePremium
                }
                getString(R.string.service_live_standard) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
                    //launchConfig.serviceType = AgoraServiceType.LiveStandard
                }
                getString(R.string.service_cdn) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
                    launchConfig.serviceType = AgoraServiceType.CDN
                }
                getString(R.string.service_fusion) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
                    launchConfig.serviceType = AgoraServiceType.Fusion
                }
                getString(R.string.service_screen_share) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
                    launchConfig.serviceType = AgoraServiceType.MixStreamCDN
                }
                getString(R.string.service_hosting_scene) -> {
                    launchConfig.latencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelLow
                    launchConfig.serviceType = AgoraServiceType.HostingScene
                }
            }
        }
    }


    /**
     * 带入课件
     */
    fun setConfigPublicCourseware(launchConfig: AgoraEduLaunchConfig) {
        val courseware0 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data0)
        val courseware1 = CoursewareUtil.transfer(DefaultPublicCoursewareJson.data1)

        val publicCoursewares = ArrayList<AgoraEduCourseware>(2)
        publicCoursewares.add(courseware0)
        publicCoursewares.add(courseware1)

        val cloudDiskExtra = mutableMapOf<String, Any>()
        cloudDiskExtra[Statics.publicResourceKey] = publicCoursewares
        cloudDiskExtra[Statics.configKey] = Pair(launchConfig.appId, launchConfig.userUuid)

        val widgetConfigs = mutableListOf<AgoraWidgetConfig>()
        widgetConfigs.add(AgoraWidgetConfig(FCRCloudDiskWidget::class.java, AgoraWidgetDefaultId.AgoraCloudDisk.id, extraInfo = cloudDiskExtra))

        // 进入1v1教室，打开课件
        //val coursewares = mutableMapOf<String, Any>()
        //coursewares[Statics.publicResourceKey] = publicCoursewares
        //widgetConfigs.add(AgoraWidgetConfig(widgetClass = AgoraWhiteBoardWidget::class.java, widgetId = AgoraWidgetDefaultId.WhiteBoard.id, extraInfo = coursewares))

        launchConfig.widgetConfigs = widgetConfigs
    }

    private fun notifyBtnJoinEnable(enable: Boolean) {
        runOnUiThread {
            if (tvRoomType.text == getString(R.string.large_class_vocational)) {
                btnJoin.isEnabled = enable && roomNameValid && userNameNameValid && roomTypeValid && roleTypeValid && serviceTypeValid
            } else {
                btnJoin.isEnabled = enable &&  roomTypeValid && roleTypeValid
            }
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
        serviceTypeLayout.visibility = GONE
        forceRoleType(false)
        notifyBtnJoinEnable(true)
    }

    private fun handleArtRoomTypeClick(strId: Int) {
        tvRoomType.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
            cardRoomType.visibility = GONE
            icDownUp.isActivated = false
        }
        forceRoleType(false)
        roomTypeValid = true
        serviceTypeLayout.visibility = GONE
        notifyBtnJoinEnable(true)
    }

    private fun handleVocationalRoomTypeClick(strId: Int) {
        tvRoomType.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardRoomType, (cardRoomType.width / 2).toFloat(), 0f) {
            cardRoomType.visibility = GONE
            icDownUp.isActivated = false
        }
        roomTypeValid = true
        notifyBtnJoinEnable(true)

        forceRoleType(true)
        serviceTypeLayout.visibility = VISIBLE
        icServiceDownUp.isActivated = false
        refreshServiceTypeListColor()
    }

    private fun handleServiceTypeClick(strId: Int) {
        tvServiceType.setText(strId)
        popupAnimationUtil.runDismissAnimation(cardServiceType, (cardServiceType.width / 2).toFloat(), 0f) {
            cardServiceType.visibility = GONE
            icServiceDownUp.isActivated = false
        }
        serviceTypeValid = true
        notifyBtnJoinEnable(true)
    }

    private fun refreshRoomTypeListColor() {
        tvOne2One.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvSmallClass.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvSmallClassArt.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvLargeClassArt.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvLargeClass.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvCloudClass.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvLargeClassVocational.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
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
            getString(R.string.fcr_login_free_class_mode_option_cloud_class) -> {
                tvCloudClass.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class_art) -> {
                tvLargeClassArt.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class) -> {
                tvLargeClass.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class_vocational) -> {
                tvLargeClassVocational.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            else -> {
            }
        }
    }

    private fun refreshServiceTypeListColor() {
        tvServiceLivePremium.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvServiceLiveStandard.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvServiceCdn.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        tvServiceFusion.setTextColor(ContextCompat.getColor(this, R.color.fcr_black))
        when (tvServiceType.text) {
            getString(R.string.service_live_premium) -> {
                tvServiceLivePremium.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.service_live_standard) -> {
                tvServiceLiveStandard.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.service_cdn) -> {
                tvServiceCdn.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
            }
            getString(R.string.large_class_art) -> {
                tvServiceFusion.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
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
            getString(R.string.large_class_vocational) -> {
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

    private fun forceRoleType(isVocational: Boolean) {
        if (isVocational) {
            roleTypeValid = true
            roleTypeLayout.isEnabled = false
            icRoleDownUp.visibility = GONE
            // vocational room force support student
            tvRoleType.setText(R.string.fcr_role_student)
        } else {
            roleTypeLayout.isEnabled = true
            icRoleDownUp.visibility = VISIBLE
        }
        refreshRoleTypeListColor()
    }

    override fun onDestroy() {
        super.onDestroy()
        tapCount?.reset()
        agoraLoading.dismiss()
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