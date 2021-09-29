package io.agora.agoraeduuikit.impl.chat

import android.graphics.Color
import android.graphics.Rect
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.viewpager2.widget.ViewPager2
import io.agora.agoraeducontext.*
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.chat.tabs.*
import io.agora.agoraeduuikit.component.RoundRectButtonStateBg

/**
 * This widget cannot be instantiated by constructors
 */
class AgoraUIChatWidget : ChatWidget() {
    private val tag = "AgoraUIChatWidget"
    private val duration = 400L

    private lateinit var parent: ViewGroup
    private var width: Int = 0
    private var height: Int = 0
    private var left: Int = 0
    private var top: Int = 0
    private var shadow: Int = 0

    // Specially designed to avoid recycler view having a minus height
    private val minHeight = 40
    private val elevation = 0
    private var unReadCount = 0

    private lateinit var layout: RelativeLayout

    private lateinit var unreadText: AppCompatTextView
    private lateinit var closeBtn: AppCompatImageView
    private lateinit var muteBtn: AppCompatImageView

    private lateinit var contentLayout: RelativeLayout
    private lateinit var titleLayout: RelativeLayout
    private lateinit var inputLayout: RelativeLayout

    private var tabConfigs: List<ChatTabConfig>? = null
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabManager: TabManager

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var contentTopMargin: Int = 0
    private var contentLeftMargin: Int = 0
    private lateinit var hideLayout: RelativeLayout

    private var titleLayoutHeight: Int = 0
    private var inputLayoutHeight: Int = 0

    @Volatile private var hidden = true

    private lateinit var edit: AppCompatEditText
    private lateinit var sendBtn: AppCompatTextView

    private val chatHandler = object : io.agora.agoraeducontext.handlerimpl.ChatHandler() {
        override fun onReceiveMessage(item: EduContextChatItem) {
            tabManager.addMessage(TabType.Public, AgoraUIChatItem.fromContextItem(item))
            if (hidden) {
                showUnreadMessages(true)
            }
        }

        override fun onReceiveChatHistory(history: List<EduContextChatItem>) {
            val result = mutableListOf<AgoraUIChatItem>()
            history.forEach { item ->
                result.add(AgoraUIChatItem.fromContextItem(item))
            }

            tabManager.addMessageList(TabType.Public, result, true)
        }

        override fun onReceiveConversationMessage(item: EduContextChatItem) {
            tabManager.addMessage(TabType.Private, AgoraUIChatItem.fromContextItem(item))
        }

        override fun onReceiveConversationHistory(history: List<EduContextChatItem>) {
            val result = mutableListOf<AgoraUIChatItem>()
            history.forEach { item ->
                result.add(AgoraUIChatItem.fromContextItem(item))
            }

            tabManager.addMessageList(TabType.Private, result, true)
        }

        override fun onChatAllowed(allowed: Boolean) {
            muteBtn.post { muteBtn.isActivated = !allowed }
            allowChat(allowed, null)
        }

        override fun onChatAllowed(allowed: Boolean, userInfo: EduContextUserInfo,
                                   operator: EduContextUserInfo?, local: Boolean) {
            allowChat(null, if (local) allowed else null)
        }
    }

    companion object {
        // Temporarily record current room info and is made companion
        // to facilitate all tabs to access.
        // It is currently used to record read message ids
        var roomInfo: EduContextRoomInfo? = null
    }

