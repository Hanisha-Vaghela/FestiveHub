package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/** Admin home: one button per type of data. */
public class AdminDashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        TextView back = findViewById(R.id.admin_back);
        Button btnEvents = findViewById(R.id.admin_btn_events);
        Button btnStudents = findViewById(R.id.admin_btn_students);
        Button btnVolunteers = findViewById(R.id.admin_btn_volunteers);
        Button btnColleges = findViewById(R.id.admin_btn_colleges);
        Button btnNotifications = findViewById(R.id.admin_btn_notifications);

        back.setOnClickListener(v -> finish());
        btnEvents.setOnClickListener(v -> open("events"));
        btnStudents.setOnClickListener(v -> open("students"));
        btnVolunteers.setOnClickListener(v -> open("volunteers"));
        btnColleges.setOnClickListener(v -> open("colleges"));
        btnNotifications.setOnClickListener(v -> open("notifications"));

        // Only users with role = "admin" may stay here (Firestore rules enforce it too)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!"admin".equals(doc.getString("role"))) {
                        Toast.makeText(this, "Admins only", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> finish());
    }

    private void open(String entityKey) {
        Intent intent = new Intent(this, ManageList.class);
        intent.putExtra("entity", entityKey);
        startActivity(intent);
    }
}
