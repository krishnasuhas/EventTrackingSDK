package com.mobilewalla.eventtracking.service;

import static com.mobilewalla.eventtracking.client.MobilewallaClient.getCallback;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getClient;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getContext;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getDeviceInfo;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getErrorCallback;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getEventDao;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getMapper;
import static com.mobilewalla.eventtracking.client.MobilewallaClient.getService;
import static com.mobilewalla.eventtracking.util.Utils.getJsonRequest;
import static com.mobilewalla.eventtracking.util.Utils.getResponse;
import static com.mobilewalla.eventtracking.util.Utils.getString;
import static com.mobilewalla.eventtracking.util.Utils.isDeviceConnected;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mobilewalla.eventtracking.model.Event;
import com.mobilewalla.eventtracking.models.Request;
import com.mobilewalla.eventtracking.models.Response;
import com.mobilewalla.eventtracking.models.ResponseCallback;
import com.mobilewalla.eventtracking.util.Constants;
import com.mobilewalla.eventtracking.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class MobilewallaServiceImpl implements MobilewallaService {
    private static final String POST_AUTHENTICATE = "POST_AUTHENTICATE";
    private static final String POST_EVENT = "POST_EVENT";

    private static final String KREDIVO_BASE_URL = "https://kredivo-sdk-api-dev.mobilewalla.com/kredivo-v1/";
    private static String TOKEN = "";
    protected String userId;
    protected String eventId;
    protected String deviceId;
    protected String platform;
    long sessionId = -1;
    private boolean newDeviceIdPerInstall = false;
    private boolean useAdvertisingIdForDeviceId = false;
    private boolean useAppSetIdForDeviceId = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = Utils.isEmptyString(platform) ? Constants.PLATFORM : platform;
    }

    public boolean isNewDeviceIdPerInstall() {
        return newDeviceIdPerInstall;
    }

    public void setNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        this.newDeviceIdPerInstall = newDeviceIdPerInstall;
    }

    public boolean isUseAdvertisingIdForDeviceId() {
        return useAdvertisingIdForDeviceId;
    }

    public void setUseAdvertisingIdForDeviceId(boolean useAdvertisingIdForDeviceId) {
        this.useAdvertisingIdForDeviceId = useAdvertisingIdForDeviceId;
    }

    public boolean isUseAppSetIdForDeviceId() {
        return useAppSetIdForDeviceId;
    }

    public void setUseAppSetIdForDeviceId(boolean useAppSetIdForDeviceId) {
        this.useAppSetIdForDeviceId = useAppSetIdForDeviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private void authenticate(ResponseCallback<Response> callback) {
        Listener<JSONObject> listener = response -> {
            Log.d(POST_AUTHENTICATE, "Received response from server");
            Response authenticateResponse = getResponse(response);
            TOKEN = authenticateResponse.getToken();
            callback.onResponse(authenticateResponse);
        };
        ErrorListener errorListener = error -> Log.e(POST_AUTHENTICATE, "Error when called an api : " + error.getLocalizedMessage());

        try {
            JSONObject jsonRequest = new JSONObject(getMapper().writeValueAsString(new Request("user", "password")));
            JsonObjectRequest request = new JsonObjectRequest(KREDIVO_BASE_URL + "authentication", jsonRequest, listener, errorListener);
            getClient().addToRequestQueue(request);
            Log.d(POST_AUTHENTICATE, "Sent an authentication request");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void logEvent(String eventType) {
        logEvent(eventType, null);
    }

    @Override
    public void logEvent(String eventType, JSONObject eventProperties) {
        logEvent(eventType, eventProperties, null);
    }

    @Override
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject globalUserProperties) {
        logEvent(eventType, eventProperties, null, globalUserProperties, null);
    }

    @Override
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject userProperties, JSONObject globalUserProperties, JSONObject groupProperties) {
        logEvent(eventType, -1, eventProperties, userProperties, globalUserProperties, groupProperties);
    }

    @Override
    public void logEvent(String eventType, long eventId, JSONObject eventProperties, JSONObject userProperties, JSONObject globalUserProperties, JSONObject groupProperties) {
        try {
            Event event = new Event();

            event.setUserId(userId);
            event.setEventId(eventId);
            event.setEventType(eventType);
            event.setEventProperties(getString(eventProperties));
            event.setUserProperties(getString(userProperties));
            event.setGlobalUserProperties(getString(globalUserProperties));
            event.setGroupProperties(getString(groupProperties));

//            event.setApp(Integer.parseInt(getDeviceInfo().getAppSetId()));
            event.setDeviceId(deviceId);
            event.setSessionId(sessionId);
            event.setVersionName(getDeviceInfo().getVersionName());
            event.setPlatform(platform);
            event.setOsName(getDeviceInfo().getOsName());
            event.setOsVersion(getDeviceInfo().getOsVersion());
            event.setDeviceBrand(getDeviceInfo().getBrand());
            event.setDeviceManufacturer(getDeviceInfo().getManufacturer());
            event.setDeviceModel(getDeviceInfo().getModel());
//            event.setDeviceFamily(getDeviceInfo().getDeviceFamily());
//            event.setDeviceType(getDeviceInfo().getDeviceType());
            event.setDeviceCarrier(getDeviceInfo().getCarrier());
            if (getDeviceInfo().getMostRecentLocation() != null) {
                event.setLatitude(getDeviceInfo().getMostRecentLocation().getLatitude());
                event.setLongitude(getDeviceInfo().getMostRecentLocation().getLongitude());
            }
//            event.setIpAddress(getDeviceInfo().getIpAddress());
            event.setCountry(getDeviceInfo().getCountry());
            event.setLanguage(getDeviceInfo().getLanguage());
            event.setLibrary(Constants.LIBRARY + "/" + Constants.VERSION);
//            event.setCity(getDeviceInfo().getCity());
//            event.setRegion(getDeviceInfo().getRegion());
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Jayapura"));
            event.setEventTime(dateFormat.format(new Date()));
//            event.setServerUploadTime(String.valueOf(System.currentTimeMillis()));
//            event.setIdfa();
//            event.setAdid();
//            event.setStartVersion();
//            event.setClientEventTime();
//            event.getUserCreationTime();
//            event.setClientUploadTime();
//            event.setProcessedTime();
            event.setUuid(UUID.randomUUID().toString());

            postRequest(getCallback()::onResponse, getErrorCallback()::onError, event);
        } catch (Exception ignored) {
        }
    }

    private void processError(Event event, VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                authenticate(response -> postRequest(getCallback()::onResponse, getErrorCallback()::onError, event));
            } else if (!isDeviceConnected(getContext())) {
                int dbCount = getEventDao().getCount();
                if (dbCount < 50) {
                    getEventDao().insert(event);
                    Log.d(POST_EVENT, "Offline saved to DB : " + (dbCount + 1));
                } else {
                    Log.i(POST_EVENT, "Offline but db limit exceeded to save" + dbCount);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized void processPendingEvents() {
        try {
            int dbCount = getEventDao().getCount();
            if (dbCount > 0) {
                Log.i(POST_EVENT, "Found pending records in DB : " + dbCount);
                List<Event> events = getEventDao().getAll();
                for (int i = 0; i < events.size(); i++) {
                    Event e = events.get(i);
                    postRequest(getCallback()::onResponse, getErrorCallback()::onError, e);
                    getEventDao().delete(e);
                    Log.d(POST_EVENT, "Pending records in DB : " + (dbCount - i + 1));
                }
                int pendingRecordsCount = getEventDao().getCount();
                if (pendingRecordsCount == 0) {
                    Log.i(POST_EVENT, "Processed all pending records in DB");
                } else {
                    Log.e(POST_EVENT, "Failed while Processing pending records in DB : " + pendingRecordsCount);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void postRequest(Listener<JSONObject> listener, ErrorListener errorListener, Event event) {
        JSONObject jsonRequest = getJsonRequest(event);

        JsonObjectRequest request = new JsonObjectRequest(KREDIVO_BASE_URL + "event", jsonRequest, listener, errorListener) {
            public Map<String, String> getHeaders() {
                Map<String, String> map = new java.util.HashMap<>();
                map.put("Authorization", TOKEN);
                return map;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError error) {
                Log.e(POST_EVENT, "Error when called an api : " + error.getLocalizedMessage());
                processError(event, error);
                return error;
            }

            @Override
            protected com.android.volley.Response parseNetworkResponse(NetworkResponse response) {
                try {
                    Log.d(POST_EVENT, "Received response from server");
                    String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    getService().processPendingEvents();
                    return com.android.volley.Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    return com.android.volley.Response.error(new ParseError(e));
                } catch (JSONException je) {
                    return com.android.volley.Response.error(new ParseError(je));
                }
            }
        };
        Log.i(POST_EVENT, "Calling an event API");
        getClient().addToRequestQueue(request);
    }

    private Set<String> getInvalidDeviceIds() {
        Set<String> invalidDeviceIds = new HashSet<String>();
        invalidDeviceIds.add("");
        invalidDeviceIds.add("9774d56d682e549c");
        invalidDeviceIds.add("unknown");
        invalidDeviceIds.add("000000000000000"); // Common Serial Number
        invalidDeviceIds.add("Android");
        invalidDeviceIds.add("DEFACE");
        invalidDeviceIds.add("00000000-0000-0000-0000-000000000000");

        return invalidDeviceIds;
    }

    public void initializeDeviceId() {
        Set<String> invalidIds = getInvalidDeviceIds();

        if (!newDeviceIdPerInstall && useAdvertisingIdForDeviceId && !getDeviceInfo().isLimitAdTrackingEnabled()) {
            String advertisingId = getDeviceInfo().getAdvertisingId();
            if (!(Utils.isEmptyString(advertisingId) || invalidIds.contains(advertisingId))) {
                deviceId = advertisingId;
            }
        }

        if (useAppSetIdForDeviceId) {
            String appSetId = getDeviceInfo().getAppSetId();
            if (!(Utils.isEmptyString(appSetId) || invalidIds.contains(appSetId))) {
                deviceId = appSetId + "S";
            }
        }

        deviceId = getDeviceInfo().generateUUID() + "R";
    }
}
