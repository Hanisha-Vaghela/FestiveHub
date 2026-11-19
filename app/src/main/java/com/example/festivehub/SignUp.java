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

public class SignUp extends AppCompatActivity {

    TextView login;
    EditText name,email,mobilenumber,password,confirmpassword;
    Button signupbtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        name = findViewById(R.id.signup_name);
        email = findViewById(R.id.signup_email);
        mobilenumber = findViewById(R.id.signup_password);
        password = findViewById(R.id.signup_password);
        confirmpassword = findViewById(R.id.signup_confirmPassword);
        signupbtn = findViewById(R.id.signup_btn);
        login = findViewById(R.id.signup_login);

        signupbtn.setOnClickListener(view -> {
            String name_text = name.getText().toString().trim();
            String email_text = email.getText().toString().trim();
            String mobile_text = mobilenumber.getText().toString().trim();
            String password_text = password.getText().toString().trim();
            String confirm_password_text = confirmpassword.getText().toString().trim();

            if(name_text.isEmpty()){
                name.setError("Enter your name");
                name.requestFocus();
                return;
            }
            if (email_text.isEmpty()){
                email.setError("enter your email");
                email.requestFocus();
                return;
            }
            if(!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()){
                email.setError("Enter valid email");
                email.requestFocus();
                return;
            }
            if(mobile_text.isEmpty()){
                mobilenumber.setError("Enter your number");
                mobilenumber.requestFocus();
                return;
            }
            if(mobile_text.length() != 10){
                mobilenumber.setError("Enter atleat 10 number");
                mobilenumber.requestFocus();
                return;
            }
            if (password_text.isEmpty()){
                password.setError("Enter your password");
                password.requestFocus();
                return;
            }
            if(password_text.length()<6){
                password.setError("Enter valid password");
                password.requestFocus();
                return;
            }
            if (confirm_password_text.isEmpty()){
                confirmpassword.setError("Enter your password");
                confirmpassword.requestFocus();
                return;
            }
            if (!confirm_password_text.equals(password_text)){
                confirmpassword.setError("Password does't match");
                confirmpassword.requestFocus();
                return;
            }else {
                    Toast.makeText(this,"User Added Successfully!",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Home.class));
                    finish();
        }
        });

        login.setOnClickListener(view -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }
}