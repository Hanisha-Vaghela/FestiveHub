//package com.example.festivehub;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Volunteer screen: full details and the schedule of one event.
// * The schedule is the event's "schedule" field: one line per slot, for example
// *     10:00 AM - Gates open
// *     11:30 AM - Opening ceremony
// */
//public class VolunteerEventDetails extends AppCompatActivity {
//
//    // "<time> - <activity>" (hyphen, en dash or em dash)
//    private static final Pattern SLOT = Pattern.compile("^(.*?)\\s+[-–—]\\s+(.*)$");
//
//    ImageView banner;
//    TextView tvCategory, tvTitle, tvDateLocation, tvDescription, tvNoSchedule;
//    LinearLayout scheduleContainer;
//    Button btnBack;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_volunteer_event_details);
//
//        banner = findViewById(R.id.volunteer_event_details_Banner);
//        tvCategory = findViewById(R.id.volunteer_event_details_Category);
//        tvTitle = findViewById(R.id.volunteer_event_details_Title);
//        tvDateLocation = findViewById(R.id.volunteer_event_details_DateLocation);
//        tvDescription = findViewById(R.id.volunteer_event_details_Description);
//        tvNoSchedule = findViewById(R.id.volunteer_event_details_NoSchedule);
//        scheduleContainer = findViewById(R.id.volunteer_event_details_ScheduleContainer);
//        btnBack = findViewById(R.id.volunteer_event_details_btnBack);
////        Button btnScanQr = findViewById(R.id.volunteer_event_details_btnScanQr);
//        btnBack.setOnClickListener(v -> finish());
//
//        String eventId = getIntent().getStringExtra("eventId");
//        if (eventId == null) {
//            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
//                .addOnSuccessListener(doc -> {
//                    EventList event = doc.exists() ? doc.toObject(EventList.class) : null;
//                    if (event == null) {
//                        Toast.makeText(this, "This event no longer exists", Toast.LENGTH_SHORT).show();
//                        finish();
//                        return;
//                    }
//                    ImageUtil.loadEventImage(banner, event);
//                    tvCategory.setText(event.getCategory() != null ? event.getCategory() : "");
//                    tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Event");
//                    tvDateLocation.setText(event.getDateAndLocation() != null ? event.getDateAndLocation() : "");
//                    tvDescription.setText(event.getDescription() != null ? event.getDescription() : "");
//                    showSchedule(doc.getString("schedule"));
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Couldn't load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    finish();
//                });
//    }
//
//    private void showSchedule(String schedule) {
//        scheduleContainer.removeAllViews();
//
//        boolean any = false;
//        if (schedule != null) {
//            for (String line : schedule.split("\\r?\\n")) {
//                line = line.trim();
//                if (line.isEmpty()) {
//                    continue;
//                }
//
//                String time = "";
//                String activity = line;
//                Matcher matcher = SLOT.matcher(line);
//                if (matcher.matches()) {
//                    time = matcher.group(1).trim();
//                    activity = matcher.group(2).trim();
//                }
//
//                View row = LayoutInflater.from(this).inflate(R.layout.item_volunteer_schedule, scheduleContainer, false);
//                TextView tvTime = row.findViewById(R.id.volunteer_schedule_Time);
//                TextView tvActivity = row.findViewById(R.id.volunteer_schedule_Activity);
//                tvTime.setText(time);
//                tvTime.setVisibility(time.isEmpty() ? View.GONE : View.VISIBLE);
//                tvActivity.setText(activity);
//                scheduleContainer.addView(row);
//                any = true;
//            }
//        }
//
//        tvNoSchedule.setVisibility(any ? View.GONE : View.VISIBLE);
//        scheduleContainer.setVisibility(any ? View.VISIBLE : View.GONE);
//    }
//}
package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VolunteerEventDetails extends AppCompatActivity {

    // "<time> - <activity>" (hyphen, en dash or em dash)
    private static final Pattern SLOT = Pattern.compile("^(.*?)\\s+[-–—]\\s+(.*)$");

    ImageView banner;
    TextView tvCategory, tvTitle, tvDateLocation, tvDescription, tvNoSchedule;
    LinearLayout scheduleContainer;
    Button btnScanQr, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_event_details);

        banner = findViewById(R.id.volunteer_event_details_Banner);
        tvCategory = findViewById(R.id.volunteer_event_details_Category);
        tvTitle = findViewById(R.id.volunteer_event_details_Title);
        tvDateLocation = findViewById(R.id.volunteer_event_details_DateLocation);
        tvDescription = findViewById(R.id.volunteer_event_details_Description);
        tvNoSchedule = findViewById(R.id.volunteer_event_details_NoSchedule);
        scheduleContainer = findViewById(R.id.volunteer_event_details_ScheduleContainer);
        btnScanQr = findViewById(R.id.volunteer_event_details_btnScanQr);
        btnBack = findViewById(R.id.volunteer_event_details_btnBack);

        btnBack.setOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Open QR Scanner for this specific event
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(VolunteerEventDetails.this, Volunteers_QR.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    EventList event = doc.exists() ? doc.toObject(EventList.class) : null;
                    if (event == null) {
                        Toast.makeText(this, "This event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    ImageUtil.loadEventImage(banner, event);
                    tvCategory.setText(event.getCategory() != null ? event.getCategory() : "");
                    tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Event");
                    tvDateLocation.setText(event.getDateAndLocation() != null ? event.getDateAndLocation() : "");
                    tvDescription.setText(event.getDescription() != null ? event.getDescription() : "");
                    showSchedule(doc.getString("schedule"));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showSchedule(String schedule) {
        scheduleContainer.removeAllViews();

        boolean any = false;
        if (schedule != null) {
            for (String line : schedule.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String time = "";
                String activity = line;
                Matcher matcher = SLOT.matcher(line);
                if (matcher.matches()) {
                    time = matcher.group(1).trim();
                    activity = matcher.group(2).trim();
                }

                View row = LayoutInflater.from(this).inflate(R.layout.item_volunteer_schedule, scheduleContainer, false);
                TextView tvTime = row.findViewById(R.id.volunteer_schedule_Time);
                TextView tvActivity = row.findViewById(R.id.volunteer_schedule_Activity);
                tvTime.setText(time);
                tvTime.setVisibility(time.isEmpty() ? View.GONE : View.VISIBLE);
                tvActivity.setText(activity);
                scheduleContainer.addView(row);
                any = true;
            }
        }

        tvNoSchedule.setVisibility(any ? View.GONE : View.VISIBLE);
        scheduleContainer.setVisibility(any ? View.VISIBLE : View.GONE);
    }
}