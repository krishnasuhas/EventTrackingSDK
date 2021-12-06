package com.mobilewalla.eventtracking.api;

import com.mobilewalla.eventtracking.BuildConfig;

import okhttp3.MediaType;

public class Constants {

    public static final String LIBRARY = "mobilewalla-android";
    public static final String VERSION = BuildConfig.MOBILEWALLA_VERSION;
    public static final String LIBRARY_UNKNOWN = "unknown-library";
    public static final String VERSION_UNKNOWN = "unknown-version";
    public static final String PLATFORM = "Android";

    public static final String EVENT_LOG_URL = "https://kredivo-sdk-api-dev.mobilewalla.com/kredivo-v1/";
    public static final String POST_AUTHENTICATE = "authentication";
    public static final String POST_EVENT = "event";
    public static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
    public static final String PACKAGE_NAME = "com.mobilewalla.eventtracking";

    public static final int API_VERSION = 1;

    public static final String DATABASE_NAME = PACKAGE_NAME;
    public static final int DATABASE_VERSION = 1;

    public static final String DEFAULT_INSTANCE = "$default_instance";

    public static final int EVENT_UPLOAD_THRESHOLD = 30;
    public static final int EVENT_UPLOAD_MAX_BATCH_SIZE = 50;
    public static final int EVENT_MAX_COUNT = 1000;
    public static final int EVENT_REMOVE_BATCH_SIZE = 20;
    public static final long EVENT_UPLOAD_PERIOD_MILLIS = 30 * 1000; // 30s
    public static final long MIN_TIME_BETWEEN_SESSIONS_MILLIS = 5 * 60 * 1000; // 5m
    public static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30m
    public static final int MAX_STRING_LENGTH = 1024;
    public static final int MAX_PROPERTY_KEYS = 1000;

    public static final String PREFKEY_LAST_EVENT_ID = PACKAGE_NAME + ".lastEventId";
    public static final String PREFKEY_LAST_EVENT_TIME = PACKAGE_NAME + ".lastEventTime";
    public static final String PREFKEY_PREVIOUS_SESSION_ID = PACKAGE_NAME + ".previousSessionId";
    public static final String PREFKEY_DEVICE_ID = PACKAGE_NAME + ".deviceId";
    public static final String PREFKEY_USER_ID = PACKAGE_NAME + ".userId";
    public static final String PREFKEY_OPT_OUT = PACKAGE_NAME + ".optOut";

    public static final String TRACKING_OPTION_ADID = "adid";
    public static final String TRACKING_OPTION_CARRIER = "carrier";
    public static final String TRACKING_OPTION_CITY = "city";
    public static final String TRACKING_OPTION_COUNTRY = "country";
    public static final String TRACKING_OPTION_DEVICE_BRAND = "device_brand";
    public static final String TRACKING_OPTION_DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String TRACKING_OPTION_DEVICE_MODEL = "device_model";
    public static final String TRACKING_OPTION_DMA = "dma";
    public static final String TRACKING_OPTION_IP_ADDRESS = "ip_address";
    public static final String TRACKING_OPTION_LANGUAGE = "language";
    public static final String TRACKING_OPTION_LAT_LNG = "lat_lng";
    public static final String TRACKING_OPTION_OS_NAME = "os_name";
    public static final String TRACKING_OPTION_OS_VERSION = "os_version";
    public static final String TRACKING_OPTION_API_LEVEL = "api_level";
    public static final String TRACKING_OPTION_PLATFORM = "platform";
    public static final String TRACKING_OPTION_REGION = "region";
    public static final String TRACKING_OPTION_VERSION_NAME = "version_name";
}
