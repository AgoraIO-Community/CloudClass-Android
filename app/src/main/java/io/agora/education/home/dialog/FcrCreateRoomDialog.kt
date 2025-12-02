package io.agora.education.home.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.text.Editable
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.databinding.DialogCreateRoomBinding
import io.agora.education.home.adapter.FrcLectureTypeListAdapter
import io.agora.education.home.adapter.FrcRoomTypeAdapter
import io.agora.education.join.FcrTextWatcher
import io.agora.education.request.AppService
import io.agora.education.request.AppUserInfoUtils
import io.agora.education.request.bean.FcrCreateRoomProperties
import io.agora.education.request.bean.FcrCreateRoomReq
import io.agora.education.request.bean.FcrCreateRoomRes
import io.agora.education.request.bean.FcrHostingScene
import io.agora.education.utils.AppUtil
import java.text.SimpleDateFormat
import java.util.*


/**
 * author : felix
 * date : 2022/9/6
 * description : create room
 */
class FcrCreateRoomDialog(context: Context) : FcrBaseDialog(context) {
    lateinit var binding: DialogCreateRoomBinding
    lateinit var selectTimeDialog: FcrSelectTimeDialog

    var onCreateRoomListener: (() -> Unit)? = null
    var curClassType = RoomType.ONE_ON_ONE //当前班级类型
    var startTime: Long = 0L
    var serviceType = AgoraServiceType.LivePremium
    var curPlayType = 0 //当前直播类型 0 rtc 1 极速直播 2 cdn

    companion object {
        fun newInstance(context: Context): FcrCreateRoomDialog {
            return FcrCreateRoomDialog(context)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        selectTimeDialog = FcrSelectTimeDialog(context)
        selectTimeDialog.onSelectTimeListener = { mTime ->
            startTime = mTime
            binding.tvCreateSelectTime.text = timeStamp2DateStr(mTime)
            binding.tvEndTime.text = getEndTime(mTime)
            binding.tvEndTime.visibility = View.VISIBLE
        }

        var defName = PreferenceManager.get(AppConstants.KEY_SP_NICKNAME, "")
        if (TextUtils.isEmpty(defName)) {
            defName = AppUserInfoUtils.getUserInfo()?.userName ?: ""
        }

        if (!TextUtils.isEmpty(defName)) {
            val roomName = defName + context.getString(R.string.fcr_create_room_end)
            val length = roomName.length
            binding.fcrInputRoomName.setText(roomName)
            binding.fcrInputRoomName.setSelection(length)
        }

        val centerLayoutManager = FcrCenterLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val roomTypeAdapter = FrcRoomTypeAdapter()
        roomTypeAdapter.setOnItemClickListener(object : FrcRoomTypeAdapter.OnItemClickListener {
            override fun onClick(position: Int) {//当前班级类型 0:小班课 1：大班课 2：一对一
                LogX.d("listCreateRoom", "item clicked position = $position")
                centerLayoutManager.smoothScrollToPosition(binding.listRoomType, RecyclerView.State(), position)
                setClassTypeFromList(position)
                displayLectureType(curClassType)
            }
        })

        binding.listRoomType.setHasFixedSize(true) //房间类型列表 1v1 小班课...
        binding.listRoomType.layoutManager = centerLayoutManager
        binding.listRoomType.adapter = roomTypeAdapter
//        binding.llSettingForLecture.visibility = GONE
//        binding.llMoreSettingRoot.visibility = GONE

        val lectureTypeListAdapter = FrcLectureTypeListAdapter()
        lectureTypeListAdapter.setOnItemClickListener(object : FrcLectureTypeListAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                LogX.d("listLectureType", "item clicked position = $position")
                //获取直播类型
                curPlayType = position
                when (position) {
                    0 -> {//rtc
                        //binding.llMoreSettingRoot.visibility = GONE
                        binding.llSettingForLecture.visibility = GONE
                    }
                    1 -> {//极速直播
                        serviceType = AgoraServiceType.LiveStandard
                        //binding.llMoreSettingRoot.visibility = GONE
                        binding.llSettingForLecture.visibility = GONE
                    }
                    2 -> {//cdn
                        serviceType = AgoraServiceType.Fusion
                        //显示录像直播选项
                        //binding.llMoreSettingRoot.visibility = VISIBLE
                        binding.llSettingForLecture.visibility = VISIBLE
                    }
                }
            }
        })

        binding.listLectureType.setHasFixedSize(true)
        binding.listLectureType.layoutManager = LinearLayoutManager(context)
        binding.listLectureType.adapter = lectureTypeListAdapter

        binding.tvCreateSelectTime.setOnClickListener {
            selectTimeDialog.show()
        }

        binding.btnJoinClose.setOnClickListener {
            dismiss()
        }

