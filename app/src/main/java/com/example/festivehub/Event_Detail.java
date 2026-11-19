package com.example.festivehub;

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

    ImageView ivDetailBanner;
    TextView tvDetailCategory, tvDetailTitle, tvDetailDateLocation, tvDetailPrice, tvDetailDescription;
    Button btnConfirmBooking, btnBackToHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivDetailBanner = findViewById(R.id.ivDetailBanner);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDateLocation = findViewById(R.id.tvDetailDateLocation);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        String title = getIntent().getStringExtra("title");
        String category = getIntent().getStringExtra("category");
        String dateAndLocation = getIntent().getStringExtra("dateAndLocation");
        String priceText = getIntent().getStringExtra("priceText");
        String description = getIntent().getStringExtra("description");
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.navratri_event);

        ivDetailBanner.setImageResource(imageResId);
        tvDetailCategory.setText(category != null ? category : "");
        tvDetailTitle.setText(title != null ? title : "Event");
        tvDetailDateLocation.setText(dateAndLocation != null ? dateAndLocation : "");
        tvDetailPrice.setText(priceText != null ? priceText : "");
        tvDetailDescription.setText(description != null ? description : "");

        btnConfirmBooking.setOnClickListener(v ->
                Toast.makeText(this, "Booking confirmed for " + tvDetailTitle.getText().toString(), Toast.LENGTH_SHORT).show()
        );

        btnBackToHome.setOnClickListener(v -> finish());
    }
}