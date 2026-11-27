package com.example.festivehub;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.util.UUID;

public class UserQR extends AppCompatActivity {

    private TextView tvEventTitle, tvUserName, tvStatusBadge;
    private ImageView ivQrCode;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_qr);

        tvEventTitle = findViewById(R.id.user_qr_EventTitle);
        ivQrCode = findViewById(R.id.user_qr_Image);
        tvUserName = findViewById(R.id.user_qr_UserName);
        tvStatusBadge = findViewById(R.id.user_qr_StatusBadge);
        btnBack = findViewById(R.id.user_qr_btnBack);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String eventId = getIntent().getStringExtra("eventId");
        String eventTitle = getIntent().getStringExtra("eventTitle");

        if (eventTitle != null && !eventTitle.isEmpty()) {
            tvEventTitle.setText(eventTitle);
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event details missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Enforce role restriction: volunteers cannot generate normal-user QR tickets
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "user";
                    if ("volunteer".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Volunteers cannot generate attendee QR codes", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String name = userDoc.exists() ? userDoc.getString("name") : null;
                    if (name == null || name.isEmpty()) {
                        name = user.getEmail();
                    }
                    tvUserName.setText(name);

                    loadRegistrationAndRenderQr(db, eventId, user.getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load your profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void loadRegistrationAndRenderQr(FirebaseFirestore db, String eventId, String uid) {
        DocumentReference regRef = db.collection("events").document(eventId)
                .collection("registrations").document(uid);

        regRef.get()
                .addOnSuccessListener(regDoc -> {
                    if (!regDoc.exists()) {
                        Toast.makeText(this, "You are not registered for this event", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String titleFromDoc = regDoc.getString("eventTitle");
                    if (titleFromDoc != null && !titleFromDoc.isEmpty()) {
                        tvEventTitle.setText(titleFromDoc);
                    }

                    Boolean checkedIn = regDoc.getBoolean("checkedIn");
                    if (Boolean.TRUE.equals(checkedIn)) {
                        tvStatusBadge.setText("Status: Checked In (Present)");
                    } else {
                        tvStatusBadge.setText("Status: Valid Entry Pass");
                    }

                    // The registration ID is always eventId_uid
                    String regId = eventId + "_" + uid;
                    String qrToken = regDoc.getString("qrToken");

                    if (qrToken != null && !qrToken.isEmpty()) {
                        renderQrBitmap(regId, eventId, qrToken);
                    } else {
                        // Old registration without a token: save one first, then show the QR
                        createMissingToken(db, regRef, regId, eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void createMissingToken(FirebaseFirestore db, DocumentReference regRef,
                                    String regId, String eventId) {
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(regRef);
            String savedToken = snap.getString("qrToken");
            if (savedToken != null && !savedToken.isEmpty()) {
                return savedToken; // never replace a token that already exists
            }
            String newToken = UUID.randomUUID().toString();
            transaction.update(regRef, "registrationId", regId, "qrToken", newToken);
            return newToken;
        }).addOnSuccessListener(savedToken ->
                // Runs only after Firestore has accepted the token
                renderQrBitmap(regId, eventId, savedToken)
        ).addOnFailureListener(e -> {
            Toast.makeText(this, "Couldn't create your QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void renderQrBitmap(String regId, String eventId, String token) {
        try {
            // Non-sensitive QR payload containing only registration & token identifiers
            JSONObject payload = new JSONObject();
            payload.put("regId", regId);
            payload.put("eventId", eventId);
            payload.put("token", token);

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(payload.toString(), BarcodeFormat.QR_CODE, 600, 600);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}