        binding.layoutRecordUrl.setOnClickListener {
            val fcrInputDialog = FcrLinkInputDialog(context, binding.tvRecordUrl.text.toString())
            fcrInputDialog.heightPixels = heightPixels
            fcrInputDialog.widthPixels = widthPixels
            fcrInputDialog.onInputListener = {
                binding.tvRecordUrl.text = it
            }
            fcrInputDialog.show()
        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.checkLecture.setOnClickListener {
            if (binding.checkLecture.isChecked) {
                binding.viewLine2.visibility = View.VISIBLE
                binding.layoutRecordUrl.visibility = VISIBLE
                //使用伪直播
                serviceType = AgoraServiceType.HostingScene
            } else {
                binding.viewLine2.visibility = View.GONE
                binding.layoutRecordUrl.visibility = GONE
                serviceType = AgoraServiceType.Fusion
            }
        }

        binding.tvCreateRoom.setOnClickListener {
            createRoom()
        }

        binding.scrollContent.setOnTouchListener { v, event ->
            //canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
            if (binding.scrollContent.canScrollVertically(-1)) {
                binding.scrollContent.requestDisallowInterceptTouchEvent(false)
            } else {
                binding.scrollContent.requestDisallowInterceptTouchEvent(true)
            }
            false
        }

        //点击 更多设置
        binding.tvMoreSetting.setOnClickListener {
            ContextCompat.getMainExecutor(context).execute {
                if (binding.llMoreSetting.visibility == GONE) {
                    //展开更多设置
                    binding.llMoreSetting.visibility = VISIBLE
                    binding.tvMoreSetting.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                    val mLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                    mLayoutParam.gravity = Gravity.LEFT
                    binding.tvMoreSetting.layoutParams = mLayoutParam

                    binding.llMoreSettingRoot.setPadding(
                        AppUtil.dip2px(context, 16f),
                        AppUtil.dip2px(context, 21f),
                        AppUtil.dip2px(context, 16f),
                        0
                    )
                }
            }

        }
        initInputView()
    }

    fun initInputView(){
        binding.fcrInputRoomName.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val roomId = s.toString()
                if (roomId.isNotEmpty()) {
                    binding.fcrRoomNameClear.visibility = View.VISIBLE
                } else {
                    binding.fcrRoomNameClear.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrRoomNameClear.setOnClickListener {
            binding.fcrRoomNameClear.visibility = View.INVISIBLE
            binding.fcrInputRoomName.setText("")
        }

        binding.fcrInputNickName.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val roomId = s.toString()
                if (roomId.isNotEmpty()) {
                    binding.fcrNickClear.visibility = View.VISIBLE
                } else {
                    binding.fcrNickClear.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrNickClear.setOnClickListener {
            binding.fcrNickClear.visibility = View.INVISIBLE
            binding.fcrInputNickName.setText("")
        }
    }

    fun createRoom() {
        val roomName = binding.fcrInputRoomName.text.toString()
        if (roomName.isEmpty()) {
            binding.fcrInputRoomName.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake))
            binding.scrollContent.fullScroll(NestedScrollView.FOCUS_UP)
            ToastManager.showShort(context,R.string.fcr_create_label_roomname_empty)
            return
        }

        val userName = binding.fcrInputNickName.text.toString()
        if (userName.length < 2) { // 2-20
            val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
            binding.fcrLayoutInputNickName.startAnimation(shake)
            ToastManager.showShort(context,R.string.fcr_login_free_tips_content_length)
            return
        }

        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
        }
        if (startTime < System.currentTimeMillis()) {
            ToastManager.showShort(context,R.string.fcr_create_tips_starttime)
            return
        }
        loading.show()

        // 默认30分钟
        val createRoomReq = FcrCreateRoomReq(roomName, curClassType.value, startTime, startTime + 1800 * 1000)
        createRoomReq.userName = userName
        createRoomReq.sceneType = curClassType.value

        val fcrRoomProperties = FcrCreateRoomProperties()
        fcrRoomProperties.watermark = binding.fcrCbWaterMark.isChecked

        if (curClassType == RoomType.LARGE_CLASS) {
            fcrRoomProperties.serviceType = serviceType.value
            fcrRoomProperties.hostingScene = FcrHostingScene(binding.tvRecordUrl.text.toString())
        }

        createRoomReq.roomProperties = fcrRoomProperties
        val call = AppRetrofitManager.instance().getService(AppService::class.java)
            .createRoom(AppUserInfoUtils.getCompanyId(), createRoomReq)

        AppRetrofitManager.Companion.exc(call, object : HttpCallback<HttpBaseRes<FcrCreateRoomRes>>() {
            override fun onSuccess(result: HttpBaseRes<FcrCreateRoomRes>?) {
                //val roomId = result?.data?.roomId
                onCreateRoomListener?.invoke()
                dismiss()
            }

            override fun onComplete() {
                super.onComplete()
                loading.dismiss()
            }
        })
    }

    private fun setClassTypeFromList(position: Int) {
        curClassType = when (position) {
            0 -> {
                RoomType.ONE_ON_ONE
            }
            1 -> {
                RoomType.LARGE_CLASS
            }
            2 -> {
                RoomType.SMALL_CLASS
            }
            else -> {
                RoomType.ONE_ON_ONE
            }
        }
    }

    private fun displayLectureType(curClassType: RoomType) {
        ContextCompat.getMainExecutor(context).execute {
            if (curClassType == RoomType.LARGE_CLASS) {
                //只有大班课显示 listLectureType
                binding.listLectureType.visibility = VISIBLE
                if (curPlayType == 2) {
                    binding.llMoreSetting.visibility = VISIBLE
                    binding.llSettingForLecture.visibility = VISIBLE
                    //binding.llMoreSettingRoot.visibility = VISIBLE

                }
            } else {
                binding.listLectureType.visibility = GONE
                binding.llSettingForLecture.visibility = GONE
                //binding.llMoreSettingRoot.visibility = GONE
            }
        }
    }

    override fun getView(): View {
        binding = DialogCreateRoomBinding.inflate(LayoutInflater.from(context))
        // full screen
        val dm = Resources.getSystem().displayMetrics
        binding.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPixels)
        return binding.root
    }

    fun getEndTime(startTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(startTime + 30 * 60 * 1000)
        val simpleDateFormat = SimpleDateFormat("HH:mm")
        return simpleDateFormat.format(calendar.timeInMillis)
    }

    private fun timeStamp2DateStr(timeStamp: Long): String {
        val date = Date(timeStamp)
        val pattern = "yyyy-MM-dd HH:mm"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val dateStr = simpleDateFormat.format(date)
        return dateStr
    }
}