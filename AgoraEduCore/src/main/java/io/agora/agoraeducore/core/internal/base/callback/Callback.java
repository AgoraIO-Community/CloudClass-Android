package io.agora.agoraeducore.core.internal.base.callback;

import androidx.annotation.Nullable;

public interface Callback<T> {
    void onSuccess(@Nullable T res);
}
