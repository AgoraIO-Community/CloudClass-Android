package com.agora.edu.component.tab

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduVideoGroupComponent
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.google.gson.Gson
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.databinding.AgoraEduTabgroupComponentBinding
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionPacket
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionSignal

/**
 * 1v1教室的tab组件（包括视频和聊天）
 * Tab component of 1v1 classroom(include videoGroup and chatComponent)
 *
 */
open class AgoraEduTabGroupComponent : AbsAgoraEduComponent {
    private val tag = "AgoraEduTabGroupComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraEduTabgroupComponentBinding = AgoraEduTabgroupComponentBinding.inflate(LayoutInflater.from(context), this, true)
    var rootContainer: ViewGroup? = null
    private var tvTabRed: TextView? = null
    private var isChatLayoutShow: Boolean = false

    /**
     * 接收未读聊天消息的观察者
     * observer receiving unread msg
     */
    private val chatObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            if (id == AgoraWidgetDefaultId.Chat.id) {
                val packet = Gson().fromJson(msg, AgoraChatInteractionPacket::class.java)
                if (packet.signal == AgoraChatInteractionSignal.UnreadTips) {
                    if (!isChatLayoutShow) {
                        showTipsUnread(true)
                    }
                }
            }
        }
    }

    fun initView(rootContainer: ViewGroup?, agoraUIProvider: IAgoraUIProvider) {
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.teacherVideoWindow.initView(agoraUIProvider)
        binding.teacherVideoWindow.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }
        eduContext?.widgetContext()?.addWidgetMessageObserver(chatObserver, AgoraWidgetDefaultId.Chat.id)
    }

    /**
     * 显示/隐藏未读消息的提示
     * show/hide tips of unread
     * @param show
     */
    fun showTipsUnread(show: Boolean) {
        ContextCompat.getMainExecutor(context).execute {
            if (show) {
                tvTabRed?.visibility = VISIBLE
            } else {
                tvTabRed?.visibility = GONE
            }
        }
    }

    fun getVideoGroup(): AgoraEduVideoGroupComponent {
        return binding.teacherVideoWindow
    }
}