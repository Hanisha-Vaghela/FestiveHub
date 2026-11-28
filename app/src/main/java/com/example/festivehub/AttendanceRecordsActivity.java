package com.example.festivehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AttendanceRecordsActivity extends AppCompatActivity {

    private TextView tvCount, tvEmpty;
    private RecyclerView rvRecords;
    private Button btnBack;

    private FirebaseFirestore db;
    private ListenerRegistration attendanceListener;
    private final List<AttendanceRecord> records = new ArrayList<>();
    private AttendanceAdapter adapter;
    private String filterEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_record);

        tvCount = findViewById(R.id.att_tv_count);
        tvEmpty = findViewById(R.id.att_tv_empty);
        rvRecords = findViewById(R.id.att_recycler_view);
        btnBack = findViewById(R.id.att_btn_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        filterEventId = getIntent().getStringExtra("eventId");
        db = FirebaseFirestore.getInstance();

        adapter = new AttendanceAdapter(records);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        rvRecords.setAdapter(adapter);

        verifyAccessAndListen();
    }

    private void verifyAccessAndListen() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "";
                    if ("admin".equalsIgnoreCase(role)) {
                        attachLiveAttendanceListener(null);
                    } else if ("volunteer".equalsIgnoreCase(role)) {
                        loadVolunteerAssignedEventIds(user.getEmail());
                    } else {
                        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Role check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadVolunteerAssignedEventIds(String email) {
        if (email == null) {
            finish();
            return;
        }
        String cleanEmail = email.trim();
        String lowerEmail = cleanEmail.toLowerCase(Locale.ROOT);

        db.collection("volunteers")
                .whereIn("email", Arrays.asList(cleanEmail, lowerEmail))
                .get()
                .addOnSuccessListener(volSnap -> {
                    Set<String> assignedTitles = new HashSet<>();
                    for (DocumentSnapshot d : volSnap.getDocuments()) {
                        String t = d.getString("assignedEvent");
                        if (t != null && !t.trim().isEmpty()) {
                            assignedTitles.add(t.trim().toLowerCase(Locale.ROOT));
                        }
                    }

                    if (assignedTitles.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    db.collection("events").get().addOnSuccessListener(eventsSnap -> {
                        Set<String> allowedEventIds = new HashSet<>();
                        for (DocumentSnapshot eDoc : eventsSnap.getDocuments()) {
                            String title = eDoc.getString("title");
                            if (title != null && assignedTitles.contains(title.trim().toLowerCase(Locale.ROOT))) {
                                allowedEventIds.add(eDoc.getId());
                            }
                        }
                        attachLiveAttendanceListener(allowedEventIds);
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading assignments: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void attachLiveAttendanceListener(Set<String> allowedEventIds) {
        Query query = (filterEventId != null && !filterEventId.isEmpty())
                ? db.collection("attendance").whereEqualTo("eventId", filterEventId)
                : db.collection("attendance");

        attendanceListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading attendance: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            records.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String evId = doc.getString("eventId");
                    if (allowedEventIds == null || (evId != null && allowedEventIds.contains(evId))) {
                        AttendanceRecord item = new AttendanceRecord();
                        item.setAttendanceId(doc.getId());
                        item.setUserId(doc.getString("userId"));
                        item.setUserName(doc.getString("userName"));
                        item.setEventId(evId);
                        item.setEventName(doc.getString("eventName"));
                        item.setRegistrationId(doc.getString("registrationId"));
                        item.setVolunteerId(doc.getString("volunteerId"));
                        item.setAttendanceStatus(doc.getString("attendanceStatus"));
                        item.setCheckInDate(doc.getString("checkInDate"));
                        item.setCheckInTime(doc.getString("checkInTime"));
                        records.add(item);
                    }
                }
            }
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        if (tvCount != null) tvCount.setText("Total Checked-In: " + records.size());
        if (tvEmpty != null) tvEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        if (rvRecords != null) rvRecords.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
    }
}