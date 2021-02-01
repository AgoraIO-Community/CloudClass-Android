package io.agora.edu.classroom.widget.whiteboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.edu.R;
import io.agora.edu.classroom.widget.window.AbstractWindow;

public class ToolWindow extends AbstractWindow {
    public interface ToolWindowListener {
        void onModeChanged(int mode);

        void onColorSelected(int rgb);

        void onThicknessSelected(int thicknessIndex);

        void onPencilStyleSelected(int styleIndex);

        void onFontSizeSelected(int fontIndex);
    }

    private final int[] mOptionIconRes = {
            R.drawable.tool_window_icon_arrow,
            R.drawable.tool_window_icon_pencil,
            R.drawable.tool_window_icon_text,
            R.drawable.tool_window_icon_rect,
            R.drawable.tool_window_icon_circle,
            R.drawable.tool_window_icon_eraser,
    };

    private final ToolPopupDialog.PopupType[] mPopupTypes = {
            ToolPopupDialog.PopupType.pencil,
            ToolPopupDialog.PopupType.text,
            ToolPopupDialog.PopupType.rect,
            ToolPopupDialog.PopupType.circle,
            ToolPopupDialog.PopupType.eraser,
    };

    private int mSelectedOption = -1;
    private ToolWindowListener mListener;

    public ToolWindow(Context context) {
        super(context);
        init();
    }

    public ToolWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToolWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.tool_window_layout, this);

        RecyclerView optionRecycler = findViewById(R.id.tool_window_option_recycler);
        optionRecycler.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, false));
        optionRecycler.setAdapter(new ToolWindowOptionAdapter());
    }

    private class ToolWindowOptionAdapter extends RecyclerView.Adapter<ToolWindowOptionViewHolder> {
        @NonNull
        @Override
        public ToolWindowOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ToolWindowOptionViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.tool_window_option_item_layout, ToolWindow.this, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ToolWindowOptionViewHolder holder, int position) {
            final int pos = holder.getAdapterPosition();
            holder.icon.setImageResource(mOptionIconRes[pos]);
            holder.position = pos;
            holder.itemView.setActivated(mSelectedOption == pos);
            holder.itemView.setOnClickListener(view -> {
                if (mSelectedOption == holder.position) {
                    if (holder.position != 0 && mListener != null) {
                        popupWindowIfNeeded(mSelectedOption, holder.itemView, mListener);
                    }
                } else {
                    mSelectedOption = holder.position;
                    if (mListener != null) mListener.onModeChanged(holder.position);
                }

                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return mOptionIconRes.length;
        }

        private void popupWindowIfNeeded(int position, View view,
                                         @Nullable ToolWindowListener listener) {
            if (position == 0) {
                // Arrow option does not need to pop up a window
                return;
            }

            ToolPopupDialog.PopupType type;
            switch (position) {
                case 2: type = ToolPopupDialog.PopupType.text; break;
                case 3: type = ToolPopupDialog.PopupType.rect; break;
                case 4: type = ToolPopupDialog.PopupType.circle; break;
                case 5: type = ToolPopupDialog.PopupType.eraser; break;
                default: type = ToolPopupDialog.PopupType.pencil; break;
            }

            new ToolPopupDialog(getContext(), view, type, listener).show();
        }
    }

    private static class ToolWindowOptionViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView icon;
        AppCompatImageView arrow;
        int position;

        public ToolWindowOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tool_window_option_item_icon);
            arrow = itemView.findViewById(R.id.tool_window_option_item_arrow);
        }
    }

    public void setListener(ToolWindowListener listener) {
        mListener = listener;
    }
}
