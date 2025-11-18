package com.example.culturalcompass.model;

public class FirestoreAttraction {

    private String id;
    private String name;
    private double lat;
    private double lng;
    private double distanceMeters;
    private String typeLabel;
    private String primaryTypeKey;
    private Double rating;
    private Integer ratingCount;

    // Needed for Firestore deserialization
    public FirestoreAttraction() { }

    public FirestoreAttraction(String id,
                               String name,
                               double lat,
                               double lng,
                               double distanceMeters,
                               String typeLabel,
                               String primaryTypeKey,
                               Double rating,
                               Integer ratingCount) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distanceMeters = distanceMeters;
        this.typeLabel = typeLabel;
        this.primaryTypeKey = primaryTypeKey;
        this.rating = rating;
        this.ratingCount = ratingCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getDistanceMeters() { return distanceMeters; }
    public String getTypeLabel() { return typeLabel; }
    public String getPrimaryTypeKey() { return primaryTypeKey; }
    public Double getRating() { return rating; }
    public Integer getRatingCount() { return ratingCount; }
}
