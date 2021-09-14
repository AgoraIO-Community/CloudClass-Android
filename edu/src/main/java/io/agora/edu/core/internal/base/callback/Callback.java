package io.agora.edu.core.internal.base.callback;

import androidx.annotation.Nullable;

public interface Callback<T> {
    void onSuccess(@Nullable T res);
}
