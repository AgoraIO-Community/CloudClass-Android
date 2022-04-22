package com.agora.edu.component.tab

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
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
    private val tabs = arrayOf(context.getString(R.string.fcr_room_one_to_one_tab_video), context.getString(R.string.fcr_room_one_to_one_tab_chat))
    private var tvTabVideoTitle: TextView? = null
    private var tvTabChatTitle: TextView? = null
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

    init {
        initTab()
    }

    private fun initTab() {
        val videoTab = binding.tabLayout.newTab()
        videoTab.setCustomView(R.layout.agora_tablayout_item)
        tvTabVideoTitle = videoTab.customView?.findViewById(R.id.tv_tab_title)
        tvTabVideoTitle?.text = tabs[0]
        tvTabVideoTitle?.setTextColor(resources.getColor(R.color.theme_text_color_black))
        tvTabVideoTitle?.paint?.isFakeBoldText = true
        binding.tabLayout.addTab(videoTab)
        val chatTab = binding.tabLayout.newTab()
        chatTab.setCustomView(R.layout.agora_tablayout_item)
        tvTabChatTitle = chatTab.customView?.findViewById(R.id.tv_tab_title)
        tvTabChatTitle?.text = tabs[1]
        tvTabRed = chatTab.customView?.findViewById(R.id.iv_tab_red)
        binding.tabLayout.addTab(chatTab)
        binding.tabLayout.setSelectedTabIndicator(0)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        Constants.AgoraLog?.d("TAG", "0")
                        binding.teacherVideoWindow.isVisible = true
                        updateTabTextStyle(tvTabVideoTitle, true)
                        isChatLayoutShow = false
                    }
                    1 -> {
                        Constants.AgoraLog?.d("TAG", "1")
                        binding.chat.isVisible = true
                        updateTabTextStyle(tvTabChatTitle, true)
                        isChatLayoutShow = true
                        showTipsUnread(false)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.teacherVideoWindow.isVisible = false
                        updateTabTextStyle(tvTabVideoTitle, false)
                    }
                    1 -> {
                        binding.chat.isVisible = false
                        updateTabTextStyle(tvTabChatTitle, false)
                    }
                }
            }

            private fun updateTabTextStyle(textView: TextView?, selected: Boolean) {
                textView?.setTextColor(resources.getColor(
                    if (selected) R.color.theme_text_color_black else R.color.theme_text_color_gray))
                textView?.paint?.isFakeBoldText = selected
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
    }

    fun initView(rootContainer: ViewGroup?, agoraUIProvider: IAgoraUIProvider) {
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        binding.teacherVideoWindow.initView(agoraUIProvider)
        binding.chat.isOnlyChat = true
        binding.chat.initView(rootContainer,agoraUIProvider)
        binding.chat.isVisible = false
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
}