package com.mobilewalla.eventtrackingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mobilewalla.eventtracking.api.Mobilewalla;
import com.mobilewalla.eventtracking.api.MobilewallaClient;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity2 extends AppCompatActivity {
    Button btn_postEvent, bt_logout;
    TextView tv_response;

    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setViews();

        sessionManager = new SessionManager(getApplicationContext());

        setupLogout();
        setupPostEvent();
    }

    private void setupLogout() {
        bt_logout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

            builder.setTitle("Logout");
            builder.setMessage("Are you sure to Log out?");

            builder.setPositiveButton("Yes", (dialog, which) -> {
                sessionManager.setLogin(false);
                sessionManager.setUsername("");
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            });

            builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }

    private void setupPostEvent() {
        MobilewallaClient client = Mobilewalla.getInstance()
                .initialize(getApplicationContext(), sessionManager.getUsername())
                .enableForegroundTracking(getApplication());
        client.setLogLevel(Log.DEBUG);

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

        btn_postEvent.setOnClickListener(v -> {
            long dateInMillis = System.currentTimeMillis();
            client.logEvent("eventType_" + dateInMillis, eventProperties, null, userProperties, groupProperties, globalUserProperties, dateInMillis, false);
        });
    }

    private void setViews() {
        btn_postEvent = findViewById(R.id.btn_postEvent);
        tv_response = findViewById(R.id.tv_response);
        tv_response.setMovementMethod(new ScrollingMovementMethod());
        bt_logout = findViewById(R.id.btn_logout);
    }
}