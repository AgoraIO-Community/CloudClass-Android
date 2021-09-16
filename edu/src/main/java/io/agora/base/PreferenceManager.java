package io.agora.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PreferenceManager {
    private static final Object sLock = new Object();
    private volatile static SharedPreferences sharedPreferences;
    private volatile static int sInitCallCnt = 0;
    private volatile static int sNullFoundCnt = 0;
    private volatile static int sNullAfterCreateCnt = 0;

    public static void init(@NonNull Context context) {
        synchronized (sLock) {
            sInitCallCnt++;
            if (sharedPreferences == null) {
                sNullFoundCnt++;
                sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                if (sharedPreferences == null) {
                    sNullAfterCreateCnt++;
                }
            }
        }
    }

    public static void put(@NonNull String key, @Nullable Object value) {
        SharedPreferences sp = getSharedPreferences();
        if (value instanceof Boolean) {
            sp.edit().putBoolean(key, (Boolean) value).apply();
        } else if (value instanceof Integer) {
            sp.edit().putInt(key, (Integer) value).apply();
        } else if (value instanceof String) {
            sp.edit().putString(key, (String) value).apply();
        } else if (value instanceof Float) {
            sp.edit().putFloat(key, (Float) value).apply();
        } else if (value instanceof Long) {
            sp.edit().putLong(key, (Long) value).apply();
        }
    }

    public static <T> T get(@NonNull String key, @Nullable T defaultValue) {
        Object result;
        SharedPreferences sp = getSharedPreferences();
        if (defaultValue instanceof Boolean) {
            result = sp.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Integer) {
            result = sp.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof String) {
            result = sp.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Float) {
            result = sp.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Long) {
            result = sp.getLong(key, (Long) defaultValue);
        } else {
            return null;
        }
        return (T) result;
    }

    private static SharedPreferences getSharedPreferences() throws IllegalStateException {
        if (sharedPreferences == null) {
            throw new IllegalStateException("PreferenceManager is not initialized. " +
                    "Please call init() before use! " +
                    "init call " + sInitCallCnt +
                    ", null found " + sNullFoundCnt +
                    ", null after create " + sNullAfterCreateCnt +
                    ", main thread " + isMainThread());
        }

        return sharedPreferences;
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
