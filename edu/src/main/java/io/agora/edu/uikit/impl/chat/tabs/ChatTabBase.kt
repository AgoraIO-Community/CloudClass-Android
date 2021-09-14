package io.agora.edu.uikit.impl.chat.tabs

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.edu.R
import io.agora.edu.core.context.EduContextUserRole
import io.agora.edu.uikit.component.RectBackgroundBuilder
import io.agora.edu.uikit.impl.chat.AgoraUIChatItem
import io.agora.edu.uikit.impl.chat.AgoraUIChatSource
import io.agora.edu.uikit.impl.chat.AgoraUIChatState

abstract class ChatTabBase : FrameLayout, IChatTab {
    private val tag = "ChatTabBase"

    var eduContext: io.agora.edu.core.context.EduContextPool? = null

    protected var container: ViewGroup? = null
    protected var layout: View? = null
    protected var emptyPlaceHolder: RelativeLayout? = null
    protected var recycler: RecyclerView? = null
    protected var layoutManager = LinearLayoutManager(
            context, LinearLayoutManager.VERTICAL, false)
    protected var adapter: ChatItemAdapter = ChatItemAdapter()

    protected var lastReadMessageId: Long = -1
    private var lastMessageIdInList: Long = -1

    private var tagManager: TabManager? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, container: ViewGroup) : super(context) {
        this.container = container
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setTabManager(manager: TabManager) {
        tagManager = manager
    }

    protected fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences("AgoraUI_tablayout", Context.MODE_PRIVATE)
    }

    override fun addMessage(item: AgoraUIChatItem) {
        adapter.addMessage(item, false)
        updateLastMessageIdInList()
        recycler?.post { adapter.notifyChanged() }
    }

    override fun addMessageList(list: List<AgoraUIChatItem>, front: Boolean) {
        adapter.addMessageInBatch(list, front)
        updateLastMessageIdInList()
        recycler?.post { adapter.notifyChanged() }
    }

    override fun onSendLocalChat(item: AgoraUIChatItem) {
        recycler?.post {
            adapter.addMessage(item, false)
            updateLastMessageIdInList()
            adapter.addToLocalPendingList(item)
        }
    }

    override fun onSendResult(userId: String, messageId: String, timestamp: Long, success: Boolean) {
        recycler?.post { adapter.onSendResult(userId, messageId, timestamp, success) }
    }

    protected inner class ChatItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_user_name)
        val message: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_message)
        val loading: ContentLoadingProgressBar = itemView.findViewById(R.id.agora_chat_item_loading)
        val guide: Guideline = itemView.findViewById(R.id.guide_line)
        val error: AppCompatTextView = itemView.findViewById(R.id.agora_chat_send_fail_button)
    }

    protected inner class ChatItemAdapter : RecyclerView.Adapter<ChatItemViewHolder>() {
        private val viewTypeChat = 0
        private val viewTypeInfo = 1

        private val chatItemList = mutableListOf<AgoraUIChatItem>()
        private val chatItemMap = mutableMapOf<String, AgoraUIChatItem>()
        private val sentItemMap = mutableMapOf<Long, AgoraUIChatItem>()
        private val retryItemMap = mutableMapOf<Long, AgoraUIChatItem>()

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
                }
            }
        }

        /**
         * Retry sending a message but not add to message list
         */
        private fun onRetrySend(item: AgoraUIChatItem) {
            retryItemMap[item.timestamp] = item
            retryItemMap[item.timestamp]?.state = AgoraUIChatState.InProgress
            notifyChanged()
            sendLocalChat(item.message, item.timestamp)
        }

        private fun sendLocalChat(message: String, timestamp: Long) : AgoraUIChatItem? {
            val item: io.agora.edu.core.context.EduContextChatItem? = when (this@ChatTabBase) {
                is PublicChatTab -> eduContext?.chatContext()?.sendLocalChannelMessage(message, timestamp,
                        object : io.agora.edu.core.context.EduContextCallback<io.agora.edu.core.context.EduContextChatItemSendResult> {
                            override fun onSuccess(target: io.agora.edu.core.context.EduContextChatItemSendResult?) {
                                target?.let { result ->
                                    adapter.onSendResult(result.fromUserId, result.messageId.toString(), result.timestamp, true)
                                }
                            }

                            override fun onFailure(error: io.agora.edu.core.context.EduContextError?) {
                                adapter.onSendResult("", "", timestamp, false)
                            }
                        })
                is PrivateChatTab -> eduContext?.chatContext()?.sendConversationMessage(message, timestamp,
                        object : io.agora.edu.core.context.EduContextCallback<io.agora.edu.core.context.EduContextChatItemSendResult> {
                            override fun onSuccess(target: io.agora.edu.core.context.EduContextChatItemSendResult?) {
                                target?.let { result ->
                                    adapter.onSendResult(result.fromUserId, result.messageId.toString(), result.timestamp, true)
                                }
                            }

                            override fun onFailure(error: io.agora.edu.core.context.EduContextError?) {
                                adapter.onSendResult("", "", timestamp, false)
                            }
                        })
                else -> null
            }

            return item?.let {
                AgoraUIChatItem.fromContextItem(it)
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

        internal fun notifyChanged() {
            container?.post {
                emptyPlaceHolder?.visibility = if (chatItemList.isEmpty()) View.VISIBLE else View.GONE
                notifyDataSetChanged()
            }
        }

        fun onSendResult(uid: String, messageId: String, timestamp: Long, success: Boolean) {
            synchronized(this@ChatItemAdapter) {
                var item: AgoraUIChatItem? = null
                var id: String? = null
                sentItemMap[timestamp]?.let {
                    item = it
                    id = it.messageId
                    it.messageId = messageId
                    it.uid = uid
                    it.state = if (success) AgoraUIChatState.Success else AgoraUIChatState.Fail
                    // Remove from the pending list whether the sending
                    // is successful or not.
                    sentItemMap.remove(timestamp)
                }

                // when this chat item is just sent locally, the message id
                // it left to local storage is temporary. So we need to replace
                // the message id with the actual message id returned from server
                item?.let {
                    id?.let { key ->
                        chatItemMap.remove(key)
                        chatItemMap.put(it.messageId, it)
                    }
                }

                retryItemMap[timestamp]?.let {
                    item = it
                    id = it.messageId
                    it.messageId = messageId
                    it.uid = uid
                    it.state = if (success) AgoraUIChatState.Success else AgoraUIChatState.Fail
                    if (success) retryItemMap.remove(timestamp)
                }

                item?.let {
                    id?.let { key ->
                        chatItemMap.remove(key)
                        chatItemMap.put(it.messageId, it)
                    }
                }

                notifyDataSetChanged()
            }
        }

        fun addToLocalPendingList(item: AgoraUIChatItem) {
            if (item.source == AgoraUIChatSource.Local) {
                synchronized(this@ChatItemAdapter) {
                    if (sentItemMap[item.timestamp] == null) {
                        sentItemMap[item.timestamp] = item
                    }
                }
            }
        }

        fun addMessage(item: AgoraUIChatItem, front: Boolean) {
            synchronized(this@ChatItemAdapter) {
                if (chatItemMap.containsKey(item.messageId)) {
                    Log.d(tag, "$this, add duplicated message, id ${item.messageId}")
                    return
                }

                if (front) {
                    chatItemList.add(0, item)
                } else {
                    chatItemList.add(item)
                }
                chatItemMap.put(item.messageId, item)
            }

            notifyChanged()
            if (!front) moveToLast()
        }

        fun addMessageInBatch(batch: List<AgoraUIChatItem>, front: Boolean) {
            synchronized(this@ChatItemAdapter) {
                for (i in batch.indices) {
                    val item = batch[i]
                    if (chatItemMap.containsKey(item.messageId)) {
                        Log.d(tag, "$this, add duplicated message, id ${item.messageId}")
                        continue
                    }

                    if (front) {
                        chatItemList.add(0, item)
                    } else {
                        chatItemList.add(item)
                    }
                    chatItemMap[item.messageId] = item
                }
            }

            notifyChanged()

            if (front && batch.isNotEmpty()) {
                moveToPosition(batch.size)
            } else if (!front) {
                moveToLast()
            }
        }

        fun getChatItem(index: Int): AgoraUIChatItem? {
            return if (index in 0 until itemCount) chatItemList[index] else null
        }

        private fun moveToLast() {
            moveToPosition(itemCount - 1)
        }

        private fun moveToPosition(position: Int) {
            if (itemCount > 0) {
                var pos = position
                if (pos < 0) {
                    pos = 0
                } else if (pos >= itemCount) {
                    pos = itemCount - 1
                }
                recycler?.smoothScrollToPosition(pos)
            }
        }
    }

    abstract fun chatAllowed() : Boolean

    /**
     * Save locally the last read chat item id,
     * usually called when sliding the list and a newer
     * message is reached
     */
    abstract fun recordLastReadItem()

    /**
     * Record the last time of current message list.
     * Called when a new item is inserted to the tail of list
     */
    protected fun updateLastMessageIdInList() {
        if (adapter.itemCount > 0) {
            lastMessageIdInList = adapter.getChatItem(adapter.itemCount - 1)?.timestamp ?: -1
            Log.d(tag, "$this, last item in message list update to $lastMessageIdInList")
        }
    }

    fun hasUnreadMessages(): Boolean {
        val hasUnread = lastMessageIdInList > lastReadMessageId
        Log.d(tag, "last: $lastMessageIdInList, last read: $lastReadMessageId, has unread message: $hasUnread")
        return hasUnread
    }
}