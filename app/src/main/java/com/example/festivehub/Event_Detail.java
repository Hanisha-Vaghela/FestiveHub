//package com.example.festivehub;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.WriteBatch;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class Event_Detail extends AppCompatActivity {
//
//    ImageView DetailBanner;
//    TextView DetailCategory, DetailTitle, DetailDateLocation, DetailPrice, DetailDescription;
//    Button btnConfirmBooking, btnBackToHome, btnvolunteerqr;
//
//    FirebaseFirestore db;
//    FirebaseAuth auth;
//
//    String eventId;
//    EventList currentEvent;
//    boolean registered = false;
//    boolean isVolunteer = false;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_event_detail);
//
//        DetailBanner = findViewById(R.id.eventdetail_DetailBanner);
//        DetailCategory = findViewById(R.id.eventdetail_DetailCategory);
//        DetailTitle = findViewById(R.id.eventdetail_DetailTitle);
//        DetailDateLocation = findViewById(R.id.eventdetail_DetailDateLocation);
//        DetailPrice = findViewById(R.id.eventdetail_DetailPrice);
//        DetailDescription = findViewById(R.id.eventdetail_DetailDescription);
//        btnConfirmBooking = findViewById(R.id.eventdetail_btnConfirmBooking);
//        btnBackToHome = findViewById(R.id.eventdetail_btnBackToHome);
//        btnvolunteerqr = findViewById(R.id.eventdetail_btnvolunteerqr);
//
//        db = FirebaseFirestore.getInstance();
//        auth = FirebaseAuth.getInstance();
//
//        eventId = getIntent().getStringExtra("eventId");
//        if (eventId == null) {
//            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        btnConfirmBooking.setEnabled(false); // enabled once the event is loaded
//        loadEvent();
//
//        btnConfirmBooking.setOnClickListener(v -> toggleRegistration());
//
//        loadRole();
//
//        btnvolunteerqr.setOnClickListener(v -> {
//            if (isVolunteer) {
//                // volunteer -> scanner
//                Intent intent = new Intent(Event_Detail.this, VolunteerScanner.class);
//                intent.putExtra("eventId", eventId);
//                startActivity(intent);
//            } else {
//                // normal user -> my QR for this event
//                if (auth.getCurrentUser() == null) {
//                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                if (!registered) {
//                    Toast.makeText(this, "Register for this event first to get your QR", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                Intent intent = new Intent(Event_Detail.this, UserQR.class);
//                intent.putExtra("eventId", eventId);
//                startActivity(intent);
//            }
//        });
//
//        btnBackToHome.setOnClickListener(v -> finish());
//    }
//
//    // Volunteer = users/{uid}.role is "volunteer" OR the email is in the volunteers list.
//    // Volunteer -> button text becomes "Scan User QR". Everyone else -> button stays as it is.
//    private void loadRole() {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            return;
//        }
//        db.collection("users").document(user.getUid()).get()
//                .addOnSuccessListener(doc -> {
//                    String role = doc.getString("role");
//                    if ("volunteer".equalsIgnoreCase(role)) {
//                        makeVolunteer();
//                    } else {
//                        checkVolunteerList(user.getEmail());
//                    }
//                })
//                .addOnFailureListener(e -> checkVolunteerList(user.getEmail()));
//    }
//
//    private void checkVolunteerList(String email) {
//        if (email == null) {
//            return;
//        }
//        db.collection("volunteers").whereEqualTo("email", email).limit(1).get()
//                .addOnSuccessListener(snap -> {
//                    if (!snap.isEmpty()) {
//                        makeVolunteer();
//                    }
//                });
//    }
//
//    private void makeVolunteer() {
//        isVolunteer = true;
//        btnvolunteerqr.setText("Scan User QR");
//    }
//
//    private void loadEvent() {
//        db.collection("events").document(eventId).get()
//                .addOnSuccessListener(doc -> {
//                    if (!doc.exists()) {
//                        Toast.makeText(this, "This event no longer exists", Toast.LENGTH_SHORT).show();
//                        finish();
//                        return;
//                    }
//                    currentEvent = doc.toObject(EventList.class);
//                    if (currentEvent == null) {
//                        finish();
//                        return;
//                    }
//                    showEvent();
//                    checkRegistration();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Couldn't load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    finish();
//                });
//    }
//
//    private void showEvent() {
//        ImageUtil.loadEventImage(DetailBanner, currentEvent);
//        DetailCategory.setText(currentEvent.getCategory() != null ? currentEvent.getCategory() : "");
//        DetailTitle.setText(currentEvent.getTitle() != null ? currentEvent.getTitle() : "Event");
//        DetailDateLocation.setText(currentEvent.getDateAndLocation() != null ? currentEvent.getDateAndLocation() : "");
//        DetailPrice.setText(currentEvent.getPriceText() != null ? currentEvent.getPriceText() : "");
//        DetailDescription.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");
//    }
//
//    // ---------- Register / Join ----------
//
//    private DocumentReference eventRegistrationRef(String uid) {
//        return db.collection("events").document(eventId).collection("registrations").document(uid);
//    }
//
//    private DocumentReference userRegistrationRef(String uid) {
//        return db.collection("users").document(uid).collection("registeredEvents").document(eventId);
//    }
//
//    private void checkRegistration() {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            updateRegisterButton();
//            return;
//        }
//        eventRegistrationRef(user.getUid()).get()
//                .addOnSuccessListener(doc -> {
//                    registered = doc.exists();
//                    updateRegisterButton();
//                })
//                .addOnFailureListener(e -> updateRegisterButton());
//    }
//
//    private void updateRegisterButton() {
//        btnConfirmBooking.setText(registered ? "Cancel Registration" : "Register for Event");
//        btnConfirmBooking.setEnabled(true);
//    }
//
//    private void toggleRegistration() {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (currentEvent == null) {
//            return;
//        }
//
//        btnConfirmBooking.setEnabled(false);
//        String uid = user.getUid();
//
//        if (registered) {
//            WriteBatch batch = db.batch();
//            batch.delete(eventRegistrationRef(uid));
//            batch.delete(userRegistrationRef(uid));
//            commitRegistration(batch);
//            return;
//        }
//
//        // Read the user's saved name and mobile so the admin can see who joined
//        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
//            String name = null;
//            String mobile = null;
//            if (task.isSuccessful() && task.getResult() != null) {
//                name = task.getResult().getString("name");
//                mobile = task.getResult().getString("mobile");
//            }
//
//            WriteBatch batch = db.batch();
//
//            // Under the event: who joined (the admin's "Registered Users" list)
//            Map<String, Object> eventSide = new HashMap<>();
//            eventSide.put("userId", uid);
//            eventSide.put("name", name != null ? name : "");
//            eventSide.put("email", user.getEmail());
//            eventSide.put("mobile", mobile != null ? mobile : "");
//            eventSide.put("registeredAt", FieldValue.serverTimestamp());
//            batch.set(eventRegistrationRef(uid), eventSide);
//
//            // Under the user: events they joined (the profile screen)
//            Map<String, Object> userSide = new HashMap<>();
//            userSide.put("eventId", eventId);
//            userSide.put("title", currentEvent.getTitle());
//            userSide.put("category", currentEvent.getCategory());
//            userSide.put("dateAndLocation", currentEvent.getDateAndLocation());
//            userSide.put("imageUrl", currentEvent.getImageUrl());
//            userSide.put("imageName", currentEvent.getImageName());
//            userSide.put("registeredAt", FieldValue.serverTimestamp());
//            batch.set(userRegistrationRef(uid), userSide);
//
//            commitRegistration(batch);
//        });
//    }
//
//    private void commitRegistration(WriteBatch batch) {
//        batch.commit()
//                .addOnSuccessListener(unused -> {
//                    registered = !registered;
//                    updateRegisterButton();
//                    Toast.makeText(this,
//                            registered ? "Registered for " + currentEvent.getTitle() : "Registration cancelled",
//                            Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    updateRegisterButton();
//                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                });
//    }
//}
package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Event_Detail extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String eventId;
    private EventList currentEvent;
    private Button btnRegister;
    private boolean isAlreadyRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView ivBanner = findViewById(R.id.detail_image);
        TextView tvCategory = findViewById(R.id.detail_category);
        TextView tvTitle = findViewById(R.id.detail_title);
        TextView tvDateLocation = findViewById(R.id.detail_date_location);
        TextView tvPrice = findViewById(R.id.detail_price);
        TextView tvDescription = findViewById(R.id.detail_description);
        btnRegister = findViewById(R.id.detail_btn_register);

        loadEventDetails(ivBanner, tvCategory, tvTitle, tvDateLocation, tvPrice, tvDescription);
        checkRegistrationStatus();

        btnRegister.setOnClickListener(v -> {
            if (isAlreadyRegistered) {
                openUserQr();
            } else {
                registerForEvent();
            }
        });
    }

    private void loadEventDetails(ImageView ivBanner, TextView tvCategory, TextView tvTitle,
                                  TextView tvDateLocation, TextView tvPrice, TextView tvDescription) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentEvent = doc.toObject(EventList.class);
                    if (currentEvent != null) {
                        currentEvent.setId(doc.getId());
                        if (ivBanner != null) ImageUtil.loadEventImage(ivBanner, currentEvent);
                        if (tvCategory != null) tvCategory.setText(currentEvent.getCategory());
                        if (tvTitle != null) tvTitle.setText(currentEvent.getTitle());
                        if (tvDateLocation != null) tvDateLocation.setText(currentEvent.getDateAndLocation());
                        if (tvPrice != null) tvPrice.setText(currentEvent.getPriceText());
                        if (tvDescription != null) tvDescription.setText(currentEvent.getDescription());
                    }
                });
    }

    private void checkRegistrationStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("events").document(eventId)
                .collection("registrations").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        isAlreadyRegistered = true;
                        btnRegister.setText("View My Entry QR");
                    }
                });
    }

    private void registerForEvent() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentEvent == null) return;

        btnRegister.setEnabled(false);
        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "user";
                    if ("volunteer".equalsIgnoreCase(role)) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "Volunteers cannot generate attendee QR codes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentReference eventRegRef = db.collection("events").document(eventId)
                            .collection("registrations").document(uid);

                    // Prevent duplicate registration or regenerating a different token
                    eventRegRef.get().addOnSuccessListener(existingDoc -> {
                        if (existingDoc.exists()) {
                            isAlreadyRegistered = true;
                            btnRegister.setEnabled(true);
                            btnRegister.setText("View My Entry QR");
                            openUserQr();
                            return;
                        }

                        String userName = userDoc.exists() ? userDoc.getString("name") : "";
                        String userEmail = user.getEmail() != null ? user.getEmail() : "";
                        String registrationId = eventId + "_" + uid;
                        String qrToken = UUID.randomUUID().toString();

                        Map<String, Object> regData = new HashMap<>();
                        regData.put("registrationId", registrationId);
                        regData.put("qrToken", qrToken);
                        regData.put("uid", uid);
                        regData.put("name", userName);
                        regData.put("email", userEmail);
                        regData.put("eventId", eventId);
                        regData.put("eventTitle", currentEvent.getTitle());
                        regData.put("checkedIn", false);
                        regData.put("registeredAt", FieldValue.serverTimestamp());

                        Map<String, Object> userEventData = new HashMap<>();
                        userEventData.put("id", eventId);
                        userEventData.put("registrationId", registrationId);
                        userEventData.put("qrToken", qrToken);
                        userEventData.put("title", currentEvent.getTitle());
                        userEventData.put("category", currentEvent.getCategory());
                        userEventData.put("dateAndLocation", currentEvent.getDateAndLocation());
                        userEventData.put("imageUrl", currentEvent.getImageUrl());
                        userEventData.put("registeredAt", FieldValue.serverTimestamp());

                        DocumentReference userRegRef = db.collection("users").document(uid)
                                .collection("registeredEvents").document(eventId);

                        eventRegRef.set(regData)
                                .addOnSuccessListener(unused -> userRegRef.set(userEventData)
                                        .addOnSuccessListener(unused2 -> {
                                            isAlreadyRegistered = true;
                                            btnRegister.setEnabled(true);
                                            btnRegister.setText("View My Entry QR");
                                            Toast.makeText(this, "Joined event! Opening your QR code...", Toast.LENGTH_SHORT).show();
                                            openUserQr();
                                        }))
                                .addOnFailureListener(e -> {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openUserQr() {
        Intent intent = new Intent(Event_Detail.this, UserQR.class);
        intent.putExtra("eventId", eventId);
        if (currentEvent != null) {
            intent.putExtra("eventTitle", currentEvent.getTitle());
        }
        startActivity(intent);
    }
}