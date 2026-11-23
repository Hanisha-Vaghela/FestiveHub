package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    TextView login;
    EditText name,email,mobilenumber,password,confirmpassword;
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
        signupbtn = findViewById(R.id.btnSignUp);
        login = findViewById(R.id.signup_login);
        auth = FirebaseAuth.getInstance();

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
//                    Toast.makeText(this,"User Added Successfully!",Toast.LENGTH_SHORT).show();
//                    startActivity(new Intent(this, Home.class));
//                    finish();
                registerUser(name_text, email_text, mobile_text, password_text);
            }
        });

        login.setOnClickListener(view -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void registerUser(String nameText, String emailText,String mobileText, String passwordText) {
        auth.createUserWithEmailAndPassword(emailText,passwordText).addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
           if (task.isSuccessful()) {

               String uid = auth.getCurrentUser().getUid();
               Map<String,Object> userData = new HashMap<>();
               userData.put("name",nameText);
               userData.put("email",emailText);
               userData.put("mobile",mobileText);
               userData.put("createAt", FieldValue.serverTimestamp());

               FirebaseFirestore.getInstance().collection("users").document(uid).set(userData).addOnCompleteListener( saveTask -> {
                   Toast.makeText(SignUp.this, "User Added Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUp.this, Home.class));
                     finish();

               });

//               Toast.makeText(SignUp.this, "User Added Successfully!", Toast.LENGTH_SHORT).show();
//               startActivity(new Intent(SignUp.this, Home.class));
//               finish();
                }else {
                    String reason = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(SignUp.this, "Registration Failed !" + reason, Toast.LENGTH_SHORT).show();

           }
            }
        });
    }
}