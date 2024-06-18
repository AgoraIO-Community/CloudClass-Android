package io.agora.classroom.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import io.agora.agoraclasssdk.databinding.ActivityAgoraClass1v1Binding
import io.agora.agoraclasssdk.databinding.ActivityAgoraClass1v1VideoLeftBinding
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.AgoraEduContextStreamInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeduuikit.component.dialog.AgoraUIHintDialogBuilder
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.common.AgoraEduClassActivity

/**
 * author : hefeng
 * date : 2022/1/20
 * description : 1v1 教室
 */
class AgoraClass1V1Activity : AgoraEduClassActivity() {
    private val TAG = "AgoraClass1V1Activity"
    lateinit var binding: ActivityAgoraClass1v1Binding
    lateinit var bindingLeft: ActivityAgoraClass1v1VideoLeftBinding
    private val directionLeft: String = "left"

    //房间标记-教师是否进入过教师的key值
    private val roomTagTeacherWhetherJoinRoomKey = "teacherWhetherJoinRoom"

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            AgoraLog?.d("$TAG->classroom ${roomInfo.roomUuid} joined success")
            setRecordProperties()

            //房间加入成功，开始倒计时或者教师加入通知
            if (AgoraEduContextUserRole.Teacher == eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role) {
                eduCore()?.eduContextPool()?.roomContext()?.updateRoomProperties(mutableMapOf(Pair(roomTagTeacherWhetherJoinRoomKey, true)),
                    mutableMapOf(Pair(roomTagTeacherWhetherJoinRoomKey, "change screen")), null)
            }else{
                startTimer()
            }
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(context = this@AgoraClass1V1Activity, text = error.msg)
            AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")

        }

        override fun onRoomPropertiesUpdated(properties: Map<String, Any>, cause: Map<String, Any>?, operator: AgoraEduContextUserInfo?) {
            super.onRoomPropertiesUpdated(properties, cause, operator)
            //判断老师是否进了教室
            var whetherJoin:Boolean? = null
            if(properties.containsKey(roomTagTeacherWhetherJoinRoomKey)){
                whetherJoin = properties[roomTagTeacherWhetherJoinRoomKey]?.toString()?.toBoolean()
            }
            if(whetherJoin == null){
                if(true == eduCore()?.eduContextPool()?.roomContext()?.getRoomProperties()?.containsKey(roomTagTeacherWhetherJoinRoomKey)){
                    whetherJoin = eduCore()?.eduContextPool()?.roomContext()?.getRoomProperties()?.get(roomTagTeacherWhetherJoinRoomKey)?.toString()?.toBoolean()
                }
            }
            //接收到房间属性更新，从中取出数据更新视图
            if (true == whetherJoin) {
                releaseTimer()
            }

        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            AgoraLog?.d("$TAG->class state updated: ${state.name}")
            startTimer()
        }
    }

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                if (localUser.userUuid == streamInfo.owner.userUuid) {
                    openSystemDevices()
                }
            }
        }
    }

    /**
     * 倒计时实例
     */
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (launchConfig?.videoDirection.equals(directionLeft)) {
            bindingLeft = ActivityAgoraClass1v1VideoLeftBinding.inflate(layoutInflater)
            setContentView(bindingLeft.root)
        } else {
            binding = ActivityAgoraClass1v1Binding.inflate(layoutInflater)
            setContentView(binding.root)
        }

        // 创建了教室对象
        createEduCore(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                // 教室资源加载完成后
                joinClassRoom()
            }

            override fun onFailure(error: EduContextError?) {
                error?.let {
                    ToastManager.showShort(it.msg)
                }
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        this.releaseTimer()
    }

    private fun joinClassRoom() {
        runOnUiThread {
            eduCore()?.eduContextPool()?.let { context ->
                context.roomContext()?.addHandler(roomHandler)
                context.streamContext()?.addHandler(streamHandler)

                if (launchConfig?.videoDirection.equals(directionLeft)) {//video 在左边
                    bindingLeft.teachAidContainer.initView(this)
                    bindingLeft.agoraClassHead.initView(this)
                    bindingLeft.agoraEduWhiteboard.initView(uuid, this)
                    launchConfig?.videoDirection?.let { bindingLeft.agoraEduWhiteboard.setDirection(it) }

                    bindingLeft.videoGroup.initView(this)
                    bindingLeft.videoGroup.let {
                        getUIDataProvider()?.addListener(it.uiDataProviderListener)
                    }
                    bindingLeft.agoraEduOptions.itemContainer = bindingLeft.agoraEduOptionsItemContainer
                    bindingLeft.agoraEduOptions.rootContainer = bindingLeft.root
                    bindingLeft.agoraEduOptions.initView(uuid, bindingLeft.root, bindingLeft.agoraEduOptionsItemContainer, this)
                    launchConfig?.roleType?.let {
                        bindingLeft.agoraEduOptions.setShowOption(RoomType.ONE_ON_ONE, it)
                    }
                } else {//video 在右边
                    binding.teachAidContainer.initView(this)
                    binding.agoraClassHead.initView(this)
                    binding.agoraEduWhiteboard.initView(uuid, this)
                    launchConfig?.videoDirection?.let { binding.agoraEduWhiteboard.setDirection(it) }
                    binding.videoGroup.initView(this)
                    binding.videoGroup.let {
                        getUIDataProvider()?.addListener(it.uiDataProviderListener)
                    }
                    binding.agoraEduOptions.itemContainer = binding.agoraEduOptionsItemContainer
                    binding.agoraEduOptions.rootContainer = binding.root
                    binding.agoraEduOptions.initView(uuid, binding.root, binding.agoraEduOptionsItemContainer, this)
                    launchConfig?.roleType?.let {
                        binding.agoraEduOptions.setShowOption(RoomType.ONE_ON_ONE, it)
                    }
                }
            }
            join()
        }
    }

    override fun onBackPressed() {
        releaseTimer()
        super.onBackPressed()
    }

    /**
     * 开始倒计时
     */
    private fun startTimer() {
        releaseTimer()
        if(AgoraEduContextUserRole.Student == eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role) {
            //教师是否进入过教室
            var whetherJoin =
                true == eduCore()?.eduContextPool()?.roomContext()?.getRoomProperties()?.get(roomTagTeacherWhetherJoinRoomKey)?.toString()?.toBoolean()
            //课堂开始时间
            val startTime = eduCore()?.eduContextPool()?.roomContext()?.getClassInfo()?.startTime
            //剩余超时时间
            val reduceTimeOut = 600000L - (System.currentTimeMillis() - (startTime ?: 0L))
            if (!whetherJoin && reduceTimeOut > 0) {
                countDownTimer = object : CountDownTimer(reduceTimeOut + 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        //倒计时结束，获取当前教师是否进入课堂
                        whetherJoin =
                            true == eduCore()?.eduContextPool()?.roomContext()?.getRoomProperties()?.get(roomTagTeacherWhetherJoinRoomKey)?.toString()?.toBoolean()
                        if (!whetherJoin) {
                            //执行退出弹窗
                            runOnUiThread {
                                AgoraUIHintDialogBuilder(this@AgoraClass1V1Activity).title(resources.getString(R.string.fcr_dialog_hint_title))
                                    .content(resources.getString(R.string.fcr_dialog_hint_content))
                                    .buttonText(resources.getString(R.string.fcr_dialog_hint_btn)).build().show()
                            }
                        }
                    }
                }
                countDownTimer.start()
            }
        }
    }

    /**
     * 释放倒计时
     */
    private fun releaseTimer() {
        // 取消计时器以避免内存泄漏
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }

}