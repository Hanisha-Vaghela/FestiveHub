package com.example.festivehub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes one type of data the admin can manage (Events, Students, ...).
 * The same list screen (ManageList) and form screen (ManageForm) work for all of them.
 * To add another type, add one more block in the static section below.
 */
public class AdminEntity {

    public final String key;           // passed between screens
    public final String title;         // "Events"
    public final String singular;      // "Event"
    public final String collection;    // Firestore collection name
    public final String titleKey;      // field shown as the row title
    public final String[] subtitleKeys; // fields shown under the title
    public final List<AdminField> fields = new ArrayList<>();

    private AdminEntity(String key, String title, String singular, String collection,
                        String titleKey, String... subtitleKeys) {
        this.key = key;
        this.title = title;
        this.singular = singular;
        this.collection = collection;
        this.titleKey = titleKey;
        this.subtitleKeys = subtitleKeys;
    }

    private AdminEntity with(AdminField... f) {
        fields.addAll(Arrays.asList(f));
        return this;
    }

    private static final Map<String, AdminEntity> ALL = new LinkedHashMap<>();

    static {
        // Category values must match the Home screen buttons
        add(new AdminEntity("events", "Events", "Event", "events",
                "title", "category", "dateAndLocation", "priceText").with(
                AdminField.text("title", "Title", true),
                AdminField.choice("category", "Category", true, "CULTURAL FEST", "MARKETPLACE", "MUSIC"),
                AdminField.text("dateAndLocation", "Date & Location", true,
                        "Oct 15, 2026 • SG Highway, Ahmedabad"),
                AdminField.number("priceAmount", "Price in ₹ (0 = free entry)", true),
                AdminField.text("imageUrl", "Banner image link (optional)", false, "https://..."),
                AdminField.multiline("description", "Description", true),
                AdminField.multiline("schedule", "Schedule (one line per slot, e.g. 10:00 AM - Gates open)", false),
                AdminField.bool("featured", "Show in Home carousel")));

        add(new AdminEntity("students", "Students", "Student", "students",
                "name", "college", "email", "mobile").with(
                AdminField.text("name", "Full name", true),
                AdminField.email("email", "Email", true),
                AdminField.phone("mobile", "Mobile number", true),
                AdminField.choiceFromCollection("college", "College", false, "colleges", "name")));

        add(new AdminEntity("volunteers", "Volunteers", "Volunteer", "volunteers",
                "name", "assignedEvent", "mobile").with(
                AdminField.text("name", "Full name", true),
                AdminField.phone("mobile", "Mobile number", true),
                AdminField.email("email", "Email (volunteer login email)", true),
                AdminField.choiceFromCollection("assignedEvent", "Assigned event", false, "events", "title")));

        add(new AdminEntity("colleges", "College List", "College", "colleges",
                "name", "city").with(
                AdminField.text("name", "College name", true),
                AdminField.text("city", "City", false)));

        add(new AdminEntity("notifications", "Notifications", "Notification", "notifications",
                "title", "message").with(
                AdminField.text("title", "Title", true),
                AdminField.multiline("message", "Message", true)));
    }

    private static void add(AdminEntity e) {
        ALL.put(e.key, e);
    }

    public static AdminEntity byKey(String key) {
        return key == null ? null : ALL.get(key);
    }
}
