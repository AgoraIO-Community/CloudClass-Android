package com.hyphenate.easeim.modules.view.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.view.ui.emoji.EaseEmojicon

/**
 * 表情adapter
 */
class EmojiGridAdapter(context: Context, resource: Int, emojiList: Array<EaseEmojicon>) : ArrayAdapter<EaseEmojicon>(context, resource, emojiList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.emoji_item, null)
        }
        val imageView = convertView!!.findViewById<AppCompatImageView>(R.id.emoji)
        val emojicon = getItem(position)
        if(emojicon?.icon != 0){
            emojicon?.icon?.let { imageView.setImageResource(it) }
        }
        return convertView
    }
}
