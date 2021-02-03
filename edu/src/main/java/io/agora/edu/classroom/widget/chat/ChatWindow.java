package io.agora.edu.classroom.widget.chat;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.agora.edu.R;
import io.agora.edu.classroom.widget.window.AbstractWindow;

public class ChatWindow extends AbstractWindow implements TextWatcher {

    /**
     * 宽占Window.ID_ANDROID_CONTENT宽的比例
     */
    public static final float widthOfContent = 0.25f;

    private AppCompatTextView mFoldMsgCount;
    private RelativeLayout mUnfoldLayout;
    private RelativeLayout mFoldLayout;
    private ChatMessageAdapter mAdapter;
    private List<ChatItem> mMessageList;

    private boolean mFold;

    public void resize(int screenWidth) {
        float width = ((float) screenWidth) * widthOfContent;
        getLayoutParams().width = ((int) width);
    }

    public ChatWindow(Context context) {
        super(context);
        init();
    }

    public ChatWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChatWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.chat_window_layout, this);
        mFoldMsgCount = findViewById(R.id.chat_window_fold_message_count);
        mFoldLayout = findViewById(R.id.chat_window_fold_layout);
        mFoldLayout.setVisibility(View.GONE);
        mUnfoldLayout = findViewById(R.id.chat_window_unfold_content);

        setLayouts(mUnfoldLayout, mFoldLayout);

        findViewById(R.id.chat_window_fold_btn).setOnClickListener(view -> startMinimize(null));

        findViewById(R.id.chat_window_min).setOnClickListener(view -> restoreMinimize(null));

        findViewById(R.id.chat_window_input_send_btn).setOnClickListener(view -> {

        });

        ((AppCompatEditText) findViewById(R.id.chat_window_input)).addTextChangedListener(this);

        RecyclerView mMessageRecycler = findViewById(R.id.chat_window_message_recycler);
        mMessageList = new ArrayList<>();
        mAdapter = new ChatMessageAdapter();
        mMessageRecycler.setAdapter(mAdapter);

        setMinimizeDirection(Direction.bottom);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    private class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageViewHolder> {
        @NonNull
        @Override
        public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int res = R.layout.chat_window_message_item_chat;
            if (viewType == ChatItem.ItemType.System.ordinal()) {
                res = R.layout.chat_window_message_item_sys;
            }
            return new ChatMessageViewHolder(LayoutInflater.from(getContext()).inflate(res, null));
        }

        @Override
        public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= mMessageList.size()) return 0;
            return mMessageList.get(position).type.ordinal();
        }
    }

    private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
