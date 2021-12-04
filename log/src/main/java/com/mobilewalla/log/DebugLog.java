package com.mobilewalla.log;

import android.util.Log;

public class DebugLog {
    public static final String TAG = "DEBUG_LOG";

    public static void d(String message) {
        Log.d(TAG, message);
    }
}
