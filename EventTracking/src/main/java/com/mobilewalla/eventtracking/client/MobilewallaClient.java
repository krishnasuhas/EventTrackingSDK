package com.mobilewalla.eventtracking.client;

import android.content.Context;

import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilewalla.eventtracking.dao.EventDao;
import com.mobilewalla.eventtracking.data.AppDatabase;
import com.mobilewalla.eventtracking.model.DeviceDetails;
import com.mobilewalla.eventtracking.models.ErrorCallback;
import com.mobilewalla.eventtracking.models.ResponseCallback;
import com.mobilewalla.eventtracking.service.MobilewallaServiceImpl;

import org.json.JSONObject;

public class MobilewallaClient {
    private static MobilewallaClient client;
    private static Context context;
    private static MobilewallaServiceImpl service;
    private static AppDatabase database;
    private static EventDao eventDao;
    private static DeviceDetails deviceDetails;
    private static ObjectMapper mapper;
    private static ResponseCallback<JSONObject> callback;
    private static ErrorCallback<VolleyError> errorCallback;
    private RequestQueue requestQueue;

    private MobilewallaClient(Context context, final String userId, final String platform) {
        MobilewallaClient.context = context;
        getMapper();
        getAppDatabase();
        getEventDao();
        getDeviceInfo();
        getRequestQueue();
        getCallback();
        getErrorCallback();
        getService().setUserId(userId);
        getService().initializeDeviceId();
        getService().setPlatform(platform);
    }

    public static MobilewallaClient getClient() {
        return client;
    }

    public static synchronized MobilewallaClient initialize(Context context) {
        return initialize(context, null);
    }

    public static synchronized MobilewallaClient initialize(Context context, final String userId) {
        return initialize(context, userId, null);
    }

    public static synchronized MobilewallaClient initialize(Context context, final String userId, final String platform) {
        if (client == null) {
            client = new MobilewallaClient(context, userId, platform);
        }
        return client;
    }

    public static synchronized MobilewallaClient initialize(Context context, ResponseCallback<JSONObject> callback, ErrorCallback<VolleyError> errorCallback) {
        if (client == null) {
            MobilewallaClient.callback = callback;
            MobilewallaClient.errorCallback = errorCallback;
            client = new MobilewallaClient(context, null, null);
        }
        return client;
    }

    public static MobilewallaServiceImpl getService() {
        if (service == null) {
            service = new MobilewallaServiceImpl();
        }
        return service;
    }

    public static Context getContext() {
        return context;
    }

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }

    private static AppDatabase getAppDatabase() {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, "mobilewalla")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return database;
    }

    public static EventDao getEventDao() {
        if (eventDao == null) {
            eventDao = getAppDatabase().eventDao();
        }
        return eventDao;
    }

    public static DeviceDetails getDeviceInfo() {
        if (deviceDetails == null) {
            deviceDetails = new DeviceDetails(true);
            deviceDetails.prefetch();
        }
        return deviceDetails;
    }

    public static ResponseCallback<JSONObject> getCallback() {
        if (callback == null) {
            callback = response -> {
            };
        }
        return callback;
    }

    public static ErrorCallback<VolleyError> getErrorCallback() {
        if (errorCallback == null) {
            errorCallback = error -> {
            };
        }
        return errorCallback;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}