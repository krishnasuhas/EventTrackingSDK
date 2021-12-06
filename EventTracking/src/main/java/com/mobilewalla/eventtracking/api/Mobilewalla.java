package com.mobilewalla.eventtracking.api;

import com.mobilewalla.eventtracking.util.Utils;

import java.util.HashMap;
import java.util.Map;


/**
 * <h1>Mobilewalla</h1>
 * This is the main Mobilewalla class that manages SDK instances. <br><br>
 * <b>NOTE:</b> All of the methods except {@code getInstance()} have been deprecated.
 * Please call those methods on the MobilewallaClient instance instead, for example:
 * {@code Mobilewalla.getInstance().logEvent();}
 *
 * @see MobilewallaClient MobilewallaClient
 */
public class Mobilewalla {

    static final Map<String, MobilewallaClient> instances = new HashMap<>();

    /**
     * Gets the default instance.
     *
     * @return the default instance
     */
    public static MobilewallaClient getInstance() {
        return getInstance(null);
    }

    /**
     * Gets the specified instance. If instance is null or empty string, fetches the default
     * instance instead.
     *
     * @param instance name to get "ex app 1"
     * @return the specified instance
     */
    public static synchronized MobilewallaClient getInstance(String instance) {
        instance = Utils.normalizeInstanceName(instance);
        MobilewallaClient client = instances.get(instance);
        if (client == null) {
            client = new MobilewallaClient(instance);
            instances.put(instance, client);
        }
        return client;
    }
}
