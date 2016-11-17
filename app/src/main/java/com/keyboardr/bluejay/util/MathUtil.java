package com.keyboardr.bluejay.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by keyboardr on 6/24/16.
 */

public class MathUtil {
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static CharSequence getSongDuration(long millis) {
        return getSongDuration(millis, false);
    }

    public static CharSequence getSongDuration(long millis, boolean forceHours) {
        long hrs = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (forceHours || hrs > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hrs, mins, seconds);
        } else {
            return String.format(Locale.US, "%d:%02d", mins, seconds);
        }
    }
}
