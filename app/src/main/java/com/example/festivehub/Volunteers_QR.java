package com.example.festivehub;

import android.Manifest;
import android.content.Intent;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Volunteers_QR extends AppCompatActivity {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_DUPLICATE = "DUPLICATE";
    private static final String RESULT_NOT_REGISTERED = "NOT_REGISTERED";
    private static final String RESULT_BAD_TOKEN = "BAD_TOKEN";

    private FirebaseFirestore db;
    private String expectedEventId;
    private String volunteerUid;
    private String volunteerEmail;
    private String currentUserRole = "";

    private TextView tvStatus;
    private Button btnScanAgain, btnViewAttendance, btnBack;

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
        btnViewAttendance = findViewById(R.id.btn_view_attendance);
        btnBack = findViewById(R.id.btn_qr_back);

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> checkCameraAndScan());
        }
        if (btnViewAttendance != null) {
            btnViewAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(Volunteers_QR.this, AttendanceRecordsActivity.class);
                if (expectedEventId != null) {
                    intent.putExtra("eventId", expectedEventId);
                }
                startActivity(intent);
            });
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
        volunteerEmail = user.getEmail() != null ? user.getEmail().trim() : "";

        db.collection("users").document(volunteerUid).get()
                .addOnSuccessListener(doc -> {
                    currentUserRole = doc.exists() ? doc.getString("role") : "";
                    if (!"volunteer".equalsIgnoreCase(currentUserRole) && !"admin".equalsIgnoreCase(currentUserRole)) {
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
        try {
            if (!rawData.startsWith("{")) {
                showResultDialog("Invalid QR Code", "Unrecognized QR code format.");
                return;
            }

            JSONObject json = new JSONObject(rawData);
            String regId = json.optString("regId", "").trim();
            String eventId = json.optString("eventId", "").trim();
            String token = json.optString("token", "").trim();

            if (regId.isEmpty() || eventId.isEmpty() || token.isEmpty() || !regId.startsWith(eventId + "_")) {
                showResultDialog("Invalid QR Code", "QR code is missing valid registration identifiers.");
                return;
            }

            if (expectedEventId != null && !expectedEventId.isEmpty() && !expectedEventId.equals(eventId)) {
                showResultDialog("Wrong Event", "This QR code belongs to a different event.");
                return;
            }

            String attendeeUid = regId.substring((eventId + "_").length());
            if (attendeeUid.isEmpty() || attendeeUid.contains("/") || eventId.contains("/")) {
                showResultDialog("Invalid QR Code", "Invalid attendee identifier in QR code.");
                return;
            }

            verifyVolunteerAssignmentAndRecord(eventId, attendeeUid, regId, token);

        } catch (Exception e) {
            showResultDialog("Invalid QR Code", "Could not parse QR code data.");
        }
    }

    private void verifyVolunteerAssignmentAndRecord(String eventId, String attendeeUid, String regId, String scannedToken) {
        if (tvStatus != null) {
            tvStatus.setText("Validating volunteer assignment & ticket...");
        }

        // Fetch event first to get eventName and verify volunteer assignment
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        showResultDialog("Invalid Event", "The event associated with this QR code does not exist.");
                        return;
                    }

                    String eventTitle = eventDoc.getString("title");
                    final String safeEventTitle = (eventTitle != null && !eventTitle.isEmpty()) ? eventTitle : "Event";

                    if ("admin".equalsIgnoreCase(currentUserRole)) {
                        executeAtomicAttendanceTransaction(eventId, safeEventTitle, attendeeUid, regId, scannedToken);
                        return;
                    }

                    // Verify that the scanning volunteer is assigned to this event
                    String lowerEmail = volunteerEmail.toLowerCase(Locale.ROOT);
                    db.collection("volunteers")
                            .whereIn("email", Arrays.asList(volunteerEmail, lowerEmail))
                            .get()
                            .addOnSuccessListener(volSnap -> {
                                boolean isAssigned = false;
                                for (DocumentSnapshot vDoc : volSnap.getDocuments()) {
                                    String assignedTitle = vDoc.getString("assignedEvent");
                                    if (assignedTitle != null
                                            && assignedTitle.trim().equalsIgnoreCase(safeEventTitle.trim())) {
                                        isAssigned = true;
                                        break;
                                    }
                                }

                                if (!isAssigned) {
                                    showResultDialog("Unauthorized Event",
                                            "You are not assigned as a volunteer for \"" + safeEventTitle + "\".");
                                    return;
                                }

                                executeAtomicAttendanceTransaction(eventId, safeEventTitle, attendeeUid, regId, scannedToken);
                            })
                            .addOnFailureListener(e ->
                                    showResultDialog("Verification Error", "Could not verify volunteer assignment: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        showResultDialog("Network Error", "Could not load event details: " + e.getMessage()));
    }

    private void executeAtomicAttendanceTransaction(String eventId, String eventName,
                                                    String attendeeUid, String regId, String scannedToken) {
        final String attendanceId = eventId + "_" + attendeeUid;
        final DocumentReference regRef = db.collection("events").document(eventId)
                .collection("registrations").document(attendeeUid);
        final DocumentReference attendanceRef = db.collection("attendance").document(attendanceId);

        Date now = new Date();
        final String checkInDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now);
        final String checkInTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);

        // The transaction returns {result, attendee name}. Real Firestore errors
        // (rules, network) go to the failure listener, so they are not shown as "Invalid QR".
        db.runTransaction(transaction -> {
            DocumentSnapshot regSnap = transaction.get(regRef);
            if (!regSnap.exists()) {
                return new String[]{RESULT_NOT_REGISTERED, ""};
            }

            String storedToken = regSnap.getString("qrToken");
            if (storedToken == null || !storedToken.equals(scannedToken)) {
                return new String[]{RESULT_BAD_TOKEN, ""};
            }

            DocumentSnapshot attSnap = transaction.get(attendanceRef);
            Boolean alreadyCheckedIn = regSnap.getBoolean("checkedIn");

            String userName = regSnap.getString("name");
            if (userName == null || userName.trim().isEmpty()) {
                userName = regSnap.getString("email");
            }
            if (userName == null || userName.trim().isEmpty()) {
                userName = attendeeUid;
            }

            if (attSnap.exists() || Boolean.TRUE.equals(alreadyCheckedIn)) {
                return new String[]{RESULT_DUPLICATE, userName};
            }

            Map<String, Object> record = new HashMap<>();
            record.put("attendanceId", attendanceId);
            record.put("userId", attendeeUid);
            record.put("userName", userName);
            record.put("eventId", eventId);
            record.put("eventName", eventName);
            record.put("registrationId", regId);
            record.put("volunteerId", volunteerUid);
            record.put("attendanceStatus", "Present");
            record.put("checkInDate", checkInDate);
            record.put("checkInTime", checkInTime);
            record.put("timestamp", FieldValue.serverTimestamp());

            transaction.set(attendanceRef, record);
            transaction.update(regRef,
                    "checkedIn", true,
                    "checkedInAt", FieldValue.serverTimestamp(),
                    "checkedInBy", volunteerUid);

            return new String[]{RESULT_SUCCESS, userName};
        }).addOnSuccessListener(result -> {
            switch (result[0]) {
                case RESULT_SUCCESS:
                    showResultDialog("Attendance Marked",
                            "Valid QR Code\nPresent: " + result[1] + "\nEvent: " + eventName
                                    + "\nTime: " + checkInDate + " at " + checkInTime);
                    break;
                case RESULT_DUPLICATE:
                    showResultDialog("Already Scanned", "Attendance already marked for " + result[1] + "!");
                    break;
                case RESULT_NOT_REGISTERED:
                    showResultDialog("Not Registered", "User is not registered for this event.");
                    break;
                default:
                    showResultDialog("Invalid QR Code",
                            "This QR does not match the current registration. Ask the user to reopen their QR.");
                    break;
            }
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) e).getCode()
                    == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                showResultDialog("Permission Denied",
                        "Firestore rules blocked this check-in. Check the published rules and your account role.");
                return;
            }
            showResultDialog("Check-In Failed",
                    e.getMessage() != null ? e.getMessage() : "Attendance failed");
        });
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