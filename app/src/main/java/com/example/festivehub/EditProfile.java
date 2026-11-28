package com.example.festivehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    private TextView tvBack, tvEmail;
    private EditText etName, etMobile;
    private Button btnSave;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        tvBack = findViewById(R.id.edit_profile_back);
        tvEmail = findViewById(R.id.edit_profile_email);
        etName = findViewById(R.id.edit_profile_name);
        etMobile = findViewById(R.id.edit_profile_mobile);
        btnSave = findViewById(R.id.edit_profile_btn_save);

        // Keep content clear of the system bars and keyboard
        View root = findViewById(R.id.edit_profile_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmail.setText(user.getEmail());
        tvBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());

        // On screen rotation, keep what the user has already typed
        loadProfile(savedInstanceState == null);
    }

    private void loadProfile(boolean fillFields) {
        // Save stays disabled until data is loaded, so blank fields can't overwrite it
        btnSave.setEnabled(false);

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (fillFields) {
                        String name = doc.getString("name");
                        String mobile = doc.getString("mobile");
                        if (name != null) etName.setText(name);
                        if (mobile != null) etMobile.setText(mobile);
                    }
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (name.length() < 2) {
            etName.setError("Enter your name (at least 2 characters)");
            etName.requestFocus();
            return;
        }
        if (!mobile.matches("\\d{10}")) {
            etMobile.setError("Enter 10 digit mobile number");
            etMobile.requestFocus();
            return;
        }

        setSaving(true);

        // merge() changes only these fields, so role and createdAt stay as they are
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("mobile", mobile);

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    updateNameInRegistrations(name);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setSaving(false);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // Each event registration stores its own copy of the name (used by admin and scanner)
    private void updateNameInRegistrations(String name) {
        String uid = user.getUid();
        db.collection("users").document(uid).collection("registeredEvents").get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // update() never creates a registration that doesn't exist
                        db.collection("events").document(doc.getId())
                                .collection("registrations").document(uid)
                                .update("name", name);
                    }
                });
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        btnSave.setText(saving ? "Saving..." : "Save Changes");
    }
}
