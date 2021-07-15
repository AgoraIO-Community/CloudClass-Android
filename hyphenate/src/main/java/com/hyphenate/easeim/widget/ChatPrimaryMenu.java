package com.hyphenate.easeim.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.interfaces.ChatPrimaryMenuListener;
import com.hyphenate.easeim.interfaces.IChatPrimaryMenu;
import com.hyphenate.util.EMLog;


public class ChatPrimaryMenu extends LinearLayout implements IChatPrimaryMenu, View.OnClickListener, InputEditText.OnEditTextChangeListener {

    protected Activity activity;
    private InputMethodManager inputManager;
    private LinearLayout rootBottom;
    private InputEditText etContent;
    private RelativeLayout faceView;
    private ImageView faceNormal;
    private ImageView faceCheck;
    private Button msgSend;
    private Handler handler = new Handler();

    private ChatPrimaryMenuListener chatMenuListener;

    public ChatPrimaryMenu(Context context) {
        this(context, null);
    }

    public ChatPrimaryMenu(Context context, @Nullable AttributeSet attrs) {
        this(context, null, 0);
    }

    public ChatPrimaryMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activity = (Activity)context;
        LayoutInflater.from(context).inflate(R.layout.chat_primary_menu, this);
        inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        initView();
    }


    private void initView(){
        rootBottom = findViewById(R.id.bottom_root);
        etContent = findViewById(R.id.edit_content);
        faceView = findViewById(R.id.view_face);
        faceNormal =findViewById(R.id.face_normal);
        faceCheck = findViewById(R.id.face_checked);
        msgSend = findViewById(R.id.btn_send);

        showNormalStatus();

        initListener();
    }

    private void initListener() {
        faceView.setOnClickListener(this);
        msgSend.setOnClickListener(this);
        etContent.setOnClickListener(this);
        etContent.setOnEditTextChangeListener(this);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.view_face) {
            if (faceNormal.getVisibility() == View.VISIBLE) {
                hideSoftKeyboard();
                etContent.requestFocus();
                //延时解决软键盘弹出闪屏的问题
                handler.postDelayed(new Runnable() {
                    public void run() {
                        showSelectedFaceImage();
                        chatMenuListener.onFaceViewClicked(false);
                    }
                }, 50);
            } else {
                showSoftKeyboard();
                chatMenuListener.onFaceViewClicked(true);
            }
        } else if (id == R.id.btn_send) {
            if (!etContent.getText().toString().isEmpty()) {
                chatMenuListener.onSendBtnClicked(etContent.getText().toString());
                hideSoftKeyboard();
                showNormalFaceImage();
                etContent.setText("");
            }
        } else if (id == R.id.edit_content){
            showTextStatus();
        }
    }

    public void showNormalFaceImage(){
        faceNormal.setVisibility(View.VISIBLE);
        faceCheck.setVisibility(View.INVISIBLE);
    }

    public void showSelectedFaceImage(){
        faceNormal.setVisibility(View.INVISIBLE);
        faceCheck.setVisibility(View.VISIBLE);
    }



    /***
     * 隐藏软键盘
     */
    public void hideSoftKeyboard() {
        if(etContent == null) {
            return;
        }
        etContent.requestFocus();
        if (activity.getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (activity.getCurrentFocus() != null)
                inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        etContent.clearFocus();
    }

    @Override
    public void showTextStatus() {
        showNormalFaceImage();
        showSoftKeyboard();
        chatMenuListener.showTextStatus();
    }

    @Override
    public void onEmojiconInputEvent(String emojiContent) {
        etContent.append(emojiContent);
    }

    @Override
    public void onEmojiconDeleteEvent() {
        if (!TextUtils.isEmpty(etContent.getText())) {
            KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
            etContent.dispatchKeyEvent(event);
        }
    }

    @Override
    public EditText getEditText() {
        return etContent;
    }


    /***
     * 显示软键盘
     */
    public void showSoftKeyboard() {
        if(etContent == null) {
            return;
        }
        etContent.requestFocus();
        inputManager.showSoftInput(etContent, InputMethodManager.SHOW_IMPLICIT);
    }

    public void showNormalStatus() {
        showNormalFaceImage();
        hideSoftKeyboard();
    }

    public void setChatPrimaryMenuListener(ChatPrimaryMenuListener chatMenuListener){
        this.chatMenuListener = chatMenuListener;
    }

    @Override
    public void onClickKeyboardSendBtn(String content) {
        if(!content.isEmpty()){
            chatMenuListener.onSendBtnClicked(content);
            hideSoftKeyboard();
            showNormalFaceImage();
            etContent.setText("");
        }
    }

    @Override
    public void onEditTextHasFocus(boolean hasFocus) {

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
    }
}
