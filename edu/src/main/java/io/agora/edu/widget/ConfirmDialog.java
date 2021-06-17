package io.agora.edu.widget;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.edu.R;
import io.agora.edu.R2;

public class ConfirmDialog extends DialogFragment {

    @BindView(R2.id.tv_content)
    protected TextView tv_content;
    @BindView(R2.id.tv_dialog_cancel)
    protected TextView tv_dialog_cancel;
    @BindView(R2.id.tv_dialog_confirm)
    protected TextView tv_dialog_confirm;
    @BindView(R2.id.line2)
    protected View line2;

    private String content;
    private String cancelText;
    private String confirmText;
    private boolean isSingle;
    @Nullable
    protected DialogClickListener listener;

    public static ConfirmDialog normal(String content, DialogClickListener listener) {
        ConfirmDialog fragment = new ConfirmDialog();
        fragment.content = content;
        fragment.isSingle = false;
        fragment.listener = listener;
        fragment.setCancelable(true);
        return fragment;
    }

    public static ConfirmDialog normalWithButton(String content, String cancelText, String confirmText, DialogClickListener listener) {
        ConfirmDialog fragment = ConfirmDialog.normal(content, listener);
        fragment.cancelText = cancelText;
        fragment.confirmText = confirmText;
        return fragment;
    }

    public static ConfirmDialog single(String content, DialogClickListener listener) {
        ConfirmDialog fragment = ConfirmDialog.normal(content, listener);
        fragment.isSingle = true;
        return fragment;
    }

    public static ConfirmDialog singleWithButton(String content, String confirmText, DialogClickListener listener) {
        ConfirmDialog fragment = ConfirmDialog.single(content, listener);
        fragment.confirmText = confirmText;
        return fragment;
    }

    public ConfirmDialog setCancel(boolean cancelable) {
        this.setCancelable(cancelable);
        return this;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if (listener != null)
            listener.onClick(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_dialog_confirm, container, false);
        ButterKnife.bind(this, root);
        tv_content.setText(content);
        if (!TextUtils.isEmpty(cancelText))
            tv_dialog_cancel.setText(cancelText);
        if (!TextUtils.isEmpty(confirmText))
            tv_dialog_confirm.setText(confirmText);
        tv_dialog_cancel.setVisibility(isSingle ? View.GONE : View.VISIBLE);
        line2.setVisibility(isSingle ? View.GONE : View.VISIBLE);
        return root;
    }

    @OnClick({R2.id.tv_dialog_cancel, R2.id.tv_dialog_confirm})
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tv_dialog_cancel) {
            if (listener != null)
                listener.onClick(false);
        } else if (id == R.id.tv_dialog_confirm) {
            if (listener != null)
                listener.onClick(true);
        }
        if (isCancelable()) {
            dismiss();
        }
    }

    public void setConfirmText(String text) {
        if(tv_dialog_confirm != null) {
            tv_dialog_confirm.setText(text);
        }
    }

    public interface DialogClickListener {
        void onClick(boolean confirm);
    }

}
