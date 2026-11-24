package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    TextView signup_page, forgot_pass;
    Button login_btn;
    EditText email, password;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();

        login_btn.setOnClickListener(view -> {
            String email_text = email.getText().toString().trim();
            String password_text = password.getText().toString().trim();

            if (email_text.isEmpty()) {
                email.setError("Enter your email");
                email.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()) {
                email.setError("Enter valid email");
                email.requestFocus();
            } else if (password_text.isEmpty()) {
                password.setError("Enter your password");
                password.requestFocus();
            } else if (password_text.length() < 6) {
                password.setError("Enter minimum 6 digit password");
                password.requestFocus();
            } else {
                loginUser(email_text, password_text);
            }
        });

        forgot_pass.setOnClickListener(view -> {
            startActivity(new Intent(this, ForgotPassword.class));
            finish();
        });

        signup_page.setOnClickListener(view -> {
            startActivity(new Intent(this, SignUp.class));
            finish();
        });
    }

    private void loginUser(String emailText, String passwordText) {
        login_btn.setEnabled(false);
        auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        login_btn.setEnabled(true);
                        return;
                    }
                    routeUserByRole(user);
                })
                .addOnFailureListener(e -> {
                    login_btn.setEnabled(true);
                    Toast.makeText(Login.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void routeUserByRole(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.exists() ? doc.getString("role") : null;

                    if ("admin".equalsIgnoreCase(role)) {
                        goTo(Home.class, "Welcome Admin"); // admin dashboard nu home karyu
                    } else if ("volunteer".equalsIgnoreCase(role)) {
                        goTo(VolunteerHomeScreen.class, "Welcome Volunteer");
                    } else {
                        // Fallback: check if admin added this email in the volunteers collection
                        checkVolunteerCollectionFallback(user.getEmail());
                    }
                })
                .addOnFailureListener(e -> checkVolunteerCollectionFallback(user.getEmail()));
    }

    private void checkVolunteerCollectionFallback(String userEmail) {
        if (userEmail == null) {
            goTo(Home.class, "Login Successful");
            return;
        }
        db.collection("volunteers").whereEqualTo("email", userEmail).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        goTo(VolunteerHomeScreen.class, "Welcome Volunteer");
                    } else {
                        goTo(Home.class, "Login Successful");
                    }
                })
                .addOnFailureListener(e -> goTo(Home.class, "Login Successful"));
    }

    private void goTo(Class<?> targetActivity, String message) {
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Login.this, targetActivity));
        finish();
    }
}