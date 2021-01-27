package io.agora.edu.classroom.widget.window;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IMinimizable {
    enum Direction {
        top, right, bottom, left,
        topLeft, topRight, bottomLeft, bottomRight
    }

    void startMinimize();

    void restoreMinimize();

    void cancelAnimate();

    boolean isMinimized();

    void setLayouts(@NonNull View original, @Nullable View minimize);
}
