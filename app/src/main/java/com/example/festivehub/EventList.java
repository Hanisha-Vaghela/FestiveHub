package com.example.festivehub;

import com.google.firebase.firestore.DocumentId;

/**
 * Event model stored in Firestore: collection "events".
 * Firestore needs a public no-arg constructor and getters/setters.
 */
public class EventList {
    @DocumentId
    private String id;              // Firestore document id (filled automatically)
    private String title;
    private String category;        // "CULTURAL FEST", "MARKETPLACE", "MUSIC"
    private String dateAndLocation;
    private String priceText;
    private int priceAmount;
    private String imageUrl;        // banner link (use this for uploaded images)
    private String imageName;       // fallback: name of a drawable, e.g. "kitefest"
    private String description;
    private boolean featured;       // true = shown in the Home carousel

    public EventList() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDateAndLocation() { return dateAndLocation; }
    public void setDateAndLocation(String dateAndLocation) { this.dateAndLocation = dateAndLocation; }

    public String getPriceText() { return priceText; }
    public void setPriceText(String priceText) { this.priceText = priceText; }

    public int getPriceAmount() { return priceAmount; }
    public void setPriceAmount(int priceAmount) { this.priceAmount = priceAmount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
}
