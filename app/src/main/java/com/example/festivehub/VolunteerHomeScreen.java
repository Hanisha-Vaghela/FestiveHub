package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VolunteerHomeScreen extends AppCompatActivity {

    TextView tvWelcome, tvTotalEvents, tvJoinedEvents, tvNoEvents;
    LinearLayout emptyBox;
    RecyclerView rvUpcoming;
    Button btnScanAttendance, btnAttendanceRecords, explorebtn, profileBtn;

    FirebaseFirestore db;
    final ArrayList<EventList> upcomingEvents = new ArrayList<>();
    final Map<String, Long> emptyCounts = new HashMap<>();
    VolunteerAssignedEvents.EventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_home_screen);

        tvWelcome = findViewById(R.id.volunteer_home_screen_Welcome);
        tvTotalEvents = findViewById(R.id.volunteer_home_screen_TotalEvents);
        tvJoinedEvents = findViewById(R.id.volunteer_home_screen_JoinedEvents);
        tvNoEvents = findViewById(R.id.volunteer_home_screen_NoEvents);
        emptyBox = findViewById(R.id.volunteer_home_screen_EmptyBox);
        rvUpcoming = findViewById(R.id.volunteer_home_screen_RecyclerView);
        btnScanAttendance = findViewById(R.id.volunteer_home_screen_btnScanAttendance);
        btnAttendanceRecords = findViewById(R.id.volunteer_home_screen_btnAttendanceRecords);
        explorebtn = findViewById(R.id.volunteer_home_screen_btnExploreEvents);
        profileBtn = findViewById(R.id.volunteer_home_screen_btnVolunteerProfile);

        db = FirebaseFirestore.getInstance();

        adapter = new VolunteerAssignedEvents.EventsAdapter(upcomingEvents, emptyCounts, event -> {
            Intent intent = new Intent(VolunteerHomeScreen.this, VolunteerEventDetails.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setAdapter(adapter);

        // 1. Open QR Scanner
        btnScanAttendance.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, Volunteers_QR.class)));

        // 2. Open Attendance Records
        btnAttendanceRecords.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, AttendanceRecordsActivity.class)));

        // 3. Open Assigned Events
        explorebtn.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, VolunteerAssignedEvents.class)));

        // 4. Open Profile
        profileBtn.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, Profile.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyRoleAndLoadDashboard();
    }

    private void verifyRoleAndLoadDashboard() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "";
                    if (!"volunteer".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, Home.class));
                        finish();
                        return;
                    }
                    String name = userDoc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvWelcome.setText("Welcome, " + name + "!");
                    }
                    loadEventsData(user.getEmail());
                });
    }

    private void loadEventsData(String email) {
        String cleanEmail = email.trim();
        String lowerEmail = cleanEmail.toLowerCase(Locale.ROOT);

        db.collection("events").get().addOnSuccessListener(allEventsSnap -> {
            tvTotalEvents.setText(String.format(Locale.getDefault(), "%02d", allEventsSnap.size()));

            db.collection("volunteers")
                    .whereIn("email", Arrays.asList(cleanEmail, lowerEmail))
                    .get()
                    .addOnSuccessListener(volSnap -> {
                        Set<String> assignedTitles = new LinkedHashSet<>();
                        for (DocumentSnapshot d : volSnap.getDocuments()) {
                            String t = d.getString("assignedEvent");
                            if (t != null && !t.trim().isEmpty()) {
                                assignedTitles.add(t.trim().toLowerCase(Locale.ROOT));
                            }
                        }

                        upcomingEvents.clear();
                        Set<String> seenIds = new HashSet<>();

                        for (DocumentSnapshot eventDoc : allEventsSnap.getDocuments()) {
                            String title = eventDoc.getString("title");
                            if (title != null
                                    && assignedTitles.contains(title.trim().toLowerCase(Locale.ROOT))
                                    && seenIds.add(eventDoc.getId())) {
                                EventList event = eventDoc.toObject(EventList.class);
                                if (event != null) {
                                    event.setId(eventDoc.getId());
                                    upcomingEvents.add(event);
                                }
                            }
                        }

                        tvJoinedEvents.setText(String.format(Locale.getDefault(), "%02d", upcomingEvents.size()));
                        if (upcomingEvents.isEmpty()) {
                            tvNoEvents.setText("No upcoming assigned events.");
                            emptyBox.setVisibility(View.VISIBLE);
                            rvUpcoming.setVisibility(View.GONE);
                        } else {
                            emptyBox.setVisibility(View.GONE);
                            rvUpcoming.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    });
        });
    }
}