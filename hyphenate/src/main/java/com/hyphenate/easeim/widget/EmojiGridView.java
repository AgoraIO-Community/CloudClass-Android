package com.hyphenate.easeim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.adapter.EmojiGridAdapter;
import com.hyphenate.easeim.interfaces.EmojiViewlistener;

public class EmojiGridView extends LinearLayout {

    private Context context;

    private String[] emojiList = new String[]{"\uD83D\uDE0A", "\uD83D\uDE03", "\uD83D\uDE09", "\uD83D\uDE2E", "\uD83D\uDE0B", "\uD83D\uDE0E", "\uD83D\uDE21", "\uD83D\uDE16", "\uD83D\uDE33", "\uD83D\uDE1E", "\uD83D\uDE2D", "\uD83D\uDE10", "\uD83D\uDE07", "\uD83D\uDE2C", "\uD83D\uDE06", "\uD83D\uDE31", "\uD83C\uDF85", "\uD83D\uDE34", "\uD83D\uDE15", "\uD83D\uDE37", "\uD83D\uDE2F", "\uD83D\uDE0F", "\uD83D\uDE11", "\uD83D\uDC96", "\uD83D\uDC94", "\uD83C\uDF19", "\uD83C\uDF1F", "\uD83C\uDF1E", "\uD83C\uDF08", "\uD83D\uDE1A", "\uD83D\uDE0D", "\uD83D\uDC8B", "\uD83C\uDF39", "\uD83C\uDF42", "\uD83D\uDC4D"};

    private GridView gridView;
    private EmojiViewlistener emojiViewlistener;

    public EmojiGridView(Context context) {
        this(context, null);
    }

    public EmojiGridView(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public EmojiGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.emoji_grid_view, this);
        initView();
    }

    private void initView() {
        gridView = findViewById(R.id.emoji_grid);
        gridView.setNumColumns(14);
        EmojiGridAdapter adapter = new EmojiGridAdapter(context, 1, emojiList);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                emojiViewlistener.onEmojiItemClick(adapter.getItem(i));
            }
        });
    }

    public void setEmojiViewlistener(EmojiViewlistener emojiViewlistener){
        this.emojiViewlistener = emojiViewlistener;
    }

}
