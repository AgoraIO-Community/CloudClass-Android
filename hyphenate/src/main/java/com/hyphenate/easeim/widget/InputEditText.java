package com.hyphenate.easeim.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

public class InputEditText extends androidx.appcompat.widget.AppCompatEditText implements TextView.OnEditorActionListener {
    private OnEditTextChangeListener listener;

    public InputEditText(Context context) {
        this(context, null);
    }

    public InputEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnEditorActionListener(this);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (listener != null) {
            listener.onEditTextHasFocus(focused);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_DONE
                || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
            String s = getText().toString();
            if(listener != null) {
                listener.onClickKeyboardSendBtn(s);
            }
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * 设置监听
     * @param listener
     */
    public void setOnEditTextChangeListener(OnEditTextChangeListener listener) {
        this.listener = listener;
    }

    public interface OnEditTextChangeListener {

        /**
         * when send button clicked
         * @param content
         */
        void onClickKeyboardSendBtn(String content);

        /**
         * if edit text has focus
         */
        void onEditTextHasFocus(boolean hasFocus);
    }
}

