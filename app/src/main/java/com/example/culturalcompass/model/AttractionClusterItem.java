package com.example.culturalcompass.model;

import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class AttractionClusterItem implements ClusterItem {

    private final Attraction attraction;
    private final LatLng position;
    private final String title;
    private final String snippet;

    public AttractionClusterItem(Attraction attraction) {
        this.attraction = attraction;
        this.position = new LatLng(attraction.getLat(), attraction.getLng());
        this.title = attraction.getName();
        this.snippet = attraction.getType();
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

    public Attraction getAttraction() {
        return attraction;
    }
}