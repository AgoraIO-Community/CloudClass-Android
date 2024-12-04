package com.hyphenate.easeimgroup.modules.view.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import io.agora.agoraeduuikit.R

/**
 * 表情adapter
 */
class EmojiGridAdapter(context: Context, resource: Int, emojiList: Array<String>) : ArrayAdapter<String?>(context, resource, emojiList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fcr_emoji_item, null)
        }
        val textView = convertView!!.findViewById<TextView>(R.id.emoji)
        textView.text = getItem(position)
        return convertView
    }
}
