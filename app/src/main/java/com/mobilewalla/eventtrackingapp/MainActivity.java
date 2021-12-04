package com.mobilewalla.eventtrackingapp;

import static com.mobilewalla.eventtracking.client.MobilewallaClient.getService;
import static com.mobilewalla.eventtracking.util.Utils.getResponse;
import static com.mobilewalla.eventtracking.util.Utils.isDeviceConnected;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.mobilewalla.eventtracking.client.MobilewallaClient;
import com.mobilewalla.eventtracking.models.Response;
import com.mobilewalla.eventtracking.service.MobilewallaServiceImpl;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    Button btn_postEvent;
    TextView tv_response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViews();

        MobilewallaClient.initialize(this, response -> {
            Response apiResponse = getResponse(response);
            tv_response.append("Event : " + apiResponse.getMessage() + "\n\n");
        }, error -> processError(error, "Event : "));

        MobilewallaServiceImpl service = getService();
        service.setUserId("user101");
        JSONObject eventProperties = new JSONObject();
        JSONObject userProperties = new JSONObject();
        JSONObject globalUserProperties = new JSONObject();
        JSONObject groupProperties = new JSONObject();
        try {
            eventProperties.put("name", "eventProperties");
            userProperties.put("name", "userProperties");
            globalUserProperties.put("name", "globalUserProperties");
            groupProperties.put("name", "groupProperties");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        btn_postEvent.setOnClickListener(v -> service.logEvent("eventType101", 345L, eventProperties, userProperties, globalUserProperties, groupProperties));
    }

    private void setViews() {
        btn_postEvent = findViewById(R.id.btn_postEvent);
        tv_response = findViewById(R.id.tv_response);
        tv_response.setMovementMethod(new ScrollingMovementMethod());
    }

    private void processError(VolleyError error, String s) {
        try {
            if (isDeviceConnected(this)) {
                String responseBody = new String(error.networkResponse.data);
                JSONObject response = new JSONObject(responseBody);
                Response apiResponse = getResponse(response);
                tv_response.append(s + apiResponse.getMessage() + "\n\n");
            }
        } catch (Exception ignored) {
        }
    }
}