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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

public class UserQR extends AppCompatActivity {

    private TextView tvEventTitle, tvUserName;
    private ImageView ivQrCode;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_qr);

        tvEventTitle = findViewById(R.id.user_qr_EventTitle);
        ivQrCode = findViewById(R.id.user_qr_Image);
        tvUserName = findViewById(R.id.user_qr_UserName);
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

        // Fetch user's display name and verify registration before showing QR
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.exists() ? userDoc.getString("name") : null;
                    if (name == null || name.isEmpty()) {
                        name = user.getEmail();
                    }
                    tvUserName.setText(name);
                });

        db.collection("events").document(eventId)
                .collection("registrations").document(user.getUid())
                .get()
                .addOnSuccessListener(regDoc -> {
                    if (!regDoc.exists()) {
                        Toast.makeText(this, "You are not registered for this event", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    generateQrBitmap(eventId, user.getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verifying registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void generateQrBitmap(String eventId, String uid) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("eventId", eventId);
            payload.put("uid", uid);

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(payload.toString(), BarcodeFormat.QR_CODE, 600, 600);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}