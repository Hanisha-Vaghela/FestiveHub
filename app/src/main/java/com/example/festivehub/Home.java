package com.example.festivehub;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class Home extends AppCompatActivity {

    RecyclerView rvEvents;
    EditText etSearchEvents;
    TextView tvEmpty, tvGreeting;
    ImageView ivProfile, ivLogout;
    Button btnCatAll, btnCatCultural, btnCatMarketplace, btnCatMusic;
    Button btnFilterFree, btnFilterUnder500;
    MaterialCardView cardBanner;
    ViewPager2 vpBanner;

    ArrayList<EventList> allEventsList = new ArrayList<>();
    EventAdapter eventAdapter;
    BannerAdapter bannerAdapter;

    FirebaseFirestore db;
    ListenerRegistration eventsListener;
    boolean eventsLoaded = false;

    String activeCategory = "ALL";
    String activePriceFilter = "ALL"; // "ALL", "FREE", "UNDER_500"
    String searchText = "";

    // Auto-slide for the banner carousel
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            int count = bannerAdapter.getItemCount();
            if (count > 1) {
                vpBanner.setCurrentItem((vpBanner.getCurrentItem() + 1) % count, true);
            }
            sliderHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        rvEvents = findViewById(R.id.rvEvents);
        etSearchEvents = findViewById(R.id.etSearchEvents);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvGreeting = findViewById(R.id.tvGreeting);
        ivProfile = findViewById(R.id.ivProfile);
        ivLogout = findViewById(R.id.ivLogout);
        cardBanner = findViewById(R.id.cardBanner);
        vpBanner = findViewById(R.id.vpBanner);
        btnCatAll = findViewById(R.id.btnCatAll);
        btnCatCultural = findViewById(R.id.btnCatCultural);
        btnCatMarketplace = findViewById(R.id.btnCatMarketplace);
        btnCatMusic = findViewById(R.id.btnCatMusic);
        btnFilterFree = findViewById(R.id.btnFilterFree);
        btnFilterUnder500 = findViewById(R.id.btnFilterUnder500);

        db = FirebaseFirestore.getInstance();

        // Event list
        eventAdapter = new EventAdapter(new ArrayList<>(), this::openEventDetail);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);

        // Banner carousel
        bannerAdapter = new BannerAdapter(this::openEventDetail);
        vpBanner.setAdapter(bannerAdapter);

        ivProfile.setOnClickListener(v -> startActivity(new Intent(Home.this, Profile.class)));
        ivLogout.setOnClickListener(v -> confirmLogout());

        setupFilters();
        refreshButtons();

        // TODO: delete this line after the first run (it uploads the 6 sample events once)
        EventSeeder.seedIfEmpty(db);

        loadEvents();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(Home.this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Home.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    // "Hello, <first name>!" from the saved profile
    private void loadGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            if (name != null && !name.trim().isEmpty()) {
                tvGreeting.setText("Hello, " + name.trim().split(" ")[0] + "!");
            }
        });
    }

    private void loadEvents() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Loading events...");

        // Real-time listener: Home updates by itself when events change in Firestore
        eventsListener = db.collection("events").addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Couldn't load events");
                Toast.makeText(Home.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            allEventsList.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    EventList event = doc.toObject(EventList.class);
                    if (event != null) {
                        allEventsList.add(event);
                    }
                }
            }
            eventsLoaded = true;
            updateBanner();
            filterEventsList();
        });
    }

    private void openEventDetail(EventList event) {
        Intent intent = new Intent(Home.this, Event_Detail.class);
        intent.putExtra("eventId", event.getId());
        startActivity(intent);
    }

    private void updateBanner() {
        ArrayList<EventList> banners = new ArrayList<>();
        for (EventList event : allEventsList) {
            if (event.isFeatured()) {
                banners.add(event);
            }
        }
        // No featured events? Use the first few events instead
        if (banners.isEmpty()) {
            for (int i = 0; i < Math.min(5, allEventsList.size()); i++) {
                banners.add(allEventsList.get(i));
            }
        }
        bannerAdapter.setItems(banners);
        cardBanner.setVisibility(banners.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupFilters() {
        btnCatAll.setOnClickListener(v -> {
            activeCategory = "ALL";
            refreshButtons();
            filterEventsList();
        });

        btnCatCultural.setOnClickListener(v -> {
            activeCategory = "CULTURAL FEST";
            refreshButtons();
            filterEventsList();
        });

        btnCatMarketplace.setOnClickListener(v -> {
            activeCategory = "MARKETPLACE";
            refreshButtons();
            filterEventsList();
        });

        btnCatMusic.setOnClickListener(v -> {
            activeCategory = "MUSIC";
            refreshButtons();
            filterEventsList();
        });

        btnFilterFree.setOnClickListener(v -> {
            activePriceFilter = activePriceFilter.equals("FREE") ? "ALL" : "FREE";
            refreshButtons();
            filterEventsList();
        });

        btnFilterUnder500.setOnClickListener(v -> {
            activePriceFilter = activePriceFilter.equals("UNDER_500") ? "ALL" : "UNDER_500";
            refreshButtons();
            filterEventsList();
        });

        etSearchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString().toLowerCase().trim();
                filterEventsList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void filterEventsList() {
        ArrayList<EventList> filteredList = new ArrayList<>();

        for (EventList event : allEventsList) {
            boolean categoryMatches = activeCategory.equals("ALL")
                    || activeCategory.equalsIgnoreCase(event.getCategory());

            boolean priceMatches = true;
            if (activePriceFilter.equals("FREE")) {
                priceMatches = (event.getPriceAmount() == 0);
            } else if (activePriceFilter.equals("UNDER_500")) {
                priceMatches = (event.getPriceAmount() <= 500);
            }

            boolean searchMatches = true;
            if (!searchText.isEmpty()) {
                searchMatches = contains(event.getTitle(), searchText)
                        || contains(event.getDateAndLocation(), searchText)
                        || contains(event.getCategory(), searchText);
            }

            if (categoryMatches && priceMatches && searchMatches) {
                filteredList.add(event);
            }
        }

        eventAdapter.setFilteredList(filteredList);

        if (filteredList.isEmpty() && eventsLoaded) {
            tvEmpty.setText("No events found");
            tvEmpty.setVisibility(View.VISIBLE);
        } else if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void refreshButtons() {
        Button[] catButtons = {btnCatAll, btnCatCultural, btnCatMarketplace, btnCatMusic};
        String[] catKeys = {"ALL", "CULTURAL FEST", "MARKETPLACE", "MUSIC"};

        for (int i = 0; i < catButtons.length; i++) {
            if (activeCategory.equals(catKeys[i])) {
                catButtons[i].setBackgroundResource(R.drawable.button_bg);
                catButtons[i].setTextColor(Color.WHITE);
            } else {
                catButtons[i].setBackgroundResource(R.drawable.edittext_bg);
                catButtons[i].setTextColor(Color.BLACK);
            }
        }

        if (activePriceFilter.equals("FREE")) {
            btnFilterFree.setBackgroundResource(R.drawable.button_bg);
            btnFilterFree.setTextColor(Color.WHITE);
        } else {
            btnFilterFree.setBackgroundResource(R.drawable.edittext_bg);
            btnFilterFree.setTextColor(Color.BLACK);
        }

        if (activePriceFilter.equals("UNDER_500")) {
            btnFilterUnder500.setBackgroundResource(R.drawable.button_bg);
            btnFilterUnder500.setTextColor(Color.WHITE);
        } else {
            btnFilterUnder500.setBackgroundResource(R.drawable.edittext_bg);
            btnFilterUnder500.setTextColor(Color.BLACK);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGreeting(); // refresh name after editing profile
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventsListener != null) {
            eventsListener.remove();
        }
    }
}
