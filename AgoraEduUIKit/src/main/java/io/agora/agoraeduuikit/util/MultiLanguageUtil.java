package io.agora.agoraeduuikit.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Locale;

import io.agora.agoraeducore.core.internal.base.http.AppHostUtil;

/**
 * author : wufang
 * date : 2022/8/17
 * description : 多语言工具类
 */
public class MultiLanguageUtil {

    public static Context attachBaseContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return createConfigurationResources(context);
        } else {
            setConfiguration(context);
            return context;
        }
    }

    /**
     * 设置语言
     *
     * @param context
     */
    public static void setConfiguration(Context context) {
        Locale appLocale = getAppLocale(context);

        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        Locale locale = null;
        String spLanguage = SpUtil.getString(context, AppHostUtil.LOCALE_LANGUAGE);
        String spCountry = SpUtil.getString(context, AppHostUtil.LOCALE_AREA);
        if (!TextUtils.isEmpty(spLanguage) && !TextUtils.isEmpty(spCountry)) {
            if (isSameLocal(appLocale, spLanguage, spCountry)) {
                locale = appLocale;
            } else {
                locale = new Locale(spLanguage, spCountry);
            }
        } else {
            locale = appLocale;
        }

        Configuration configuration = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);//语言更换生效的代码!
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context createConfigurationResources(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale appLocale = getAppLocale(context);

        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        Locale locale = null;
        String spLanguage = SpUtil.getString(context, AppHostUtil.LOCALE_LANGUAGE);
        String spCountry = SpUtil.getString(context, AppHostUtil.LOCALE_AREA);
        if (!TextUtils.isEmpty(spLanguage) && !TextUtils.isEmpty(spCountry)) {
            if (isSameLocal(appLocale, spLanguage, spCountry)) {
                locale = appLocale;
            } else {
                locale = new Locale(spLanguage, spCountry);
            }
        } else {
            locale = appLocale;
        }

        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }

    /**
     * 更改应用语言
     *
     * @param
     * @param locale      语言地区
     * @param persistence 是否持久化
     */
    public static void changeAppLanguage(Context context, Locale locale, boolean persistence) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new LocaleList(locale));
            context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
        resources.updateConfiguration(configuration, metrics);

        if (persistence) {
            saveLanguageSetting(context, locale);
        }
    }

    //保存多语言信息到sp中
    public static void saveLanguageSetting(Context context, Locale locale) {
        SpUtil.saveString(context, AppHostUtil.LOCALE_LANGUAGE, locale.getLanguage());
        SpUtil.saveString(context, AppHostUtil.LOCALE_AREA, locale.getCountry());
    }

    //获取本地应用的实际的多语言信息
    public static Locale getAppLocale(Context context) {
        //获取应用语言
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = configuration.getLocales().get(0);
        } else {
            locale = configuration.locale;
        }
        return locale;
    }

    //判断sp中和app中的多语言信息是否相同
    public static boolean isSameWithSetting(Context context) {
        Locale locale = getAppLocale(context);
        String language = locale.getLanguage();
        String country = locale.getCountry();

        String sp_language = SpUtil.getString(context, AppHostUtil.LOCALE_LANGUAGE);
        String sp_country = SpUtil.getString(context, AppHostUtil.LOCALE_AREA);
        if (language.equals(sp_language) && country.equals(sp_country)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSameLocal(Locale appLocale, String sp_language, String sp_country) {
        String appLanguage = appLocale.getLanguage();
        String appCountry = appLocale.getCountry();
        if (appLanguage.equals(sp_language) && appCountry.equals(sp_country)) {
            return true;
        } else {
            return false;
        }
    }
}
