package com.hyphenate.easeim.modules.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.view.`interface`.MessageListItemClickListener
import com.hyphenate.easeim.modules.view.viewholder.ChatRowViewHolder
import com.hyphenate.easeim.modules.view.viewholder.MuteViewHolder
import com.hyphenate.easeim.modules.view.viewholder.TextViewHolder

/**
 * 聊天列表adapter
 */
class MessageAdapter : RecyclerView.Adapter<ChatRowViewHolder>() {

    companion object {
        const val DIRECT_TXT_SEND: Int = 1
        const val DIRECT_TXT_REC: Int = 2
        const val DIRECT_CUSTOM: Int = 3
    }

    lateinit var context: Context
    private var data: List<EMMessage> = mutableListOf()
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
        return if (getItem(position).direct() == EMMessage.Direct.SEND) {
            when (getItem(position).type) {
                EMMessage.Type.TXT -> DIRECT_TXT_SEND
                EMMessage.Type.CUSTOM -> DIRECT_CUSTOM
                else -> DIRECT_TXT_SEND
            }
        } else {
            when (getItem(position).type) {
                EMMessage.Type.TXT -> DIRECT_TXT_REC
                EMMessage.Type.CUSTOM -> DIRECT_CUSTOM
                else -> DIRECT_TXT_REC
            }
        }
    }

    private fun getItem(position: Int): EMMessage {
        return data[position]
    }

    private fun getViewHolder(parent: ViewGroup, viewType: Int): ChatRowViewHolder {
        return when (viewType) {
            DIRECT_TXT_SEND -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.send_message_item, parent, false), itemClickListener, parent.context
            )
            DIRECT_TXT_REC -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recv_message_item, parent, false), itemClickListener, parent.context
            )
            DIRECT_CUSTOM -> MuteViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.mute_message_item, parent, false), itemClickListener, parent.context
            )
            else -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.send_message_item, parent, false), itemClickListener, parent.context
            )
        }
    }

    fun setMessageListItemClickListener(itemClickListener: MessageListItemClickListener) {
        this.itemClickListener = itemClickListener
    }


    fun setData(data: List<EMMessage>) {
        this.data = data
        notifyDataSetChanged()
    }
}