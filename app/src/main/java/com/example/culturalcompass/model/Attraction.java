package com.example.culturalcompass.model;
import com.google.android.libraries.places.api.model.PhotoMetadata;

public class Attraction {
    private String name;
    private double lat;
    private double lng;
    private double distanceMeters;
    private String type;
    private Double rating;        // nullable
    private Integer ratingCount;  // nullable
    private boolean favorite;
    private PhotoMetadata photoMetadata;

    public Attraction(String name,
                      double lat,
                      double lng,
                      double distanceMeters,
                      String type,
                      Double rating,
                      Integer ratingCount, PhotoMetadata photoMetadata) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distanceMeters = distanceMeters;
        this.type = type;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.photoMetadata = photoMetadata;
    }

    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getDistanceMeters() { return distanceMeters; }
    public String getType() { return type; }
    public Double getRating() { return rating; }
    public Integer getRatingCount() { return ratingCount; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public PhotoMetadata getPhotoMetadata() { return photoMetadata; }

}

