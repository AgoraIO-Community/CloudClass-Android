package io.agora.education.join

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import io.agora.education.base.BaseActivity
import io.agora.education.databinding.ActivityQuickStartBinding
import io.agora.education.join.presenter.FcrCreateRoomPresenter
import io.agora.education.join.presenter.FcrJoinRoomPresenter
import io.agora.education.login.FcrLoginManager
import io.agora.education.setting.FcrSettingActivity
import io.agora.education.setting.FcrSettingTestActivity
import io.agora.education.utils.AppUtil

/**
 * author : felix
 * date : 2023/8/1
 * description :
 */
class FcrQuickStartActivity : BaseActivity() {
    lateinit var binding: ActivityQuickStartBinding
    lateinit var fcrJoinRoomPresenter: FcrJoinRoomPresenter
    lateinit var fcrCreateRoomPresenter: FcrCreateRoomPresenter
    lateinit var loginManager: FcrLoginManager

    var isJoinRoomPage = true
    var isAgreePrivateTerms = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtil.hideStatusBar(window, false)
        binding = ActivityQuickStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initCreateJoinView()
    }

    fun initCreateJoinView() {
        loginManager = FcrLoginManager(loadingView, this)
        fcrJoinRoomPresenter = FcrJoinRoomPresenter(this, binding.fcrJoinContentView)
        fcrJoinRoomPresenter.onPrivateTermsCheckUpdate = {
            isAgreePrivateTerms = it
            fcrCreateRoomPresenter.updatePrivateTermsView(it)
        }
        fcrJoinRoomPresenter.initView()

        fcrCreateRoomPresenter = FcrCreateRoomPresenter(this, binding.fcrCreateContentView)
        fcrCreateRoomPresenter.onPrivateTermsCheckUpdate = {
            isAgreePrivateTerms = it
            fcrJoinRoomPresenter.updatePrivateTermsView(it)
        }
        fcrCreateRoomPresenter.initView()

        binding.fcrJoinContentView.apply {
            fcrJoinCreateBtn.setOnClickListener {
                setSwitchCreateJoinView(false)
            }
            fcrJoinJoinBtn.setOnClickListener {
                setSwitchCreateJoinView(true)
            }
        }
        binding.fcrCreateContentView.apply {
            fcrCreateCreateBtn.setOnClickListener {
                setSwitchCreateJoinView(false)
            }
            fcrCreateJoinBtn.setOnClickListener {
                setSwitchCreateJoinView(true)
            }
        }

        binding.fcrSetting.setOnClickListener {
            startActivity(Intent(this, FcrSettingActivity::class.java))
        }

        binding.fcrLayoutRoomBottomInfo.fcrBtnLogin2.setOnClickListener {
            binding.fcrBtnLogin1.performClick()
        }

        binding.fcrBtnLogin1.setOnClickListener {
            if (isAgreePrivateTerms) {
                loginManager.startLogin()
            } else {
                if (isJoinRoomPage) {
                    fcrJoinRoomPresenter.shakePrivateTermsView()
                } else {
                    fcrCreateRoomPresenter.shakePrivateTermsView()
                }
            }
        }

        // test mode
        binding.layoutHeader.setOnClickListener(object : View.OnClickListener {
            val COUNTS = 5
            val DURATION = (3 * 1000).toLong()
            var mHits = LongArray(COUNTS)
            override fun onClick(v: View) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
                mHits[mHits.size - 1] = SystemClock.uptimeMillis()
                if (mHits[0] >= SystemClock.uptimeMillis() - DURATION) {
                    startActivity(Intent(this@FcrQuickStartActivity, FcrSettingTestActivity::class.java))
                }
            }
        })
    }

    fun setSwitchCreateJoinView(isJoinRoomPage: Boolean) {
        this.isJoinRoomPage = isJoinRoomPage

        if (isJoinRoomPage) {
            binding.fcrJoinContentView.root.visibility = View.VISIBLE
            binding.fcrCreateContentView.root.visibility = View.GONE
        } else {
            binding.fcrJoinContentView.root.visibility = View.GONE
            binding.fcrCreateContentView.root.visibility = View.VISIBLE
        }

        fcrJoinRoomPresenter.hiddenTips()
        fcrCreateRoomPresenter.hiddenTips()
    }

}