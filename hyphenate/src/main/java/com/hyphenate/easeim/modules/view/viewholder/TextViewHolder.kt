package com.hyphenate.easeim.modules.view.viewholder

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hyphenate.chat.EMTextMessageBody
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.view.`interface`.MessageListItemClickListener

/**
 * 文本消息ViewhHolder
 */
class TextViewHolder(view: View,
                     itemClickListener: MessageListItemClickListener,
                     context: Context
) : ChatRowViewHolder(view, itemClickListener, context) {
    val content: TextView = itemView.findViewById(R.id.tv_content)
    override fun onSetUpView() {
        val body = message.body as EMTextMessageBody
        content.text = body.message
    }
}