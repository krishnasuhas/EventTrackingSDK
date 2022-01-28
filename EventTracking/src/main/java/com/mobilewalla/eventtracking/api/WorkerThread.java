package com.mobilewalla.eventtracking.api;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

public class WorkerThread extends HandlerThread {

    private Handler handler;

    public WorkerThread(String name) {
        super(name, Process.THREAD_PRIORITY_BACKGROUND);
    }

    void post(Runnable r) {
        waitForInitialization();
        handler.post(r);
    }

    void postDelayed(Runnable r, long delayMillis) {
        waitForInitialization();
        handler.postDelayed(r, delayMillis);
    }

    private synchronized void waitForInitialization() {
        if (handler == null) {
            handler = new Handler(getLooper());
        }
    }
}
