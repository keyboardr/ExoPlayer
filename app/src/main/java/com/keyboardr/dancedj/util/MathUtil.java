package com.keyboardr.dancedj.util;

/**
 * Created by keyboardr on 6/24/16.
 */

public class MathUtil {
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}
