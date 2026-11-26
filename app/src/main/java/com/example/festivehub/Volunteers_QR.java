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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private String expectedEventId;
    private String volunteerUid;
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
                        tvStatus.setText("Scan cancelled. Tap 'Scan QR Code' to try again.");
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

        tvStatus = findViewById(R.id.tv_qr_status);
        btnScanAgain = findViewById(R.id.btn_scan_qr);
        btnBack = findViewById(R.id.btn_qr_back);

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> checkCameraAndScan());
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        verifyVolunteerBeforeScanning();
    }

    private void verifyVolunteerBeforeScanning() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        volunteerUid = user.getUid();

        db.collection("users").document(volunteerUid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.exists() ? doc.getString("role") : "";
                    if (!"volunteer".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Access denied: Only volunteers can scan attendance", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    checkCameraAndScan();
                })
                .addOnFailureListener(e -> finish());
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
                JSONObject json = new JSONObject(rawData);
                if (json.has("eventId")) eventId = json.getString("eventId");
                if (json.has("uid")) uid = json.getString("uid");
            } else if (rawData.contains("|")) {
                String[] parts = rawData.split("\\|");
                if (parts.length >= 2) {
                    eventId = parts[0].trim();
                    uid = parts[1].trim();
                }
            } else {
                uid = rawData;
            }
        } catch (Exception e) {
            showResultDialog("Invalid QR", "Could not read QR code format.");
            return;
        }

        if (eventId == null || eventId.isEmpty() || uid == null || uid.isEmpty()) {
            showResultDialog("Invalid QR Code", "QR code is missing event or user registration details.");
            return;
        }

        if (expectedEventId != null && !expectedEventId.equals(eventId)) {
            showResultDialog("Wrong Event", "This QR code is registered for a different event.");
            return;
        }

        markAttendanceInFirestore(eventId, uid);
    }

    private void markAttendanceInFirestore(String eventId, String attendeeUid) {
        if (tvStatus != null) {
            tvStatus.setText("Verifying registration...");
        }

        DocumentReference regRef = db.collection("events")
                .document(eventId)
                .collection("registrations")
                .document(attendeeUid);

        regRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                showResultDialog("Not Registered", "This user is not registered for this event.");
                return;
            }

            Boolean alreadyCheckedIn = doc.getBoolean("checkedIn");
            String attendeeName = doc.getString("name");
            if (attendeeName == null || attendeeName.isEmpty()) {
                attendeeName = doc.getString("email");
            }
            if (attendeeName == null) {
                attendeeName = attendeeUid;
            }

            if (Boolean.TRUE.equals(alreadyCheckedIn)) {
                showResultDialog("Duplicate Attendance", attendeeName + " is already marked present!");
                return;
            }

            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("checkedIn", true);
            attendanceData.put("checkedInAt", FieldValue.serverTimestamp());
            attendanceData.put("checkedInBy", volunteerUid);

            final String displayName = attendeeName;
            regRef.update(attendanceData)
                    .addOnSuccessListener(unused ->
                            showResultDialog("Attendance Marked", "Attendance marked successfully for " + displayName + "!"))
                    .addOnFailureListener(e ->
                            showResultDialog("Permission / Save Error", "Failed to mark attendance: " + e.getMessage()));
        }).addOnFailureListener(e ->
                showResultDialog("Verification Failed", e.getMessage()));
    }

    private void showResultDialog(String title, String message) {
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