package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.Scene
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduWhiteboardControlBinding
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import java.util.*

/**
 *  白板控制按钮
 */
class AgoraEduWhiteBoardControlComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

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
            boardRoom.room?.addPage(Scene(UUID.randomUUID().toString()), true)
            boardRoom.room?.nextPage(object : Promise<Boolean> {
                override fun then(t: Boolean?) {
                    Constants.AgoraLog?.i("nextPage :->$t")
                }

                override fun catchEx(t: SDKError?) {
                    Constants.AgoraLog?.e("nextPage :catchEx->" + t?.message)
                }
            })

            /*val scenes = arrayOf(Scene(UUID.randomUUID().toString()))
            if (boardRoom.boardState != null && boardRoom.boardState.sceneState != null) {
                val scenePath = boardRoom.boardState.sceneState.scenePath

                val lastSlashIndex = scenePath.lastIndexOf("/")
                val sceneDir = if (lastSlashIndex == 0) {
                    scenePath.substring(0, lastSlashIndex)
                } else {
                    "/"
                }
                val targetIndex = boardRoom.boardState.sceneState.index + 1

                boardRoom.putScenes(
                    sceneDir,
                    scenes,
                    targetIndex
                )
                boardRoom.setSceneIndex(targetIndex)
            }*/
        }
        binding.agoraWbPre.setOnClickListener {
            boardRoom.room?.prevPage(object : Promise<Boolean> {
                override fun then(t: Boolean?) {
                    Constants.AgoraLog?.i("prevPage :->$t")
                }

                override fun catchEx(t: SDKError?) {
                    Constants.AgoraLog?.e("prevPage :catchEx->" + t?.message)
                }
            })
            //setSceneIndex(boardRoom, false)
            //boardRoom.pptPreviousStep()
        }
        binding.agoraWbNext.setOnClickListener {
            boardRoom.room?.nextPage(object : Promise<Boolean> {
                override fun then(t: Boolean?) {
                    Constants.AgoraLog?.i("nextPage :->$t")
                }

                override fun catchEx(t: SDKError?) {
                    Constants.AgoraLog?.e("nextPage :catchEx->" + t?.message)
                }
            })

            //setSceneIndex(boardRoom, true)
            //boardRoom.pptNextStep()
        }
    }

    fun setWhiteBoardPage(boardRoom: BoardRoom) {
        if (boardRoom.boardState != null && boardRoom.boardState.pageState != null) {
            Constants.AgoraLog?.e("$tag:onSceneStateChanged-> index = ${boardRoom.boardState.pageState.index} || total=${boardRoom.boardState.pageState.length}")

            setWhiteBoardPage(boardRoom.boardState.pageState.index, boardRoom.boardState.pageState.length)
        }
    }

    fun setWhiteBoardPage(currentIndex: Int, totalPage: Int) {
        binding.agoraWbPageCount.text =
            String.format(resources.getString(R.string.fcr_agora_wb_page_count), currentIndex + 1, totalPage)
    }

    fun setSceneIndex(boardRoom: BoardRoom, isAdd: Boolean) {
        if (boardRoom.boardState != null && boardRoom.boardState.sceneState != null) {
            var index = boardRoom.boardState.sceneState.index

            if (isAdd) {
                if (index + 1 < boardRoom.boardState.sceneState.scenes.size) {
                    index += 1
                }
            } else {
                if (index - 1 >= 0) {
                    index -= 1
                }
            }
            boardRoom.setSceneIndex(index)
        }
    }

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
        if (roomContext?.getClassInfo()?.state == AgoraEduContextClassState.Before) {
            binding.startClassBtn.visibility = VISIBLE
            binding.startClassBtn.setOnClickListener {
                roomContext.startClass(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                        handler.post { binding.startClassBtn.visibility = GONE }
                    }

                    override fun onFailure(error: EduContextError?) {
                        AgoraLog?.e("$tag->startClass-failed: ${error?.let { GsonUtil.toJson(it) }}")
                    }
                })
            }
        } else {
            binding.startClassBtn.visibility = GONE
        }
    }
}