package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {

    TextView signup_page,forgot_pass;
    Button login_btn;
    EditText email,password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        signup_page = findViewById(R.id.login_SignUp);
        forgot_pass = findViewById(R.id.login_forgot);
        login_btn = findViewById(R.id.login_btn);
        email = findViewById(R.id.login_email);
        password = findViewById(R.id.login_password);

        login_btn.setOnClickListener(view -> {

            String email_text = email.getText().toString().trim();
            String password_text = password.getText().toString().trim();

            if (email_text.isEmpty()) {
                email.setError("Enter your email");
                email.requestFocus();
            }
            if(!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()){
                email.setError("Enter valid email");
                email.requestFocus();
            }
            if (password_text.isEmpty()){
                password.setError("Enter your password");
                password.requestFocus();
            }
            if(password_text.length()<6){
                password.setError("Enter minimum 6 digit password");
                password.requestFocus();
            }
            else {
                Toast.makeText(this, "welcome again", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Home.class));
                finish();
                }
            });
        forgot_pass.setOnClickListener(view -> {
            startActivity(new Intent(this,ForgotPassword.class));
            finish();
        });
        signup_page.setOnClickListener(view -> {
            startActivity(new Intent(this, SignUp.class));
            finish();
        });
    }
}