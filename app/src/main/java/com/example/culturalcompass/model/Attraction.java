package com.example.culturalcompass.model;

public class Attraction {
    private String name;
    private double lat;
    private double lng;
    private double distanceMeters;
    private String type; // e.g. "Museum", "Historical landmark"

    public Attraction(String name,
                      double lat,
                      double lng,
                      double distanceMeters,
                      String type) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distanceMeters = distanceMeters;
        this.type = type;
    }

    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getDistanceMeters() { return distanceMeters; }
    public String getType() { return type; }
}