    fun setTabConfig(tabConfigs: List<ChatTabConfig>) {
        this.tabConfigs = tabConfigs
    }

    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {
        this.parent = parent
        this.width = width
        this.height = height
        this.top = top
        this.left = left

        shadow = parent.context.resources.getDimensionPixelSize(R.dimen.shadow_width)
        hideIconSize = parent.context.resources.getDimensionPixelSize(R.dimen.agora_message_hide_icon_size)
        roomInfo = getEduContext()?.roomContext()?.roomInfo()

        layout = LayoutInflater.from(parent.context).inflate(
                R.layout.agora_chat_layout, parent, false) as RelativeLayout

        unreadText = layout.findViewById(R.id.agora_chat_unread_text)
        closeBtn = layout.findViewById(R.id.agora_chat_icon_close)
        muteBtn = layout.findViewById(R.id.agora_chat_mute_icon)

        contentLayout = layout.findViewById(R.id.agora_chat_layout)
        titleLayout = layout.findViewById(R.id.agora_chat_title_layout)
        inputLayout = layout.findViewById(R.id.agora_chat_input_layout)

        tabLayout = layout.findViewById(R.id.agora_chat_tabs)
        viewPager = layout.findViewById(R.id.agora_chat_view_pager)

        if (tabConfigs != null) {
            tabManager = TabManager(tabLayout, tabConfigs!!, viewPager, getEduContext(),
                    object : OnTabSelectedListener {
                        override fun onTabSelected(position: Int) {
                            resetInputMethodState()
                        }
                    })
            tabManager.setCurrent(0)
        } else {
            Log.w(tag, "Tab configs have not been initialized before tab is created")
        }

        unreadText.visibility = View.GONE

        contentLayout.clipToOutline = true
        contentLayout.elevation = elevation.toFloat()
        var params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(shadow, shadow, shadow, shadow)
        contentLeftMargin = params.leftMargin
        contentTopMargin = params.topMargin
        contentWidth = width - contentLeftMargin * 2
        contentHeight = height - contentTopMargin * 2

        parent.addView(layout, width, height)
        params = layout.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top
        params.leftMargin = left
        layout.layoutParams = params

        titleLayoutHeight = (titleLayout.layoutParams as ViewGroup.MarginLayoutParams).height
        inputLayoutHeight = (inputLayout.layoutParams as ViewGroup.MarginLayoutParams).height

        edit = layout.findViewById(R.id.agora_chat_message_edit)
        edit.setOnKeyListener { view, keyCode, event ->
            return@setOnKeyListener if (keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_UP) {
                onSendClick(view)
                true
            } else false
        }

        sendBtn = layout.findViewById(R.id.agora_chat_send_btn)
        sendBtn.setTextColor(Color.WHITE)
        sendBtn.background = RoundRectButtonStateBg(
                layout.resources.getDimensionPixelSize(R.dimen.agora_message_send_btn_width),
                layout.resources.getDimensionPixelSize(R.dimen.agora_message_send_btn_height),
                layout.resources.getColor(R.color.theme_blue_light),
                layout.resources.getColor(R.color.theme_blue_light),
                layout.resources.getColor(R.color.theme_blue_gray),
                layout.resources.getColor(R.color.theme_blue_gray),
                layout.resources.getColor(R.color.theme_disable),
                layout.resources.getColor(R.color.theme_disable),
                layout.resources.getDimensionPixelSize(R.dimen.stroke_small))

        sendBtn.setOnClickListener { onSendClick(it) }

        closeBtn.setOnClickListener {
            if (!hidden) {
                hideAnimate()
            }
        }
        setClosable(true)

        hideLayout = layout.findViewById(R.id.agora_chat_hide_icon_layout)
        hideLayout.visibility = View.GONE
        hideLayout.setOnClickListener {
            if (hidden) {
                showAnimate()
            }
        }

        getEduContext()?.chatContext()?.addHandler(chatHandler)
        getWidgetManager()?.addWidget(this)
    }

    private fun onSendClick(view: View) {
        val content = edit.text.toString().trim()
        if (TextUtils.isEmpty(content)) {
            return
        }

        edit.setText("")
        getContainer()?.hideSoftInput(parent.context, view)

        val timestamp = System.currentTimeMillis()
        sendAndInsertLocalChat(content, timestamp, tabManager.getCurrentTab())
    }

    private fun sendAndInsertLocalChat(message: String, timestamp: Long, tab: ChatTabBase?) {
        sendLocalChat(message, timestamp, tab)?.let { item ->
            tab?.onSendLocalChat(item)
        }
    }

    private fun sendLocalChat(message: String, timestamp: Long, tab: ChatTabBase?) : AgoraUIChatItem? {
        val item = when (tab) {
            is PublicChatTab -> {
                getEduContext()?.chatContext()?.sendLocalChannelMessage(message, timestamp,
                        object : EduContextCallback<EduContextChatItemSendResult> {
                            override fun onSuccess(target: EduContextChatItemSendResult?) {
                                target?.let { result ->
                                    tab.onSendResult(result.fromUserId, result.messageId, result.timestamp, true)
                                }
                            }

                            override fun onFailure(error: EduContextError?) {
                                tab.onSendResult("", "", timestamp, false)
                            }
                        })
            }
            is PrivateChatTab -> {
                getEduContext()?.chatContext()?.sendConversationMessage(message, timestamp,
                    object : EduContextCallback<EduContextChatItemSendResult> {
                        override fun onSuccess(target: EduContextChatItemSendResult?) {
                            target?.let { result ->
                                tab.onSendResult(result.fromUserId, result.messageId, timestamp, true)
                            }
                        }

                        override fun onFailure(error: EduContextError?) {
                            tab.onSendResult("", "", timestamp, false)
                        }
                    })
            }
            else -> null
        }

        return item?.let{
            AgoraUIChatItem.fromContextItem(it)
        }
    }

