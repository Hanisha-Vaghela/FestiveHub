package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPassword extends AppCompatActivity {

    EditText etEmail;
    Button btnSendResetLink;
    TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.forgot_email);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.forgot_back_to_login);

        btnSendResetLink.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            Toast.makeText(ForgotPassword.this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
        });

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPassword.this, Login.class));
            finish();
        });
    }
}