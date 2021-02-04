package io.agora.edu.classroom.widget.whiteboard;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.edu.R;

public class ToolPopupDialog extends Dialog {
    public enum PopupType {
        pencil, text, rect, circle, eraser
    }

    private static final int FONT_GRID_SPAN = 3;

    private final int[] mColorIconResSelect = {
            R.drawable.tool_window_color_blue,
            R.drawable.tool_window_color_yellow,
            R.drawable.tool_window_color_red,
            R.drawable.tool_window_color_green,
            R.drawable.tool_window_color_black,
            R.drawable.tool_window_color_white
    };

    private final int[] mThicknessIconRes = {
            R.drawable.tool_window_thickness_1,
            R.drawable.tool_window_thickness_2,
            R.drawable.tool_window_thickness_3,
            R.drawable.tool_window_thickness_4,
    };

    private final int[] mPencilStyleRes = {
            R.drawable.tool_window_pencil_type_arrow,
            R.drawable.tool_window_pencil_type_line,
            R.drawable.tool_window_pencil_type_marker,
            R.drawable.tool_window_pencil_type_brush ,
    };

    // -11024391
    private static final int BLUE = Color.rgb(87, 199, 249);

    // -13055
    private static final int YELLOW = Color.rgb(255, 205, 1);

    // -1163718
    private static final int RED = Color.rgb(238, 62, 58);

    // -16595162
    private static final int GREEN = Color.rgb(2, 199, 38);

    // -15066598
    private static final int BLACK = Color.rgb(26, 26, 26);

    // -855310
    private static final int WHITE = Color.rgb(242, 242, 242);

    private final int[] mColors = {
            BLUE, YELLOW, RED, GREEN, BLACK, WHITE
    };

    public static int colorToIndex(int color) {
        switch (color) {
            case -11024391: return 0;
            case -13055: return 1;
            case -1163718: return 2;
            case -16595162: return 3;
            case -15066598: return 4;
            case -855310: return 5;
            default: return -1;
        }
    }

    private int mDialogPadding;

    private int mColorSelectIndex;
    private int mThicknessSelectIndex;
    private int mPencilStyleIndex;
    private int mFontSizeSelectIndex;

    private int mImageSpacing;
    private int mFontItemPadding;

    private String[] mFontSizes;

    private ColorAdapter mColorAdapter;
    private ThicknessAdapter mThicknessAdapter;
    private PencilStyleAdapter mPencilAdapter;
    private FontSizeAdapter mFontAdapter;

    private final ToolWindow.ToolWindowListener mListener;
    private ToolWindow.ToolConfig mConfig;

    public ToolPopupDialog(@NonNull Context context, View anchor, PopupType type,
                           @Nullable ToolWindow.ToolWindowListener listener,
                           @NonNull ToolWindow.ToolConfig config) {
        super(context, R.style.tool_window_dialog);
        setContentView(getLayoutByType(type));
        init(anchor, type);
        mListener = listener;
        setConfig(config);
    }

    private int getLayoutByType(PopupType type) {
        switch (type) {
            case text:
                return R.layout.tool_window_popup_font_layout;
            case rect:
            case circle:
                return R.layout.tool_window_popup_shape_layout;
            case eraser:
                return R.layout.tool_window_popup_eraser_layout;
            default: return R.layout.tool_window_popup_pencil_layout;
        }
    }

    private int getLayoutHeight(PopupType type) {
        int dimen;
        switch (type) {
            case text:
                dimen = R.dimen.tool_window_popup_height_font;
                break;
            case rect:
                dimen = R.dimen.tool_window_popup_height_shape;
                break;
            case circle:
                dimen = R.dimen.tool_window_popup_height_shape;
                break;
            case eraser:
                dimen = R.dimen.tool_window_popup_height_eraser;
                break;
            default:
                dimen = R.dimen.tool_window_popup_height_pencil;
                break;
        }
        return getContext().getResources().getDimensionPixelSize(dimen);
    }

