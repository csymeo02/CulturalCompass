package com.example.culturalcompass.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
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
import com.google.android.libraries.places.api.model.PhotoMetadata;
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
    private NearbyAdapter nearbyAdapter;


    // To avoid too many API calls:
    private static final float MIN_DISTANCE_CHANGE = 100f; // meters
    private double lastFetchLat = 0;
    private double lastFetchLon = 0;


    // master list from Places
    private final List<Attraction> allAttractions = new ArrayList<>();

    private Spinner spinnerFilter;
    private Spinner spinnerSort;



    // filter state
    private boolean filterTouristAttraction = true;
    private boolean filterMuseum = true;
    private boolean filterArtGallery = true;

    private enum SortMode { DISTANCE, RATING,BEST }
    private SortMode currentSortMode = SortMode.DISTANCE;

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
        nearbyAdapter = new NearbyAdapter(new ArrayList<>(), placesClient);
        recyclerNearby.setAdapter(nearbyAdapter);

        // --- Spinners ---
        spinnerFilter = view.findViewById(R.id.spinnerFilter);
        spinnerSort   = view.findViewById(R.id.spinnerSort);

        // initial state
        filterTouristAttraction = true;
        filterMuseum = true;
        filterArtGallery = true;
        currentSortMode = SortMode.DISTANCE;

        setupFilterSpinner();
        setupSortSpinner();

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

    private void setupFilterSpinner() {

        // Options shown to the user
        String[] filterOptions = new String[] {
                "All types",
                "Tourist attractions",
                "Museums",
                "Art galleries"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                filterOptions
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Update filter booleans depending on selection
                switch (position) {
                    case 0: // All types
                        filterTouristAttraction = true;
                        filterMuseum = true;
                        filterArtGallery = true;
                        break;
                    case 1: // Tourist attractions only
                        filterTouristAttraction = true;
                        filterMuseum = false;
                        filterArtGallery = false;
                        break;
                    case 2: // Museums only
                        filterTouristAttraction = false;
                        filterMuseum = true;
                        filterArtGallery = false;
                        break;
                    case 3: // Art galleries only
                        filterTouristAttraction = false;
                        filterMuseum = false;
                        filterArtGallery = true;
                        break;
                }

                applyFiltersAndSorting();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
    }

    private void setupSortSpinner() {
        String[] sortOptions = new String[] {
                "Distance",
                "Rating",
                "Best overall"

        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                sortOptions
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentSortMode = SortMode.DISTANCE;
                        break;
                    case 1:
                        currentSortMode = SortMode.RATING;
                        break;
                    case 2:
                        currentSortMode = SortMode.BEST;
                        break;
                }
                applyFiltersAndSorting();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

// default selection: Distance
        spinnerSort.setSelection(0);

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
    private double computeCombinedScore(Attraction a) {
        // 1) Rating component
        double weightedRating = computeWeightedRating(a); // 0..5-ish
        if (weightedRating < 0) {
            // no rating -> treat as very low rating
            weightedRating = 0.0;
        }

        // 2) Distance penalty (in km)
        double distanceKm = a.getDistanceMeters() / 1000.0;

        // Each extra km subtracts a bit from the score.
        // Tune 0.15 as you like: higher = distance more important.
        double penalty = 0.15 * distanceKm;

        return weightedRating - penalty;
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
                Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
                Place.Field.RATING,
                Place.Field.USER_RATING_COUNT,
                Place.Field.PHOTO_METADATAS
        );

        // 2) Define circular search area (10km radius)
        LatLng center = new LatLng(lat, lon);
        CircularBounds circle = CircularBounds.newInstance(center, /* radius = */ 10000);

        // 3) Types to include (cultural / historical)
// 3) Types to include (cultural / historical)
        final List<String> includedTypes = Arrays.asList(
                "tourist_attraction",
                "museum",
                "art_gallery"
        );




        // 5) Build Nearby Search request
        SearchNearbyRequest request =
                SearchNearbyRequest.builder(circle, placeFields)
                        .setIncludedTypes(includedTypes)
                        .setMaxResultCount(20)
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

                        String primaryTypeKey = place.getPrimaryType(); // "museum", "art_gallery", "tourist_attraction"

                        // Label, e.g. "Museum"
                        String typeLabel = place.getPrimaryTypeDisplayName();
                        if (typeLabel == null || typeLabel.isEmpty()) {
                            typeLabel = primaryTypeKey;
                        }

                        PhotoMetadata photoMetadata = null;
                        if (place.getPhotoMetadatas() != null && !place.getPhotoMetadatas().isEmpty()) {
                            photoMetadata = place.getPhotoMetadatas().get(0);
                        }


                        Double rating = place.getRating();              // can be null
                        Integer ratingCount = place.getUserRatingCount(); // can be null

                        attractions.add(
                                new Attraction(
                                        name,
                                        placeLoc.latitude,
                                        placeLoc.longitude,
                                        distanceMeters,
                                        typeLabel,
                                        primaryTypeKey,
                                        rating,
                                        ratingCount,
                                        photoMetadata
                                )
                        );

                    }

                    // Sort by distance just in case
                    Collections.sort(
                            attractions,
                            Comparator.comparingDouble(Attraction::getDistanceMeters)
                    );

                    // Keep only top 20 by distance (optional pre-limit)
                    List<Attraction> top20 =
                            attractions.subList(0, Math.min(20, attractions.size()));

                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        allAttractions.clear();
                        allAttractions.addAll(top20);
                        applyFiltersAndSorting();
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

    private void applyFiltersAndSorting() {
        if (getActivity() == null || mMap == null) return;

        // 1) Filter by type
        List<Attraction> filtered = new ArrayList<>();

        for (Attraction a : allAttractions) {
            String key = a.getPrimaryTypeKey();
            if (key == null) continue;

            boolean include = false;

            if (filterTouristAttraction && "tourist_attraction".equals(key)) {
                include = true;
            }
            if (filterMuseum && "museum".equals(key)) {
                include = true;
            }
            if (filterArtGallery && "art_gallery".equals(key)) {
                include = true;
            }

            if (include) {
                filtered.add(a);
            }
        }

        // If all filters off, show nothing (or you could decide to show all)
        // 2) Sort
        if (currentSortMode == SortMode.DISTANCE) {

            Collections.sort(filtered,
                    Comparator.comparingDouble(Attraction::getDistanceMeters));

        } else if (currentSortMode == SortMode.RATING) {

            // Sort by weighted rating (rating + number of reviews), desc
            Collections.sort(filtered, (a, b) -> {
                double sa = computeWeightedRating(a);
                double sb = computeWeightedRating(b);
                return Double.compare(sb, sa); // descending
            });

        } else if (currentSortMode == SortMode.BEST) {

            // Combined score: rating & distance
            Collections.sort(filtered, (a, b) -> {
                double sa = computeCombinedScore(a);
                double sb = computeCombinedScore(b);
                return Double.compare(sb, sa); // descending
            });
        }



        // 3) Update UI: adapter + markers
        getActivity().runOnUiThread(() -> {
            // update adapter
            nearbyAdapter.updateItems(filtered);

            // update markers
            mMap.clear();
            for (Attraction a : filtered) {
                mMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(a.getLat(), a.getLng()))
                                .title(a.getName())
                );
            }
        });
    }
    private double computeWeightedRating(Attraction a) {
        Double r = a.getRating();
        Integer v = a.getRatingCount();

        if (r == null || v == null || v == 0) {
            // No rating or no votes -> treat as very low score
            return -1.0;
        }

        double R = r;          // rating
        double vD = v;         // votes as double
        double m = 20.0;       // minimum reviews threshold (tune if you want)
        double C = 4.0;        // assumed average rating

        return (vD / (vD + m)) * R + (m / (vD + m)) * C;
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
