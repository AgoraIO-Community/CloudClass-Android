package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.whiteboard.adpater.AgoraEduApplianceInfo
import com.agora.edu.component.whiteboard.adpater.AgoraEduToolInfo
import com.agora.edu.component.whiteboard.adpater.AgoraEduToolsAdapter
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListAppliance
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData.Companion.getListTools
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager
import io.agora.agoraeduuikit.impl.whiteboard.AgoraWhiteBoardManager
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.SimpleBoardEventListener
import io.agora.agoraeduuikit.interfaces.protocols.AgoraUIDrawingConfig

/**
 * author : hefeng
 * date : 2022/2/16
 * description : 白板教具和教室工具
 */
class AgoraEduApplianceComponent : AgoraEduBaseApplianceComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    val TAG = "AgoraEduApplianceComponent"

    private lateinit var toolsAdapter: AgoraEduToolsAdapter<AgoraEduToolInfo>  // 顶部工具
    private var applianceAdapter: AgoraEduToolsAdapter<AgoraEduApplianceInfo>? = null   // 白板教具

    var onApplianceListener: OnAgoraEduApplianceListener? = null

    lateinit var uuid: String

    fun initView(uuid: String, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)

        divider2.visibility = View.GONE
        bottomListView.visibility = View.GONE

        setWhiteboardListener(uuid)
    }

    /**
     * 监听白板是否可以撤销上一步和下一步
     */
    fun setWhiteboardListener(uuid: String) {
        AgoraWhiteBoardManager.addWhiteBoardListener(uuid, object : SimpleBoardEventListener() {
            override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
                isCanUndoStepsUpdate = canUndoSteps > 0

                Constants.AgoraLog?.i("$TAG -> 是否支持撤销上一步 $isCanUndoStepsUpdate || canUndoSteps= $canUndoSteps")

                applianceAdapter?.isCanUndoStepsUpdate = isCanUndoStepsUpdate
                applianceAdapter?.notifyDataSetChanged()
            }

            override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
                isCanRedoStepsUpdate = canRedoSteps > 0

                Constants.AgoraLog?.i("$TAG -> 是否支持撤销下一步 $isCanRedoStepsUpdate || canRedoSteps= $canRedoSteps")

                applianceAdapter?.isCanRedoStepsUpdate = isCanRedoStepsUpdate
                applianceAdapter?.notifyDataSetChanged()
            }
        })
    }

    fun show(isShowTools: Boolean) {
        if (isShowTools) {
            showToolList()
            showApplianceList(centerListView)
        } else {
            showApplianceList(topListView)

            divider1.visibility = View.GONE
            centerListView.visibility = View.GONE
        }
    }

    var isCanUndoStepsUpdate: Boolean = false
    var isCanRedoStepsUpdate: Boolean = false

    protected fun showToolList() {
        toolsAdapter = AgoraEduToolsAdapter(AgoraUIDrawingConfig())
        toolsAdapter.setViewData(getListTools())
        toolsAdapter.onClickItemListener = { position, info ->
            onApplianceListener?.onToolsSelected(info.activeAppliance, info.iconRes)
            when (info.activeAppliance) {
                WhiteboardApplianceType.Tool_Selector -> {
                    // 答题器
                    AgoraTransportManager.notify(AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_TOOL_SELECTOR))
                }
                WhiteboardApplianceType.Tool_CountDown -> {
                    // 倒计时
                    AgoraTransportManager.notify(AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_TOOL_COUNTDOWN))
                }

                WhiteboardApplianceType.Tool_Polling -> {
                    // 投票器
                    AgoraTransportManager.notify(AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_TOOL_POLLING))
                }

                WhiteboardApplianceType.Tool_Cloud -> {
                    // 云盘
                    AgoraTransportManager.notify(AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_TOOL_CLOUD))
                }

                WhiteboardApplianceType.Tool_WhiteBoard_Switch -> {
                    // 白板开关
                    AgoraTransportManager.notify(AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_TOOL_WHITEBOARD_SWITCH))
                }
            }
        }
        topListView.adapter = toolsAdapter
    }

    /**
     * 教具一级菜单
     */
    fun showApplianceList(listView: RecyclerView) {
        applianceAdapter = AgoraEduToolsAdapter(config)
        applianceAdapter?.let { applianceAdapter ->
            applianceAdapter.isCanUndoStepsUpdate = isCanUndoStepsUpdate
            applianceAdapter.isCanRedoStepsUpdate = isCanRedoStepsUpdate
            applianceAdapter.operationType = 1
            applianceAdapter.selectPosition = getSelectPosition()
            applianceAdapter.setViewData(getListAppliance())
            applianceAdapter.onClickItemListener = { position, info ->
                onApplianceListener?.onApplianceSelected(info.activeAppliance, info.iconRes)
            }
            listView.adapter = applianceAdapter
        }
    }

    /**
     * 选择的位置
     */
    fun getSelectPosition(): Int {
        var pos = 0
        for ((index, value) in getListAppliance().withIndex()) {
            if (config.activeAppliance == value.activeAppliance) {
                pos = index
                break
            }
        }

        return pos
    }
}