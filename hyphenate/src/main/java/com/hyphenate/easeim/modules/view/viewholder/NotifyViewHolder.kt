package com.hyphenate.easeim.modules.view.viewholder

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hyphenate.chat.EMCustomMessageBody
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.MessageListItemClickListener

/**
 * 全员禁言提示ViewhHolder
 */
class NotifyViewHolder(view: View,
                       itemClickListener: MessageListItemClickListener,
                       context: Context
) : ChatRowViewHolder(view, itemClickListener, context) {
    val content: TextView = itemView.findViewById(R.id.notify_content)
    override fun onSetUpView() {
        val body = message.body as EMCustomMessageBody
        when(body.params[EaseConstant.OPERATION]){
            EaseConstant.SET_ALL_MUTE -> {
                content.text = context.getString(R.string.muted_all)
            }
            EaseConstant.REMOVE_ALL_MUTE -> {
                content.text = context.getString(R.string.cancel_muted_all)
            }
            EaseConstant.DEL -> {
                content.text = String.format(context.getString(R.string.remove_message_notify), message.getStringAttribute(EaseConstant.NICK_NAME))
            }
        }
    }
}