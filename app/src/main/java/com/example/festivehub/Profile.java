package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class Profile extends AppCompatActivity {

    TextView tvBack, tvAvatar, tvName, tvEmail, tvMobile, tvNoEvents;
    RecyclerView rvRegistered;
    Button btnLogout, btnAdmin, btnEditProfile;

    FirebaseAuth auth;
    FirebaseFirestore db;
    ListenerRegistration registeredListener;

    ArrayList<EventList> registeredEvents = new ArrayList<>();
    RegisteredEventAdapter adapter;
    String currentUserRole = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvBack = findViewById(R.id.profile_back);
        tvAvatar = findViewById(R.id.profile_avatar);
        tvName = findViewById(R.id.profile_name);
        tvEmail = findViewById(R.id.profile_email);
        tvMobile = findViewById(R.id.profile_mobile);
        tvNoEvents = findViewById(R.id.profile_no_events);
        rvRegistered = findViewById(R.id.profile_rv_events);
        btnLogout = findViewById(R.id.profile_btn_logout);
        btnAdmin = findViewById(R.id.profile_btn_admin);
        btnEditProfile = findViewById(R.id.profile_btn_edit);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }
        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> startActivity(new Intent(Profile.this, AdminDashboard.class)));
        }
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> startActivity(new Intent(Profile.this, EditProfile.class)));
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                auth.signOut();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                goToLogin();
            });
        }

        if (rvRegistered != null) {
            adapter = new RegisteredEventAdapter(registeredEvents, this::onRegisteredEventClicked);
            rvRegistered.setLayoutManager(new LinearLayoutManager(this));
            rvRegistered.setAdapter(adapter);
        }

        showBasicInfo(user);
        loadRegisteredEvents(user.getUid());
    }

    // Runs on first open and again after returning from EditProfile
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loadProfile(user.getUid());
        }
    }

    private void onRegisteredEventClicked(EventList event) {
        if ("volunteer".equalsIgnoreCase(currentUserRole)) {
            Intent intent = new Intent(Profile.this, Event_Detail.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
            return;
        }

        String[] options = {"Show Entry QR Code", "View Event Details"};
        new AlertDialog.Builder(this)
                .setTitle(event.getTitle() != null ? event.getTitle() : "Registered Event")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent qrIntent = new Intent(Profile.this, UserQR.class);
                        qrIntent.putExtra("eventId", event.getId());
                        qrIntent.putExtra("eventTitle", event.getTitle());
                        startActivity(qrIntent);
                    } else {
                        Intent detailIntent = new Intent(Profile.this, Event_Detail.class);
                        detailIntent.putExtra("eventId", event.getId());
                        startActivity(detailIntent);
                    }
                })
                .show();
    }

    private void showBasicInfo(FirebaseUser user) {
        String email = user.getEmail() != null ? user.getEmail() : "";
        if (tvEmail != null) tvEmail.setText(email);
        setName(email.contains("@") ? email.substring(0, email.indexOf("@")) : "User");
        if (tvMobile != null) tvMobile.setText("Not added");
    }

    private void setName(String name) {
        if (tvName != null) tvName.setText(name);
        if (tvAvatar != null) tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
    }

    private void loadProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            String name = doc.getString("name");
            String mobile = doc.getString("mobile");
            String role = doc.getString("role");
            if (role != null && !role.isEmpty()) {
                currentUserRole = role;
            }

            if ("admin".equalsIgnoreCase(currentUserRole) && btnAdmin != null) {
                btnAdmin.setVisibility(View.VISIBLE);
            }
            if ("volunteer".equalsIgnoreCase(currentUserRole)) {
                if (rvRegistered != null) rvRegistered.setVisibility(View.GONE);
                if (tvNoEvents != null) tvNoEvents.setVisibility(View.GONE);
            }
            if (name != null && !name.isEmpty()) {
                setName(name);
            }
            if (mobile != null && !mobile.isEmpty() && tvMobile != null) {
                tvMobile.setText(mobile);
            }
        });
    }

    private void loadRegisteredEvents(String uid) {
        registeredListener = db.collection("users").document(uid).collection("registeredEvents")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(Profile.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    registeredEvents.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            EventList event = new EventList();
                            event.setId(doc.getId());
                            event.setTitle(doc.getString("title"));
                            event.setCategory(doc.getString("category"));
                            event.setDateAndLocation(doc.getString("dateAndLocation"));
                            event.setImageUrl(doc.getString("imageUrl"));
                            event.setImageName(doc.getString("imageName")); // Local drawable name load karega
                            registeredEvents.add(event);

                            // Purane registered events jinme imageName save nahi tha, unki image main events collection se laayein
                            if ((event.getImageUrl() == null || event.getImageUrl().isEmpty())
                                    && (event.getImageName() == null || event.getImageName().isEmpty())) {
                                db.collection("events").document(doc.getId()).get()
                                        .addOnSuccessListener(evDoc -> {
                                            if (evDoc.exists()) {
                                                event.setImageUrl(evDoc.getString("imageUrl"));
                                                event.setImageName(evDoc.getString("imageName"));
                                                if (adapter != null) adapter.notifyDataSetChanged();
                                            }
                                        });
                            }
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (!"volunteer".equalsIgnoreCase(currentUserRole) && tvNoEvents != null) {
                        tvNoEvents.setVisibility(registeredEvents.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registeredListener != null) {
            registeredListener.remove();
        }
    }
}