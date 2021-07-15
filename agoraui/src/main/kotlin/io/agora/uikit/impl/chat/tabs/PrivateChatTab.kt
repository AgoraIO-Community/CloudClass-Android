package io.agora.uikit.impl.chat.tabs

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.educontext.*
import io.agora.uikit.R
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.impl.chat.AgoraUIChatWindow

class PrivateChatTab : ChatTabBase {
    private val tag = "PrivateChatTab"

    var peerUser: EduContextUserInfo? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, container: ViewGroup) : super(context, container) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        lastReadMessageId = getSharedPreferences().getLong("$tag$${AgoraUIChatWindow.roomInfo?.roomUuid}", -1)
        Log.d(tag, "last read message id from local storage: $lastReadMessageId")

        LayoutInflater.from(context).inflate(R.layout.agora_chat_tab_private_layout, this)?.let {
            layout = it
            emptyPlaceHolder = it.findViewById(R.id.agora_chat_no_message_placeholder)
            recycler = it.findViewById(R.id.agora_public_chat_recycler)
            recycler!!.layoutManager = layoutManager
            recycler!!.adapter = adapter

            recycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val firstItemPosition = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0))
                        if (firstItemPosition == 0) {
                            pullPrivateChatHistory(adapter.getChatItem(0)?.messageId)
                        }

                        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                        if (lastVisiblePosition >= 0) {
                            adapter.getChatItem(lastVisiblePosition)?.let { item ->
                                if (item.timestamp > lastReadMessageId) {
                                    lastReadMessageId = item.timestamp
                                    recordLastReadItem()
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    override fun isActive(active: Boolean) {
        Log.i(tag, "isActive $active")
        updateLastMessageIdInList()
    }

    override fun recordLastReadItem() {
        getSharedPreferences().edit().putLong("$tag$${AgoraUIChatWindow.roomInfo?.roomUuid}", lastReadMessageId).apply()
    }

    private fun pullPrivateChatHistory(messageId: String?) {
        eduContext?.chatContext()?.fetchConversationHistory(startId = messageId,
                callback = object : EduContextCallback<List<EduContextChatItem>> {
                    override fun onSuccess(target: List<EduContextChatItem>?) {
                        target?.let {
                            val list = mutableListOf<AgoraUIChatItem>()
                            for (i in it.indices) {
                                list.add(AgoraUIChatItem.fromContextItem(it[i]))
                            }
                            addMessageList(list, true)
                        }
                    }

                    override fun onFailure(error: EduContextError?) {
                        adapter.notifyChanged()
                    }
                })
    }

    override fun chatAllowed(): Boolean {
        return true
    }

    override fun allowChat(group: Boolean?, local: Boolean?) {
        // Do nothing
    }

    override fun getType(): TabType {
        return TabType.Private
    }
}