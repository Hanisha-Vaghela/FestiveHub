package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class Event_Detail extends AppCompatActivity {

    ImageView DetailBanner;
    TextView DetailCategory, DetailTitle, DetailDateLocation, DetailPrice, DetailDescription;
    Button btnConfirmBooking, btnBackToHome, btnvolunteerqr;

    FirebaseFirestore db;
    FirebaseAuth auth;

    String eventId;
    EventList currentEvent;
    boolean registered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_detail);

        DetailBanner = findViewById(R.id.eventdetail_DetailBanner);
        DetailCategory = findViewById(R.id.eventdetail_DetailCategory);
        DetailTitle = findViewById(R.id.eventdetail_DetailTitle);
        DetailDateLocation = findViewById(R.id.eventdetail_DetailDateLocation);
        DetailPrice = findViewById(R.id.eventdetail_DetailPrice);
        DetailDescription = findViewById(R.id.eventdetail_DetailDescription);
        btnConfirmBooking = findViewById(R.id.eventdetail_btnConfirmBooking);
        btnBackToHome = findViewById(R.id.eventdetail_btnBackToHome);
        btnvolunteerqr = findViewById(R.id.eventdetail_btnvolunteerqr);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnConfirmBooking.setEnabled(false); // enabled once the event is loaded
        loadEvent();

        btnConfirmBooking.setOnClickListener(v -> toggleRegistration());

        btnvolunteerqr.setOnClickListener(v -> {
            Intent intent = new Intent(Event_Detail.this, Volunteers_QR.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnBackToHome.setOnClickListener(v -> finish());
    }

    private void loadEvent() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "This event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentEvent = doc.toObject(EventList.class);
                    if (currentEvent == null) {
                        finish();
                        return;
                    }
                    showEvent();
                    checkRegistration();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showEvent() {
        ImageUtil.loadEventImage(DetailBanner, currentEvent);
        DetailCategory.setText(currentEvent.getCategory() != null ? currentEvent.getCategory() : "");
        DetailTitle.setText(currentEvent.getTitle() != null ? currentEvent.getTitle() : "Event");
        DetailDateLocation.setText(currentEvent.getDateAndLocation() != null ? currentEvent.getDateAndLocation() : "");
        DetailPrice.setText(currentEvent.getPriceText() != null ? currentEvent.getPriceText() : "");
        DetailDescription.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");
    }

    // ---------- Register / Join ----------

    private DocumentReference eventRegistrationRef(String uid) {
        return db.collection("events").document(eventId).collection("registrations").document(uid);
    }

    private DocumentReference userRegistrationRef(String uid) {
        return db.collection("users").document(uid).collection("registeredEvents").document(eventId);
    }

    private void checkRegistration() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            updateRegisterButton();
            return;
        }
        eventRegistrationRef(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    registered = doc.exists();
                    updateRegisterButton();
                })
                .addOnFailureListener(e -> updateRegisterButton());
    }

    private void updateRegisterButton() {
        btnConfirmBooking.setText(registered ? "Cancel Registration" : "Register for Event");
        btnConfirmBooking.setEnabled(true);
    }

    private void toggleRegistration() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentEvent == null) {
            return;
        }

        btnConfirmBooking.setEnabled(false);
        String uid = user.getUid();

        if (registered) {
            WriteBatch batch = db.batch();
            batch.delete(eventRegistrationRef(uid));
            batch.delete(userRegistrationRef(uid));
            commitRegistration(batch);
            return;
        }

        // Read the user's saved name and mobile so the admin can see who joined
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            String name = null;
            String mobile = null;
            if (task.isSuccessful() && task.getResult() != null) {
                name = task.getResult().getString("name");
                mobile = task.getResult().getString("mobile");
            }

            WriteBatch batch = db.batch();

            // Under the event: who joined (the admin's "Registered Users" list)
            Map<String, Object> eventSide = new HashMap<>();
            eventSide.put("userId", uid);
            eventSide.put("name", name != null ? name : "");
            eventSide.put("email", user.getEmail());
            eventSide.put("mobile", mobile != null ? mobile : "");
            eventSide.put("registeredAt", FieldValue.serverTimestamp());
            batch.set(eventRegistrationRef(uid), eventSide);

            // Under the user: events they joined (the profile screen)
            Map<String, Object> userSide = new HashMap<>();
            userSide.put("eventId", eventId);
            userSide.put("title", currentEvent.getTitle());
            userSide.put("category", currentEvent.getCategory());
            userSide.put("dateAndLocation", currentEvent.getDateAndLocation());
            userSide.put("imageUrl", currentEvent.getImageUrl());
            userSide.put("imageName", currentEvent.getImageName());
            userSide.put("registeredAt", FieldValue.serverTimestamp());
            batch.set(userRegistrationRef(uid), userSide);

            commitRegistration(batch);
        });
    }

    private void commitRegistration(WriteBatch batch) {
        batch.commit()
                .addOnSuccessListener(unused -> {
                    registered = !registered;
                    updateRegisterButton();
                    Toast.makeText(this,
                            registered ? "Registered for " + currentEvent.getTitle() : "Registration cancelled",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    updateRegisterButton();
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
