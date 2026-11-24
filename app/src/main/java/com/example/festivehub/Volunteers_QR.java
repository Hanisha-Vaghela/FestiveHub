package com.example.festivehub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Volunteers_QR extends AppCompatActivity {

    private FirebaseFirestore db;
    private String expectedEventId; // Optional: passed if opened from a specific event
    private TextView tvStatus;
    private Button btnScanAgain, btnBack;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    if (tvStatus != null) {
                        tvStatus.setText("Scan cancelled. Tap 'Scan QR' to try again.");
                    }
                } else {
                    handleScannedData(result.getContents().trim());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteers_qr);

        db = FirebaseFirestore.getInstance();
        expectedEventId = getIntent().getStringExtra("eventId");

        // Bind views if present in activity_volunteers_qr.xml
        tvStatus = findViewById(R.id.tv_qr_status);
        btnScanAgain = findViewById(R.id.btn_scan_qr);
        btnBack = findViewById(R.id.btn_qr_back);

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> checkCameraAndScan());
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        checkCameraAndScan();
    }

    private void checkCameraAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Align attendee QR code inside the frame");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        qrLauncher.launch(options);
    }

    private void handleScannedData(String rawData) {
        String eventId = expectedEventId;
        String uid = null;

        try {
            if (rawData.startsWith("{")) {
                // Case 1: JSON QR e.g. {"eventId":"...","uid":"..."}
                JSONObject json = new JSONObject(rawData);
                if (json.has("eventId")) eventId = json.getString("eventId");
                if (json.has("uid")) uid = json.getString("uid");
                else if (json.has("userId")) uid = json.getString("userId");
            } else if (rawData.contains("|")) {
                // Case 2: Pipe-separated QR e.g. "eventId|uid"
                String[] parts = rawData.split("\\|");
                if (parts.length >= 2) {
                    eventId = parts[0].trim();
                    uid = parts[1].trim();
                }
            } else if (rawData.contains(":")) {
                // Case 3: Colon-separated QR e.g. "eventId:uid"
                String[] parts = rawData.split(":");
                if (parts.length >= 2) {
                    eventId = parts[0].trim();
                    uid = parts[1].trim();
                }
            } else {
                // Case 4: QR contains only the user's UID
                uid = rawData;
            }
        } catch (Exception e) {
            showResultDialog("Invalid QR", "Could not read QR format: " + rawData, false);
            return;
        }

        if (eventId == null || eventId.isEmpty() || uid == null || uid.isEmpty()) {
            showResultDialog("Invalid Ticket", "Missing event or user ID in QR code.", false);
            return;
        }

        if (expectedEventId != null && !expectedEventId.equals(eventId)) {
            showResultDialog("Wrong Event", "This ticket belongs to a different event!", false);
            return;
        }

        verifyAndCheckIn(eventId, uid);
    }

    private void verifyAndCheckIn(String eventId, String uid) {
        if (tvStatus != null) {
            tvStatus.setText("Verifying ticket...");
        }

        DocumentReference regRef = db.collection("events")
                .document(eventId)
                .collection("registrations")
                .document(uid);

        regRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                showResultDialog("Not Registered", "No registration found for this user.", false);
                return;
            }

            Boolean alreadyCheckedIn = doc.getBoolean("checkedIn");
            String attendeeName = doc.getString("name");
            if (attendeeName == null || attendeeName.isEmpty()) {
                attendeeName = doc.getString("email");
            }
            if (attendeeName == null) {
                attendeeName = uid;
            }

            if (Boolean.TRUE.equals(alreadyCheckedIn)) {
                showResultDialog("Already Checked In", attendeeName + " has already entered!", false);
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("checkedIn", true);
            updates.put("checkedInAt", FieldValue.serverTimestamp());

            final String displayName = attendeeName;
            regRef.update(updates)
                    .addOnSuccessListener(unused ->
                            showResultDialog("Check-In Successful", "Welcome, " + displayName + "!", true))
                    .addOnFailureListener(e ->
                            showResultDialog("Check-In Failed", e.getMessage(), false));
        }).addOnFailureListener(e ->
                showResultDialog("Verification Error", e.getMessage(), false));
    }

    private void showResultDialog(String title, String message, boolean success) {
        if (tvStatus != null) {
            tvStatus.setText(title + "\n" + message);
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Scan Next", (d, w) -> launchScanner())
                .setNegativeButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}