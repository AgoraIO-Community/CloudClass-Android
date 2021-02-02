package io.agora.edu.classroom.widget.room;

import android.view.View;

import io.agora.edu.R;

public class ClassContainer {
    private final View mBaseLayout;

    private ClassTitleBar mClassTitleBar;

    public ClassContainer(View layout) {
        mBaseLayout = layout;
        init();
    }

    private void init() {
        mClassTitleBar = new ClassTitleBar(mBaseLayout
                .findViewById(R.id.classroom_title_bar_layout));
    }

    public ClassTitleBar getTitleBar() {
        return mClassTitleBar;
    }
}