    private fun hideAnimate() {
        contentLayout.animate().setDuration(duration)
                .scaleX(1.0f)
                .setInterpolator(DecelerateInterpolator())
                .setUpdateListener {
                    if (contentLayout.height <= minHeight) {
                        return@setUpdateListener
                    }

                    val fraction = it.animatedFraction
                    val diffWidth = contentWidth * fraction
                    val diffHeight = contentHeight * fraction
                    val width = contentWidth - diffWidth
                    val height = contentHeight - diffHeight

                    var params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = width.toInt()
                    params.height = height.toInt()
                    params.leftMargin = diffWidth.toInt() + contentLeftMargin
                    params.topMargin = diffHeight.toInt() + contentTopMargin
                    contentLayout.layoutParams = params

                    chatWidgetAnimateListener?.onChatWidgetAnimate(false, fraction, params.leftMargin,
                            params.topMargin, params.width, params.height)

                    params = titleLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.height = (titleLayoutHeight * (1 - fraction)).toInt()
                    titleLayout.layoutParams = params

                    params = inputLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.height = (inputLayoutHeight * (1 - fraction)).toInt()
                    inputLayout.layoutParams = params
                }
                .withStartAction {

                }
                .withEndAction {
                    val params = layout.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = hideIconSize
                    params.height = hideIconSize
                    params.leftMargin = this.left + this.width - hideIconSize
                    params.topMargin = this.top + this.height - hideIconSize
                    layout.layoutParams = params

                    hideLayout.visibility = View.VISIBLE
                    contentLayout.visibility = View.GONE
                    unreadText.visibility = View.GONE
                    hidden = true
                }
    }

    private fun showAnimate() {
        contentLayout.animate().setDuration(duration)
                .scaleX(1.0f)
                .setInterpolator(DecelerateInterpolator())
                .setUpdateListener {
                    val fraction = it.animatedFraction
                    val diffWidth = contentWidth * fraction
                    val diffHeight = contentHeight * fraction
                    val left = contentWidth - diffWidth + contentLeftMargin
                    val top = contentHeight - diffHeight + contentTopMargin

                    if (diffHeight <= minHeight) {
                        return@setUpdateListener
                    }

                    var params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = diffWidth.toInt()
                    params.height = diffHeight.toInt()
                    params.leftMargin = left.toInt()
                    params.topMargin = top.toInt()
                    contentLayout.layoutParams = params

                    chatWidgetAnimateListener?.onChatWidgetAnimate(true, fraction, params.leftMargin,
                            params.topMargin, params.width, params.height)

                    params = titleLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.height = (titleLayoutHeight * fraction).toInt()
                    titleLayout.layoutParams = params

                    params = inputLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.height = (inputLayoutHeight * fraction).toInt()
                    inputLayout.layoutParams = params
                }
                .withStartAction {
                    hideLayout.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE

                    val params = layout.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = width
                    params.height = height
                    params.topMargin = top
                    params.leftMargin = left
                    layout.layoutParams = params
                }
                .withEndAction {
                    hidden = false
                    unReadCount = 0
                    setClosable(true)
                }
    }

