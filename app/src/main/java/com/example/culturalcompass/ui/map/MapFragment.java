package com.example.culturalcompass.ui.map;

import static com.example.culturalcompass.R.id.placesContainer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Attraction;
import com.example.culturalcompass.model.AttractionClusterItem;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.model.Session;
import com.example.culturalcompass.ui.description.DescriptionFragment;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Map screen that:
 * - Tracks live GPS or a searched location
 * - Loads nearby cultural places via Places Nearby Search
 * - Filters by type (tourist attraction / museum / art gallery)
 * - Sorts by distance, rating, or best overall
 * - Shows clustered markers on Google Maps
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final float MIN_DISTANCE_CHANGE = 100f; // meters

    private MapView mapView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;

    private double userLat;
    private double userLon;

    private double lastFetchLat = 0;
    private double lastFetchLon = 0;

    private RecyclerView recyclerNearby;
    private NearbyAdapter nearbyAdapter;

    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private Spinner spinnerFilter;
    private Spinner spinnerSort;
    private EditText searchBar;

    // Filter state
    private boolean filterTouristAttraction = true;
    private boolean filterMuseum = true;
    private boolean filterArtGallery = true;

    private enum SortMode {
        DISTANCE,
        RATING,
        BEST
    }

    private SortMode currentSortMode = SortMode.DISTANCE;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> autocompleteLauncher;

    // true = follow live GPS; false = user is exploring a searched location
    private boolean followUserLocation = true;

    // Master list from latest Places response
    private final List<Attraction> allAttractions = new ArrayList<>();

    private ClusterManager<AttractionClusterItem> clusterManager;

    private FirebaseFirestore db;
    private String userEmail;



    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userEmail = Session.currentUser != null
                ? Session.currentUser.getEmail()
                : "guest@example.com"; // fallback

        // DEBUG LOG (ADD THIS)
        Log.d("USER EMAIL", "[" + userEmail + "]");

        db = FirebaseFirestore.getInstance();

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    requireContext().getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }
        placesClient = Places.createClient(requireContext());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                5000
        )
                .setMinUpdateDistanceMeters(10)
                .build();

        // Receives continuous GPS updates and triggers nearby fetch when user moves enough
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!followUserLocation) {
                    return;
                }

                android.location.Location location = locationResult.getLastLocation();
                if (location == null) return;

                double newLat = location.getLatitude();
                double newLon = location.getLongitude();

                userLat = newLat;
                userLon = newLon;

                float[] dist = new float[1];
                android.location.Location.distanceBetween(
                        lastFetchLat, lastFetchLon,
                        newLat, newLon,
                        dist
                );

                if (dist[0] > MIN_DISTANCE_CHANGE) {
                    lastFetchLat = newLat;
                    lastFetchLon = newLon;

                    requestNearbyForCurrentFilter(newLat, newLon);

                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(newLat, newLon),
                                16f
                        ));
                    }
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
                                Toast.makeText(
                                        requireContext(),
                                        "Precise location permission is required",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );

        autocompleteLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            int code = result.getResultCode();
                            Intent data = result.getData();

                            if (code == Activity.RESULT_OK && data != null) {
                                Place place = Autocomplete.getPlaceFromIntent(data);
                                onPlaceSelected(place);
                            } else if (code == AutocompleteActivity.RESULT_ERROR && data != null) {
                                Status status = Autocomplete.getStatusFromIntent(data);
                                Toast.makeText(
                                        requireContext(),
                                        "Search error: " + status.getStatusMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        searchBar = view.findViewById(R.id.searchBar);
        searchBar.setFocusable(false);
        searchBar.setOnClickListener(v -> openSearchAutocomplete());

        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        recyclerNearby = view.findViewById(R.id.recyclerNearby);
        recyclerNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyAdapter = new NearbyAdapter(new ArrayList<>(), placesClient);
        nearbyAdapter.setOnAttractionClickListener(attraction -> {
            requestAIDescriptionAndOpenFragment(attraction);
        });
        recyclerNearby.setAdapter(nearbyAdapter);

        spinnerFilter = view.findViewById(R.id.spinnerFilter);
        spinnerSort = view.findViewById(R.id.spinnerSort);

        filterTouristAttraction = true;
        filterMuseum = true;
        filterArtGallery = true;
        currentSortMode = SortMode.DISTANCE;

        setupFilterSpinner();
        setupSortSpinner();

        return view;
    }

    private void requestAIDescriptionAndOpenFragment(Attraction a) {

        String prompt =
                "Give me a short friendly explanation (3–5 sentences, no markdown) "
                        + "about this cultural place: " + a.getName()
                        + ". The type is " + a.getType() + ".";

        // Build Gemini request JSON
        JSONObject body = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");

            JSONArray parts = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("text", prompt);
            parts.put(text);

            userObj.put("parts", parts);
            contents.put(userObj);
            body.put("contents", contents);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Correct okhttp3 RequestBody
        okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(
                body.toString(),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + "gemini-2.0-flash:generateContent?key="
                + getString(R.string.gemini_api_key);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(reqBody)
                .build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                getContext(),
                                "Failed to load description",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response)
                    throws IOException {

                String json = response.body().string();
                String description = parseGeminiDescription(json);

                requireActivity().runOnUiThread(() ->
                        openDescriptionFragment(a, description)
                );
            }
        });
    }


    private String parseGeminiDescription(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray candidates = obj.getJSONArray("candidates");
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            return parts.getJSONObject(0).getString("text").trim();
        } catch (Exception e) {
            return "No description available.";
        }
    }

    private void openDescriptionFragment(Attraction a, String aiText) {

        DescriptionFragment fragment = DescriptionFragment.newInstance(
                a.getPlaceId(),
                a.getName(),
                a.getType(),
                a.getPrimaryTypeKey(),
                a.getDistanceMeters(),
                a.getRating(),
                a.getRatingCount(),
                aiText
        );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .addToBackStack(null)
                .commit();
    }


    private CollectionReference userAttractionsRef() {
        return db.collection("users")
                .document(userEmail)
                .collection("attractions");
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void openSearchAutocomplete() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION
        );

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                fields
        ).build(requireContext());

        autocompleteLauncher.launch(intent);
    }

    private void onPlaceSelected(Place place) {
        LatLng latLng = place.getLocation();
        if (latLng == null) return;

        followUserLocation = false;

        if (searchBar != null) {
            String name = place.getDisplayName();
            if (name != null) {
                searchBar.setText(name);
            }
        }

        userLat = latLng.latitude;
        userLon = latLng.longitude;
        lastFetchLat = userLat;
        lastFetchLon = userLon;

        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }

        requestNearbyForCurrentFilter(userLat, userLon);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(() -> {
            followUserLocation = true;

            if (searchBar != null) {
                searchBar.setText("");
                searchBar.setHint("Search for a place");
            }

            getUserLocation();
            return true;
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);

        initClusterManager();
        enableMyLocation();

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    requireActivity().getMainLooper()
            );
        }
    }

    private void initClusterManager() {
        if (mMap == null || getContext() == null) return;

        clusterManager = new ClusterManager<>(requireContext(), mMap);
        clusterManager.setRenderer(
                new AttractionClusterRenderer(requireContext(), mMap, clusterManager)
        );

        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
        mMap.setOnInfoWindowClickListener(clusterManager);

        clusterManager.setOnClusterClickListener(cluster -> {
            showClusterNamesDialog(cluster);
            return true;
        });
    }

    private void setupFilterSpinner() {
        String[] filterOptions = new String[]{
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
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id
            ) {
                switch (position) {
                    case 0:
                        filterTouristAttraction = true;
                        filterMuseum = true;
                        filterArtGallery = true;
                        break;
                    case 1:
                        filterTouristAttraction = true;
                        filterMuseum = false;
                        filterArtGallery = false;
                        break;
                    case 2:
                        filterTouristAttraction = false;
                        filterMuseum = true;
                        filterArtGallery = false;
                        break;
                    case 3:
                        filterTouristAttraction = false;
                        filterMuseum = false;
                        filterArtGallery = true;
                        break;
                }

                if (lastFetchLat != 0 || lastFetchLon != 0) {
                    requestNearbyForCurrentFilter(lastFetchLat, lastFetchLon);
                } else {
                    applyFiltersAndSorting();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void setupSortSpinner() {
        String[] sortOptions = new String[]{
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
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id
            ) {
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
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        spinnerSort.setSelection(0);
    }

    private void enableMyLocation() {
        if (mMap == null) return;

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            getUserLocation();
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

                        requestNearbyForCurrentFilter(userLat, userLon);

                        if (mMap != null) {
                            mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(userPosition, 16f)
                            );
                        }
                    } else {
                        Toast.makeText(
                                requireContext(),
                                "Could not get current location",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                requireContext(),
                                "Error getting location",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    /**
     * Performs a Nearby Search using the Places SDK and converts results to Attraction objects.
     */
    private void loadNearbyAttractions(
            double lat,
            double lon,
            List<String> includedTypes
    ) {

        clearAttractionsCollection(() -> {

            List<Place.Field> placeFields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.LOCATION,
                    Place.Field.PRIMARY_TYPE,
                    Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
                    Place.Field.RATING,
                    Place.Field.USER_RATING_COUNT,
                    Place.Field.PHOTO_METADATAS
            );

            LatLng center = new LatLng(lat, lon);
            CircularBounds circle = CircularBounds.newInstance(center, 5000);

            SearchNearbyRequest request =
                    SearchNearbyRequest.builder(circle, placeFields)
                            .setIncludedTypes(includedTypes)
                            .setMaxResultCount(20)
                            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                            .build();

            placesClient.searchNearby(request)
                    .addOnSuccessListener(response -> {

                        List<Attraction> attractions = new ArrayList<>();
                        AtomicInteger pending = new AtomicInteger(0);

                        List<Place> places = response.getPlaces();

                        for (Place place : places) {

                            LatLng placeLoc = place.getLocation();
                            if (placeLoc == null) continue;

                            float[] dist = new float[1];
                            android.location.Location.distanceBetween(
                                    userLat, userLon,
                                    placeLoc.latitude, placeLoc.longitude,
                                    dist
                            );
                            double distanceMeters = dist[0];

                            String name = place.getDisplayName();


                            if (name == null || name.isEmpty()) name = "Unknown place";

                            String primaryTypeKey = place.getPrimaryType();
                            String typeLabel = place.getPrimaryTypeDisplayName();
                            if (typeLabel == null || typeLabel.isEmpty())
                                typeLabel = primaryTypeKey;

                            PhotoMetadata photoMetadata;
                            if (place.getPhotoMetadatas() != null &&
                                    !place.getPhotoMetadatas().isEmpty()) {
                                photoMetadata = place.getPhotoMetadatas().get(0);
                            } else {
                                photoMetadata = null;
                            }

                            Double rating = place.getRating();
                            Integer ratingCount = place.getUserRatingCount();

                            String placeId = place.getId();

                            Log.d("WRITE_DEBUG", "Writing: " + placeId + " / name=" + name);


                            FirestoreAttraction firestoreAttraction = new FirestoreAttraction(
                                    placeId,
                                    name,
                                    placeLoc.latitude,
                                    placeLoc.longitude,
                                    distanceMeters,
                                    typeLabel,
                                    primaryTypeKey,
                                    rating,
                                    ratingCount
                            );

                            pending.incrementAndGet();

                            userAttractionsRef()
                                    .document(placeId)
                                    .set(firestoreAttraction)
                                    .addOnSuccessListener(v -> {

                                        Source source = Source.DEFAULT;

                                        userAttractionsRef()
                                                .document(placeId)
                                                .get(source)
                                                .addOnSuccessListener(snapshot -> {

                                                    if (snapshot.exists()) {

                                                        FirestoreAttraction fs =
                                                                snapshot.toObject(FirestoreAttraction.class);

                                                        Attraction attraction = new Attraction(
                                                                fs.getName(),
                                                                fs.getLat(),
                                                                fs.getLng(),
                                                                fs.getDistanceMeters(),
                                                                fs.getTypeLabel(),
                                                                fs.getPrimaryTypeKey(),
                                                                fs.getRating(),
                                                                fs.getRatingCount(),
                                                                photoMetadata,
                                                                fs.getId()
                                                        );



                                                        synchronized (attractions) {
                                                            attractions.add(attraction);
                                                        }
                                                    }

                                                    if (pending.decrementAndGet() == 0) {
                                                        finalizeFirestoreAttractions(attractions);
                                                    }
                                                });
                                    });

                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error loading places: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        });
    }


    private void finalizeFirestoreAttractions(List<Attraction> list) {

        // SAFETY: Fragment is not attached → do nothing
        if (!isAdded() || getActivity() == null) {
            return;
        }

        Collections.sort(list, Comparator.comparingDouble(Attraction::getDistanceMeters));

        List<Attraction> top20 = list.subList(0, Math.min(20, list.size()));

        // On UI thread safely
        requireActivity().runOnUiThread(() -> {

            // DOUBLE SAFETY — callbacks might fire after UI thread posts again
            if (!isAdded() || getActivity() == null) {
                return;
            }

            allAttractions.clear();
            allAttractions.addAll(top20);
            applyFiltersAndSorting();
        });
    }


    private void clearAttractionsCollection(Runnable onComplete) {

        Source source = Source.DEFAULT;

        userAttractionsRef()
                .get(source)
                .addOnSuccessListener(snapshot -> {

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> onComplete.run())
                            .addOnFailureListener(e -> {
                                e.printStackTrace();
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    onComplete.run();
                });
    }

    private void loadOfflineAttractions() {
        Source source = Source.CACHE;

        userAttractionsRef()
                .get(source)
                .addOnSuccessListener(snapshot -> {

                    List<Attraction> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        FirestoreAttraction fs = doc.toObject(FirestoreAttraction.class);

                        // PhotoMetadata is null in offline mode
                        Attraction a = new Attraction(
                                fs.getName(),
                                fs.getLat(),
                                fs.getLng(),
                                fs.getDistanceMeters(),
                                fs.getTypeLabel(),
                                fs.getPrimaryTypeKey(),
                                fs.getRating(),
                                fs.getRatingCount(),
                                null,
                                fs.getId()
                        );

                        list.add(a);
                    }

                    finalizeFirestoreAttractions(list);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "No offline data available", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Chooses which place types to request, based on the current filter flags.
     */
    private void requestNearbyForCurrentFilter(double lat, double lon) {
        List<String> types;

        if (filterTouristAttraction && filterMuseum && filterArtGallery) {
            types = Arrays.asList("tourist_attraction", "museum", "art_gallery");
        } else if (filterTouristAttraction && !filterMuseum && !filterArtGallery) {
            types = Arrays.asList("tourist_attraction");
        } else if (!filterTouristAttraction && filterMuseum && !filterArtGallery) {
            types = Arrays.asList("museum");
        } else if (!filterTouristAttraction && !filterMuseum && filterArtGallery) {
            types = Arrays.asList("art_gallery");
        } else {
            types = Arrays.asList("tourist_attraction", "museum", "art_gallery");
        }

        if (isOnline()) {
            loadNearbyAttractions(lat, lon, types);
        } else {
            loadOfflineAttractions();
        }
    }



    /**
     * When a cluster is tapped, show a dialog listing all places inside that cluster.
     */
    private void showClusterNamesDialog(Cluster<AttractionClusterItem> cluster) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_cluster_places, null);
        LinearLayout container = dialogView.findViewById(placesContainer);

        for (AttractionClusterItem item : cluster.getItems()) {
            Attraction a = item.getAttraction();

            View row = inflater.inflate(R.layout.row_cluster_place, container, false);

            TextView txtPlaceName = row.findViewById(R.id.txtPlaceName);
            TextView txtPlaceType = row.findViewById(R.id.txtPlaceType);
            ImageView imgPlaceType = row.findViewById(R.id.imgPlaceType);

            txtPlaceName.setText(a.getName());
            txtPlaceType.setText(a.getType());

            String typeKey = a.getPrimaryTypeKey();
            if (typeKey != null) {
                switch (typeKey) {
                    case "museum":
                        txtPlaceType.setBackgroundResource(R.drawable.chip_museum);
                        imgPlaceType.setImageResource(R.drawable.ic_museum);
                        break;
                    case "art_gallery":
                        txtPlaceType.setBackgroundResource(R.drawable.chip_art_gallery);
                        imgPlaceType.setImageResource(R.drawable.ic_art_gallery);
                        break;
                    case "tourist_attraction":
                    default:
                        txtPlaceType.setBackgroundResource(R.drawable.chip_attraction);
                        imgPlaceType.setImageResource(R.drawable.ic_tourist_attraction);
                        break;
                }
            }

            container.addView(row);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(cluster.getSize() + " places here")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Applies type filters and selected sorting mode, then updates list and map clusters.
     */
    private void applyFiltersAndSorting() {
        if (getActivity() == null || mMap == null) return;

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

        if (currentSortMode == SortMode.DISTANCE) {
            Collections.sort(
                    filtered,
                    Comparator.comparingDouble(Attraction::getDistanceMeters)
            );
        } else if (currentSortMode == SortMode.RATING) {
            Collections.sort(filtered, (a, b) -> {
                double sa = computeWeightedRating(a);
                double sb = computeWeightedRating(b);
                return Double.compare(sb, sa);
            });
        } else if (currentSortMode == SortMode.BEST) {
            Collections.sort(filtered, (a, b) -> {
                double sa = computeCombinedScore(a);
                double sb = computeCombinedScore(b);
                return Double.compare(sb, sa);
            });
        }

        getActivity().runOnUiThread(() -> {
            nearbyAdapter.updateItems(filtered);

            if (clusterManager == null) return;

            clusterManager.clearItems();
            for (Attraction a : filtered) {
                clusterManager.addItem(new AttractionClusterItem(a));
            }
            clusterManager.cluster();
        });
    }

    /**
     * Wilson score rating that favors places with more reviews.
     */
    private double computeWeightedRating(Attraction a) {
        Double r = a.getRating();
        Integer v = a.getRatingCount();

        if (r == null || v == null || v == 0) {
            return -1.0;
        }

        double rating = r;
        double n = v;
        double z = 1.96;

        double p = rating / 5.0;
        double z2 = z * z;

        double numerator = p + z2 / (2.0 * n)
                - z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n);
        double denominator = 1.0 + z2 / n;

        double lowerBound = numerator / denominator;
        return lowerBound * 5.0;
    }

    /**
     * Combined score for "Best overall": high rating, penalized by distance.
     */
    private double computeCombinedScore(Attraction a) {
        double weightedRating = computeWeightedRating(a);
        if (weightedRating < 0) {
            weightedRating = 0.0;
        }

        double distanceKm = a.getDistanceMeters() / 1000.0;
        double penalty = 0.15 * distanceKm;

        return weightedRating - penalty;
    }

    // Lifecycle

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

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
