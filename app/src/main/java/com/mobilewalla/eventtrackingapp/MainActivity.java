package com.mobilewalla.eventtrackingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button btLogin;
    EditText etUsername, etPassword;

    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sessionManager = new SessionManager(getApplicationContext());

        setupLogin();
    }

    private void setupLogin() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btLogin = findViewById(R.id.bt_login);
        btLogin.setOnClickListener((View v) -> {
            String sUserName = etUsername.getText().toString().trim();
            String sPassword = etPassword.getText().toString().trim();

            if (sPassword.equals("")) {
                etPassword.setError("Please enter password");
            } else if (sPassword.equals("root")) {
                sessionManager.setLogin(true);
                sessionManager.setUsername(sUserName);
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        });
        if (sessionManager.getLogin()) {
            startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            finish();
        }
    }
}