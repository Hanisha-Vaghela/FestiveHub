package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class VolunteerHomeScreen extends AppCompatActivity {

    Button explorebtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_home_screen);

        explorebtn = findViewById(R.id.volunteer_home_screen_btnExploreEvents);

        explorebtn.setOnClickListener(view -> {
            startActivity(new Intent(
                    VolunteerHomeScreen.this,
                    VolunteerAssignedEvents.class
            ));
        });
    }
}