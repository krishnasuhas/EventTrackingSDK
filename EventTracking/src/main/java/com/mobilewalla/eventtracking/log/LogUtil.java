package com.mobilewalla.eventtracking.log;

import android.util.Log;

public class LogUtil {
    public static final String TAG = "LOG_UTIL";

    public static void i(String message) {
        Log.i(TAG, message);
    }
}
