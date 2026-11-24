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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Volunteer screen: the events assigned to the logged-in volunteer.
 *
 * How the assignment is found:
 *   volunteers collection  ->  records whose "email" is the volunteer's login email
 *   each record's "assignedEvent" (an event title)  ->  the matching events
 * (To assign several events, the admin adds one volunteer record per event with the same email.)
 */
public class VolunteerAssignedEvents extends AppCompatActivity {

    TextView tvTotalAssigned, tvTotalRegistrations, tvNoEvents;
    LinearLayout emptyLayout;
    RecyclerView rvEvents;
    Button btnBack;

    FirebaseFirestore db;
    final ArrayList<EventList> events = new ArrayList<>();
    final Map<String, Long> counts = new HashMap<>(); // eventId -> number registered
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvNoEvents.setText("Loading your events...");
        emptyLayout.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.GONE);

        loadAssignedTitles(user.getEmail());
    }

    // Step 1: which events (by title) did the admin assign to this email?
    private void loadAssignedTitles(String email) {
        db.collection("volunteers").whereEqualTo("email", email).get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> titles = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String title = doc.getString("assignedEvent");
                        if (title != null && !title.trim().isEmpty()) {
                            titles.add(title.trim());
                        }
                    }
                    if (titles.isEmpty()) {
                        showResult();
                    } else {
                        loadEvents(titles);
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // Step 2: load those events
    private void loadEvents(Set<String> titles) {
        List<Task<QuerySnapshot>> queries = new ArrayList<>();
        for (String title : titles) {
            queries.add(db.collection("events").whereEqualTo("title", title).get());
        }

        Tasks.whenAllSuccess(queries)
                .addOnSuccessListener(results -> {
                    events.clear();
                    Set<String> seen = new HashSet<>();
                    for (Object result : results) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) result).getDocuments()) {
                            EventList event = doc.toObject(EventList.class);
                            if (event != null && seen.add(doc.getId())) {
                                events.add(event);
                            }
                        }
                    }
                    showResult();
                    for (EventList event : events) {
                        loadRegisteredCount(event.getId());
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // Step 3: how many people registered for each event
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
        // if this fails (for example missing permission) the row simply shows "Registered: --"
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

    // ---------- list adapter (uses item_volunteer_assigned_event.xml) ----------

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
