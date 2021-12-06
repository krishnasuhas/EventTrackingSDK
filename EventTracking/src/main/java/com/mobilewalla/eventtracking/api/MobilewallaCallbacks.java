package com.mobilewalla.eventtracking.api;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

class MobilewallaCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = MobilewallaCallbacks.class.getName();
    private static final String NULLMSG = "Need to initialize MobilewallaCallbacks with MobilewallaClient instance";

    private MobilewallaClient clientInstance = null;
    private static MobilewallaLog logger = MobilewallaLog.getLogger();

    public MobilewallaCallbacks(MobilewallaClient clientInstance) {
        if (clientInstance == null) {
            logger.e(TAG, NULLMSG);
            return;
        }

        this.clientInstance = clientInstance;
        clientInstance.useForegroundTracking();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {
        if (clientInstance == null) {
            logger.e(TAG, NULLMSG);
            return;
        }

        clientInstance.onExitForeground(getCurrentTimeMillis());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (clientInstance == null) {
            logger.e(TAG, NULLMSG);
            return;
        }

        clientInstance.onEnterForeground(getCurrentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outstate) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
