package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.*
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.context.EduContextVideoMode
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionPacket
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionSignal
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidget
import io.agora.agoraeduuikit.impl.chat.EaseChatWidgetPopup
import io.agora.agoraeduuikit.provider.UIDataProvider

class AgoraUIVideoGroupWithChat(
    context: Context,
    private val eduContext: EduContextPool?,
    private val parent: ViewGroup,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    margin: Int,
    mode: EduContextVideoMode = EduContextVideoMode.Single,
    uiDataProvider: UIDataProvider?) : AbsComponent() {

    private val tag = "AgoraUIVideoGroup"
    private val dividerHeight = 5
    private val tabLayoutId = 1
    private val dividerId = 2

    private val tabLayout = TabLayout(context)
    private val tabs = arrayOf(context.getString(R.string.tab_video), context.getString(R.string.tab_chat))
    private val layoutView = RelativeLayout(context)
    private val divider = View(context)
    private val contentLayout = RelativeLayout(context)
    private val videoLayout = LinearLayout(context)
    private val chatLayout = LinearLayout(context)
    private var teacherVideoWindow: AgoraUIVideoGroup? = null
    private var chat: ChatPopupWidget? = null
    private var tvTabVideoTitle: TextView? = null
    private var tvTabChatTitle: TextView? = null
    private var tvTabRed: TextView? = null
    private var isChatLayoutShow: Boolean = false
    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.Chat.id)?.let { config ->
                (eduContext.widgetContext()?.create(config) as? ChatPopupWidget)?.let { popup ->
                    (contentLayout as? ViewGroup)?.let { container ->
                        container.post {
                            (popup as? EaseChatWidgetPopup)?.setInputViewParent(parent)
                            popup.init(chatLayout, container.width, container.height, 0, 0)
                            popup.setClosable(false)
                            popup.setTabDisplayed(false)
                        }
                    }
                    chat = popup
                }
            }
        }
    }

    private val chatObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            if (id == AgoraWidgetDefaultId.Chat.id) {
                val packet = Gson().fromJson(msg, AgoraChatInteractionPacket::class.java)
                if (packet.signal == AgoraChatInteractionSignal.UnreadTips && (packet.body as? Boolean) == true) {
                    if (!isChatLayoutShow) {
                        showTipsUnread(true)
                    }
                }
            }
        }
    }

    init {
        initLayoutView(width, height, left, top)
        val tabHeight = (height * 33f / 350).toInt()
        initTab(tabHeight)
        initDivider()
        initContentView()
        initVideoWindow(width, height - tabHeight - dividerHeight, margin, tabHeight)
        initChatLayout()

        eduContext?.roomContext()?.addHandler(roomHandler)
        teacherVideoWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }

        eduContext?.widgetContext()?.addWidgetMessageObserver(
            chatObserver, AgoraWidgetDefaultId.Chat.id)
    }

    private fun initLayoutView(width: Int, height: Int, left: Int, top: Int) {
        parent.addView(layoutView, width, height)

        (layoutView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.leftMargin = left
            params.topMargin = top
            layoutView.layoutParams = params
        }

        layoutView.setBackgroundResource(
            R.color.agora_board_preload_progress_view_progressbar_bg)
    }

    private fun initTab(height: Int) {
        var tab = tabLayout.newTab()
        tab.setCustomView(R.layout.agora_tablayout_item)
        tvTabVideoTitle = tab.customView?.findViewById(R.id.tv_tab_title)
        tvTabVideoTitle?.text = tabs[0]
        tvTabVideoTitle?.setTextColor(Color.BLACK)
        tabLayout.addTab(tab)
        tab = tabLayout.newTab()
        tab.setCustomView(R.layout.agora_tablayout_item)
        tvTabChatTitle = tab.customView?.findViewById(R.id.tv_tab_title)
        tvTabChatTitle?.text = tabs[1]
        tvTabRed = tab.customView?.findViewById(R.id.iv_tab_red)
        tabLayout.addTab(tab)
        tabLayout.setSelectedTabIndicator(0)
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        AgoraLog.d("TAG", "0")
                        videoLayout.isVisible = true
                        chatLayout.isVisible = false
                        tvTabVideoTitle?.setTextColor(Color.BLACK)
                        isChatLayoutShow = false
                    }
                    1 -> {
                        AgoraLog.d("TAG", "1")
                        videoLayout.isVisible = false
                        chatLayout.isVisible = true
                        tvTabChatTitle?.setTextColor(Color.BLACK)
                        isChatLayoutShow = true
                        showTipsUnread(false)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        tvTabVideoTitle?.setTextColor(Color.GRAY)
                    }
                    1 -> {
                        tvTabChatTitle?.setTextColor(Color.GRAY)
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        tabLayout.id = tabLayoutId
        layoutView.addView(tabLayout, RelativeLayout.LayoutParams.WRAP_CONTENT, height)
    }

    private fun initDivider() {
        divider.setBackgroundResource(R.color.gray_F9F9FC)
        val param = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, dividerHeight)
        param.addRule(RelativeLayout.BELOW, tabLayout.id)
        divider.id = dividerId
        layoutView.addView(divider, param)
    }

    private fun initContentView() {
        layoutView.addView(contentLayout,
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT)
        (contentLayout.layoutParams as? RelativeLayout.LayoutParams)?.let { param ->
            param.addRule(RelativeLayout.BELOW, divider.id)
            param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            contentLayout.layoutParams = param
        }
    }

    private fun initVideoWindow(width: Int, height: Int, margin: Int, tabHeight: Int) {
        teacherVideoWindow = AgoraUIVideoGroup(parent.context, eduContext,
            videoLayout, 0, 0, width, height, margin,
            EduContextVideoMode.Pair)

        contentLayout.addView(videoLayout,
            ViewGroup.MarginLayoutParams.MATCH_PARENT,
            ViewGroup.MarginLayoutParams.MATCH_PARENT)
    }

    private fun initChatLayout() {
        contentLayout.addView(chatLayout,
            ViewGroup.MarginLayoutParams.MATCH_PARENT,
            ViewGroup.MarginLayoutParams.MATCH_PARENT)
        chatLayout.isVisible = false
    }

    fun show(show: Boolean) {
        layoutView.post {
            layoutView.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun setRect(rect: Rect) {
        layoutView.post {
            val params = layoutView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            layoutView.layoutParams = params
        }
    }

    fun showTipsUnread(show: Boolean) {
        tabLayout.post {
            if (show) {
                tvTabRed?.visibility = VISIBLE
            } else {
                tvTabRed?.visibility = GONE
            }
        }
    }
}