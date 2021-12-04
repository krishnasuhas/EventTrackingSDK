package com.mobilewalla.eventtracking.model;

import static com.mobilewalla.eventtracking.client.MobilewallaClient.getContext;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.mobilewalla.eventtracking.util.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("MissingPermission")
public class DeviceDetails {
    public static final String OS_NAME = "android";
    private static final String SETTING_LIMIT_AD_TRACKING = "limit_ad_tracking";
    private static final String SETTING_ADVERTISING_ID = "advertising_id";

    private boolean locationListening;

    private RawInfo rawInfo;

    public DeviceDetails(boolean locationListening) {
        this.locationListening = locationListening;
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private RawInfo getCachedInfo() {
        if (rawInfo == null) {
            rawInfo = new RawInfo();
        }
        return rawInfo;
    }

    public void prefetch() {
        getCachedInfo();
    }

    public String getVersionName() {
        return getCachedInfo().versionName;
    }

    public String getOsName() {
        return getCachedInfo().osName;
    }

    public String getOsVersion() {
        return getCachedInfo().osVersion;
    }

    public String getBrand() {
        return getCachedInfo().brand;
    }

    public String getManufacturer() {
        return getCachedInfo().manufacturer;
    }

    public String getModel() {
        return getCachedInfo().model;
    }

    public String getCarrier() {
        return getCachedInfo().carrier;
    }

    public String getCountry() {
        return getCachedInfo().country;
    }

    public String getLanguage() {
        return getCachedInfo().language;
    }

    public String getAdvertisingId() {
        return getCachedInfo().advertisingId;
    }

    public boolean isLimitAdTrackingEnabled() {
        return getCachedInfo().limitAdTrackingEnabled;
    }

    public String getAppSetId() {
        return getCachedInfo().appSetId;
    }

    public boolean isGooglePlayServicesEnabled() {
        return getCachedInfo().gpsEnabled;
    }

    public Location getMostRecentLocation() {
        if (!isLocationListening()) {
            return null;
        }
        if (!Utils.checkLocationPermissionAllowed(getContext())) {
            return null;
        }
        LocationManager locationManager = (LocationManager) getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }
        List<String> providers = null;
        try {
            providers = locationManager.getProviders(true);
        } catch (Exception ignored) {
        }
        if (providers == null) {
            return null;
        }

        List<Location> locations = new ArrayList<>();
        for (String provider : providers) {
            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(provider);
            } catch (Exception ignored) {
            }
            if (location != null) {
                locations.add(location);
            }
        }

        long maximumTimestamp = -1;
        Location bestLocation = null;
        for (Location location : locations) {
            if (location.getTime() > maximumTimestamp) {
                maximumTimestamp = location.getTime();
                bestLocation = location;
            }
        }

