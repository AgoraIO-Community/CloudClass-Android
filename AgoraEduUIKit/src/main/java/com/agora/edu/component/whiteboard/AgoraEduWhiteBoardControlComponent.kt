package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduWhiteboardControlBinding
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom

/**
 *  白板控制按钮
 */
class AgoraEduWhiteBoardControlComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val TAG = "BoardControlComponent"
    private var binding: AgoraEduWhiteboardControlBinding =
        AgoraEduWhiteboardControlBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setWhiteBoardPage(0, 1)
    }

    @Deprecated("暂时不用")
    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
    }

    fun initView(boardRoom: BoardRoom) {
        setWhiteBoardPage(boardRoom)
        binding.agoraWbAddBackup.setOnClickListener {
            boardRoom.addPage()
            boardRoom.nextPage()
        }
        binding.agoraWbPre.setOnClickListener {
            boardRoom.prevPage()
        }

        binding.agoraWbNext.setOnClickListener {
            boardRoom.nextPage()
        }
    }

    fun setWhiteBoardPage(boardRoom: BoardRoom) {
        val state = boardRoom.boardState
        if (state != null && state.pageState != null) {
            val pageState = state.pageState
            setWhiteBoardPage(pageState.index, pageState.length)
        }
    }

    fun setWhiteBoardPage(index: Int, length: Int) {
        LogX.e(TAG, " index = $index || total=$length")
        val index = index + 1

        binding.agoraWbPageCount.text = String.format(resources.getString(R.string.fcr_agora_wb_page_count), index, length)

        if (length <= 1) {
            binding.agoraWbPre.setImageResource(R.drawable.agora_wb_page_pre_disable)
            binding.agoraWbNext.setImageResource(R.drawable.agora_wb_page_next_disable)
        } else if (index == 1) {
            binding.agoraWbPre.setImageResource(R.drawable.agora_wb_page_pre_disable)
            binding.agoraWbNext.setImageResource(R.drawable.agora_wb_page_next)
        } else if (index == length) {
            binding.agoraWbPre.setImageResource(R.drawable.agora_wb_page_pre)
            binding.agoraWbNext.setImageResource(R.drawable.agora_wb_page_next_disable)
        } else {
            binding.agoraWbPre.setImageResource(R.drawable.agora_wb_page_pre)
            binding.agoraWbNext.setImageResource(R.drawable.agora_wb_page_next)
        }
    }

//    fun setSceneIndex(boardRoom: BoardRoom, isAdd: Boolean) {
//        if (boardRoom.boardState != null && boardRoom.boardState.sceneState != null) {
//            var index = boardRoom.boardState.sceneState.index
//
//            if (isAdd) {
//                if (index + 1 < boardRoom.boardState.sceneState.scenes.size) {
//                    index += 1
//                }
//            } else {
//                if (index - 1 >= 0) {
//                    index -= 1
//                }
//            }
//            boardRoom.setSceneIndex(index)
//        }
//    }

    /**
     * 根据课堂状态信息设置开始上课按钮的状态；学生无此权限
     * set roomContext and update startClassBtn's state follow roomContext.getClassInfo()
     * but student not access to call
     */
    fun setRoomContext(roomContext: RoomContext?, localUserInfo: AgoraWidgetUserInfo?) {
        if (localUserInfo?.userRole == AgoraEduContextUserRole.Student.value) {
            binding.startClassBtn.visibility = GONE
            return
        }
        if (localUserInfo?.userRole == AgoraEduContextUserRole.Assistant.value || localUserInfo?.userRole == AgoraEduContextUserRole.Observer.value) {
            binding.startClassBtn.visibility = GONE
            binding.agoraSlidePage.visibility = GONE
            return
        }
        if (roomContext?.getClassInfo()?.state == AgoraEduContextClassState.Before) {
            binding.startClassBtn.visibility = VISIBLE
            binding.startClassBtn.setOnClickListener {
                roomContext.startClass(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                        handler.post { binding.startClassBtn.visibility = GONE }
                    }

                    override fun onFailure(error: EduContextError?) {
                        LogX.e(TAG, "startClass-failed: ${error?.let { GsonUtil.toJson(it) }}")
                        ContextCompat.getMainExecutor(context).execute {
                            ToastManager.showShort("startClass failed：${error?.msg} || ${error?.code}")
                        }
                    }
                })
            }
        } else {
            binding.startClassBtn.visibility = GONE
        }
    }

    fun setStartView(isShow: Boolean) {
        ContextCompat.getMainExecutor(context).execute {
            if (isShow) {
                binding.startClassBtn.visibility = View.VISIBLE
            } else {
                binding.startClassBtn.visibility = View.GONE
            }
        }
    }
}