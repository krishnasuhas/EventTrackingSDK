package com.mobilewalla.eventtracking.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.mobilewalla.eventtracking.api.Constants;
import com.mobilewalla.eventtracking.api.MobilewallaLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    private static final String TAG = Utils.class.getName();

    private static final MobilewallaLog logger = MobilewallaLog.getLogger();

    /**
     * Do a shallow copy of a JSONObject. Takes a bit of code to avoid
     * stringify and reparse given the API.
     */
    public static JSONObject cloneJSONObject(final JSONObject obj) {
        if (obj == null) {
            return null;
        }

        if (obj.length() == 0) {
            return new JSONObject();
        }

        // obj.names returns null if the json obj is empty.
        JSONArray nameArray = null;
        try {
            nameArray = obj.names();
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.e(TAG, e.toString());
        }
        int len = (nameArray != null ? nameArray.length() : 0);

        String[] names = new String[len];
        for (int i = 0; i < len; i++) {
            names[i] = nameArray.optString(i);
        }

        try {
            return new JSONObject(obj, names);
        } catch (JSONException e) {
            logger.e(TAG, e.toString());
            return null;
        }
    }

    public static boolean isEmptyString(String s) {
        return (s == null || s.length() == 0);
    }

    public static String normalizeInstanceName(String instance) {
        if (isEmptyString(instance)) {
            instance = Constants.DEFAULT_INSTANCE;
        }
        return instance.toLowerCase();
    }

    public static boolean checkLocationPermissionAllowed(Context context) {
        return checkPermissionAllowed(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                checkPermissionAllowed(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    static boolean checkPermissionAllowed(Context context, String permission) {
        // ANDROID 6.0 AND UP!
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean hasPermission = false;
            try {
                // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
                java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", String.class);
                Object resultObj = methodCheckPermission.invoke(context, permission);
                int result = Integer.parseInt(resultObj.toString());
                hasPermission = (result == PackageManager.PERMISSION_GRANTED);
            } catch (Exception ex) {

            }

            return hasPermission;
        } else {
            return true;
        }
    }
}
