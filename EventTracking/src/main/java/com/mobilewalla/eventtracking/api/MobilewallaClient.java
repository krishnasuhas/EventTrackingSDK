package com.mobilewalla.eventtracking.api;

import static com.mobilewalla.eventtracking.api.Constants.API_PASSWORD;
import static com.mobilewalla.eventtracking.api.Constants.JSON;
import static com.mobilewalla.eventtracking.api.Constants.POST_AUTHENTICATE;
import static com.mobilewalla.eventtracking.api.Constants.POST_EVENT;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilewalla.eventtracking.util.DoubleCheck;
import com.mobilewalla.eventtracking.util.Provider;
import com.mobilewalla.eventtracking.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * <h1>MobilewallaClient</h1>
 * This is the SDK instance class that contains all of the SDK functionality.<br><br>
 * <b>Note:</b> call the methods on the default shared instance in the Mobilewalla class,
 * for example: {@code Mobilewalla.getInstance().logEvent();}<br><br>
 * Many of the SDK functions return the SDK instance back, allowing you to chain multiple method
 * calls together, for example: {@code Mobilewalla.getInstance().initialize(this).enableForegroundTracking(getApplication())}
 */
public class MobilewallaClient {

    /**
     * The event type for start session events.
     */
    public static final String START_SESSION_EVENT = "session_start";
    /**
     * The event type for end session events.
     */
    public static final String END_SESSION_EVENT = "session_end";
    /**
     * The pref/database key for the device ID value.
     */
    public static final String DEVICE_ID_KEY = "device_id";
    /**
     * The pref/database key for the user ID value.
     */
    public static final String USER_ID_KEY = "user_id";
    /**
     * The pref/database key for the opt out flag.
     */
    public static final String OPT_OUT_KEY = "opt_out";
    /**
     * The pref/database key for the sequence number.
     */
    public static final String SEQUENCE_NUMBER_KEY = "sequence_number";
    /**
     * The pref/database key for the last event time.
     */
    public static final String LAST_EVENT_TIME_KEY = "last_event_time";
    /**
     * The pref/database key for the last event ID value.
     */
    public static final String LAST_EVENT_ID_KEY = "last_event_id";
    /**
     * The pref/database key for the previous session ID value.
     */
    public static final String PREVIOUS_SESSION_ID_KEY = "previous_session_id";
    /**
     * The class identifier tag used in logging. TAG = {@code "com.mobilewalla.eventtracking.api.MobilewallaClient";}
     */
    private static final String TAG = MobilewallaClient.class.getName();
    private static final MobilewallaLog logger = MobilewallaLog.getLogger();
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * The Android App Context.
     */
    protected Context context;
    /**
     * The shared OkHTTPClient instance.
     */
    protected Call.Factory callFactory;
    /**
     * The shared Mobilewalla database helper instance.
     */
    protected DatabaseHelper dbHelper;
    /**
     * The name for this instance of MobilewallaClient.
     */
    protected String instanceName;
    /**
     * The user's ID value.
     */
    protected String userId;
    /**
     * The user's Device ID value.
     */
    protected String deviceId;
    protected boolean initialized = false;
    /**
     * The device's Platform value.
     */
    protected String platform;
    protected DeviceInfo deviceInfo;
    TrackingOptions inputTrackingOptions = new TrackingOptions();
    TrackingOptions appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
    JSONObject apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
    /**
     * Event metadata
     */
    long sessionId = -1;
    long sequenceNumber = 0;
    long lastEventId = -1;
    long lastEventTime = -1;
    long previousSessionId = -1;
    /**
     * Whether or not the SDK is in the process of uploading events.
     */
    AtomicBoolean uploadingCurrently = new AtomicBoolean(false);
    /**
     * The last SDK error - used for testing.
     */
    Throwable lastError;
    /**
     * The url and credentials for Mobilewalla API endpoint
     */
    String url = Constants.API_BASE_URL;
    String serverUsername = Constants.API_USERNAME;
    String serverPassword = API_PASSWORD;
    /**
     * The Bearer Token for authentication
     */
    String bearerToken = null;
    /**
     * The background event logging worker thread instance.
     */
    WorkerThread logThread = new WorkerThread("logThread");
    /**
     * The background event uploading worker thread instance.
     */
    WorkerThread httpThread = new WorkerThread("httpThread");
    private boolean newDeviceIdPerInstall = false;
    private boolean useAdvertisingIdForDeviceId = false;
    private boolean optOut = false;
    private boolean offline = false;
    private boolean coppaControlEnabled = false;
    private boolean locationListening = true;
    /**
     * The current session ID value.
     */
    private int eventUploadThreshold = Constants.EVENT_UPLOAD_THRESHOLD;
    private int eventUploadMaxBatchSize = Constants.EVENT_UPLOAD_MAX_BATCH_SIZE;
    private int eventMaxCount = Constants.EVENT_MAX_COUNT;
    private long eventUploadPeriodMillis = Constants.EVENT_UPLOAD_PERIOD_MILLIS;
    private long minTimeBetweenSessionsMillis = Constants.MIN_TIME_BETWEEN_SESSIONS_MILLIS;
    private long sessionTimeoutMillis = Constants.SESSION_TIMEOUT_MILLIS;
    private boolean backoffUpload = false;
    private int backoffUploadBatchSize = eventUploadMaxBatchSize;
    private boolean usingForegroundTracking = false;
    private boolean trackingSessionEvents = false;
    private boolean inForeground = false;
    private boolean flushEventsOnClose = true;
    private String libraryName = Constants.LIBRARY;
    private String libraryVersion = Constants.VERSION;
    private AtomicBoolean updateScheduled = new AtomicBoolean(false);
    private SimpleDateFormat dateFormat;

    /**
     * Instantiates a new default instance MobilewallaClient and starts worker threads.
     */
    public MobilewallaClient() {
        this(null);
    }

    /**
     * Instantiates a new MobilewallaClient with instance name and starts worker threads.
     *
     * @param instance
     */
    public MobilewallaClient(String instance) {
        this.instanceName = Utils.normalizeInstanceName(instance);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        logThread.start();
        httpThread.start();
    }

    /**
     * Truncate a string to 1024 characters.
     *
     * @param value the value
     * @return the truncated string
     */
    public static String truncate(String value) {
        return value.length() <= Constants.MAX_STRING_LENGTH ? value :
                value.substring(0, Constants.MAX_STRING_LENGTH);
    }

    /**
     * Move all preference data from the legacy name to the new, static name if needed.
     * <p/>
     * Constants.PACKAGE_NAME used to be set using {@code Constants.class.getPackage().getName()}
     * Some aggressive proguard optimizations broke the reflection and caused apps
     * to crash on startup.
     * <p/>
     * Now that Constants.PACKAGE_NAME is changed, old data on devices needs to be
     * moved over to the new location so that device ids remain consistent.
     * <p/>
     * This should only happen once -- the first time a user loads the app after updating.
     * This logic needs to remain in place for quite a long time. It was first introduced in
     * April 2015 in version 1.6.0.
     *
     * @param context the context
     * @return the boolean
     */
    static boolean upgradePrefs(Context context) {
        return upgradePrefs(context, null, null);
    }

