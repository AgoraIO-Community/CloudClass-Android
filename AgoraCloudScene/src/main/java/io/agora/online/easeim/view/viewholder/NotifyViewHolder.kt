package io.agora.online.easeim.view.viewholder

import android.content.Context
import android.view.View
import android.widget.TextView
import io.agora.online.R
import io.agora.online.easeim.constant.EaseConstant
import io.agora.online.easeim.view.`interface`.MessageListItemClickListener
import io.agora.chat.CustomMessageBody

/**
 * 提示消息ViewhHolder
 */
class NotifyViewHolder(view: View,
                       itemClickListener: MessageListItemClickListener,
                       context: Context
) : ChatRowViewHolder(view, itemClickListener, context) {
    val content: TextView = itemView.findViewById(R.id.notify_content)
    override fun onSetUpView() {
        val body = message.body as CustomMessageBody
        when(body.params[EaseConstant.OPERATION]){
            EaseConstant.SET_ALL_MUTE -> {
                content.text = context.getString(R.string.fcr_hyphenate_im_teacher_mute_all)
            }
            EaseConstant.REMOVE_ALL_MUTE -> {
                content.text = context.getString(R.string.fcr_hyphenate_im_teacher_unmute_all)
            }
            EaseConstant.DEL -> {
                content.text = String.format(context.getString(R.string.fcr_hyphenate_im_remove_message_notify), message.getStringAttribute(EaseConstant.NICK_NAME))
            }
            EaseConstant.MUTE -> {
                content.text = String.format(context.getString(R.string.fcr_hyphenate_im_you_have_been_muted_by_teacher), message.getStringAttribute(EaseConstant.NICK_NAME))
            }
            EaseConstant.UN_MUTE -> {
                content.text = String.format(context.getString(R.string.fcr_hyphenate_im_you_have_been_unmuted_by_teacher), message.getStringAttribute(EaseConstant.NICK_NAME))
            }
        }
    }
}