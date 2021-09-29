package io.agora.agoraeduextapp;

import android.os.SystemClock;

import java.util.Formatter;
import java.util.Locale;

public class TimeUtil {
    private static long serverTsOfBase;
    private static long bootTimeOfBase;

    public static String stringForTimeHMS(long timeS, String formatStrHMS) {
        long seconds = timeS % 60;
        long minutes = timeS / 60 % 60;
        long hours = timeS / 3600;
        return new Formatter(new StringBuffer(), Locale.getDefault())
                .format(formatStrHMS, hours, minutes, seconds).toString();
    }

    public static void calibrateTimestamp(long serverTs) {
        serverTsOfBase = serverTs;
        bootTimeOfBase = SystemClock.elapsedRealtime();
    }

    public static long currentTimeMillis() {
        return serverTsOfBase + SystemClock.elapsedRealtime() - bootTimeOfBase;
    }
}