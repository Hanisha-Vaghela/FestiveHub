package com.example.festivehub;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Home extends AppCompatActivity {

    RecyclerView rvEvents;
    EditText etSearchEvents;
    Button btnCatAll, btnCatCultural, btnCatMarketplace, btnCatMusic;
    Button btnFilterFree, btnFilterUnder500;

    ArrayList<EventList> allEventsList = new ArrayList<>();
    EventAdapter eventAdapter;

    String activeCategory = "ALL";
    String activePriceFilter = "ALL"; // "ALL", "FREE", "UNDER_500"
    String searchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvEvents = findViewById(R.id.rvEvents);
        etSearchEvents = findViewById(R.id.etSearchEvents);
        btnCatAll = findViewById(R.id.btnCatAll);
        btnCatCultural = findViewById(R.id.btnCatCultural);
        btnCatMarketplace = findViewById(R.id.btnCatMarketplace);
        btnCatMusic = findViewById(R.id.btnCatMusic);
        btnFilterFree = findViewById(R.id.btnFilterFree);
        btnFilterUnder500 = findViewById(R.id.btnFilterUnder500);

        addSampleEvents();

        eventAdapter = new EventAdapter(allEventsList, event ->
                Toast.makeText(Home.this, "Booking: " + event.getTitle(), Toast.LENGTH_SHORT).show()
        );
        rvEvents.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        rvEvents.setAdapter(eventAdapter);

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
            if (activePriceFilter.equals("FREE")) {
                activePriceFilter = "ALL";
            } else {
                activePriceFilter = "FREE";
            }
            refreshButtons();
            filterEventsList();
        });

        btnFilterUnder500.setOnClickListener(v -> {
            if (activePriceFilter.equals("UNDER_500")) {
                activePriceFilter = "ALL";
            } else {
                activePriceFilter = "UNDER_500";
            }
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

        refreshButtons();
    }

    private void addSampleEvents() {
        allEventsList.add(new EventList(
                "Navratri Garba Night",
                "CULTURAL FEST",
                "Oct 15, 2026 • SG Highway, Ahmedabad",
                "₹499",
                499,
                R.drawable.navratri_event
        ));
        allEventsList.add(new EventList(
                "Diwali Night Bazaar & Crafts",
                "MARKETPLACE",
                "Nov 01, 2026 • Sabarmati Riverfront, Ahmedabad",
                "Free Entry",
                0,
                R.drawable.diwalinight
        ));
        allEventsList.add(new EventList(
                "Sufi & Folk Live Night",
                "MUSIC",
                "Nov 28, 2026 • Sindhu Bhavan Road, Ahmedabad",
                "₹799",
                799,
                R.drawable.sufifolks
        ));
        allEventsList.add(new EventList(
                "Uttarayan Kite & Heritage Fest",
                "CULTURAL FEST",
                "Jan 14, 2027 • Ashram Road, Ahmedabad",
                "Free Entry",
                0,
                R.drawable.kitefest
        ));
        allEventsList.add(new EventList(
                "Handloom & Festive Street Fair",
                "MARKETPLACE",
                "Feb 08, 2027 • Law Garden, Ahmedabad",
                "₹199",
                199,
                R.drawable.streetfair
        ));
        allEventsList.add(new EventList(
                "Acoustic Sunset Jam",
                "MUSIC",
                "Feb 21, 2027 • Thaltej, Ahmedabad",
                "₹350",
                350,
                R.drawable.sunset
        ));
    }

    private void filterEventsList() {
        ArrayList<EventList> filteredList = new ArrayList<>();

        for (int i = 0; i < allEventsList.size(); i++) {
            EventList event = allEventsList.get(i);

            boolean categoryMatches = activeCategory.equals("ALL")
                    || event.getCategory().equalsIgnoreCase(activeCategory);

            boolean priceMatches = true;
            if (activePriceFilter.equals("FREE")) {
                priceMatches = (event.getPriceAmount() == 0);
            } else if (activePriceFilter.equals("UNDER_500")) {
                priceMatches = (event.getPriceAmount() <= 500);
            }

            boolean searchMatches = true;
            if (!searchText.isEmpty()) {
                boolean inTitle = event.getTitle().toLowerCase().contains(searchText);
                boolean inLocation = event.getDateAndLocation().toLowerCase().contains(searchText);
                boolean inCategory = event.getCategory().toLowerCase().contains(searchText);
                searchMatches = inTitle || inLocation || inCategory;
            }

            if (categoryMatches && priceMatches && searchMatches) {
                filteredList.add(event);
            }
        }

        eventAdapter.setFilteredList(filteredList);
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
}