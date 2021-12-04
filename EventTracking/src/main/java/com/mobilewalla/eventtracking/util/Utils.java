package com.mobilewalla.eventtracking.util;

import static com.mobilewalla.eventtracking.client.MobilewallaClient.getMapper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mobilewalla.eventtracking.log.LogUtil;
import com.mobilewalla.eventtracking.model.Event;
import com.mobilewalla.eventtracking.models.Response;

import org.json.JSONObject;

public class Utils {
    public static boolean isEmptyString(String s) {
        LogUtil.i("");
        return (s == null || s.length() == 0);
    }

    public static boolean checkLocationPermissionAllowed(Context context) {
        return checkPermissionAllowed(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                checkPermissionAllowed(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static String getString(JSONObject properties) {
        if (properties != null) return properties.toString();
        else return null;
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

    public static boolean isDeviceConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static Response getResponse(JSONObject response) {
        try {
            return getMapper().readValue(response.toString(), Response.class);
        } catch (Exception e) {
            return new Response("", e.getMessage());
        }
    }

    public static JSONObject getJsonRequest(Event e) {
        try {
            return new JSONObject(getMapper().writeValueAsString(e));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }
}
