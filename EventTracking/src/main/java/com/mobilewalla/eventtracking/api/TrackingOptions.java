package com.mobilewalla.eventtracking.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class TrackingOptions {
    private static final String TAG = TrackingOptions.class.getName();

    private static String[] SERVER_SIDE_PROPERTIES = {
            Constants.TRACKING_OPTION_CITY,
            Constants.TRACKING_OPTION_COUNTRY,
            Constants.TRACKING_OPTION_DMA,
            Constants.TRACKING_OPTION_IP_ADDRESS,
            Constants.TRACKING_OPTION_LAT_LNG,
            Constants.TRACKING_OPTION_REGION,
    };

    private static String[] COPPA_CONTROL_PROPERTIES = {
            Constants.TRACKING_OPTION_ADID,
            Constants.TRACKING_OPTION_CITY,
            Constants.TRACKING_OPTION_IP_ADDRESS,
            Constants.TRACKING_OPTION_LAT_LNG,
    };

    Set<String> disabledFields = new HashSet<String>();

    public TrackingOptions disableAdid() {
        disableTrackingField(Constants.TRACKING_OPTION_ADID);
        return this;
    }

    boolean shouldTrackAdid() {
        return shouldTrackField(Constants.TRACKING_OPTION_ADID);
    }

    public TrackingOptions disableCarrier() {
        disableTrackingField(Constants.TRACKING_OPTION_CARRIER);
        return this;
    }

    boolean shouldTrackCarrier() {
        return shouldTrackField(Constants.TRACKING_OPTION_CARRIER);
    }

    public TrackingOptions disableCity() {
        disableTrackingField(Constants.TRACKING_OPTION_CITY);
        return this;
    }

    boolean shouldTrackCity() {
        return shouldTrackField(Constants.TRACKING_OPTION_CITY);
    }

    public TrackingOptions disableCountry() {
        disableTrackingField(Constants.TRACKING_OPTION_COUNTRY);
        return this;
    }

    boolean shouldTrackCountry() {
        return shouldTrackField(Constants.TRACKING_OPTION_COUNTRY);
    }

    public TrackingOptions disableDeviceBrand() {
        disableTrackingField(Constants.TRACKING_OPTION_DEVICE_BRAND);
        return this;
    }

    boolean shouldTrackDeviceBrand() {
        return shouldTrackField(Constants.TRACKING_OPTION_DEVICE_BRAND);
    }

    public TrackingOptions disableDeviceManufacturer() {
        disableTrackingField(Constants.TRACKING_OPTION_DEVICE_MANUFACTURER);
        return this;
    }

    boolean shouldTrackDeviceManufacturer() {
        return shouldTrackField(Constants.TRACKING_OPTION_DEVICE_MANUFACTURER);
    }

    public TrackingOptions disableDeviceModel() {
        disableTrackingField(Constants.TRACKING_OPTION_DEVICE_MODEL);
        return this;
    }

    boolean shouldTrackDeviceModel() {
        return shouldTrackField(Constants.TRACKING_OPTION_DEVICE_MODEL);
    }

    public TrackingOptions disableDma() {
        disableTrackingField(Constants.TRACKING_OPTION_DMA);
        return this;
    }

    boolean shouldTrackDma() {
        return shouldTrackField(Constants.TRACKING_OPTION_DMA);
    }

    public TrackingOptions disableIpAddress() {
        disableTrackingField(Constants.TRACKING_OPTION_IP_ADDRESS);
        return this;
    }

    boolean shouldTrackIpAddress() {
        return shouldTrackField(Constants.TRACKING_OPTION_IP_ADDRESS);
    }

    public TrackingOptions disableLanguage() {
        disableTrackingField(Constants.TRACKING_OPTION_LANGUAGE);
        return this;
    }

    boolean shouldTrackLanguage() {
        return shouldTrackField(Constants.TRACKING_OPTION_LANGUAGE);
    }

    public TrackingOptions disableLatLng() {
        disableTrackingField(Constants.TRACKING_OPTION_LAT_LNG);
        return this;
    }

    boolean shouldTrackLatLng() {
        return shouldTrackField(Constants.TRACKING_OPTION_LAT_LNG);
    }

    public TrackingOptions disableOsName() {
        disableTrackingField(Constants.TRACKING_OPTION_OS_NAME);
        return this;
    }

    boolean shouldTrackOsName() {
        return shouldTrackField(Constants.TRACKING_OPTION_OS_NAME);
    }

    public TrackingOptions disableOsVersion() {
        disableTrackingField(Constants.TRACKING_OPTION_OS_VERSION);
        return this;
    }

    boolean shouldTrackOsVersion() {
        return shouldTrackField(Constants.TRACKING_OPTION_OS_VERSION);
    }

    public TrackingOptions disableApiLevel() {
        disableTrackingField(Constants.TRACKING_OPTION_API_LEVEL);
        return this;
    }

    boolean shouldTrackApiLevel() {
        return shouldTrackField(Constants.TRACKING_OPTION_API_LEVEL);
    }

    public TrackingOptions disablePlatform() {
        disableTrackingField(Constants.TRACKING_OPTION_PLATFORM);
        return this;
    }

    boolean shouldTrackPlatform() {
        return shouldTrackField(Constants.TRACKING_OPTION_PLATFORM);
    }

    public TrackingOptions disableRegion() {
        disableTrackingField(Constants.TRACKING_OPTION_REGION);
        return this;
    }

    boolean shouldTrackRegion() {
        return shouldTrackField(Constants.TRACKING_OPTION_REGION);
    }

    public TrackingOptions disableVersionName() {
        disableTrackingField(Constants.TRACKING_OPTION_VERSION_NAME);
        return this;
    }

    boolean shouldTrackVersionName() {
        return shouldTrackField(Constants.TRACKING_OPTION_VERSION_NAME);
    }

    private void disableTrackingField(String field) {
        disabledFields.add(field);
    }

    protected JSONObject getApiPropertiesTrackingOptions() {
        JSONObject apiPropertiesTrackingOptions = new JSONObject();
        if (disabledFields.isEmpty()) {
            return apiPropertiesTrackingOptions;
        }

        for (String key : SERVER_SIDE_PROPERTIES) {
            if (disabledFields.contains(key)) {
                try {
                    apiPropertiesTrackingOptions.put(key, false);
                } catch (JSONException e) {
                    MobilewallaLog.getLogger().e(TAG, e.toString());
                }
            }
        }
        return apiPropertiesTrackingOptions;
    }

    private boolean shouldTrackField(String field) {
        return !disabledFields.contains(field);
    }

    TrackingOptions mergeIn(TrackingOptions other) {
        for (String key : other.disabledFields) {
            disableTrackingField(key);
        }

        return this;
    }

    static TrackingOptions copyOf(TrackingOptions other) {
        TrackingOptions trackingOptions = new TrackingOptions();
        for (String key : other.disabledFields) {
            trackingOptions.disableTrackingField(key);
        }

        return trackingOptions;
    }

    static TrackingOptions forCoppaControl() {
        TrackingOptions trackingOptions = new TrackingOptions();
        for (String key : COPPA_CONTROL_PROPERTIES) {
            trackingOptions.disableTrackingField(key);
        }

        return trackingOptions;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;  // self check
        }
        if (other == null) {
            return false;  // null check
        }
        if (getClass() != other.getClass()) {
            return false;  // type check and cast
        }

        TrackingOptions options = (TrackingOptions) other;
        return options.disabledFields.equals(this.disabledFields);
    }
}
