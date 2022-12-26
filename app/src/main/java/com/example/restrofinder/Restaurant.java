package com.example.restrofinder;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

public class Restaurant implements Serializable, Comparable<Restaurant> {

    private String restaurantID;
    private String restaurantName;
    private String latitude;
    private String longitude;
    private String address;
    private String imageUrl;
    private double distance;
    private List<String> historyList;
    private List<String> shocksList;
    private boolean isFavorite;

    public Restaurant() {
    }

    public Restaurant(String restaurantName, String latitude, String longitude, String address) {
        this.restaurantName = restaurantName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public String getRestaurantID() {
        return restaurantID;
    }

    public void setRestaurantID(String restaurantID) {
        this.restaurantID = restaurantID;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public List<String> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<String> historyList) {
        this.historyList = historyList;
    }

    public List<String> getShocksList() {
        return shocksList;
    }

    public void setShocksList(List<String> shocksList) {
        this.shocksList = shocksList;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public int compareTo(Restaurant r) {
        return (this.getDistance() < r.getDistance() ? -1 :
                (this.getDistance() == r.getDistance() ? 0 : 1));
    }

    public boolean equals(@Nullable Restaurant restaurant) {
        if (restaurant.getRestaurantName().equalsIgnoreCase(this.getRestaurantName())) {
            if (restaurant.getAddress().equalsIgnoreCase(this.getAddress())) {
                return true;
            }
        }
        return false;
    }
}
