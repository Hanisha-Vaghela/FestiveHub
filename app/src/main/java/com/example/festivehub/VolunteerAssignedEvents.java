package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
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

public class VolunteerAssignedEvents extends AppCompatActivity {

    TextView tvTotalAssigned, tvTotalRegistrations, tvNoEvents;
    LinearLayout emptyLayout;
    RecyclerView rvEvents;
    Button btnBack;

    FirebaseFirestore db;
    final ArrayList<EventList> events = new ArrayList<>();
    final Map<String, Long> counts = new HashMap<>();
    EventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_assigned_events);

        tvTotalAssigned = findViewById(R.id.volunteer_assigned_events_TotalAssigned);
        tvTotalRegistrations = findViewById(R.id.volunteer_assigned_events_TotalRegistrations);
        tvNoEvents = findViewById(R.id.volunteer_assigned_events_NoEvents);
        emptyLayout = findViewById(R.id.volunteer_assigned_events_EmptyLayout);
        rvEvents = findViewById(R.id.volunteer_assigned_events_RecyclerView);
        btnBack = findViewById(R.id.volunteer_assigned_events_btnBack);

        btnBack.setOnClickListener(v -> finish());

        adapter = new EventsAdapter(events, counts, this::openDetails);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyVolunteerAndLoad();
    }

    private void verifyVolunteerAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Security check: prevent normal users from viewing volunteer screens
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "";
                    if (!"volunteer".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Access denied: Volunteers only", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    tvNoEvents.setText("Loading your events...");
                    emptyLayout.setVisibility(View.VISIBLE);
                    rvEvents.setVisibility(View.GONE);
                    loadAssignedTitles(user.getEmail());
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void loadAssignedTitles(String email) {
        String cleanEmail = email.trim();
        String lowerEmail = cleanEmail.toLowerCase(Locale.ROOT);

        db.collection("volunteers")
                .whereIn("email", Arrays.asList(cleanEmail, lowerEmail))
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> normalizedTitles = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String title = doc.getString("assignedEvent");
                        if (title != null && !title.trim().isEmpty()) {
                            normalizedTitles.add(title.trim().toLowerCase(Locale.ROOT));
                        }
                    }
                    if (normalizedTitles.isEmpty()) {
                        events.clear();
                        counts.clear();
                        showResult();
                    } else {
                        loadAllMatchingEvents(normalizedTitles);
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void loadAllMatchingEvents(Set<String> normalizedTitles) {
        db.collection("events").get()
                .addOnSuccessListener(snapshot -> {
                    events.clear();
                    counts.clear();
                    Set<String> seenIds = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String eventTitle = doc.getString("title");
                        if (eventTitle != null
                                && normalizedTitles.contains(eventTitle.trim().toLowerCase(Locale.ROOT))
                                && seenIds.add(doc.getId())) {
                            EventList event = doc.toObject(EventList.class);
                            if (event != null) {
                                event.setId(doc.getId()); // Critical fix: attach document ID
                                events.add(event);
                            }
                        }
                    }

                    showResult();
                    for (EventList event : events) {
                        if (event.getId() != null) {
                            loadRegisteredCount(event.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void loadRegisteredCount(String eventId) {
        db.collection("events").document(eventId).collection("registrations")
                .count().get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    counts.put(eventId, snapshot.getCount());
                    adapter.notifyDataSetChanged();
                    long total = 0;
                    for (long value : counts.values()) {
                        total += value;
                    }
                    tvTotalRegistrations.setText(String.format(Locale.getDefault(), "%02d", total));
                });
    }

    private void showResult() {
        tvTotalAssigned.setText(String.format(Locale.getDefault(), "%02d", events.size()));
        if (events.isEmpty()) {
            tvNoEvents.setText("No events assigned to you yet.");
            emptyLayout.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showError(String message) {
        tvNoEvents.setText("Couldn't load your events: " + message);
        emptyLayout.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.GONE);
    }

    private void openDetails(EventList event) {
        Intent intent = new Intent(this, VolunteerEventDetails.class);
        intent.putExtra("eventId", event.getId());
        startActivity(intent);
    }

    static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

        interface OnEventClick {
            void onClick(EventList event);
        }

        private final ArrayList<EventList> items;
        private final Map<String, Long> counts;
        private final OnEventClick listener;

        EventsAdapter(ArrayList<EventList> items, Map<String, Long> counts, OnEventClick listener) {
            this.items = items;
            this.counts = counts;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_volunteer_assigned_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EventList event = items.get(position);

            ImageUtil.loadEventImage(holder.image, event);
            holder.category.setText(event.getCategory());
            holder.title.setText(event.getTitle());
            holder.dateLocation.setText(event.getDateAndLocation());

            Long count = counts.get(event.getId());
            holder.registeredCount.setText(count == null
                    ? "Registered: --"
                    : String.format(Locale.getDefault(), "Registered: %02d", count));

            View.OnClickListener open = v -> listener.onClick(event);
            holder.details.setOnClickListener(open);
            holder.itemView.setOnClickListener(open);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView category, title, dateLocation, registeredCount;
            Button details;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.volunteer_assigned_event_Image);
                category = itemView.findViewById(R.id.volunteer_assigned_event_Category);
                title = itemView.findViewById(R.id.volunteer_assigned_event_Title);
                dateLocation = itemView.findViewById(R.id.volunteer_assigned_event_DateLocation);
                registeredCount = itemView.findViewById(R.id.volunteer_assigned_event_RegisteredCount);
                details = itemView.findViewById(R.id.volunteer_assigned_event_btnDetails);
            }
        }
    }
}