package io.agora.agoraeducore.core.internal.util;

import android.os.Looper;

public class ThreadUtil {
    public static boolean isMainThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }
}