package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Event_Detail extends AppCompatActivity {

    ImageView DetailBanner;
    TextView DetailCategory, DetailTitle, DetailDateLocation, DetailPrice, DetailDescription;
    Button btnConfirmBooking, btnBackToHome,btnvolunteerqr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_detail);

        DetailBanner = findViewById(R.id.eventdetail_DetailBanner);
        DetailCategory = findViewById(R.id.eventdetail_DetailCategory);
        DetailTitle = findViewById(R.id.eventdetail_DetailTitle);
        DetailDateLocation = findViewById(R.id.eventdetail_DetailDateLocation);
        DetailPrice = findViewById(R.id.eventdetail_DetailPrice);
        DetailDescription = findViewById(R.id.eventdetail_DetailDescription);
        btnConfirmBooking = findViewById(R.id.eventdetail_btnConfirmBooking);
        btnBackToHome = findViewById(R.id.eventdetail_btnBackToHome);
        btnvolunteerqr = findViewById(R.id.eventdetail_btnvolunteerqr);

        String title = getIntent().getStringExtra("title");
        String category = getIntent().getStringExtra("category");
        String dateAndLocation = getIntent().getStringExtra("dateAndLocation");
        String priceText = getIntent().getStringExtra("priceText");
        String description = getIntent().getStringExtra("description");
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.navratri_event);

        DetailBanner.setImageResource(imageResId);
        DetailCategory.setText(category != null ? category : "");
        DetailTitle.setText(title != null ? title : "Event");
        DetailDateLocation.setText(dateAndLocation != null ? dateAndLocation : "");
        DetailPrice.setText(priceText != null ? priceText : "");
        DetailDescription.setText(description != null ? description : "");

        btnConfirmBooking.setOnClickListener(v -> {
            Toast.makeText(this, "Booking confirmed for " + DetailTitle.getText().toString(), Toast.LENGTH_SHORT).show();

        });
        btnvolunteerqr.setOnClickListener(v -> {
            startActivity(new Intent(Event_Detail.this, Volunteers_QR.class));
        });

        btnBackToHome.setOnClickListener(v -> finish());
    }
}