    /**
     * Upgrade prefs boolean.
     *
     * @param context       the context
     * @param sourcePkgName the source pkg name
     * @param targetPkgName the target pkg name
     * @return the boolean
     */
    static boolean upgradePrefs(Context context, String sourcePkgName, String targetPkgName) {
        try {
            if (sourcePkgName == null) {
                // Try to load the package name using the old reflection strategy.
                sourcePkgName = Constants.PACKAGE_NAME;
                try {
                    sourcePkgName = Constants.class.getPackage().getName();
                } catch (Exception e) {
                }
            }

            if (targetPkgName == null) {
                targetPkgName = Constants.PACKAGE_NAME;
            }

            // No need to copy if the source and target are the same.
            if (targetPkgName.equals(sourcePkgName)) {
                return false;
            }

            // Copy over any preferences that may exist in a source preference store.
            String sourcePrefsName = sourcePkgName + "." + context.getPackageName();
            SharedPreferences source =
                    context.getSharedPreferences(sourcePrefsName, Context.MODE_PRIVATE);

            // Nothing left in the source store to copy
            if (source.getAll().size() == 0) {
                return false;
            }

            String prefsName = targetPkgName + "." + context.getPackageName();
            SharedPreferences targetPrefs =
                    context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            SharedPreferences.Editor target = targetPrefs.edit();

            // Copy over all existing data.
            if (source.contains(sourcePkgName + ".previousSessionId")) {
                target.putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID,
                        source.getLong(sourcePkgName + ".previousSessionId", -1));
            }
            if (source.contains(sourcePkgName + ".deviceId")) {
                target.putString(Constants.PREFKEY_DEVICE_ID,
                        source.getString(sourcePkgName + ".deviceId", null));
            }
            if (source.contains(sourcePkgName + ".userId")) {
                target.putString(Constants.PREFKEY_USER_ID,
                        source.getString(sourcePkgName + ".userId", null));
            }
            if (source.contains(sourcePkgName + ".optOut")) {
                target.putBoolean(Constants.PREFKEY_OPT_OUT,
                        source.getBoolean(sourcePkgName + ".optOut", false));
            }

            // Commit the changes and clear the source store so we don't recopy.
            target.apply();
            source.edit().clear().apply();

            logger.i(TAG, "Upgraded shared preferences from " + sourcePrefsName + " to " + prefsName);
            return true;

        } catch (Exception e) {
            logger.e(TAG, "Error upgrading shared preferences", e);
            return false;
        }
    }

    /**
     * Upgrade shared prefs to db boolean.
     *
     * @param context the context
     * @return the boolean
     */
    static boolean upgradeSharedPrefsToDB(Context context) {
        // Move all data from sharedPrefs to sqlite key value store to support multi-process apps.
        // sharedPrefs is known to not be process-safe.
        return upgradeSharedPrefsToDB(context, null);
    }

    /**
     * Upgrade shared prefs to db boolean.
     *
     * @param context       the context
     * @param sourcePkgName the source pkg name
     * @return the boolean
     */
    static boolean upgradeSharedPrefsToDB(Context context, String sourcePkgName) {
        if (sourcePkgName == null) {
            sourcePkgName = Constants.PACKAGE_NAME;
        }

        // check if upgrade needed
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        String deviceId = dbHelper.getValue(DEVICE_ID_KEY);
        Long previousSessionId = dbHelper.getLongValue(PREVIOUS_SESSION_ID_KEY);
        Long lastEventTime = dbHelper.getLongValue(LAST_EVENT_TIME_KEY);
        if (!Utils.isEmptyString(deviceId) && previousSessionId != null && lastEventTime != null) {
            return true;
        }

        String prefsName = sourcePkgName + "." + context.getPackageName();
        SharedPreferences preferences =
                context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        migrateStringValue(
                preferences, Constants.PREFKEY_DEVICE_ID, null, dbHelper, DEVICE_ID_KEY
        );

        migrateLongValue(
                preferences, Constants.PREFKEY_LAST_EVENT_TIME, -1, dbHelper, LAST_EVENT_TIME_KEY
        );

        migrateLongValue(
                preferences, Constants.PREFKEY_LAST_EVENT_ID, -1, dbHelper, LAST_EVENT_ID_KEY
        );

        migrateLongValue(
                preferences, Constants.PREFKEY_PREVIOUS_SESSION_ID, -1,
                dbHelper, PREVIOUS_SESSION_ID_KEY
        );

        migrateStringValue(
                preferences, Constants.PREFKEY_USER_ID, null, dbHelper, USER_ID_KEY
        );

        migrateBooleanValue(
                preferences, Constants.PREFKEY_OPT_OUT, false, dbHelper, OPT_OUT_KEY
        );

        return true;
    }

    private static void migrateLongValue(SharedPreferences prefs, String prefKey, long defValue, DatabaseHelper dbHelper, String dbKey) {
        Long value = dbHelper.getLongValue(dbKey);
        if (value != null) { // If value already exists, it doesn't need to migrate.
            return;
        }
        long oldValue = prefs.getLong(prefKey, defValue);
        dbHelper.insertOrReplaceKeyLongValue(dbKey, oldValue);
        prefs.edit().remove(prefKey).apply();
    }

    private static void migrateStringValue(SharedPreferences prefs, String prefKey, String defValue, DatabaseHelper dbHelper, String dbKey) {
        String value = dbHelper.getValue(dbKey);
        if (!Utils.isEmptyString(value)) {
            return;
        }
        String oldValue = prefs.getString(prefKey, defValue);
        if (!Utils.isEmptyString(oldValue)) {
            dbHelper.insertOrReplaceKeyValue(dbKey, oldValue);
            prefs.edit().remove(prefKey).apply();
        }
    }

    private static void migrateBooleanValue(SharedPreferences prefs, String prefKey, boolean defValue, DatabaseHelper dbHelper, String dbKey) {
        Long value = dbHelper.getLongValue(dbKey);
        if (value != null) {
            return;
        }
        boolean oldValue = prefs.getBoolean(prefKey, defValue);
        dbHelper.insertOrReplaceKeyLongValue(dbKey, oldValue ? 1L : 0L);
        prefs.edit().remove(prefKey).apply();
    }

    /**
     * Initialize the Mobilewalla SDK with the Android application context, your Mobilewalla App API
     * key, and a user ID for the current user. <b>Note:</b> initialization is required before
     * you log events and modify user properties.
     *
     * @param context the Android application context
     * @param userId  the user id to set
     * @return the MobilewallaClient
     */
    public MobilewallaClient initialize(Context context, String userId, String api_base_url, String apiUsername, String apiPassword) {
        setServerUrl(api_base_url);
        setServerUsername(apiUsername);
        setServerPassword(apiPassword);
        return initializeInternal(context, userId, null, null);
    }

    /**
     * Initialize the Mobilewalla SDK with the Android application context, your Mobilewalla App API
     * key, and a user ID for the current user. <b>Note:</b> initialization is required before
     * you log events and modify user properties.
     *
     * @param context the Android application context
     * @param userId  the user id to set
     * @return the MobilewallaClient
     */
    public MobilewallaClient initialize(Context context, String userId) {
        return initializeInternal(context, userId, null, null);
    }

    /**
     * Initialize the Mobilewalla SDK with the Android application context, a user ID for the current user,
     * and a custom platform value.
     * <b>Note:</b> initialization is required before you log events and modify user properties.
     *
     * @param context the Android application context
     * @param userId  the user id to set
     * @param
     * @return the MobilewallaClient
     */
    public synchronized MobilewallaClient initializeInternal(
            final Context context,
            final String userId,
            final String platform,
            final Call.Factory callFactory
    ) {
        if (context == null) {
            logger.e(TAG, "Argument context cannot be null in initialize()");
            return this;
        }

        this.context = context.getApplicationContext();
        this.dbHelper = DatabaseHelper.getDatabaseHelper(this.context, this.instanceName);
        this.platform = Utils.isEmptyString(platform) ? Constants.PLATFORM : platform;

        final MobilewallaClient client = this;
        runOnLogThread(() -> {
            if (!initialized) {
                // this try block is idempotent, so it's safe to retry initialize if failed
                try {
                    if (callFactory == null) {
                        // defer OkHttp client to first call
                        final Provider<Call.Factory> callProvider
                                = DoubleCheck.provider(OkHttpClient::new);
                        this.callFactory = request -> callProvider.get().newCall(request);
                    } else {
                        this.callFactory = callFactory;
                    }

                    deviceInfo = new DeviceInfo(context, this.locationListening);
                    deviceId = initializeDeviceId();
                    deviceInfo.prefetch();

                    if (userId != null) {
                        client.userId = userId;
                        dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
                    } else {
                        client.userId = dbHelper.getValue(USER_ID_KEY);
                    }
                    final Long optOutLong = dbHelper.getLongValue(OPT_OUT_KEY);
                    optOut = optOutLong != null && optOutLong == 1;

                    // try to restore previous session id
                    previousSessionId = getLongvalue(PREVIOUS_SESSION_ID_KEY, -1);
                    if (previousSessionId >= 0) {
                        sessionId = previousSessionId;
                    }

                    // reload event meta data
                    sequenceNumber = getLongvalue(SEQUENCE_NUMBER_KEY, 0);
                    lastEventId = getLongvalue(LAST_EVENT_ID_KEY, -1);
                    lastEventTime = getLongvalue(LAST_EVENT_TIME_KEY, -1);

                    // install database reset listener to re-insert metadata in memory
                    dbHelper.setDatabaseResetListener(db -> {
                        dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.STORE_TABLE_NAME, DEVICE_ID_KEY, client.deviceId);
                        dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.STORE_TABLE_NAME, USER_ID_KEY, client.userId);
                        dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, OPT_OUT_KEY, client.optOut ? 1L : 0L);
                        dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, PREVIOUS_SESSION_ID_KEY, client.sessionId);
                        dbHelper.insertOrReplaceKeyValueToTable(db, DatabaseHelper.LONG_STORE_TABLE_NAME, LAST_EVENT_TIME_KEY, client.lastEventTime);
                    });

                    initialized = true;
                } catch (CursorWindowAllocationException e) {  // treat as uninitialized SDK
                    logger.e(TAG, String.format(
                            "Failed to initialize Mobilewalla SDK due to: %s", e.getMessage()
                    ));
                }
            }
        });

        return this;
    }

    /**
     * Enable foreground tracking for the SDK. This is <b>HIGHLY RECOMMENDED</b>, and will allow
     * for accurate session tracking.
     *
     * @param app the Android application
     * @return the MobilewallaClient
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#tracking-sessions">
     * Tracking Sessions</a>
     */
    public MobilewallaClient enableForegroundTracking(Application app) {
        if (usingForegroundTracking || !contextSet("enableForegroundTracking()")) {
            return this;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            app.registerActivityLifecycleCallbacks(new MobilewallaCallbacks(this));
        }

        return this;
    }

    public MobilewallaClient enableDiagnosticLogging() {
        return this;
    }

    public MobilewallaClient disableDiagnosticLogging() {
        return this;
    }

    public MobilewallaClient setDiagnosticEventMaxCount(int eventMaxCount) {
        return this;
    }

    /**
     * Whether to set a new device ID per install. If true, then the SDK will always generate a new
     * device ID on app install (as opposed to re-using an existing value like ADID).
     *
     * @param newDeviceIdPerInstall whether to set a new device ID on app install.
     * @return the MobilewallaClient
     */
    public MobilewallaClient enableNewDeviceIdPerInstall(boolean newDeviceIdPerInstall) {
        this.newDeviceIdPerInstall = newDeviceIdPerInstall;
        return this;
    }

    /**
     * Whether to use the Android advertising ID (ADID) as the user's device ID.
     *
     * @return the MobilewallaClient
     */
    public MobilewallaClient useAdvertisingIdForDeviceId() {
        this.useAdvertisingIdForDeviceId = true;
        return this;
    }

    /**
     * Enable location listening in the SDK. This will add the user's current lat/lon coordinates
     * to every event logged.
     * <p>
     * This function should be called before SDK initialization, e.g. {@link #initialize(Context, String)}.
     *
     * @return the MobilewallaClient
     */
    public MobilewallaClient enableLocationListening() {
        this.locationListening = true;
        if (this.deviceInfo != null) {
            this.deviceInfo.setLocationListening(true);
        }
        return this;
    }

    /**
     * Disable location listening in the SDK. This will stop the sending of the user's current
     * lat/lon coordinates.
     * <p>
     * This function should be called before SDK initialization, e.g. {@link #initialize(Context, String)}.
     *
     * @return the MobilewallaClient
     */
    public MobilewallaClient disableLocationListening() {
        this.locationListening = false;
        if (this.deviceInfo != null) {
            this.deviceInfo.setLocationListening(false);
        }
        return this;
    }

    /**
     * Sets event upload threshold. The SDK will attempt to batch upload unsent events
     * every eventUploadPeriodMillis milliseconds, or if the unsent event count exceeds the
     * event upload threshold.
     *
     * @param eventUploadThreshold the event upload threshold
     * @return the MobilewallaClient
     */
    public MobilewallaClient setEventUploadThreshold(int eventUploadThreshold) {
        this.eventUploadThreshold = eventUploadThreshold;
        return this;
    }

    /**
     * Sets event upload max batch size. This controls the maximum number of events sent with
     * each upload request.
     *
     * @param eventUploadMaxBatchSize the event upload max batch size
     * @return the MobilewallaClient
     */
    public MobilewallaClient setEventUploadMaxBatchSize(int eventUploadMaxBatchSize) {
        this.eventUploadMaxBatchSize = eventUploadMaxBatchSize;
        this.backoffUploadBatchSize = eventUploadMaxBatchSize;
        return this;
    }

    /**
     * Sets event max count. This is the maximum number of unsent events to keep on the device
     * (for example if the device does not have internet connectivity and cannot upload events).
     * If the number of unsent events exceeds the max count, then the SDK begins dropping events,
     * starting from the earliest logged.
     *
     * @param eventMaxCount the event max count
     * @return the MobilewallaClient
     */
    public MobilewallaClient setEventMaxCount(int eventMaxCount) {
        this.eventMaxCount = eventMaxCount;
        return this;
    }

    /**
     * Sets event upload period millis. The SDK will attempt to batch upload unsent events
     * every eventUploadPeriodMillis milliseconds, or if the unsent event count exceeds the
     * event upload threshold.
     *
     * @param eventUploadPeriodMillis the event upload period millis
     * @return the MobilewallaClient
     */
    public MobilewallaClient setEventUploadPeriodMillis(int eventUploadPeriodMillis) {
        this.eventUploadPeriodMillis = eventUploadPeriodMillis;
        return this;
    }

    /**
     * Sets min time between sessions millis.
     *
     * @param minTimeBetweenSessionsMillis the min time between sessions millis
     * @return the min time between sessions millis
     */
    public MobilewallaClient setMinTimeBetweenSessionsMillis(long minTimeBetweenSessionsMillis) {
        this.minTimeBetweenSessionsMillis = minTimeBetweenSessionsMillis;
        return this;
    }

    /**
     * Sets a custom server url for event upload.
     *
     * @param serverUrl - a string url for event upload.
     * @return the MobilewallaClient
     */
    public MobilewallaClient setServerUrl(String serverUrl) {
        url = serverUrl;
        return this;
    }

    /**
     * Sets a custom server url for event upload.
     *
     * @param username - a string username for event upload.
     * @return the MobilewallaClient
     */
    public MobilewallaClient setServerUsername(String username) {
        this.serverUsername = username;
        return this;
    }

    /**
     * Sets a custom server url for event upload.
     *
     * @param password - a string password for event upload.
     * @return the MobilewallaClient
     */
    public MobilewallaClient setServerPassword(String password) {
        this.serverPassword = password;
        return this;
    }

    /**
     * Set Bearer Token to be included in request header.
     *
     * @param token
     * @return the MobilewallaClient
     */
    public MobilewallaClient setBearerToken(String token) {
        this.bearerToken = token;
        return this;
    }

    /**
     * Sets session timeout millis. If foreground tracking has not been enabled with
     *
     * @param sessionTimeoutMillis the session timeout millis
     * @return the MobilewallaClient
     * @{code enableForegroundTracking()}, then new sessions will be started after
     * sessionTimeoutMillis milliseconds have passed since the last event logged.
     */
    public MobilewallaClient setSessionTimeoutMillis(long sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        return this;
    }

    public MobilewallaClient setTrackingOptions(TrackingOptions trackingOptions) {
        inputTrackingOptions = trackingOptions;
        appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
        if (coppaControlEnabled) {
            appliedTrackingOptions.mergeIn(TrackingOptions.forCoppaControl());
        }
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Enable COPPA (Children's Online Privacy Protection Act) restrictions on ADID, city, IP address and location tracking.
     * This can be used by any customer that does not want to collect ADID, city, IP address and location tracking.
     */
    public MobilewallaClient enableCoppaControl() {
        coppaControlEnabled = true;
        appliedTrackingOptions.mergeIn(TrackingOptions.forCoppaControl());
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Disable COPPA (Children's Online Privacy Protection Act) restrictions on ADID, city, IP address and location tracking.
     */
    public MobilewallaClient disableCoppaControl() {
        coppaControlEnabled = false;
        appliedTrackingOptions = TrackingOptions.copyOf(inputTrackingOptions);
        apiPropertiesTrackingOptions = appliedTrackingOptions.getApiPropertiesTrackingOptions();
        return this;
    }

    /**
     * Sets opt out. If true then the SDK does not track any events for the user.
     *
     * @param optOut whether or not to opt the user out of tracking
     * @return the MobilewallaClient
     */
    public MobilewallaClient setOptOut(final boolean optOut) {
        if (!contextSet("setOptOut()")) {
            return this;
        }

        final MobilewallaClient client = this;
        runOnLogThread(() -> {
            client.optOut = optOut;
            dbHelper.insertOrReplaceKeyLongValue(OPT_OUT_KEY, optOut ? 1L : 0L);
        });
        return this;
    }

    /**
     * Library name is default as `mobilewalla-android`.
     * Notice: You will only want to set it when following conditions are met.
     * 1. You develop your own library which bridges Mobilewalla Android native library.
     * 2. You want to track your library as one of the data sources.
     */
    public MobilewallaClient setLibraryName(final String libraryName) {
        this.libraryName = libraryName;
        return this;
    }

    /**
     * Library version is default as the latest Mobilewalla Android SDK version.
     * Notice: You will only want to set it when following conditions are met.
     * 1. You develop your own library which bridges Mobilewalla Android native library.
     * 2. You want to track your library as one of the data sources.
     */
    public MobilewallaClient setLibraryVersion(final String libraryVersion) {
        this.libraryVersion = libraryVersion;
        return this;
    }

    /**
     * Returns whether or not the user is opted out of tracking.
     *
     * @return the optOut flag value
     */
    public boolean isOptedOut() {
        return optOut;
    }

    /**
     * Enable/disable message logging by the SDK.
     *
     * @param enableLogging whether to enable message logging by the SDK.
     * @return the MobilewallaClient
     */
    public MobilewallaClient enableLogging(boolean enableLogging) {
        logger.setEnableLogging(enableLogging);
        return this;
    }

    /**
     * Sets the logging level. Logging messages will only appear if they are the same severity
     * level or higher than the set log level.
     *
     * @param logLevel the log level
     * @return the MobilewallaClient
     */
    public MobilewallaClient setLogLevel(int logLevel) {
        logger.setLogLevel(logLevel);
        return this;
    }

    /**
     * Sets offline. If offline is true, then the SDK will not upload events to Mobilewalla servers;
     * however, it will still log events.
     *
     * @param offline whether or not the SDK should be offline
     * @return the MobilewallaClient
     */
    public MobilewallaClient setOffline(boolean offline) {
        this.offline = offline;

        // Try to update to the server once offline mode is disabled.
        if (!offline) {
            uploadEvents();
        }

        return this;
    }

    /**
     * Enable/disable flushing of unsent events on app close (enabled by default).
     *
     * @param flushEventsOnClose whether to flush unsent events on app close
     * @return the MobilewallaClient
     */
    public MobilewallaClient setFlushEventsOnClose(boolean flushEventsOnClose) {
        this.flushEventsOnClose = flushEventsOnClose;
        return this;
    }

    /**
     * Track session events mobilewalla client. If enabled then the SDK will automatically send
     * start and end session events to mark the start and end of the user's sessions.
     *
     * @param trackingSessionEvents whether to enable tracking of session events
     * @return the MobilewallaClient
     */
    public MobilewallaClient trackSessionEvents(boolean trackingSessionEvents) {
        this.trackingSessionEvents = trackingSessionEvents;
        return this;
    }

    /**
     * Set foreground tracking to true.
     */
    void useForegroundTracking() {
        usingForegroundTracking = true;
    }

    /**
     * Whether foreground tracking is enabled.
     *
     * @return whether foreground tracking is enabled
     */
    boolean isUsingForegroundTracking() {
        return usingForegroundTracking;
    }

    /**
     * Whether app is in the foreground.
     *
     * @return whether app is in the foreground
     */
    boolean isInForeground() {
        return inForeground;
    }

    /**
     * Log an event with the specified event type.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType the event type
     */
    public void logEvent(String eventType) {
        logEvent(eventType, null);
    }

    /**
     * Log an event with the specified event type and event properties.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     */
    public void logEvent(String eventType, JSONObject eventProperties) {
        logEvent(eventType, eventProperties, false);
    }

    /**
     * Log an event with the specified event type, event properties, with optional out of session
     * flag. If out of session is true, then the sessionId will be -1 for the event, indicating
     * that it is not part of the current session. Note: this might be useful when logging events
     * for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param outOfSession    the out of session
     */
    public void logEvent(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEvent(eventType, eventProperties, null, outOfSession);
    }

    /**
     * Log an event with the specified event type, event properties, and groups. Use this to set
     * event-level groups, meaning the group(s) set only apply for this specific event and does
     * not persist on the user.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups) {
        logEvent(eventType, eventProperties, groups, false);
    }

    /**
     * Log event with the specified event type, event properties, groups, with optional out of
     * session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param outOfSession    the out of session
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        logEvent(eventType, eventProperties, groups, getCurrentTimeMillis(), outOfSession);
    }

    /**
     * Log event with the specified event type, event properties, groups, timestamp, with optional
     * out of session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is asynchronous and happens on a background thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param timestamp       the timestamp in millisecond since epoch
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#setting-event-properties">
     * Setting Event Properties</a>
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#setting-groups">
     * Setting Groups</a>
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#tracking-sessions">
     * Tracking Sessions</a>
     */
    public void logEvent(String eventType, JSONObject eventProperties, JSONObject groups, long timestamp, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEventAsync(
                    eventType, eventProperties, null, null, groups, null,
                    timestamp, outOfSession
            );
        }
    }

    /**
     * Log an event with the specified event type.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType the event type
     */
    public void logEventSync(String eventType) {
        logEventSync(eventType, null);
    }

    /**
     * Log an event with the specified event type and event properties.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#setting-event-properties">
     * Setting Event Properties</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties) {
        logEventSync(eventType, eventProperties, false);
    }

    /**
     * Log an event with the specified event type, event properties, with optional out of session
     * flag. If out of session is true, then the sessionId will be -1 for the event, indicating
     * that it is not part of the current session. Note: this might be useful when logging events
     * for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param outOfSession    the out of session
     */
    public void logEventSync(String eventType, JSONObject eventProperties, boolean outOfSession) {
        logEventSync(eventType, eventProperties, null, outOfSession);
    }

    /**
     * Log an event with the specified event type, event properties, and groups. Use this to set
     * event-level groups, meaning the group(s) set only apply for this specific event and does
     * not persist on the user.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups) {
        logEventSync(eventType, eventProperties, groups, false);
    }

    /**
     * Log event with the specified event type, event properties, groups, with optional out of
     * session flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param outOfSession    the out of session
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#setting-event-properties">
     * Setting Event Properties</a>
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#setting-groups">
     * Setting Groups</a>
     * @see <a href="https://github.com/mobilewalla/Mobilewalla-Android#tracking-sessions">
     * Tracking Sessions</a>
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups, boolean outOfSession) {
        logEventSync(eventType, eventProperties, groups, getCurrentTimeMillis(), outOfSession);
    }

    /**
     * Log event with the specified event type, event properties, groups, timestamp,  with optional
     * sout of ession flag. If out of session is true, then the sessionId will be -1 for the event,
     * indicating that it is not part of the current session. Note: this might be useful when
     * logging events for notifications received.
     * <b>Note:</b> this is version is synchronous and blocks the main thread until done.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param groups          the groups
     * @param timestamp       the timestamp in milliseconds since epoch
     * @param outOfSession    the out of session
     */
    public void logEventSync(String eventType, JSONObject eventProperties, JSONObject groups, long timestamp, boolean outOfSession) {
        if (validateLogEvent(eventType)) {
            logEvent(eventType, eventProperties, null, null, groups, null, timestamp, outOfSession);
        }
    }

    /**
     * Validate the event type being logged. Also verifies that the context and API key
     * have been set already with an initialize call.
     *
     * @param eventType the event type
     * @return true if the event type is valid
     */
    protected boolean validateLogEvent(String eventType) {
        if (Utils.isEmptyString(eventType)) {
            logger.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
            return false;
        }

        return contextSet("logEvent()");
    }

    /**
     * Log event async. Internal method to handle the synchronous logging of events.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param apiProperties   the api properties
     * @param userProperties  the user properties
     * @param groups          the groups
     * @param timestamp       the timestamp
     * @param outOfSession    the out of session
     */
    protected void logEventAsync(final String eventType, JSONObject eventProperties,
                                 JSONObject apiProperties, JSONObject userProperties, JSONObject groups,
                                 JSONObject groupProperties, final long timestamp, final boolean outOfSession) {
        // Clone the incoming eventProperties object before sending over
        // to the log thread. Helps avoid ConcurrentModificationException
        // if the caller starts mutating the object they passed in.
        // Only does a shallow copy, so it's still possible, though unlikely,
        // to hit concurrent access if the caller mutates deep in the object.
        if (eventProperties != null) {
            eventProperties = Utils.cloneJSONObject(eventProperties);
        }

        if (apiProperties != null) {
            apiProperties = Utils.cloneJSONObject(apiProperties);
        }

        if (userProperties != null) {
            userProperties = Utils.cloneJSONObject(userProperties);
        }

        if (groups != null) {
            groups = Utils.cloneJSONObject(groups);
        }

        if (groupProperties != null) {
            groupProperties = Utils.cloneJSONObject(groupProperties);
        }

        final JSONObject copyEventProperties = eventProperties;
        final JSONObject copyApiProperties = apiProperties;
        final JSONObject copyUserProperties = userProperties;
        final JSONObject copyGroups = groups;
        final JSONObject copyGroupProperties = groupProperties;
        runOnLogThread(() -> logEvent(
                eventType, copyEventProperties, copyApiProperties,
                copyUserProperties, copyGroups, copyGroupProperties, timestamp, outOfSession
        ));
    }

    /**
     * Log event. Internal method to handle the asynchronous logging of events on background
     * thread.
     *
     * @param eventType       the event type
     * @param eventProperties the event properties
     * @param apiProperties   the api properties
     * @param userProperties  the user properties
     * @param groups          the groups
     * @param timestamp       the timestamp
     * @param outOfSession    the out of session
     * @return the event ID if succeeded, else -1.
     */
    public long logEvent(String eventType, JSONObject eventProperties, JSONObject apiProperties,
                         JSONObject userProperties, JSONObject groups, JSONObject groupProperties,
                         long timestamp, boolean outOfSession) {
        logger.d(TAG, "Logged event to Mobilewalla: " + eventType);

        if (optOut) {
            return -1;
        }

        // skip session check if logging start_session or end_session events
        boolean loggingSessionEvent = trackingSessionEvents &&
                (eventType.equals(START_SESSION_EVENT) || eventType.equals(END_SESSION_EVENT));

        if (!loggingSessionEvent && !outOfSession) {
            // default case + corner case when async logEvent between onPause and onResume
            if (!inForeground) {
                startNewSessionIfNeeded(timestamp);
            } else {
                refreshSessionTime(timestamp);
            }
        }

        long result = -1;
        JSONObject event = new JSONObject();
        try {
            String eventTime = dateFormat.format(new Date(timestamp));
            event.put("eventType", eventType);
            event.put("eventTime", eventTime);
            event.put("userId", userId);
            event.put("deviceId", deviceId);
            event.put("sessionId", outOfSession ? -1 : sessionId);
            event.put("uuid", UUID.randomUUID().toString());
            event.put("sequenceNumber", getNextSequenceNumber());
            event.put("versionName", this.libraryVersion == null ? Constants.VERSION_UNKNOWN : this.libraryVersion);
            event.put("library", this.libraryName == null ? Constants.LIBRARY_UNKNOWN : this.libraryName);

            if (appliedTrackingOptions.shouldTrackVersionName()) {
                event.put("versionName", replaceWithJSONNull(deviceInfo.getVersionName()));
            }
            if (appliedTrackingOptions.shouldTrackOsName()) {
                event.put("osName", replaceWithJSONNull(deviceInfo.getOsName()));
            }
            if (appliedTrackingOptions.shouldTrackOsVersion()) {
                event.put("osVersion", replaceWithJSONNull(deviceInfo.getOsVersion()));
            }
            if (appliedTrackingOptions.shouldTrackApiLevel()) {
                event.put("apiLevel", replaceWithJSONNull(Build.VERSION.SDK_INT));
            }
            if (appliedTrackingOptions.shouldTrackDeviceBrand()) {
                event.put("deviceBrand", replaceWithJSONNull(deviceInfo.getBrand()));
            }
            if (appliedTrackingOptions.shouldTrackDeviceManufacturer()) {
                event.put("deviceManufacturer", replaceWithJSONNull(deviceInfo.getManufacturer()));
            }
            if (appliedTrackingOptions.shouldTrackDeviceModel()) {
                event.put("deviceModel", replaceWithJSONNull(deviceInfo.getModel()));
            }
            if (appliedTrackingOptions.shouldTrackCarrier()) {
                event.put("deviceCarrier", replaceWithJSONNull(deviceInfo.getCarrier()));
            }
            if (appliedTrackingOptions.shouldTrackCountry()) {
                event.put("country", replaceWithJSONNull(deviceInfo.getCountry()));
            }
            if (appliedTrackingOptions.shouldTrackLanguage()) {
                event.put("language", replaceWithJSONNull(deviceInfo.getLanguage()));
            }
            if (appliedTrackingOptions.shouldTrackPlatform()) {
                event.put("platform", platform);
            }
            if (appliedTrackingOptions.shouldTrackLatLng()) {
                Location location = deviceInfo.getMostRecentLocation();
                if (location != null) {
                    event.put("latitude", location.getLatitude());
                    event.put("longitude", location.getLongitude());
                }
            }

            apiProperties = (apiProperties == null) ? new JSONObject() : apiProperties;
            if (apiPropertiesTrackingOptions != null && apiPropertiesTrackingOptions.length() > 0) {
                apiProperties.put("trackingOptions", apiPropertiesTrackingOptions);
            }
            if (appliedTrackingOptions.shouldTrackAdid() && deviceInfo.getAdvertisingId() != null) {
                apiProperties.put("androidADID", deviceInfo.getAdvertisingId());
            }
            apiProperties.put("limitAdTracking", deviceInfo.isLimitAdTrackingEnabled());
            apiProperties.put("gpsEnabled", deviceInfo.isGooglePlayServicesEnabled());
            event.put("apiProperties", apiProperties);

            event.put("eventProperties", truncate(eventProperties));
            event.put("userProperties", truncate(userProperties));
            event.put("globalUserProperties", truncate(groups));
            event.put("groupProperties", truncate(groupProperties));

            event.put("groupProperties", truncate(groupProperties));
            result = saveEvent(eventType, event);
        } catch (JSONException e) {
            logger.e(TAG, String.format(
                    "JSON Serialization of event type %s failed, skipping: %s", eventType, e.toString()
            ));
        }

        return result;
    }

    /**
     * Save event long. Internal method to save an event to the database.
     *
     * @param eventType the event type
     * @param event     the event
     * @return the event ID if succeeded, else -1
     */
    protected long saveEvent(String eventType, JSONObject event) {
        String eventString = event.toString();
        if (Utils.isEmptyString(eventString)) {
            logger.e(TAG, String.format(
                    "Detected empty event string for event type %s, skipping", eventType
            ));
            return -1;
        }

        lastEventId = dbHelper.addEvent(eventString);
        setLastEventId(lastEventId);

        int numEventsToRemove = Math.min(
                Math.max(1, eventMaxCount / 10),
                Constants.EVENT_REMOVE_BATCH_SIZE
        );
        if (dbHelper.getEventCount() > eventMaxCount) {
            dbHelper.removeEvents(dbHelper.getNthEventId(numEventsToRemove));
        }

        long totalEventCount = dbHelper.getTotalEventCount(); // counts may have changed, refetch
        if ((totalEventCount % eventUploadThreshold) == 0 &&
                totalEventCount >= eventUploadThreshold) {
            updateServer();
        } else {
            updateServerLater(eventUploadPeriodMillis);
        }

        return lastEventId;
    }

    // fetches key from dbHelper longValueStore
    // if key does not exist, return defaultValue instead
    private long getLongvalue(String key, long defaultValue) {
        Long value = dbHelper.getLongValue(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Internal method to increment and fetch the next event sequence number.
     *
     * @return the next sequence number
     */
    long getNextSequenceNumber() {
        sequenceNumber++;
        dbHelper.insertOrReplaceKeyLongValue(SEQUENCE_NUMBER_KEY, sequenceNumber);
        return sequenceNumber;
    }

    /**
     * Internal method to set the last event time.
     *
     * @param timestamp the timestamp
     */
    void setLastEventTime(long timestamp) {
        lastEventTime = timestamp;
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_TIME_KEY, timestamp);
    }

    /**
     * Internal method to set the last event id.
     *
     * @param eventId the event id
     */
    void setLastEventId(long eventId) {
        lastEventId = eventId;
        dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_ID_KEY, eventId);
    }

    /**
     * Gets the current session id.
     *
     * @return The current sessionId value.
     */
    public long getSessionId() {
        return sessionId;
    }

    private void setSessionId(long timestamp) {
        sessionId = timestamp;
        setPreviousSessionId(timestamp);
    }

    /**
     * Internal method to set the previous session id.
     *
     * @param timestamp the timestamp
     */
    void setPreviousSessionId(long timestamp) {
        previousSessionId = timestamp;
        dbHelper.insertOrReplaceKeyLongValue(PREVIOUS_SESSION_ID_KEY, timestamp);
    }

    /**
     * Public method to start a new session if needed.
     *
     * @param timestamp the timestamp
     * @return whether or not a new session was started
     */
    public boolean startNewSessionIfNeeded(long timestamp) {
        if (inSession()) {

            if (isWithinMinTimeBetweenSessions(timestamp)) {
                refreshSessionTime(timestamp);
                return false;
            }

            startNewSession(timestamp);
            return true;
        }

        // no current session - check for previous session
        if (isWithinMinTimeBetweenSessions(timestamp)) {
            if (previousSessionId == -1) {
                startNewSession(timestamp);
                return true;
            }

            // extend previous session
            setSessionId(previousSessionId);
            refreshSessionTime(timestamp);
            return false;
        }

        startNewSession(timestamp);
        return true;
    }

    private void startNewSession(long timestamp) {
        // end previous session
        if (trackingSessionEvents) {
            sendSessionEvent(END_SESSION_EVENT);
        }

        // start new session
        setSessionId(timestamp);
        refreshSessionTime(timestamp);
        if (trackingSessionEvents) {
            sendSessionEvent(START_SESSION_EVENT);
        }
    }

    private boolean inSession() {
        return sessionId >= 0;
    }

    private boolean isWithinMinTimeBetweenSessions(long timestamp) {
        long sessionLimit = usingForegroundTracking ?
                minTimeBetweenSessionsMillis : sessionTimeoutMillis;
        return (timestamp - lastEventTime) < sessionLimit;
    }

    /**
     * Internal method to refresh the current session time.
     *
     * @param timestamp the timestamp
     */
    void refreshSessionTime(long timestamp) {
        if (!inSession()) {
            return;
        }

        setLastEventTime(timestamp);
    }

    private void sendSessionEvent(final String sessionEvent) {
        if (!contextSet(String.format("sendSessionEvent('%s')", sessionEvent))) {
            return;
        }

        if (!inSession()) {
            return;
        }

        JSONObject apiProperties = new JSONObject();
        try {
            apiProperties.put("special", sessionEvent);
        } catch (JSONException e) {
            return;
        }

        logEvent(sessionEvent, null, apiProperties, null, null, null, lastEventTime, false);
    }

    /**
     * Internal method to handle on app exit foreground behavior.
     *
     * @param timestamp the timestamp
     */
    void onExitForeground(final long timestamp) {
        runOnLogThread(() -> {
            refreshSessionTime(timestamp);
            inForeground = false;
            if (flushEventsOnClose) {
                updateServer();
            }

            // re-persist metadata into database for good measure
            dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
            dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);
            dbHelper.insertOrReplaceKeyLongValue(OPT_OUT_KEY, optOut ? 1L : 0L);
            dbHelper.insertOrReplaceKeyLongValue(PREVIOUS_SESSION_ID_KEY, sessionId);
            dbHelper.insertOrReplaceKeyLongValue(LAST_EVENT_TIME_KEY, lastEventTime);
        });
    }

    /**
     * Internal method to handle on app enter foreground behavior.
     *
     * @param timestamp the timestamp
     */
    void onEnterForeground(final long timestamp) {
        runOnLogThread(() -> {
            startNewSessionIfNeeded(timestamp);
            inForeground = true;
        });
    }

    /**
     * Truncate values in a JSON object. Any string values longer than 1024 characters will be
     * truncated to 1024 characters.
     * Any dictionary with more than 1000 items will be ignored.
     *
     * @param object the object
     * @return the truncated JSON object
     */
    public String truncate(JSONObject object) {
        if (object == null) {
            return new JSONObject().toString();
        }

        if (object.length() > Constants.MAX_PROPERTY_KEYS) {
            logger.w(TAG, "Warning: too many properties (more than 1000), ignoring");
            return new JSONObject().toString();
        }

        Iterator<?> keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                Object value = object.get(key);
                if (value.getClass().equals(String.class)) {
                    object.put(key, truncate((String) value));
                } else if (value.getClass().equals(JSONObject.class)) {
                    object.put(key, truncate((JSONObject) value));
                } else if (value.getClass().equals(JSONArray.class)) {
                    object.put(key, truncate((JSONArray) value));
                }
            } catch (JSONException e) {
                logger.e(TAG, e.toString());
            }
        }

        return object.toString();
    }

    /**
     * Truncate values in a JSON array. Any string values longer than 1024 characters will be
     * truncated to 1024 characters.
     *
     * @param array the array
     * @return the truncated JSON array
     * @throws JSONException the json exception
     */
    public JSONArray truncate(JSONArray array) throws JSONException {
        if (array == null) {
            return new JSONArray();
        }

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value.getClass().equals(String.class)) {
                array.put(i, truncate((String) value));
            } else if (value.getClass().equals(JSONObject.class)) {
                array.put(i, truncate((JSONObject) value));
            } else if (value.getClass().equals(JSONArray.class)) {
                array.put(i, truncate((JSONArray) value));
            }
        }
        return array;
    }

    /**
     * Gets the user's id. Can be null.
     *
     * @return The developer specified identifier for tracking within the analytics system.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user id (can be null).
     *
     * @param userId the user id
     * @return the MobilewallaClient
     */
    public MobilewallaClient setUserId(final String userId) {
        return setUserId(userId, false);
    }

    /**
     * Sets the user id (can be null).
     * If startNewSession is true, ends the session for the previous user and starts a new
     * session for the new user id.
     *
     * @param userId the user id
     * @return the MobilewallaClient
     */
    public MobilewallaClient setUserId(final String userId, final boolean startNewSession) {
        if (!contextSet("setUserId()")) {
            return this;
        }

        final MobilewallaClient client = this;
        runOnLogThread(() -> {
            // end previous session
            if (startNewSession && trackingSessionEvents) {
                sendSessionEvent(END_SESSION_EVENT);
            }

            client.userId = userId;
            dbHelper.insertOrReplaceKeyValue(USER_ID_KEY, userId);

            // start new session
            if (startNewSession) {
                long timestamp = getCurrentTimeMillis();
                setSessionId(timestamp);
                refreshSessionTime(timestamp);
                if (trackingSessionEvents) {
                    sendSessionEvent(START_SESSION_EVENT);
                }
            }
        });
        return this;
    }

    /**
     * Regenerates a new random deviceId for current user. Note: this is not recommended unless you
     * know what you are doing. This can be used in conjunction with setUserId(null) to anonymize
     * users after they log out. With a null userId and a completely new deviceId, the current user
     * would appear as a brand new user in dashboard.
     */
    public MobilewallaClient regenerateDeviceId() {
        if (!contextSet("regenerateDeviceId()")) {
            return this;
        }

        runOnLogThread(() -> {
            String randomId = DeviceInfo.generateUUID() + "R";
            setDeviceId(randomId);
        });
        return this;
    }

    /**
     * Force SDK to upload any unsent events.
     */
    public void uploadEvents() {
        if (!contextSet("uploadEvents()")) {
            return;
        }

        logThread.post(this::updateServer);
    }

    private void updateServerLater(long delayMillis) {
        if (updateScheduled.getAndSet(true)) {
            return;
        }

        logThread.postDelayed(() -> {
            updateScheduled.set(false);
            updateServer();
        }, delayMillis);
    }

    /**
     * Internal method to upload unsent events.
     */
    protected void updateServer() {
        updateServer(false);
    }

    /**
     * Internal method to upload unsent events. Limit controls whether to use event upload max
     * batch size or backoff upload batch size. <b>Note: </b> always call this on logThread
     *
     * @param limit the limit
     */
    protected void updateServer(boolean limit) {
        if (optOut || offline) {
            return;
        }

        // if returning out of this block, always be sure to set uploadingCurrently to false!!
        if (!uploadingCurrently.getAndSet(true)) {
            long totalEventCount = dbHelper.getTotalEventCount();
            long batchSize = Math.min(
                    limit ? backoffUploadBatchSize : eventUploadMaxBatchSize,
                    totalEventCount
            );

            if (batchSize <= 0) {
                uploadingCurrently.set(false);
                return;
            }

            try {
                List<JSONObject> events = dbHelper.getEvents(lastEventId, batchSize);

                final Pair<Long, JSONArray> merged = getEventsWithMaxId(events, batchSize);
                if (events.size() == 0) {
                    uploadingCurrently.set(false);
                    return;
                }
                JSONObject eventWrapper = new JSONObject();
                eventWrapper.put("events", merged.second);
                httpThread.post(() -> makeEventUploadPostRequest(callFactory, eventWrapper, merged.first));
            } catch (JSONException e) {
                uploadingCurrently.set(false);
                logger.e(TAG, e.toString());
            } catch (CursorWindowAllocationException e) {
                // handle CursorWindowAllocationException when fetching events, defer upload
                uploadingCurrently.set(false);
                logger.e(TAG, String.format(
                        "Caught Cursor window exception during event upload, deferring upload: %s",
                        e.getMessage()
                ));
            }
        }
    }

    /**
     * Internal method to merge unsent events and identifies into a single array by sequence number.
     *
     * @param events    the events
     * @param numEvents the num events
     * @return the merged array, max event id, and max identify id
     * @throws JSONException the json exception
     */
    protected Pair<Long, JSONArray> getEventsWithMaxId(List<JSONObject> events, long numEvents) throws JSONException {
        JSONArray merged = new JSONArray();
        long maxEventId = -1;
        int count = 0;

        while (count < numEvents) {
            JSONObject event = events.get(count++);
            event.put("serverUploadTime", dateFormat.format(new Date()));
            maxEventId = event.getLong("eventId");
            merged.put(event);
        }

        return new Pair<>(maxEventId, merged);
    }

    /**
     * Internal method to generate the event upload post request.
     *
     * @param client     the client
     * @param events     the events
     * @param maxEventId the max event id
     */
    protected void makeEventUploadPostRequest(Call.Factory client, JSONObject events, final long maxEventId) {
        Request authenticateRequest;
        Request.Builder eventRequestBuilder;
        try {
            ApiRequest authenticateReq = new ApiRequest(this.serverUsername, this.serverPassword);
            RequestBody authenticateRequestBody = RequestBody.create(JSON, mapper.writeValueAsString(authenticateReq));
            Request.Builder authenticateRequestBuilder = new Request.Builder()
                    .url(url + POST_AUTHENTICATE)
                    .post(authenticateRequestBody);

            RequestBody eventRequestBody = RequestBody.create(JSON, events.toString());
            eventRequestBuilder = new Request.Builder()
                    .url(url + POST_EVENT)
                    .post(eventRequestBody);

            authenticateRequest = authenticateRequestBuilder.build();
        } catch (IllegalArgumentException | JsonProcessingException e) {
            logger.e(TAG, e.toString());
            uploadingCurrently.set(false);
            return;
        }

        boolean uploadSuccess = false;

        try {
            if (Utils.isEmptyString(bearerToken)) {
                postAuthenticateRequest(client, authenticateRequest);
            }

            if (!Utils.isEmptyString(bearerToken)) {
                eventRequestBuilder.addHeader("Authorization", bearerToken);
                Request eventRequest = eventRequestBuilder.build();
                Response eventResponse = client.newCall(eventRequest).execute();
                if (eventResponse.code() == 200) {
                    logger.d(TAG, "Successfully posted an events to API server");
                    uploadSuccess = true;
                    logThread.post(() -> {
                        if (maxEventId >= 0) dbHelper.removeEvents(maxEventId);
                        uploadingCurrently.set(false);
                        if (dbHelper.getTotalEventCount() > eventUploadThreshold) {
                            logThread.post(() -> updateServer(backoffUpload));
                        } else {
                            backoffUpload = false;
                            backoffUploadBatchSize = eventUploadMaxBatchSize;
                        }
                    });
                } else if (eventResponse.code() == 401) {
                    boolean isAuthenticated = postAuthenticateRequest(client, authenticateRequest);
                    if (isAuthenticated) {
                        makeEventUploadPostRequest(client, events, maxEventId);
                    }
                } else if (eventResponse.code() == 413) {
                    // If blocked by one massive event, drop it
                    if (backoffUpload && backoffUploadBatchSize == 1) {
                        if (maxEventId >= 0) dbHelper.removeEvent(maxEventId);
                        // maybe we want to reset backoffUploadBatchSize after dropping massive event
                    }

                    // Server complained about length of request, backoff and try again
                    backoffUpload = true;
                    int numEvents = Math.min((int) dbHelper.getEventCount(), backoffUploadBatchSize);
                    backoffUploadBatchSize = (int) Math.ceil(numEvents / 2.0);
                    logger.w(TAG, "Request too large, will decrease size and attempt to reupload");
                    logThread.post(() -> {
                        uploadingCurrently.set(false);
                        updateServer(true);
                    });
                } else {
                    logger.w(TAG, "Upload failed, " + eventResponse.code() + ", will attempt to reupload later");
                }
            }
        } catch (java.net.ConnectException e) {
            logger.w(TAG, "No internet connection found, unable to upload events");
            lastError = e;
        } catch (java.net.UnknownHostException e) {
            logger.w(TAG, "No internet connection found, unable to upload events");
            lastError = e;
        } catch (IOException e) {
            logger.e(TAG, e.toString());
            lastError = e;
        } catch (AssertionError e) {
            // This can be caused by a NoSuchAlgorithmException thrown by DefaultHttpClient
            logger.e(TAG, "Exception:", e);
            lastError = e;
        } catch (Exception e) {
            // Just log any other exception so things don't crash on upload
            logger.e(TAG, "Exception:", e);
            lastError = e;
        }

        if (!uploadSuccess) {
            uploadingCurrently.set(false);
        }
    }

    private boolean postAuthenticateRequest(Call.Factory client, Request authenticateRequest) throws IOException {
        Response authenticateResponse = client.newCall(authenticateRequest).execute();
        ResponseBody response = authenticateResponse.body();
        try {
            ApiResponse authenticateApiResponse = mapper.readValue(response.string(), ApiResponse.class);
            if (authenticateResponse.code() == 200) {
                bearerToken = authenticateApiResponse.getToken();
                logger.d(TAG, "Successfully called an authenticate API");
                return true;
            } else {
                logger.e(TAG, "Error in calling authenticate API : " + authenticateApiResponse.getMessage());
            }
        } catch (Exception e) {
            logger.e(TAG, "Error in calling authenticate API");
        }
        return false;
    }

    /**
     * Get the current device id. Can be null if deviceId hasn't been initialized yet.
     *
     * @return A unique identifier for tracking within the analytics system.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets a custom device id. <b>Note: only do this if you know what you are doing!</b>
     *
     * @param deviceId the device id
     * @return the MobilewallaClient
     */
    public MobilewallaClient setDeviceId(final String deviceId) {
        Set<String> invalidDeviceIds = getInvalidDeviceIds();
        if (!contextSet("setDeviceId()") || Utils.isEmptyString(deviceId) ||
                invalidDeviceIds.contains(deviceId)) {
            return this;
        }

        final MobilewallaClient client = this;
        runOnLogThread(() -> {
            client.deviceId = deviceId;
            saveDeviceId(deviceId);
        });
        return this;
    }

    // don't need to keep this in memory, if only using it at most 1 or 2 times
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

    private String initializeDeviceId() {
        Set<String> invalidIds = getInvalidDeviceIds();

        // see if device id already stored in db
        String deviceId = dbHelper.getValue(DEVICE_ID_KEY);
        if (!(Utils.isEmptyString(deviceId) || invalidIds.contains(deviceId))) {
            return deviceId;
        }

        if (!newDeviceIdPerInstall && useAdvertisingIdForDeviceId && !deviceInfo.isLimitAdTrackingEnabled()) {
            // Android ID is deprecated by Google.
            // We are required to use Advertising ID, and respect the advertising ID preference

            String advertisingId = deviceInfo.getAdvertisingId();
            if (!(Utils.isEmptyString(advertisingId) || invalidIds.contains(advertisingId))) {
                saveDeviceId(advertisingId);
                return advertisingId;
            }
        }

        // If this still fails, generate random identifier that does not persist
        // across installations. Append R to distinguish as randomly generated
        String randomId = deviceInfo.generateUUID() + "R";
        saveDeviceId(randomId);
        return randomId;
    }

    private void saveDeviceId(String deviceId) {
        dbHelper.insertOrReplaceKeyValue(DEVICE_ID_KEY, deviceId);
    }

    protected void runOnLogThread(Runnable r) {
        if (Thread.currentThread() != logThread) {
            logThread.post(r);
        } else {
            r.run();
        }
    }

    /**
     * Internal method to replace null event fields with JSON null object.
     *
     * @param obj the obj
     * @return the object
     */
    protected Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    /**
     * Internal method to check whether application context is set
     *
     * @param methodName the parent method name to print in error message
     * @return whether application context is set
     */
    protected synchronized boolean contextSet(String methodName) {
        if (context == null) {
            logger.e(TAG, "context cannot be null, set context with initialize() before calling " + methodName);
            return false;
        }
        return true;
    }

    /**
     * Internal method to convert bytes to hex string
     *
     * @param bytes the bytes
     * @return the string
     */
    protected String bytesToHexString(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Internal method to fetch the current time millis. Used for testing.
     *
     * @return the current time millis
     */
    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
