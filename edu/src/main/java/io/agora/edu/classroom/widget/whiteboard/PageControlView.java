package io.agora.edu.classroom.widget.whiteboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.agora.edu.R2;
import androidx.cardview.widget.CardView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.edu.R;

public class PageControlView extends CardView {

    @BindView(R2.id.tv_page)
    protected TextView tv_page;

    private PageControlListener listener;

    public PageControlView(@NonNull Context context) {
        this(context, null);
    }

    public PageControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.layout_page_control, this);
        ButterKnife.bind(this);
    }

    public void setPageIndex(int index, int count) {
        tv_page.setText(String.format(Locale.getDefault(), "%d/%d", index + 1, count));
    }

    @OnClick({R2.id.iv_start, R2.id.iv_previous, R2.id.iv_next, R2.id.iv_end})
    public void onClick(View view) {
        if (listener == null) return;
        int id = view.getId();
        if (id == R.id.iv_start) {
            listener.toStart();
        } else if (id == R.id.iv_previous) {
            listener.toPrevious();
        } else if (id == R.id.iv_next) {
            listener.toNext();
        } else if (id == R.id.iv_end) {
            listener.toEnd();
        }
    }

    public void setListener(PageControlListener listener) {
        this.listener = listener;
    }

    public interface PageControlListener {
        void toStart();

        void toPrevious();

        void toNext();

        void toEnd();
    }

}
