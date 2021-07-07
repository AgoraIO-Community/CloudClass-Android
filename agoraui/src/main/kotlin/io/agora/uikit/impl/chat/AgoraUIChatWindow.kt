package io.agora.uikit.impl.chat

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.educontext.*
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.component.RectBackgroundBuilder
import io.agora.uikit.component.RoundRectButtonStateBg
import io.agora.uikit.educontext.handlers.ChatHandler

class AgoraUIChatWindow(private val parent: ViewGroup,
                        private val eduContext: EduContextPool?,
                        private var width: Int,
                        private var height: Int,
                        private var left: Int,
                        private var top: Int,
                        private val shadow: Int) : AbsComponent() {

    private val tag = "AgoraUIChatWindow"
    private val duration = 400L

    // Specially designed to avoid recycler view having a minus height
    private val minHeight = 40

    private val messageRecycler: RecyclerView
    private val adapter: ChatItemAdapter
    private val messageLayoutManager: LinearLayoutManager
    private val elevation = parent.context.resources.getDimensionPixelSize(R.dimen.shadow_width)
    private var unReadCount = 0

    private val layout: RelativeLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.agora_chat_layout, parent, false) as RelativeLayout

    private val emptyLayout = layout.findViewById<RelativeLayout>(R.id.agora_chat_no_message_placeholder)
    private val unreadText = layout.findViewById<AppCompatTextView>(R.id.agora_chat_unread_text)
    private val closeBtn = layout.findViewById<AppCompatImageView>(R.id.agora_chat_close)

    private val contentLayout: RelativeLayout = layout.findViewById(R.id.agora_chat_layout)
    private val titleLayout: RelativeLayout = layout.findViewById(R.id.agora_chat_title_layout)
    private val inputLayout: RelativeLayout = layout.findViewById(R.id.agora_chat_input_layout)

    private var contentWidth: Int
    private var contentHeight: Int
    private var contentTopMargin: Int
    private var contentLeftMargin: Int
    private val hideLayout: RelativeLayout

    private var titleLayoutHeight: Int = 0
    private var inputLayoutHeight: Int = 0

    val hideIconSize = parent.context.resources.getDimensionPixelSize(R.dimen.agora_message_hide_icon_size)

    @Volatile
    private var hidden = false

    private var animateListener: OnChatWindowAnimateListener? = null

    private val edit: AppCompatEditText
    private val sendBtn: AppCompatTextView

    private val studentMuteLayout: RelativeLayout

    private val chatHandler = object : ChatHandler() {
        override fun onReceiveMessage(item: EduContextChatItem) {
            addMessage(AgoraUIChatItem.fromContextItem(item))
        }

        override fun onReceiveChatHistory(history: List<EduContextChatItem>) {
            history.forEach { item ->
                addMessage(AgoraUIChatItem.fromContextItem(item), true)
            }
        }

        override fun onChatAllowed(allowed: Boolean) {
            this@AgoraUIChatWindow.allowChat(allowed)
        }
    }

    init {
        emptyLayout.visibility = View.GONE
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

        messageRecycler = layout.findViewById(R.id.agora_chat_message_recycler)
        messageLayoutManager = LinearLayoutManager(messageRecycler.context,
                LinearLayoutManager.VERTICAL, false)
        messageRecycler.layoutManager = messageLayoutManager
        adapter = ChatItemAdapter()
        messageRecycler.adapter = adapter
        adapter.notifyChanged()

        messageRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val firstItemPosition = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0))
                    if (firstItemPosition == 0) {
                        eduContext?.chatContext()?.fetchHistory(adapter.getChatItem(0)?.messageId!!.minus(1),
                                callback = object : EduContextCallback<List<EduContextChatItem>> {
                                    override fun onSuccess(target: List<EduContextChatItem>?) {
                                        target?.let {
                                            addMessageInBatch(it, true, adapter.itemCount - 1)
                                        }
                                    }

                                    override fun onFailure(error: EduContextError?) {

                                    }
                                })
                    }
                }
            }
        })

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

        studentMuteLayout = layout.findViewById(R.id.agora_chat_student_mute_layout)
        studentMuteLayout.visibility = View.GONE

        eduContext?.chatContext()?.addHandler(chatHandler)
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

                    animateListener?.onChatWindowAnimate(false, fraction, params.leftMargin,
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

                    animateListener?.onChatWindowAnimate(true, fraction, params.leftMargin,
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

                    emptyLayout.visibility = if (adapter.itemCount > 0) View.GONE else View.VISIBLE
                }
                .withEndAction {
                    hidden = false
                    unReadCount = 0
                    scrollToLast()
                }
    }

    /**
     * Directly show the chat window, and display the last item
     * of the chat list
     */
    fun show(show: Boolean) {
        emptyLayout.visibility = if (adapter.itemCount > 0) View.GONE else View.VISIBLE

        if (adapter.itemCount > 0) {
            messageRecycler.scrollToPosition(adapter.itemCount - 1)
        }

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

    private fun onSendClick(view: View) {
        val content = edit.text.toString().trim()
        if (TextUtils.isEmpty(content)) {
            return
        }

        edit.setText("")
        getContainer()?.hideSoftInput(parent.context, view)

        val timestamp = System.currentTimeMillis()
        sendAndInsertLocalChat(content, timestamp)
    }

    private fun sendLocalChat(message: String, timestamp: Long): AgoraUIChatItem? {
        val item = eduContext?.chatContext()?.sendLocalMessage(message, timestamp,
                object : EduContextCallback<EduContextChatItemSendResult> {
                    override fun onSuccess(target: EduContextChatItemSendResult?) {
                        target?.let { result ->
                            adapter.onSendResult(result.userId, result.messageId, result.timestamp, true)
                        }
                    }

                    override fun onFailure(error: EduContextError?) {
                        adapter.onSendResult("", 0, timestamp, false)
                    }
                })
        return item?.let {
            AgoraUIChatItem.fromContextItem(it)
        }
    }

    private fun sendAndInsertLocalChat(message: String, timestamp: Long) {
        sendLocalChat(message, timestamp)?.let { item ->
            adapter.addMessage(item, false)
            adapter.addToLocalPendingList(item)
        }
    }

    private inner class ChatItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_user_name)
        val message: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_message)
        val loading: ContentLoadingProgressBar = itemView.findViewById(R.id.agora_chat_item_loading)
        val guide: Guideline = itemView.findViewById(R.id.guide_line)
        val error: AppCompatTextView = itemView.findViewById(R.id.agora_chat_send_fail_button)
    }

    private inner class ChatItemAdapter : RecyclerView.Adapter<ChatItemViewHolder>() {
        private val viewTypeChat = 0
        private val viewTypeInfo = 1

        val chatItemList = mutableListOf<AgoraUIChatItem>()
        val sentItemMap = mutableMapOf<Long, AgoraUIChatItem>()
        val retryItemMap = mutableMapOf<Long, AgoraUIChatItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatItemViewHolder {
            return when (viewType) {
                viewTypeChat -> ChatItemViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.agora_chat_item_layout, parent, false))
                else -> ChatItemViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.agora_chat_item_layout, parent, false))
            }
        }

        override fun onBindViewHolder(holder: ChatItemViewHolder, position: Int) {
            val pos = holder.adapterPosition
            val item = chatItemList[pos]

            if (item.source != AgoraUIChatSource.System) {
                val name = if (item.role == EduContextUserRole.Assistant.value) {
                    val tmp = String.format(holder.name.context.resources.getString(R.string.agora_message_item_role_tip),
                            holder.name.context.resources.getString(R.string.agora_message_item_role_assistant))
                    item.name.plus(tmp)
                } else {
                    item.name
                }
                holder.name.text = name
                holder.message.text = item.message

                if (item.source == AgoraUIChatSource.Local) {
                    var params = holder.guide.layoutParams as ConstraintLayout.LayoutParams
                    params.guideBegin = holder.itemView.resources.getDimensionPixelSize(
                            R.dimen.agora_message_item_message_min_margin)
                    params.guideEnd = ConstraintLayout.LayoutParams.UNSET
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    params.startToStart = ConstraintLayout.LayoutParams.UNSET
                    holder.guide.layoutParams = params

                    params = holder.name.layoutParams as ConstraintLayout.LayoutParams
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    params.startToStart = ConstraintLayout.LayoutParams.UNSET
                    holder.name.layoutParams = params

                    params = holder.message.layoutParams as ConstraintLayout.LayoutParams
                    params.startToStart = holder.guide.id
                    params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToStart = ConstraintLayout.LayoutParams.UNSET
                    params.horizontalBias = 1f
                    holder.message.layoutParams = params

                    holder.message.background = RectBackgroundBuilder(
                            color = holder.itemView.context.resources.getColor(R.color.theme_gray_dialog_bg),
                            corner = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.corner_small)).build()
                } else {
                    var params = holder.guide.layoutParams as ConstraintLayout.LayoutParams
                    params.guideBegin = ConstraintLayout.LayoutParams.UNSET
                    params.guideEnd = holder.itemView.resources.getDimensionPixelSize(
                            R.dimen.agora_message_item_message_min_margin)
                    params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    holder.guide.layoutParams = params

                    params = holder.name.layoutParams as ConstraintLayout.LayoutParams
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                    holder.name.layoutParams = params

                    params = holder.message.layoutParams as ConstraintLayout.LayoutParams
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                    params.endToStart = holder.guide.id
                    params.horizontalBias = 0f
                    holder.message.layoutParams = params

                    holder.message.background = RectBackgroundBuilder(
                            color = Color.WHITE,
                            strokeWidth = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.stroke_small),
                            strokeColor = holder.itemView.context.resources.getColor(R.color.theme_border_class_room),
                            corner = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.corner_small)).build()
                }

                holder.loading.visibility = if (item.state == AgoraUIChatState.InProgress) View.VISIBLE else View.GONE

                holder.error.visibility = if (item.state == AgoraUIChatState.Fail) View.VISIBLE else View.GONE
                holder.error.setOnClickListener {
                    onRetrySend(item)
                    sendAndInsertLocalChat(item.message, item.timestamp)
                }
            }
        }

        override fun getItemCount(): Int {
            return chatItemList.size
        }

        override fun getItemViewType(position: Int): Int {
            return when (chatItemList[position].source) {
                AgoraUIChatSource.Local -> viewTypeChat
                AgoraUIChatSource.Remote -> viewTypeChat
                else -> viewTypeInfo
            }
        }

        fun notifyChanged() {
            emptyLayout.visibility = if (chatItemList.isEmpty()) View.VISIBLE else View.GONE
            notifyDataSetChanged()
        }

        fun onRetrySend(item: AgoraUIChatItem) {
            retryItemMap[item.timestamp] = item
            retryItemMap[item.timestamp]?.state = AgoraUIChatState.InProgress
            notifyChanged()
        }

        fun onSendResult(uid: String, messageId: Int, timestamp: Long, success: Boolean) {
            sentItemMap[timestamp]?.let {
                it.messageId = messageId
                it.uid = uid
                it.state = if (success) AgoraUIChatState.Success else AgoraUIChatState.Fail
                // Remove from the pending list whether the sending
                // is successful or not.
                sentItemMap.remove(timestamp)
            }

            retryItemMap[timestamp]?.let {
                it.messageId = messageId
                it.uid = uid
                it.state = if (success) AgoraUIChatState.Success else AgoraUIChatState.Fail
                if (success) retryItemMap.remove(timestamp)
            }

            notifyDataSetChanged()
        }

        fun addToLocalPendingList(item: AgoraUIChatItem) {
            if (item.source == AgoraUIChatSource.Local) {
                if (sentItemMap[item.timestamp] == null) {
                    sentItemMap[item.timestamp] = item
                }
            }
        }

        fun addMessage(item: AgoraUIChatItem, front: Boolean) {
            if (front) {
                chatItemList.add(0, item)
            } else {
                chatItemList.add(item)
            }

            if (hidden) {
                showUnreadMessages(true)
            } else {
                messageRecycler.smoothScrollToPosition(itemCount - 1)
                notifyChanged()
            }
        }

        fun addMessageInBatch(batch: List<EduContextChatItem>, front: Boolean, stayPosition: Int) {
            batch.forEach { item ->
                val chatItem = AgoraUIChatItem.fromContextItem(item)
                if (front) {
                    chatItemList.add(0, chatItem)
                } else {
                    chatItemList.add(chatItem)
                }

                if (hidden) {
                    showUnreadMessages(true)
                } else {
                    if (0 <= stayPosition && stayPosition < chatItemList.size) {
                        messageLayoutManager.scrollToPosition(stayPosition)
                    } else {
                        Log.w(tag, "invalid position to scroll to")
                    }

                    notifyChanged()
                }
            }
        }

        fun getChatItem(index: Int): AgoraUIChatItem? {
            return if (index in 0 until itemCount) chatItemList[index] else null
        }
    }

    @UiThread
    private fun showUnreadMessages(add: Boolean) {
        if (add) unReadCount++
        if (unreadText.visibility == View.GONE) unreadText.visibility = View.VISIBLE
        unreadText.text = if (unReadCount > 99) "99+" else unReadCount.toString()
    }

    fun addMessage(item: AgoraUIChatItem) {
        addMessage(item, false)
    }

    @UiThread
    private fun scrollToLast() {
        if (adapter.itemCount > 0) {
            messageLayoutManager.scrollToPosition(adapter.itemCount - 1)
        }
    }

    @UiThread
    private fun addMessage(item: AgoraUIChatItem, front: Boolean) {
        messageRecycler.post {
            adapter.addMessage(item, front)
        }
    }

    @UiThread
    private fun addMessageInBatch(batch: List<EduContextChatItem>, front: Boolean, stayPosition: Int) {
        adapter.addMessageInBatch(batch, front, stayPosition)
    }

    @UiThread
    fun allowChat(allowed: Boolean) {
        sendBtn.post {
            sendBtn.isEnabled = allowed
            studentMuteLayout.visibility = if (allowed) View.GONE else View.VISIBLE
            edit.isEnabled = allowed
            edit.hint = edit.context.getString(if (allowed) R.string.agora_message_input_hint else
                R.string.agora_message_student_chat_mute_hint)
        }
    }

    fun showShadow(show: Boolean) {
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

    fun setFullscreenRect(fullScreen: Boolean, rect: Rect) {
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

    /**
     * Called when the size of unfold chat window is changed
     */
    fun setFullDisplayRect(rect: Rect) {
        this.width = rect.width()
        this.height = rect.height()
        this.contentWidth = width - shadow * 2
        this.contentHeight = height - shadow * 2
        this.left = rect.left
        this.top = rect.top
    }

    @UiThread
    fun setClosable(closable: Boolean) {
        closeBtn.visibility = if (closable) View.VISIBLE else View.GONE
    }

    fun setAnimateListener(listener: OnChatWindowAnimateListener) {
        this.animateListener = listener
    }

    fun isShowing(): Boolean {
        return !hidden
    }
}

interface OnChatWindowAnimateListener {
    fun onChatWindowAnimate(enlarge: Boolean, fraction: Float, left: Int, top: Int, width: Int, height: Int)
}