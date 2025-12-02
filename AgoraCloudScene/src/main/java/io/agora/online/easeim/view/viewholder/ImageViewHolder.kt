package io.agora.online.easeim.view.viewholder

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import io.agora.online.R
import io.agora.online.easeim.utils.CommonUtil
import io.agora.online.easeim.view.`interface`.MessageListItemClickListener

class ImageViewHolder(
        val view: View,
        itemClickListener: MessageListItemClickListener,
        context: Context,
) : ChatRowViewHolder(view, itemClickListener, context) {
    private val img: AppCompatImageView = itemView.findViewById(R.id.iv_img)
    override fun onSetUpView() {
        CommonUtil.showImage(view.context.applicationContext, img, message)
    }

    override fun onMessageSuccess() {
        super.onMessageSuccess()
        CommonUtil.showImage(view.context.applicationContext, img, message)
    }

    override fun onMessageInProgress() {
        super.onMessageInProgress()
    }

    override fun setListener() {
        super.setListener()
        img.setOnClickListener { v ->
            itemClickListener.onItemClick(v, message)
        }
    }
}