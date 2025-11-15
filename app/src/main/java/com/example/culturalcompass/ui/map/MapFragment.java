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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationResult;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                5000 // 5 seconds
        ).setMinUpdateDistanceMeters(10) // trigger callback when user moves 10 meters
                .build();

// Create the continuous location listener
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location location = locationResult.getLastLocation();
                if (location == null) return;

                double newLat = location.getLatitude();
                double newLon = location.getLongitude();

                userLat = newLat;
                userLon = newLon;

                // Calculate how far the user moved since last API fetch
                float[] dist = new float[1];
                android.location.Location.distanceBetween(
                        lastFetchLat, lastFetchLon,
                        newLat, newLon,
                        dist
                );

                if (dist[0] > MIN_DISTANCE_CHANGE) { // moved more than 100m?

                    lastFetchLat = newLat;
                    lastFetchLon = newLon;

                    loadNearbyAttractions(newLat, newLon);

                    // Move camera smoothly
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(newLat, newLon), 16f
                    ));
                }
            }
        };

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
            getUserLocation();                // Get coordinates + move camera

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

                        lastFetchLat = userLat;
                        lastFetchLon = userLon;

                        LatLng userPosition = new LatLng(userLat, userLon);

                        loadNearbyAttractions(userLat, userLon);

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 16f));

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



    private void loadNearbyAttractions(double lat, double lon) {
        String apiKey = "AIzaSyCTpvm5CkW8tpgwsrP9yGHd932X34Ly6bQ";


        String url =
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                        "?location=" + lat + "," + lon +
                        "&radius=5000" +
                        "&keyword=tourist attraction OR museum OR art gallery OR monument OR archaeological site OR historical landmark OR ancient ruins OR cultural site" +
                        "&key=" + apiKey;

        new Thread(() -> {
            try {
                URL apiUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                conn.connect();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                parseAttractionsResponse(result.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    private boolean contains(JSONArray arr, String value) {
        if (arr == null) return false;
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optString(i).equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private void parseAttractionsResponse(String json) {

        List<Attraction> attractions = new ArrayList<>();

        try {
            JSONObject obj = new JSONObject(json);
            JSONArray results = obj.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {

                JSONObject item = results.getJSONObject(i);
                if (!item.has("geometry")) continue;

                String name = item.optString("name", "Unknown Place");

                JSONObject locationObj =
                        item.getJSONObject("geometry").getJSONObject("location");

                double lat = locationObj.getDouble("lat");
                double lng = locationObj.getDouble("lng");

                // ---- Calculate distance from the user ----
                float[] distanceResult = new float[1];
                android.location.Location.distanceBetween(
                        userLat, userLon,
                        lat, lng,
                        distanceResult
                );

                double distanceMeters = distanceResult[0];

                // ---- Save attraction ----
                attractions.add(new Attraction(name, lat, lng, distanceMeters));
            }

            // ---- Sort by nearest ----
            Collections.sort(attractions, Comparator.comparingDouble(Attraction::getDistanceMeters));

            // ---- Keep only top 10 ----
            List<Attraction> top10 = attractions.subList(0, Math.min(10, attractions.size()));

            // ---- Update UI ----
            requireActivity().runOnUiThread(() -> {
                mMap.clear();
                recyclerNearby.setAdapter(new NearbyAdapter(top10));

                for (Attraction a : top10) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(a.getLat(), a.getLng()))
                            .title(a.getName())
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // Lifecycle
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
    @Override public void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