    /**
     * Directly show the chat window, and display the last item
     * of the chat list
     */
    override fun show(show: Boolean) {
        layout.post {
            if (show) {
                var params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = contentWidth
                params.height = contentHeight
                params.leftMargin = contentLeftMargin
                params.topMargin = contentTopMargin
                contentLayout.layoutParams = params

                params = layout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = width
                params.height = height
                params.topMargin = top
                params.leftMargin = left
                layout.layoutParams = params

                params = titleLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.height = titleLayoutHeight
                titleLayout.layoutParams = params

                params = inputLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.height = inputLayoutHeight
                inputLayout.layoutParams = params

                hideLayout.visibility = View.GONE
                contentLayout.visibility = View.VISIBLE
                hidden = false
            } else {
                var params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = 0
                params.height = minHeight
                params.leftMargin = contentLeftMargin
                params.topMargin = contentTopMargin
                contentLayout.layoutParams = params

                params = layout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = hideIconSize
                params.height = hideIconSize
                params.leftMargin = this.left + this.width - hideIconSize
                params.topMargin = this.top + this.height - hideIconSize
                layout.layoutParams = params

                params = titleLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.height = 0
                titleLayout.layoutParams = params

                params = inputLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.height = 0
                inputLayout.layoutParams = params

                hideLayout.visibility = View.VISIBLE
                contentLayout.visibility = View.GONE

                if (unReadCount > 0) {
                    unreadText.visibility = View.VISIBLE
                    showUnreadMessages(false)
                } else {
                    unreadText.visibility = View.GONE
                }

                hidden = true
            }
        }
    }

    @UiThread
    private fun showUnreadMessages(add: Boolean) {
        unreadText.post {
            if (add) unReadCount++
            if (unreadText.visibility == View.GONE) unreadText.visibility = View.VISIBLE
            unreadText.text = if (unReadCount > 99) "99+" else unReadCount.toString()
        }
    }

    @UiThread
    fun allowChat(group: Boolean?, local: Boolean?) {
        tabManager.allowChat(group, local)
        resetInputMethodState()
    }

    private fun resetInputMethodState() {
        var allow = true
        tabManager.getCurrentTab()?.let { tab ->
            allow = tab.chatAllowed()
        }

        sendBtn.post {
            sendBtn.isEnabled = allow
            edit.isEnabled = allow
            edit.hint = edit.context.getString(
                    if (allow) R.string.agora_message_input_hint
                    else R.string.agora_message_student_chat_mute_hint)
        }
    }

    override fun showShadow(show: Boolean) {
        super.showShadow(show)
        if (show) {
            contentLayout.elevation = shadow.toFloat()
            val params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(shadow, shadow, shadow, shadow)
            contentLeftMargin = params.leftMargin
            contentTopMargin = params.topMargin
            contentWidth = width - contentLeftMargin * 2
            contentHeight = height - contentTopMargin * 2
            contentLayout.layoutParams = params
            contentLayout.setBackgroundResource(R.drawable.agora_class_room_round_rect_bg)
        } else {
            contentLayout.elevation = 0f
            val params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(0, 0, 0, 0)
            contentLeftMargin = params.leftMargin
            contentTopMargin = params.topMargin
            contentWidth = width - contentLeftMargin * 2
            contentHeight = height - contentTopMargin * 2
            contentLayout.layoutParams = params
            contentLayout.setBackgroundResource(R.drawable.agora_class_room_round_rect_stroke_bg)
        }
    }

    override fun setFullscreenRect(fullScreen: Boolean, rect: Rect) {
        setRect(rect)

        layout.post {
            if (fullScreen) {
                val params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = 0
                params.height = minHeight
                contentLayout.layoutParams = params
                contentLayout.visibility = View.GONE
                hideLayout.visibility = View.VISIBLE
                hidden = true
            } else {
                val params = contentLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.width = contentWidth
                params.height = contentHeight
                params.topMargin = contentTopMargin
                params.leftMargin = contentLeftMargin
                contentLayout.layoutParams = params
                hideLayout.visibility = View.GONE
                contentLayout.visibility = View.VISIBLE
                hidden = false
            }
        }
    }

    override fun setRect(rect: Rect) {
        layout.post {
            val params = layout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            layout.layoutParams = params
        }
    }

    override fun receive(fromCompId: String, cmd: String, vararg: Any?) {

    }

    override fun release() {
        getWidgetManager()?.removeWidget(this)
    }

    /**
     * * Called when the size of unfold chat window is changed
     */
    override fun setFullDisplayRect(rect: Rect) {
        this.width = rect.width()
        this.height = rect.height()
        this.contentWidth = width - shadow * 2
        this.contentHeight = height - shadow * 2
        this.left = rect.left
        this.top = rect.top
    }

    @UiThread
    override fun setClosable(closable: Boolean) {
        closeBtn.visibility = if (closable) View.VISIBLE else View.GONE
    }

    override fun isShowing(): Boolean {
        return !hidden
    }
}