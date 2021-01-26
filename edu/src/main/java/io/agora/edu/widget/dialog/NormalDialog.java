package io.agora.edu.widget.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.NotNull;

import io.agora.edu.R;

public class NormalDialog extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "ClassNoEndExitDialog";
    private AppCompatTextView tv_dialog_cancel, tv_dialog_confirm;
    private AppCompatImageView close;

    @Nullable
    private DialogClickListener listener;
    @NotNull
    private String content, cancelText, confirmText;
    private int contentImgId;

    public NormalDialog(@NotNull String content, @NotNull String cancelText, @NotNull String confirmText,
                        int contentImgId) {
        this.content = content;
        this.cancelText = cancelText;
        this.confirmText = confirmText;
        this.contentImgId = contentImgId;
    }

    public NormalDialog(@NotNull String content, @NotNull String cancelText, @NotNull String confirmText,
                        int contentImgId, @Nullable DialogClickListener listener) {
        this.content = content;
        this.cancelText = cancelText;
        this.confirmText = confirmText;
        this.contentImgId = contentImgId;
        this.listener = listener;
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
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_normal_layout, container, false);
        close = root.findViewById(R.id.close);
        close.setOnClickListener(this);
        tv_dialog_cancel = root.findViewById(R.id.cancel);
        tv_dialog_cancel.setOnClickListener(this);
        tv_dialog_cancel.setText(cancelText);
        tv_dialog_confirm = root.findViewById(R.id.confirm);
        tv_dialog_confirm.setOnClickListener(this);
        tv_dialog_confirm.setText(confirmText);
        ((AppCompatImageView) root.findViewById(R.id.contentImg)).setImageResource(contentImgId);
        ((AppCompatTextView) root.findViewById(R.id.content)).setText(content);
        return root;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.close) {
        } else if (id == R.id.cancel) {
            if (listener != null)
                listener.onClick(false);
        } else if (id == R.id.confirm) {
            if (listener != null)
                listener.onClick(true);
        }
        dismiss();
    }

}
