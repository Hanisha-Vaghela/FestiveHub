package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VolunteerHomeScreen extends AppCompatActivity {

    TextView tvWelcome, tvTotalEvents, tvJoinedEvents, tvNoEvents;
    Button explorebtn, profileBtn;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_home_screen);

        tvWelcome = findViewById(R.id.volunteer_home_screen_Welcome);
        tvTotalEvents = findViewById(R.id.volunteer_home_screen_TotalEvents);
        tvJoinedEvents = findViewById(R.id.volunteer_home_screen_JoinedEvents);
        tvNoEvents = findViewById(R.id.volunteer_home_screen_NoEvents);
        explorebtn = findViewById(R.id.volunteer_home_screen_btnExploreEvents);
        profileBtn = findViewById(R.id.volunteer_home_screen_btnVolunteerProfile);

        db = FirebaseFirestore.getInstance();

        explorebtn.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, VolunteerAssignedEvents.class)));

        profileBtn.setOnClickListener(view ->
                startActivity(new Intent(VolunteerHomeScreen.this, Profile.class)));

        loadVolunteerDashboard();
    }

    private void loadVolunteerDashboard() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // Load total available events count
        db.collection("events").get().addOnSuccessListener(snap ->
                tvTotalEvents.setText(String.format(Locale.getDefault(), "%02d", snap.size())));

        // Load volunteer profile & assigned events
        db.collection("volunteers").whereEqualTo("email", user.getEmail()).get()
                .addOnSuccessListener(snap -> {
                    Set<String> titles = new LinkedHashSet<>();
                    String volunteerName = null;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (volunteerName == null) {
                            volunteerName = doc.getString("name");
                        }
                        String assigned = doc.getString("assignedEvent");
                        if (assigned != null && !assigned.trim().isEmpty()) {
                            titles.add(assigned.trim());
                        }
                    }

                    if (volunteerName != null && !volunteerName.isEmpty()) {
                        tvWelcome.setText("Welcome, " + volunteerName + "!");
                    }

                    tvJoinedEvents.setText(String.format(Locale.getDefault(), "%02d", titles.size()));

                    if (titles.isEmpty()) {
                        tvNoEvents.setText("No assigned events yet.");
                    } else {
                        loadFirstAssignedEventPreview(titles);
                    }
                })
                .addOnFailureListener(e -> tvNoEvents.setText("Error loading assignments: " + e.getMessage()));
    }

    private void loadFirstAssignedEventPreview(Set<String> titles) {
        List<Task<QuerySnapshot>> queries = new ArrayList<>();
        for (String title : titles) {
            queries.add(db.collection("events").whereEqualTo("title", title).get());
        }

        Tasks.whenAllSuccess(queries).addOnSuccessListener(results -> {
            StringBuilder summary = new StringBuilder();
            String firstEventId = null;

            for (Object res : results) {
                for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                    if (firstEventId == null) {
                        firstEventId = doc.getId();
                    }
                    String title = doc.getString("title");
                    String dateLoc = doc.getString("dateAndLocation");
                    summary.append("• ").append(title);
                    if (dateLoc != null && !dateLoc.isEmpty()) {
                        summary.append(" (").append(dateLoc).append(")");
                    }
                    summary.append("\n");
                }
            }

            if (summary.length() > 0) {
                tvNoEvents.setText(summary.toString().trim() + "\n\n(Tap to view schedule & details)");
                tvNoEvents.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                final String targetId = firstEventId;
                tvNoEvents.setOnClickListener(v -> {
                    Intent intent = new Intent(this, VolunteerEventDetails.class);
                    intent.putExtra("eventId", targetId);
                    startActivity(intent);
                });
            }
        });
    }
}