        return bestLocation;
    }

    public boolean isLocationListening() {
        return locationListening;
    }

    public void setLocationListening(boolean locationListening) {
        this.locationListening = locationListening;
    }

    protected Geocoder getGeocoder() {
        return new Geocoder(getContext(), Locale.ENGLISH);
    }

    private class RawInfo {
        private String advertisingId;
        private String country;
        private String versionName;
        private String osName;
        private String osVersion;
        private String brand;
        private String manufacturer;
        private String model;
        private String carrier;
        private String language;
        private boolean limitAdTrackingEnabled;
        private boolean gpsEnabled;
        private String appSetId;

        private RawInfo() {
            advertisingId = getAdvertisingId();
            versionName = getVersionName();
            osName = getOsName();
            osVersion = getOsVersion();
            brand = getBrand();
            manufacturer = getManufacturer();
            model = getModel();
            carrier = getCarrier();
            country = getCountry();
            language = getLanguage();
            gpsEnabled = checkGPSEnabled();
            appSetId = getAppSetId();
        }

        private String getVersionName() {
            PackageInfo packageInfo;
            try {
                packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                return packageInfo.versionName;
            } catch (Exception ignored) {
            }
            return null;
        }

        private String getOsName() {
            return OS_NAME;
        }

        private String getOsVersion() {
            return Build.VERSION.RELEASE;
        }

        private String getBrand() {
            return Build.BRAND;
        }

        private String getManufacturer() {
            return Build.MANUFACTURER;
        }

        private String getModel() {
            return Build.MODEL;
        }

        private String getCarrier() {
            try {
                TelephonyManager manager = (TelephonyManager) getContext()
                        .getSystemService(Context.TELEPHONY_SERVICE);
                return manager.getNetworkOperatorName();
            } catch (Exception ignored) {
            }
            return null;
        }

        private String getCountry() {
            String country = getCountryFromLocation();
            if (!Utils.isEmptyString(country)) {
                return country;
            }

            country = getCountryFromNetwork();
            if (!Utils.isEmptyString(country)) {
                return country;
            }
            return getCountryFromLocale();
        }

        private String getCountryFromLocation() {
            if (!isLocationListening()) {
                return null;
            }

            Location recent = getMostRecentLocation();
            if (recent != null) {
                try {
                    if (Geocoder.isPresent()) {
                        Geocoder geocoder = getGeocoder();
                        List<Address> addresses = geocoder.getFromLocation(recent.getLatitude(),
                                recent.getLongitude(), 1);
                        if (addresses != null) {
                            for (Address address : addresses) {
                                if (address != null) {
                                    return address.getCountryCode();
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        private String getCountryFromNetwork() {
            try {
                TelephonyManager manager = (TelephonyManager) getContext()
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                    String country = manager.getNetworkCountryIso();
                    if (country != null) {
                        return country.toUpperCase(Locale.US);
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        private String getCountryFromLocale() {
            return Locale.getDefault().getCountry();
        }

        private String getLanguage() {
            return Locale.getDefault().getLanguage();
        }

        private String getAdvertisingId() {
            if ("Amazon".equals(getManufacturer())) {
                return getAndCacheAmazonAdvertisingId();
            } else {
                return getAndCacheGoogleAdvertisingId();
            }
        }

        private String getAppSetId() {
            try {
                Class AppSet = Class.forName("com.google.android.gms.appset.AppSet");
                Method getClient = AppSet.getMethod("getClient", Context.class);
                Object appSetClient = getClient.invoke(null, getContext());
                Method getAppSetInfo = appSetClient.getClass().getMethod("getAppSetInfo");
                Object taskWithAppSetInfo = getAppSetInfo.invoke(appSetClient);
                Class Tasks = Class.forName("com.google.android.gms.tasks.Tasks");
                Method await = Tasks.getMethod("await", Class.forName("com.google.android.gms.tasks.Task"));
                Object appSetInfo = await.invoke(null, taskWithAppSetInfo);
                Method getId = appSetInfo.getClass().getMethod("getId");
                appSetId = (String) getId.invoke(appSetInfo);
            } catch (Exception ignored) {
            }

            return appSetId;
        }

        private String getAndCacheAmazonAdvertisingId() {
            ContentResolver cr = getContext().getContentResolver();

            limitAdTrackingEnabled = Secure.getInt(cr, SETTING_LIMIT_AD_TRACKING, 0) == 1;
            advertisingId = Secure.getString(cr, SETTING_ADVERTISING_ID);

            return advertisingId;
        }

        private String getAndCacheGoogleAdvertisingId() {
            try {
                Class AdvertisingIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo", Context.class);
                Object advertisingInfo = getAdvertisingInfo.invoke(null, getContext());
                Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod("isLimitAdTrackingEnabled");
                Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled.invoke(advertisingInfo);
                this.limitAdTrackingEnabled = limitAdTrackingEnabled != null && limitAdTrackingEnabled;
                Method getId = advertisingInfo.getClass().getMethod("getId");
                advertisingId = (String) getId.invoke(advertisingInfo);
            } catch (Exception ignored) {
            }
            return advertisingId;
        }

        private boolean checkGPSEnabled() {
            try {
                Class GPSUtil = Class
                        .forName("com.google.android.gms.common.GooglePlayServicesUtil");
                Method getGPSAvailable = GPSUtil.getMethod("isGooglePlayServicesAvailable",
                        Context.class);
                Integer status = (Integer) getGPSAvailable.invoke(null, getContext());
                return status != null && status == 0;
            } catch (Exception ignored) {
            }
            return false;
        }
    }

}
