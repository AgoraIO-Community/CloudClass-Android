package io.agora.online.impl.chat.rtm

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.online.R

class AgoraChatManager(appId: String,
                       roomId: String,
                       private val userId: String,
                       private val userName: String,
                       private val userRole: Int,
                       private val recyclerView: RecyclerView,
                       private val listener: MessageListListener? = null) {

    @SuppressLint("NotifyDataSetChanged")
    private val proxyListener = object : AgoraChatProxyListener {
        override fun onLocalMessageSendBeforeResponse() {
            refreshRecycler()
        }

        override fun onLocalMessageSendResult() {
            refreshRecycler()
        }

        override fun onMessageRecordPulled() {
            refreshRecycler()
        }

        private fun refreshRecycler() {
            ContextCompat.getMainExecutor(recyclerView.context).execute {
                adapter.notifyDataSetChanged()
                recyclerLayoutManager.scrollToPosition(adapter.itemCount - 1)
                listener?.onMessageListEmpty(adapter.itemCount == 0)
            }
        }
    }

    private val proxy: ChatProxy = ChatProxy(appId, roomId, proxyListener)

    private val recyclerLayoutManager = LinearLayoutManager(
        recyclerView.context, LinearLayoutManager.VERTICAL, false)

    private val adapter = ChatItemAdapter(proxy)

    init {
        recyclerView.layoutManager = recyclerLayoutManager
        recyclerView.adapter = adapter
    }

    fun sendLocalTextMessage(message: String) {
        proxy.sendLocalMessage(userId, userName,
            userRole, message, System.currentTimeMillis())
    }

    fun addRemoteTextMessage(item: AgoraChatItem) {
        proxy.addNewMessageToList(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshRecyclerView() {
        ContextCompat.getMainExecutor(recyclerView.context).execute {
            adapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(adapter.itemCount)
            recyclerView.setHasFixedSize(true)
            //recyclerLayoutManager.scrollToPosition(adapter.itemCount - 1)
        }
    }

    fun pullChatRecord(count: Int = 100) {
        proxy.pullMessageRecord(userId, count, null, true,
            object : ChatProxyCallback<Int> {
                override fun onSuccess(id: String, userId: String, elapsed: Long, data: Int?) {
                    data?.let { count ->
                        if (count > 0) {
                            listener?.onNewMessageReceived()
                        }
                    }
                }

                override fun onFailure(reason: Int, message: String) {

                }
            })
    }

    fun getChatItemCount(): Int {
        return proxy.getMessageList().size
    }
}

interface MessageListListener {
    fun onMessageListEmpty(empty: Boolean)

    fun onNewMessageReceived()
}

class ChatItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_user_name)
    val message: AppCompatTextView = itemView.findViewById(R.id.agora_chat_item_message)
    val loading: ContentLoadingProgressBar = itemView.findViewById(R.id.agora_chat_item_loading)
    val guide: Guideline = itemView.findViewById(R.id.guide_line)
    val error: AppCompatTextView = itemView.findViewById(R.id.agora_chat_send_fail_button)
}

class ChatItemAdapter(private val proxy: ChatProxy) : RecyclerView.Adapter<ChatItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatItemViewHolder {
        return ChatItemViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.fcr_online_chat_item_layout, parent, false))
    }

    override fun onBindViewHolder(holder: ChatItemViewHolder, position: Int) {
        val pos = holder.absoluteAdapterPosition
        val item = proxy.getMessageList()[pos]

        val name = if (item.role == AgoraChatUserRole.Assistant.value) {
            holder.name.context.resources.let { resources ->
                val tmp = String.format(resources.getString(R.string.fcr_agora_message_item_role_tip),
                    resources.getString(R.string.fcr_rtm_im_role_assistant))
                item.name.plus(tmp)
            }
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
            proxy.retrySendMessage(item.messageId, item.uid, item.timestamp)
        }
    }

    override fun getItemCount(): Int {
        return proxy.getMessageList().size
    }

}

class RectBackgroundBuilder(
    private val width: Int = 0,
    private val height: Int = 0,
    private val color: Int = Color.TRANSPARENT,
    private val strokeWidth: Int = 0,
    private val strokeColor: Int = Color.TRANSPARENT,
    private val corner: Int = 0) {

    fun build(): GradientDrawable {
        val drawable = GradientDrawable()
        if (width > 0 && height > 0) {
            drawable.setSize(width, height)
        }

        drawable.setColor(color)
        drawable.setStroke(strokeWidth, strokeColor)
        drawable.cornerRadius = corner.toFloat()
        return drawable
    }
}