    private void init(View anchor, PopupType type) {
        int mDialogWidth = getContext().getResources()
                .getDimensionPixelSize(R.dimen.tool_window_popup_width);
        mDialogPadding = getContext().getResources()
                .getDimensionPixelSize(R.dimen.tool_window_popup_margin_left);
        int height = getLayoutHeight(type);
        setDialogPosition(anchor, mDialogWidth, height);

        mFontSizes = getContext().getResources().getStringArray(R.array.tool_window_popup_font_size);
        mImageSpacing = getContext().getResources()
                .getDimensionPixelSize(R.dimen.tool_window_popup_image_item_spacing);
        mFontItemPadding = getContext().getResources()
                .getDimensionPixelSize(R.dimen.tool_window_popup_font_line_padding);
        initRecycler(type);
    }

    private void setDialogPosition(View anchor, int width, int height) {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        hideStatusBar(window);

        params.width = width;
        params.height = height;
        params.gravity = Gravity.TOP | Gravity.START;

        int[] locationsOnScreen = new int[2];
        anchor.getLocationOnScreen(locationsOnScreen);
        params.x = locationsOnScreen[0] + anchor.getMeasuredWidth() + mDialogPadding;
        params.y = locationsOnScreen[1] + (anchor.getMeasuredHeight() - height) / 2;

        window.setAttributes(params);
    }

    private void hideStatusBar(Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);

        int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        window.getDecorView().setSystemUiVisibility(flag |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void initRecycler(PopupType type) {
        if (type == PopupType.pencil || type == PopupType.text ||
            type == PopupType.rect || type == PopupType.circle) {
            RecyclerView colorRecycler = findViewById(R.id.tool_window_popup_recycler_color);
            colorRecycler.setLayoutManager(new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false));
            mColorAdapter = new ColorAdapter();
            colorRecycler.setAdapter(mColorAdapter);
        }

        if (type == PopupType.pencil || type == PopupType.rect ||
                type == PopupType.circle || type == PopupType.eraser) {
            RecyclerView thicknessRecycler = findViewById(R.id.tool_window_popup_recycler_thickness);
            thicknessRecycler.setLayoutManager(new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false));
            thicknessRecycler.addItemDecoration(new ToolWindowImageItemDecorator());
            mThicknessAdapter = new ThicknessAdapter();
            thicknessRecycler.setAdapter(mThicknessAdapter);
        }

