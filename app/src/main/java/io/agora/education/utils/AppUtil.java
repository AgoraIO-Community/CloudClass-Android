package io.agora.education.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.base.http.AppHostUtil;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion;
import io.agora.agoraeduuikit.util.MultiLanguageUtil;
import io.agora.agoraeduuikit.util.SpUtil;
import io.agora.education.R;
import io.agora.education.config.AppConstants;

public class AppUtil {
    public static boolean isChina = true;

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

    public synchronized static boolean isFastClick(long interval) {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < interval) {
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
        ClipData mClipData = ClipData.newPlainText("agora", text);
        cm.setPrimaryClip(mClipData);
    }

    public static void hideStatusBar(@Nullable Window window, boolean darkText) {
        if (window == null) return;
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);

        int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && darkText) {
            flag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }

        window.getDecorView().setSystemUiVisibility(flag |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean isTabletDevice(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    static SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public static String getRoomDate(long startTime, long endTime) {
        Date start = new Date(startTime);
        Date end = new Date(endTime);
        return yearFormat.format(start) + "," + timeFormat.format(start) + "-" + timeFormat.format(end);
    }

    public static String getRoomDate2(Context context, long startTime, long duration) {
        Date start = new Date(startTime);
        return yearFormat.format(start) + "," + timeFormat.format(start) + "  |  " + duration / 60 + context.getString(R.string.fcr_duration_time_unit);
    }

    /**
     * 获取当前apk的版本名
     *
     * @param context 上下文
     * @return
     */
    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionName
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static void setDefLanguageForSys(Context context) { // 如果非中文，设置默认为na区域和英文
        String language = SpUtil.getString(context, AppConstants.LOCALE_LANGUAGE);
        if (TextUtils.isEmpty(language)) {
            Locale locale = MultiLanguageUtil.getAppLocale(context);
            // zh_CN ||  zh_TW
            String sysLanguage = locale.getLanguage(); // zh
            String sysCountry = locale.getCountry(); // CN

            if (sysLanguage.equalsIgnoreCase(Locale.SIMPLIFIED_CHINESE.getLanguage()) &&
                    sysCountry.equalsIgnoreCase(Locale.SIMPLIFIED_CHINESE.getCountry())) {
                // zh_CN
            } else {
                Log.e("Application", "system language is not zh_CN, set default :en-US");
                Locale localeX = new Locale(Locale.US.getLanguage(), Locale.US.getCountry()); // en-US
                SpUtil.saveString(context, AppHostUtil.LOCALE_LANGUAGE, localeX.getLanguage());
                SpUtil.saveString(context, AppHostUtil.LOCALE_AREA, localeX.getCountry());
            }
        }
    }

    public static void setDefaultRegin(Context context) { // 如果非中文，设置默认为na区域和英文
        String language = SpUtil.getString(context, AppConstants.LOCALE_LANGUAGE);
        if (TextUtils.isEmpty(language)) {
            Locale locale = MultiLanguageUtil.getAppLocale(context);
            // zh_CN ||  zh_TW
            String sysLanguage = locale.getLanguage(); // zh
            String sysCountry = locale.getCountry(); // CN

            if (sysLanguage.equalsIgnoreCase(Locale.SIMPLIFIED_CHINESE.getLanguage()) &&
                    sysCountry.equalsIgnoreCase(Locale.SIMPLIFIED_CHINESE.getCountry())) {
                // zh_CN
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn);
            } else {
                Log.e("Application", "system language is not zh_CN, set default :en-US");
                PreferenceManager.put(AppConstants.KEY_SP_REGION, AgoraEduRegion.na);
            }
        }
    }
}
