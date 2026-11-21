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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    TextView signup_page,forgot_pass;
    Button login_btn;
    EditText email,password;
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        signup_page = findViewById(R.id.login_SignUp);
        forgot_pass = findViewById(R.id.login_forgot);
        login_btn = findViewById(R.id.login_btn);
        email = findViewById(R.id.login_email);
        password = findViewById(R.id.login_password);

        auth = FirebaseAuth.getInstance();

        login_btn.setOnClickListener(view -> {

            String email_text = email.getText().toString().trim();
            String password_text = password.getText().toString().trim();


            if(email_text.isEmpty()) {
                email.setError("Enter your email");
                email.requestFocus();
            }
            else if(!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()){
                email.setError("Enter valid email");
                email.requestFocus();
            }
            else if (password_text.isEmpty()){
                password.setError("Enter your password");
                password.requestFocus();
            }
            else if(password_text.length()<6){
                password.setError("Enter minimum 6 digit password");
                password.requestFocus();
            }else {
                loginUser(email_text,password_text);

            }

//                Toast.makeText(this, "welcome again", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(this, Home.class));
//                finish();
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

//    private void loginUser(String emailText, String passwordText) {
//
//        auth.signInWithEmailAndPassword(emailText,passwordText).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
//            @Override
//            public void onSuccess(AuthResult authResult) {
//                Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(Login.this, Home.class));
//                finish();
//            }
//        });
//    }
private void loginUser(String emailText, String passwordText) {
    auth.signInWithEmailAndPassword(emailText, passwordText)
            .addOnSuccessListener(authResult -> {
                Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Login.this, Home.class));
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(Login.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
}
}