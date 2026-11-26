package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    TextView login;
    EditText name, email, mobilenumber, password, confirmpassword;
    Spinner roleSpinner;
    Button signupbtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        name = findViewById(R.id.signup_name);
        email = findViewById(R.id.signup_email);
        mobilenumber = findViewById(R.id.signup_phone);
        password = findViewById(R.id.signup_password);
        confirmpassword = findViewById(R.id.signup_confirm_password);
        roleSpinner = findViewById(R.id.signup_role_spinner);
        signupbtn = findViewById(R.id.btnSignUp);
        login = findViewById(R.id.signup_login);
        auth = FirebaseAuth.getInstance();

        String[] roles = {"Normal User", "Volunteer"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        signupbtn.setOnClickListener(view -> {
            String name_text = name.getText().toString().trim();
            String email_text = email.getText().toString().trim();
            String mobile_text = mobilenumber.getText().toString().trim();
            String password_text = password.getText().toString().trim();
            String confirm_password_text = confirmpassword.getText().toString().trim();

            if (name_text.isEmpty()) {
                name.setError("Enter your name");
                name.requestFocus();
                return;
            }
            if (email_text.isEmpty()) {
                email.setError("Enter your email");
                email.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()) {
                email.setError("Enter valid email");
                email.requestFocus();
                return;
            }
            if (mobile_text.isEmpty()) {
                mobilenumber.setError("Enter your number");
                mobilenumber.requestFocus();
                return;
            }
            if (mobile_text.length() != 10) {
                mobilenumber.setError("Enter 10 digit mobile number");
                mobilenumber.requestFocus();
                return;
            }
            if (password_text.isEmpty()) {
                password.setError("Enter your password");
                password.requestFocus();
                return;
            }
            if (password_text.length() < 6) {
                password.setError("Enter minimum 6 digit password");
                password.requestFocus();
                return;
            }
            if (confirm_password_text.isEmpty()) {
                confirmpassword.setError("Confirm your password");
                confirmpassword.requestFocus();
                return;
            }
            if (!confirm_password_text.equals(password_text)) {
                confirmpassword.setError("Passwords don't match");
                confirmpassword.requestFocus();
                return;
            }

            String selectedOption = roleSpinner.getSelectedItem().toString();
            String roleValue = "Volunteer".equals(selectedOption) ? "volunteer" : "user";

            registerUser(name_text, email_text, mobile_text, password_text, roleValue);
        });

        login.setOnClickListener(view -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void registerUser(String nameText, String emailText, String mobileText, String passwordText, String roleValue) {
        signupbtn.setEnabled(false);
        String cleanEmail = emailText.toLowerCase(Locale.ROOT);

        auth.createUserWithEmailAndPassword(cleanEmail, passwordText)
                .addOnCompleteListener(SignUp.this, task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", nameText);
                        userData.put("email", cleanEmail);
                        userData.put("mobile", mobileText);
                        userData.put("role", roleValue);
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance().collection("users").document(uid).set(userData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(SignUp.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                    Class<?> nextScreen = "volunteer".equals(roleValue)
                                            ? VolunteerHomeScreen.class
                                            : Home.class;
                                    Intent intent = new Intent(SignUp.this, nextScreen);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    signupbtn.setEnabled(true);
                                    Toast.makeText(SignUp.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        signupbtn.setEnabled(true);
                        String reason = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SignUp.this, "Registration Failed: " + reason, Toast.LENGTH_LONG).show();
                    }
                });
    }
}