package com.example.festivehub;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * One-time helper: if the "events" collection is empty it uploads your 6 sample events.
 * Run the app once, check Firestore, then you can delete this class and its call in Home.
 */
public class EventSeeder {

    public static void seedIfEmpty(FirebaseFirestore db) {
        db.collection("events").limit(1).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.isEmpty()) {
                return;
            }
            WriteBatch batch = db.batch();

            add(db, batch, "Navratri Garba Night", "CULTURAL FEST",
                    "Oct 15, 2026 • SG Highway, Ahmedabad", "₹499", 499, "navratri_event",
                    "Dance the night away with live folk orchestra, traditional garba circles, and festive street food.", true);
            add(db, batch, "Diwali Night Bazaar & Crafts", "MARKETPLACE",
                    "Nov 01, 2026 • Sabarmati Riverfront, Ahmedabad", "Free Entry", 0, "diwalinight",
                    "Explore handmade diyas, festive decor, local artisan stalls, and evening food counters along the riverfront.", true);
            add(db, batch, "Sufi & Folk Live Night", "MUSIC",
                    "Nov 28, 2026 • Sindhu Bhavan Road, Ahmedabad", "₹799", 799, "sufifolks",
                    "An evening of soulful sufi and regional folk performances under open skies with refreshments.", true);
            add(db, batch, "Uttarayan Kite & Heritage Fest", "CULTURAL FEST",
                    "Jan 14, 2027 • Ashram Road, Ahmedabad", "Free Entry", 0, "kitefest",
                    "Celebrate Makar Sankranti with rooftop kite flying, traditional sweets like undhiyu and chikki, and music.", false);
            add(db, batch, "Handloom & Festive Street Fair", "MARKETPLACE",
                    "Feb 08, 2027 • Law Garden, Ahmedabad", "₹199", 199, "streetfair",
                    "Browse authentic bandhani, textiles, jewelry, and handcrafted gifts from regional makers.", false);
            add(db, batch, "Acoustic Sunset Jam", "MUSIC",
                    "Feb 21, 2027 • Thaltej, Ahmedabad", "₹350", 350, "sunset",
                    "Relax at sunset with unplugged acoustic sets, indie artists, and cozy cafe vibes.", false);

            batch.commit()
                    .addOnSuccessListener(v -> Log.d("EventSeeder", "Sample events uploaded"))
                    .addOnFailureListener(e -> Log.e("EventSeeder", "Seeding failed: " + e.getMessage()));
        });
    }

    private static void add(FirebaseFirestore db, WriteBatch batch, String title, String category,
                            String dateAndLocation, String priceText, int priceAmount,
                            String imageName, String description, boolean featured) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("category", category);
        event.put("dateAndLocation", dateAndLocation);
        event.put("priceText", priceText);
        event.put("priceAmount", priceAmount);
        event.put("imageUrl", "");
        event.put("imageName", imageName);
        event.put("description", description);
        event.put("featured", featured);
        batch.set(db.collection("events").document(), event);
    }
}
