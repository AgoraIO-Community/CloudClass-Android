package io.agora.edu.uikit.impl.chat.tabs

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import io.agora.edu.R
import io.agora.edu.uikit.impl.chat.AgoraUIChatItem
import io.agora.edu.uikit.impl.chat.AgoraUIChatWidget

class PublicChatTab : ChatTabBase {
    private val tag = "PublicChatTab"

    private var chatAllowedGroup: Boolean = true
    private var chatAllowedLocal: Boolean = true
    private var muteLayout: RelativeLayout? = null

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
        lastReadMessageId = getSharedPreferences().getLong("$tag$${AgoraUIChatWidget.roomInfo?.roomUuid}", -1)
        Log.d(tag, "last read message id from local storage: $lastReadMessageId")

        LayoutInflater.from(context).inflate(R.layout.agora_chat_tab_public_layout, this)?.let { it ->
            layout = it
            muteLayout = it.findViewById(R.id.agora_chat_student_mute_layout)
            emptyPlaceHolder = it.findViewById(R.id.agora_chat_no_message_placeholder)
            recycler = it.findViewById(R.id.agora_public_chat_recycler)
            recycler!!.layoutManager = layoutManager
            recycler!!.adapter = adapter

            recycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val firstItemPosition = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0))
                        if (firstItemPosition == 0) {
                            pullPublicChatHistory(adapter.getChatItem(0)?.messageId)
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

    private fun pullPublicChatHistory(messageId: String?) {
        eduContext?.chatContext()?.fetchChannelHistory(startId = messageId,
                callback = object : io.agora.edu.core.context.EduContextCallback<List<io.agora.edu.core.context.EduContextChatItem>> {
                    override fun onSuccess(target: List<io.agora.edu.core.context.EduContextChatItem>?) {
                        target?.let {
                            val list = mutableListOf<AgoraUIChatItem>()
                            for (i in it.indices) {
                                list.add(AgoraUIChatItem.fromContextItem(it[i]))
                            }
                            addMessageList(list, true)
                        }
                    }

                    override fun onFailure(error: io.agora.edu.core.context.EduContextError?) {
                        adapter.notifyChanged()
                    }
                })
    }

    @UiThread
    override fun isActive(active: Boolean) {
        Log.i(tag, "isActive $active")
        updateLastMessageIdInList()
    }

    override fun recordLastReadItem() {
        getSharedPreferences().edit().putLong("$tag$${AgoraUIChatWidget.roomInfo?.roomUuid}", lastReadMessageId).apply()
    }

    @UiThread
    override fun allowChat(group: Boolean?, local: Boolean?) {
        chatAllowedGroup = group ?: chatAllowedGroup
        chatAllowedLocal = local ?: chatAllowedLocal

        container?.post {
            muteLayout?.let {
                it.visibility = if (!chatAllowedGroup) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getType(): TabType {
        return TabType.Public
    }

    override fun chatAllowed() : Boolean {
        return chatAllowedGroup && chatAllowedLocal
    }
}