package io.agora.edu.classroom.widget.whiteboard;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.edu.R;
import io.agora.edu.classroom.widget.window.AbstractWindow;

public class ToolWindow extends AbstractWindow {
    private static final String TAG = ToolWindow.class.getSimpleName();
    private static final int FOLD_DURATION = 500;

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
    private boolean mFold;
    private int mToolRecyclerHeight;

    private ToolWindowListener mListener;
    private AppCompatImageView mFoldIcon;
    private FrameLayout mIconRecyclerLayout;
    private RecyclerView mIconRecycler;

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

        mIconRecyclerLayout = findViewById(R.id.tool_window_option_layout);
        mIconRecycler = findViewById(R.id.tool_window_option_recycler);
        mIconRecycler.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, false));
        mIconRecycler.setAdapter(new ToolWindowOptionAdapter());

        mFoldIcon = findViewById(R.id.tool_window_fold_icon);
        if (mFold) {
            mFoldIcon.setImageResource(R.drawable.tool_window_icon_unfold);
        } else {
            mFoldIcon.setImageResource(R.drawable.tool_window_icon_fold);
        }

        findViewById(R.id.tool_window_fold_icon_layout).setOnClickListener(view -> {
           if (mFold) unfold(); else fold();
        });
    }

    private void fold() {
        mToolRecyclerHeight = mIconRecyclerLayout.getHeight();
        ViewPropertyAnimator animator = mIconRecycler.animate();
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mFoldIcon.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mFoldIcon.setEnabled(true);
                mFold = !mFold;
                mFoldIcon.setImageResource(R.drawable.tool_window_icon_unfold);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mFoldIcon.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animator.setUpdateListener(valueAnimator -> {
            float fraction = valueAnimator.getAnimatedFraction();
            int curHeight = (int) (mToolRecyclerHeight * (1 - fraction));
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) mIconRecyclerLayout.getLayoutParams();
            params.height = curHeight;
            mIconRecyclerLayout.setLayoutParams(params);
        });

        animator.setDuration(FOLD_DURATION);
        animator.yBy(-mIconRecycler.getHeight());
    }

    private void unfold() {
        ViewPropertyAnimator animator = mIconRecycler.animate();
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mFoldIcon.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mFoldIcon.setEnabled(true);
                mFold = !mFold;
                mFoldIcon.setImageResource(R.drawable.tool_window_icon_fold);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mFoldIcon.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animator.setUpdateListener(valueAnimator -> {
            float fraction = valueAnimator.getAnimatedFraction();
            int curHeight = (int) (mToolRecyclerHeight * fraction);
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) mIconRecyclerLayout.getLayoutParams();
            params.height = curHeight;
            mIconRecyclerLayout.setLayoutParams(params);
        });

        animator.setDuration(FOLD_DURATION);
        animator.yBy(mToolRecyclerHeight);
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
