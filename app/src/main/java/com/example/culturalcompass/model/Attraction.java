package com.example.culturalcompass.model;
import com.google.android.libraries.places.api.model.PhotoMetadata;

import com.google.android.libraries.places.api.model.PhotoMetadata;

public class Attraction {
    private String name;
    private double lat;
    private double lng;
    private double distanceMeters;
    private String typeLabel;
    private String primaryTypeKey; // "museum", "art_gallery", "tourist_attraction"
    private Double rating;
    private Integer ratingCount;
    private boolean favorite;
    private PhotoMetadata photoMetadata;

    public Attraction(String name,
                      double lat,
                      double lng,
                      double distanceMeters,
                      String typeLabel,
                      String primaryTypeKey,
                      Double rating,
                      Integer ratingCount,
                      PhotoMetadata photoMetadata) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distanceMeters = distanceMeters;
        this.typeLabel = typeLabel;
        this.primaryTypeKey = primaryTypeKey;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.photoMetadata = photoMetadata;
    }

    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getDistanceMeters() { return distanceMeters; }

    public String getType() { return typeLabel; }
    public String getPrimaryTypeKey() { return primaryTypeKey; }

    public Double getRating() { return rating; }
    public Integer getRatingCount() { return ratingCount; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public PhotoMetadata getPhotoMetadata() { return photoMetadata; }
}


