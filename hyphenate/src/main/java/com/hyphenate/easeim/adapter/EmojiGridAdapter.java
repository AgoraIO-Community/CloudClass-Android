package com.hyphenate.easeim.adapter;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyphenate.easeim.R;

import org.w3c.dom.Text;

public class EmojiGridAdapter extends ArrayAdapter<String> {

    public EmojiGridAdapter(@NonNull Context context, int resource, @NonNull String[] emojiList) {
        super(context, resource, emojiList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = View.inflate(getContext(), R.layout.emoji_item, null);
        }
        TextView textView = convertView.findViewById(R.id.emoji);
        textView.setText(getItem(position));
        return convertView;
    }
}
