package io.agora.online.easeim.view.viewholder

import android.content.Context
import android.view.View
import android.widget.TextView
import io.agora.online.R
import io.agora.online.easeim.view.`interface`.MessageListItemClickListener
import io.agora.chat.TextMessageBody

/**
 * 文本消息ViewhHolder
 */
class TextViewHolder(
    view: View,
    itemClickListener: MessageListItemClickListener,
    context: Context
) : ChatRowViewHolder(view, itemClickListener, context) {
    val content: TextView = itemView.findViewById(R.id.tv_content)
    override fun onSetUpView() {
        val body = message.body as TextMessageBody
        content.text = body.message
        content.requestLayout()
        content.invalidate()

    }
}