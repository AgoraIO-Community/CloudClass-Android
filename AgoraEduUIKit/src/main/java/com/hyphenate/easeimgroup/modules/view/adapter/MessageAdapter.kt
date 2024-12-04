package com.hyphenate.easeimgroup.modules.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeduuikit.R
import com.hyphenate.easeimgroup.modules.view.`interface`.MessageListItemClickListener
import com.hyphenate.easeimgroup.modules.view.viewholder.ChatRowViewHolder
import com.hyphenate.easeimgroup.modules.view.viewholder.ImageViewHolder
import com.hyphenate.easeimgroup.modules.view.viewholder.NotifyViewHolder
import com.hyphenate.easeimgroup.modules.view.viewholder.TextViewHolder
import io.agora.chat.ChatMessage

/**
 * 聊天列表adapter
 */
class MessageAdapter : RecyclerView.Adapter<ChatRowViewHolder>() {

    companion object {
        const val DIRECT_TXT_SEND: Int = 1
        const val DIRECT_TXT_REC: Int = 2
        const val DIRECT_CUSTOM: Int = 3
        const val DIRECT_IMG_SEND: Int = 4
        const val DIRECT_IMG_REC: Int = 5
    }

    lateinit var context: Context
    private var data: List<ChatMessage> = mutableListOf()
    private lateinit var itemClickListener: MessageListItemClickListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRowViewHolder {
        context = parent.context
        return getViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: ChatRowViewHolder, position: Int) {
        if (data.isNotEmpty()) {
            val message = getItem(position)
            holder.setUpView(message)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).direct() == ChatMessage.Direct.SEND) {
            when (getItem(position).type) {
                ChatMessage.Type.TXT -> DIRECT_TXT_SEND
                ChatMessage.Type.CUSTOM -> DIRECT_CUSTOM
                ChatMessage.Type.IMAGE -> DIRECT_IMG_SEND
                else -> DIRECT_TXT_SEND
            }
        } else {
            when (getItem(position).type) {
                ChatMessage.Type.TXT -> DIRECT_TXT_REC
                ChatMessage.Type.CUSTOM -> DIRECT_CUSTOM
                ChatMessage.Type.IMAGE -> DIRECT_IMG_REC
                else -> DIRECT_TXT_REC
            }
        }
    }

    private fun getItem(position: Int): ChatMessage {
        return data[position]
    }

    private fun getViewHolder(parent: ViewGroup, viewType: Int): ChatRowViewHolder {
        return when (viewType) {
            DIRECT_TXT_SEND -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.fcr_send_message_item, parent, false), itemClickListener, parent.context
            )
            DIRECT_TXT_REC -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.fcr_recv_message_item, parent, false), itemClickListener, parent.context
            )
            DIRECT_CUSTOM -> NotifyViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.fcr_notify_message_item, parent, false), itemClickListener, parent.context
            )
            DIRECT_IMG_SEND -> ImageViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.fcr_send_img_message_item, parent, false), itemClickListener, parent.context,
            )
            DIRECT_IMG_REC -> ImageViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.fcr_recv_img_message_item, parent, false), itemClickListener, parent.context
            )
            else -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.fcr_send_message_item, parent, false), itemClickListener, parent.context
            )
        }
    }

    fun setMessageListItemClickListener(itemClickListener: MessageListItemClickListener) {
        this.itemClickListener = itemClickListener
    }


    fun setData(data: List<ChatMessage>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}