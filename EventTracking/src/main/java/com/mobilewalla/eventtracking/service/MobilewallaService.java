package com.mobilewalla.eventtracking.service;

import org.json.JSONObject;

public interface MobilewallaService {
    void logEvent(String eventType);

    void logEvent(String eventType, JSONObject eventProperties);

    void logEvent(String eventType, JSONObject eventProperties, JSONObject globalUserProperties);

    void logEvent(String eventType, JSONObject eventProperties, JSONObject userProperties, JSONObject globalUserProperties, JSONObject groupProperties);

    void logEvent(String eventType, long eventId, JSONObject eventProperties, JSONObject userProperties, JSONObject globalUserProperties, JSONObject groupProperties);

}