        if (type == PopupType.pencil) {
            RecyclerView pencilStyleRecycler = findViewById(R.id.tool_window_popup_recycler_style);
            pencilStyleRecycler.setLayoutManager(new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false));
            pencilStyleRecycler.addItemDecoration(new ToolWindowImageItemDecorator());
            mPencilAdapter = new PencilStyleAdapter();
            pencilStyleRecycler.setAdapter(mPencilAdapter);
        }

        if (type == PopupType.text) {
            RecyclerView fontRecycler = findViewById(R.id.tool_window_popup_recycler_font_size);
            fontRecycler.setLayoutManager(new GridLayoutManager(getContext(), FONT_GRID_SPAN));
            fontRecycler.addItemDecoration(new FontItemDecorator());
            mFontAdapter = new FontSizeAdapter();
            fontRecycler.setAdapter(mFontAdapter);
        }
    }

    private void setConfig(ToolWindow.ToolConfig config) {
        mConfig = config;
        mColorSelectIndex = config.colorIndex;
        mThicknessSelectIndex = config.thicknessIndex;
        mPencilStyleIndex = config.pencilStyleIndex;
        mFontSizeSelectIndex = config.fontSizeIndex;
    }

    private class ColorAdapter extends RecyclerView.Adapter<ColorViewHolder> {
        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ColorViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.tool_window_popup_color_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            int pos = holder.getAdapterPosition();
            holder.icon.setImageResource(mColorIconResSelect[pos]);
            holder.position = pos;
            holder.itemView.setActivated(pos == mColorSelectIndex);
            holder.itemView.setOnClickListener(view -> {
                mColorSelectIndex = holder.position;
                mConfig.setColorIndex(mColorSelectIndex);
                if (mColorAdapter != null) mColorAdapter.notifyDataSetChanged();
                if (mListener != null) mListener.onColorSelected(mColors[holder.position]);
            });
        }

        @Override
        public int getItemCount() {
            return mColorIconResSelect.length;
        }
    }

    private static class ColorViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView icon;
        private int position;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tool_window_popup_recycler_icon);
        }
    }

    private class ThicknessAdapter extends RecyclerView.Adapter<ThicknessViewHolder> {
        @NonNull
        @Override
        public ThicknessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ThicknessViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.tool_window_popup_thickness_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ThicknessViewHolder holder, int position) {
            int pos = holder.getAdapterPosition();
            holder.icon.setImageResource(mThicknessIconRes[pos]);
            holder.position = pos;
            holder.itemView.setActivated(holder.position == mThicknessSelectIndex);
            holder.itemView.setOnClickListener(view -> {
                mThicknessSelectIndex = holder.position;
                mConfig.setThicknessIndex(mThicknessSelectIndex);
                if (mThicknessAdapter != null) mThicknessAdapter.notifyDataSetChanged();
                if (mListener != null) mListener.onThicknessSelected(holder.position);
            });
        }

        @Override
        public int getItemCount() {
            return mThicknessIconRes.length;
        }
    }

    private static class ThicknessViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView icon;
        private int position;

        public ThicknessViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tool_window_popup_recycler_icon);
        }
    }

    private class ToolWindowImageItemDecorator extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildAdapterPosition(view);
            int count = parent.getChildCount();
            outRect.left = mImageSpacing;
            outRect.right = mImageSpacing;

            if (position == 0) {
                outRect.left = 0;
            } else if (position == count - 1) {
                outRect.right = 0;
            }
        }
    }

    private class PencilStyleAdapter extends RecyclerView.Adapter<PencilStyleViewHolder> {
        @NonNull
        @Override
        public PencilStyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PencilStyleViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.tool_window_popup_pencil_style_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PencilStyleViewHolder holder, int position) {
            int pos = holder.getAdapterPosition();
            holder.position = pos;
            holder.icon.setImageResource(mPencilStyleRes[pos]);
            holder.itemView.setActivated(mPencilStyleIndex == pos);
            holder.itemView.setOnClickListener(view -> {
                mPencilStyleIndex = holder.position;
                mConfig.setPencilIndex(mPencilStyleIndex);
                if (mPencilAdapter != null) mPencilAdapter.notifyDataSetChanged();
                if (mListener != null) mListener.onPencilStyleSelected(holder.position);
            });
        }

        @Override
        public int getItemCount() {
            return mPencilStyleRes.length;
        }
    }

    private static class PencilStyleViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView icon;
        private int position;

        public PencilStyleViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tool_window_popup_recycler_icon);
        }
    }

    private class FontSizeAdapter extends RecyclerView.Adapter<FontSizeViewHolder> {
        @NonNull
        @Override
        public FontSizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FontSizeViewHolder(LayoutInflater.from(getContext())
                    .inflate(R.layout.tool_window_popup_font_size_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FontSizeViewHolder holder, int position) {
            int pos = holder.getAdapterPosition();
            holder.font.setText(mFontSizes[pos]);
            holder.position = pos;
            holder.itemView.setActivated(mFontSizeSelectIndex == pos);
            holder.itemView.setOnClickListener(view -> {
                mFontSizeSelectIndex = holder.position;
                mConfig.seFontSizeIndex(mFontSizeSelectIndex);
                if (mFontAdapter != null) mFontAdapter.notifyDataSetChanged();
                if (mListener != null) mListener.onFontSizeSelected(holder.position);
            });
        }

        @Override
        public int getItemCount() {
            return mFontSizes.length;
        }
    }

    private static class FontSizeViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView font;
        private int position;

        public FontSizeViewHolder(@NonNull View itemView) {
            super(itemView);
            font = itemView.findViewById(R.id.tool_window_popup_recycler_font_size);
        }
    }

    private class FontItemDecorator extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);

            int position = parent.getChildAdapterPosition(view);
            int size = mFontSizes.length;

            if (FONT_GRID_SPAN <= position && (size - position + 1) > FONT_GRID_SPAN) {
                outRect.top = mFontItemPadding;
                outRect.bottom = mFontItemPadding;
            }

            if ((position + 1) % FONT_GRID_SPAN != 0) {
                outRect.right = mFontItemPadding;
            }
        }
    }
 }
