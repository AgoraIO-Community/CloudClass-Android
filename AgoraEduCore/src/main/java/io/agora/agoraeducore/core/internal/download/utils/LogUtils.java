package io.agora.agoraeducore.core.internal.download.utils;

import android.util.Log;

import io.agora.agoraeducore.BuildConfig;


public class LogUtils {

    public static final String PREFIX = "Agora";
    private static boolean debug = BuildConfig.DEBUG;

    public static void setDebug(boolean b){
        debug = b;
    }

    @Deprecated
    public static void v(String msg) {
        if (!debug){
            return;
        }

        Log.v(getTAG(), msg);
    }

    @Deprecated
    public static void v(String TAG, String msg) {
        if (!debug){
            return;
        }
        Log.v(PREFIX + TAG, msg);
    }

    @Deprecated
    public static void d(String msg) {
        if (!debug){
            return;
        }
        Log.d(getTAG(), msg);
    }


    @Deprecated
    public static void d(String TAG, String msg) {
        if (!debug){
            return;
        }
        String NewTAG = PREFIX + TAG;
        Log.d(NewTAG, msg);
    }

    public static void i(String msg) {
        if (!debug){
            return;
        }
        Log.i(getTAG(), msg);
    }

    public static void i(String TAG, String msg) {
        if (!debug){
            return;
        }
        Log.i(PREFIX + TAG, msg);
    }


    public static void w(String msg) {
        if (!debug){
            return;
        }
        Log.w(getTAG(), msg);
    }

    public static void w(String TAG, String msg) {
        if (!debug){
            return;
        }
        Log.w(PREFIX + TAG, msg);
    }

    public static void e(String msg) {
        if (!debug){
            return;
        }
        Log.e(getTAG(), msg);
    }

    public static void e(String TAG, String msg) {
        if (!debug){
            return;
        }
        Log.e(PREFIX + TAG, msg);
    }

    public static void wtf(String msg){
        if (!debug){
            return;
        }
        Log.wtf(getTAG(), msg);
    }

    public static void wtf(String TAG, String msg){
        if (!debug){
            return;
        }
        Log.wtf(PREFIX + TAG, msg);
    }

    /**
     *  return default TAG, PREFIX + className + methodName() + lineNum;
     *
     * @return  eg : PREFIX MainActivity initData() 37
     *          eg : PREFIX MainActivity onClick() 58
     *          eg:  PREFIX MainActivity onClick() 69
     */
    private static String getTAG() {
        StringBuilder sb = new StringBuilder();

        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        sb.append(PREFIX + " ");

        StringBuilder classNameSb = new StringBuilder();

        for (StackTraceElement element : stackTrace){
            classNameSb.delete(0, classNameSb.length());
            classNameSb.append(element.getClassName());
            if (classNameSb.indexOf("io.agora.edu.core.internal.download")>=0
                    && !classNameSb.toString().contains("LogUtils")){
                int index = classNameSb.lastIndexOf(".");
                sb.append(classNameSb.subSequence(index+1, classNameSb.length()));
                sb.append(" ");
                sb.append(element.getMethodName() + "()");
                sb.append(" " + element.getLineNumber() + " ");
                break;
            }
        }

        return sb.toString();
    }

}
