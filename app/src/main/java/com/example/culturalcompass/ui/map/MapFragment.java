package com.example.culturalcompass.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Attraction;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// Places SDK (New)
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;

    private double userLat;
    private double userLon;

    private RecyclerView recyclerNearby;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // To avoid too many API calls:
    private static final float MIN_DISTANCE_CHANGE = 100f; // meters
    private double lastFetchLat = 0;
    private double lastFetchLon = 0;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public MapFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- 1) Initialize Places SDK (New) once ---
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    requireContext().getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }
        placesClient = Places.createClient(requireContext());

        // --- 2) Location client ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Continuous location updates (every 5s, at least 10m movement)
        locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                5000 // 5 seconds
        )
                .setMinUpdateDistanceMeters(10)
                .build();

        // Continuous location listener
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location location = locationResult.getLastLocation();
                if (location == null) return;

                double newLat = location.getLatitude();
                double newLon = location.getLongitude();

                userLat = newLat;
                userLon = newLon;

                // Calculate how far the user moved since last Places fetch
                float[] dist = new float[1];
                android.location.Location.distanceBetween(
                        lastFetchLat, lastFetchLon,
                        newLat, newLon,
                        dist
                );

                if (dist[0] > MIN_DISTANCE_CHANGE) { // moved more than 100m?

                    lastFetchLat = newLat;
                    lastFetchLon = newLon;

                    // Call Nearby Search (New)
                    loadNearbyAttractions(newLat, newLon);

                    // Move camera smoothly
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(newLat, newLon), 16f
                        ));
                    }
                }
            }
        };

        // Runtime permission request launcher
        requestPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                enableMyLocation();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Precise location permission is required",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        recyclerNearby = view.findViewById(R.id.recyclerNearby);
        recyclerNearby.setLayoutManager(new LinearLayoutManager(requireContext()));

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Try enabling location now that map is ready
        enableMyLocation();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    requireActivity().getMainLooper()
            );
        }
    }

    private void enableMyLocation() {
        if (mMap == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);  // SHOW BLUE DOT
            getUserLocation();                // Get coordinates + first camera move

        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getUserLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {

                        userLat = location.getLatitude();
                        userLon = location.getLongitude();

                        // for first fetch threshold
                        lastFetchLat = userLat;
                        lastFetchLon = userLon;

                        LatLng userPosition = new LatLng(userLat, userLon);

                        // First Nearby Search
                        loadNearbyAttractions(userLat, userLon);

                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 16f));
                        }

                    } else {
                        Toast.makeText(requireContext(),
                                "Could not get current location",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error getting location",
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Uses Places SDK for Android (New) Nearby Search to get nearby cultural places.
     */
    private void loadNearbyAttractions(double lat, double lon) {

        // 1) Define which fields we want back for each Place
        final List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.PRIMARY_TYPE,
                Place.Field.PRIMARY_TYPE_DISPLAY_NAME
        );

        // 2) Define circular search area (5km radius)
        LatLng center = new LatLng(lat, lon);
        CircularBounds circle = CircularBounds.newInstance(center, /* radius = */ 5000);

        // 3) Types to include (cultural / historical)
        final List<String> includedTypes = Arrays.asList(
                "tourist_attraction",
                "museum",
                "art_gallery",
                "church",
                "hindu_temple",
                "mosque",
                "synagogue",
                "park",
                "library",
                "cemetery"
        );



        // 5) Build Nearby Search request
        SearchNearbyRequest request =
                SearchNearbyRequest.builder(circle, placeFields)
                        .setIncludedTypes(includedTypes)
                        .setMaxResultCount(10)
                        .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                        .build();

        // 6) Call PlacesClient
        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {

                    List<Place> places = response.getPlaces();
                    List<Attraction> attractions = new ArrayList<>();

                    for (Place place : places) {
                        LatLng placeLoc = place.getLocation();
                        if (placeLoc == null) continue;

                        // distance from user
                        float[] dist = new float[1];
                        android.location.Location.distanceBetween(
                                userLat, userLon,
                                placeLoc.latitude, placeLoc.longitude,
                                dist
                        );
                        double distanceMeters = dist[0];

                        String name = place.getDisplayName();
                        if (name == null || name.isEmpty()) {
                            name = "Unknown place";
                        }

                        // Human readable type label, e.g. "Museum"
                        String typeLabel = place.getPrimaryTypeDisplayName();
                        if (typeLabel == null || typeLabel.isEmpty()) {
                            // fallback to raw primaryType, e.g. "museum"
                            typeLabel = place.getPrimaryType();
                        }

                        attractions.add(
                                new Attraction(
                                        name,
                                        placeLoc.latitude,
                                        placeLoc.longitude,
                                        distanceMeters,
                                        typeLabel
                                )
                        );
                    }

                    // Sort by distance just in case
                    Collections.sort(
                            attractions,
                            Comparator.comparingDouble(Attraction::getDistanceMeters)
                    );

                    // Keep only top 10
                    List<Attraction> top10 =
                            attractions.subList(0, Math.min(10, attractions.size()));

                    // Update UI: clear markers, set adapter, add markers
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        if (mMap != null) {
                            mMap.clear();
                        }

                        recyclerNearby.setAdapter(new NearbyAdapter(top10));

                        if (mMap != null) {
                            for (Attraction a : top10) {
                                mMap.addMarker(
                                        new MarkerOptions()
                                                .position(new LatLng(a.getLat(), a.getLng()))
                                                .title(a.getName())
                                );
                            }
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Error loading nearby places: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // --- Lifecycle ---
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    requireActivity().getMainLooper()
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
