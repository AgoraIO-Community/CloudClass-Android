package com.hyphenate.easeim.adapter;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.domain.EaseEmojicon;

import org.w3c.dom.Text;

public class EmojiGridAdapter extends ArrayAdapter<EaseEmojicon> {

    public EmojiGridAdapter(@NonNull Context context, int resource, @NonNull EaseEmojicon[] emojiList) {
        super(context, resource, emojiList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = View.inflate(getContext(), R.layout.emoji_item, null);
        }
        ImageView imageView = convertView.findViewById(R.id.emoji);
        imageView.setImageResource(getItem(position).getIcon());
        return convertView;
    }
}
