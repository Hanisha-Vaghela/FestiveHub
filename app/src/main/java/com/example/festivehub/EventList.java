package com.example.festivehub;

public class EventList {
    private String title;
    private String category;
    private String dateAndLocation;
    private String priceText;
    private int priceAmount;
    private int imageResId;

    public EventList(String title, String category, String dateAndLocation, String priceText, int priceAmount, int imageResId) {
        this.title = title;
        this.category = category;
        this.dateAndLocation = dateAndLocation;
        this.priceText = priceText;
        this.priceAmount = priceAmount;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDateAndLocation() {
        return dateAndLocation;
    }

    public void setDateAndLocation(String dateAndLocation) {
        this.dateAndLocation = dateAndLocation;
    }

    public String getPriceText() {
        return priceText;
    }

    public void setPriceText(String priceText) {
        this.priceText = priceText;
    }

    public int getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(int priceAmount) {
        this.priceAmount = priceAmount;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}