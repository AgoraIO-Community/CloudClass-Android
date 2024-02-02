package io.agora.education.home

import android.Manifest
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.education.R
import io.agora.education.base.BaseActivity
import io.agora.education.config.AppConstants
import io.agora.education.databinding.ActivityHomeBinding
import io.agora.education.home.adapter.FrcJoinListAdapter
import io.agora.education.home.dialog.FcrCreateRoomDialog
import io.agora.education.home.dialog.FcrJoinRoom
import io.agora.education.home.dialog.FcrJoinRoomDialog
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrJoinListRoomRes
import io.agora.education.setting.FcrSettingActivity
import io.agora.education.setting.FcrSettingTestActivity
import io.agora.education.utils.AppUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * author : felix
 * date : 2022/8/31
 * description : join room
 */
class FcrHomeActivity : BaseActivity() {
    lateinit var binding: ActivityHomeBinding
    lateinit var listAdapter: FrcJoinListAdapter

    var maxDistance = 0
    var nextId: String? = null
    var isLoading = AtomicBoolean(false)
    var fcrJoinRoom: FcrJoinRoom? = null
    // 为了防止进入教室，按照比例显示后，pixels变化
    val heightPixels = Resources.getSystem().displayMetrics.heightPixels
    val widthPixels = Resources.getSystem().displayMetrics.widthPixels

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppUtil.hideStatusBar(window, true)
        initView()
    }

    override fun onResume() {
        super.onResume()
        loadingView.show()
        nextId = null
        requestRoomList()
    }

    fun initView() {
        maxDistance = resources.getDimensionPixelOffset(R.dimen.fcr_join_move_distance)

        fcrJoinRoom = FcrJoinRoom(this)
        fcrJoinRoom?.setJoinRoomListener = { isJoinSuccess, isDismissDailg, message ->
            loadingView.dismiss()
        }

        binding.btnJoinSetting2.setOnClickListener {
            if (binding.layoutJoinHeader.alpha != 0f) {
                binding.btnJoinSetting.performClick()
            }
        }
        binding.btnJoin2.setOnClickListener {
            if (binding.layoutJoinHeader.alpha != 0f) {
                binding.btnJoin.performClick()
            }
        }
        binding.btnCreate2.setOnClickListener {
            if (binding.layoutJoinHeader.alpha != 0f) {
                binding.btnCreate.performClick()
            }
        }

        binding.btnJoinSetting.setOnClickListener {
            startActivity(Intent(this, FcrSettingActivity::class.java))
        }

        binding.btnJoin.setOnClickListener {
            FcrJoinRoomDialog.newInstance(this).show()
        }

        binding.btnCreate.setOnClickListener {
            val roomDialog = FcrCreateRoomDialog.newInstance(this)
            roomDialog.heightPixels = heightPixels
            roomDialog.widthPixels = widthPixels
            roomDialog.onCreateRoomListener = {
                // 创建教室成功
                nextId = null
                requestRoomList()
                setShowCreateSuccessTips()
            }
            roomDialog.show()
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.isAutoMeasureEnabled = true
        layoutManager.isSmoothScrollbarEnabled = true

        listAdapter = FrcJoinListAdapter()
        listAdapter.onEnterRoomListener = {        // 直接进入教室
            PermissionX.init(this@FcrHomeActivity)
                .permissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        loadingView.show()
                        var userName = it.userName
                        if (TextUtils.isEmpty(it.userName)) {
                            userName = PreferenceManager.get(AppConstants.KEY_SP_NICKNAME, "")
                        }
                        fcrJoinRoom?.joinQueryRoom(it.roomId, it.role, userName)
                    } else {
                        Toast.makeText(this@FcrHomeActivity, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.listJoinClass.setHasFixedSize(true)
        binding.listJoinClass.layoutManager = layoutManager
        binding.listJoinClass.adapter = listAdapter

        binding.refreshLayout.setProgressViewEndTarget(true, 250)
        binding.refreshLayout.setColorSchemeResources(R.color.agora_def_color, R.color.agora_def_color)
        binding.refreshLayout.setOnRefreshListener {
            // refresh
            nextId = null
            requestRoomList()
        }

        binding.layoutJoinScroll.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                if (scrollY == 0) {
                    binding.layoutJoinHeader.alpha = 0f
                }

                if (scrollY >= maxDistance) {
                    binding.layoutJoinHeader.alpha = 1f
                } else {
                    val alpha = scrollY * 1.0f / maxDistance
                    binding.layoutJoinHeader.alpha = alpha
                }

                if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight)) {
                    requestRoomList()
                }
            }
        })

        // test mode
        binding.layoutHeader.setOnClickListener(object : View.OnClickListener {
            val COUNTS = 5
            val DURATION = (3 * 1000).toLong()
            var mHits = LongArray(COUNTS)
            override fun onClick(v: View) {
                System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
                mHits[mHits.size - 1] = SystemClock.uptimeMillis()
                if (mHits[0] >= SystemClock.uptimeMillis() - DURATION) {
                    startActivity(Intent(this@FcrHomeActivity, FcrSettingTestActivity::class.java))
                }
            }
        })
    }

    var handler = Handler(Looper.myLooper()!!)
    private fun setShowCreateSuccessTips() {
        binding.tvCreateSuccessTips.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            binding.tvCreateSuccessTips.visibility = View.GONE
        }, 2500)
    }

    fun requestRoomList() {
        if(isLoading.get()){
            return
        }
        isLoading.set(true)
        val map = HashMap<String, String>()
        if (!TextUtils.isEmpty(nextId)) {
            map["nextId"] = nextId!!
        }
        val call = AppRetrofitManager.instance().getService(AppService::class.java).getJoinRoomList(AppUserInfoUtils.getCompanyId(), map)
        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrJoinListRoomRes>>() {
            override fun onSuccess(result: HttpBaseRes<FcrJoinListRoomRes>?) {
                val list = result?.data?.list
                if (list == null || list.isEmpty()) {
                    // 没有下一页了
                    listAdapter.isPreloading = true
                } else {
                    binding.layoutJoinEmpty.visibility = View.GONE
                    binding.listJoinClass.visibility = View.VISIBLE

                    listAdapter.setData(list, !TextUtils.isEmpty(nextId))
                    nextId = result.data?.nextId
                    listAdapter.isPreloading = false
                }
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                if (code != 401) {
                    ToastManager.showShort("$message($code)")
                }
            }

            override fun onComplete() {
                super.onComplete()
                isLoading.set(false)
                loadingView.dismiss()
                binding.refreshLayout.isRefreshing = false
            }
        })
    }
}