package io.agora.agoraeducore.core.internal.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AppUtil {

    public static String getDeviceID(Context context) {
        // XXX according to the API docs, this value may change after factory reset
        // use Android id as device id
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static boolean checkAndRequestAppPermission(@NonNull Activity activity, String[] permissions, int reqCode) {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        List<String> permissionList = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED)
                permissionList.add(permission);
        }
        if (permissionList.size() == 0)
            return true;

        String[] requestPermissions = permissionList.toArray(new String[permissionList.size()]);
        activity.requestPermissions(requestPermissions, reqCode);
        return false;
    }

    /**
     * 防止按钮连续点击
     */
    private static long lastClickTime;

    public synchronized static boolean isFastClick() {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 700) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("simple text", text);
        cm.setPrimaryClip(mClipData);
    }

    public static void playWav(@NotNull Context context, int wavId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context.getApplicationContext(), wavId);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    /**
     * 使用系统API，根据url获得对应的MIME类型
     */
    public static String getMimeTypeFromUrl(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }

    public static int[] getDisplaySize(Activity activity) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(outMetrics);
        int widthPixel = outMetrics.widthPixels;
        int heightPixel = outMetrics.heightPixels;
        return new int[]{widthPixel, heightPixel};
    }

    /**
     * 获取虚拟导航栏高度 方法1
     */
    public static int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }
}
