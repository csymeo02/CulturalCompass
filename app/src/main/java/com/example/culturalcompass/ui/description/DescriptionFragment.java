package com.example.culturalcompass.ui.description;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.model.Session;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Locale;

public class DescriptionFragment extends Fragment {

    private static final String ARG_PLACE_ID = "arg_place_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_TYPE_LABEL = "arg_type_label";
    private static final String ARG_PRIMARY_TYPE_KEY = "arg_primary_type_key";
    private static final String ARG_DISTANCE_METERS = "arg_distance_meters";
    private static final String ARG_RATING = "arg_rating";
    private static final String ARG_RATING_COUNT = "arg_rating_count";
    private static final String ARG_DESCRIPTION = "arg_description";

    // ---- Data fields ----
    private String placeId;
    private String name;
    private String typeLabel;
    private String primaryTypeKey;
    private double distanceMeters;
    private double rating;
    private int ratingCount;
    private String aiDescription;

    // ---- UI ----
    private ImageView btnBack;
    private ImageView imgPhoto;
    private TextView txtName;
    private TextView txtType;
    private TextView txtDistance;
    private LinearLayout layoutRating;
    private RatingBar ratingBar;
    private TextView txtRatingValue;
    private TextView txtRatingCount;
    private ImageView imgFavorite;
    private TextView txtDescription;

    private PlacesClient placesClient;
    private FirebaseFirestore db;

    public DescriptionFragment() {}

    public static DescriptionFragment newInstance(
            String placeId,
            String name,
            String typeLabel,
            String primaryTypeKey,
            double distanceMeters,
            Double rating,
            Integer ratingCount,
            String aiDescription
    ) {
        DescriptionFragment f = new DescriptionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLACE_ID, placeId);
        args.putString(ARG_NAME, name);
        args.putString(ARG_TYPE_LABEL, typeLabel);
        args.putString(ARG_PRIMARY_TYPE_KEY, primaryTypeKey);
        args.putDouble(ARG_DISTANCE_METERS, distanceMeters);
        args.putDouble(ARG_RATING, rating != null ? rating : -1.0);
        args.putInt(ARG_RATING_COUNT, ratingCount != null ? ratingCount : 0);
        args.putString(ARG_DESCRIPTION, aiDescription != null ? aiDescription : "");
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            placeId = args.getString(ARG_PLACE_ID);
            name = args.getString(ARG_NAME);
            typeLabel = args.getString(ARG_TYPE_LABEL);
            primaryTypeKey = args.getString(ARG_PRIMARY_TYPE_KEY);
            distanceMeters = args.getDouble(ARG_DISTANCE_METERS, 0.0);
            rating = args.getDouble(ARG_RATING, -1.0);
            ratingCount = args.getInt(ARG_RATING_COUNT, 0);
            aiDescription = args.getString(ARG_DESCRIPTION, "");
        }

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    requireContext().getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }

        placesClient = Places.createClient(requireContext());
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_description, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // --- Bind UI ----
        btnBack = v.findViewById(R.id.btnBack);
        imgPhoto = v.findViewById(R.id.imgPhotoLarge);
        txtName = v.findViewById(R.id.txtNameLarge);
        txtType = v.findViewById(R.id.txtTypeChip);
        txtDistance = v.findViewById(R.id.txtDistanceDetail);
        layoutRating = v.findViewById(R.id.layoutRatingDetail);
        ratingBar = v.findViewById(R.id.ratingBarDetail);
        txtRatingValue = v.findViewById(R.id.txtRatingValueDetail);
        txtRatingCount = v.findViewById(R.id.txtRatingCountDetail);
        imgFavorite = v.findViewById(R.id.imgFavoriteDetail);
        txtDescription = v.findViewById(R.id.txtDescriptionAI);

        // --- Back button (FIXED) ---
        btnBack.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // --- Basic text ---
        txtName.setText(name != null ? name : "Unknown place");
        txtType.setText(typeLabel != null ? typeLabel : primaryTypeKey);

        styleTypeChip(primaryTypeKey);
        txtDistance.setText(formatDistance(distanceMeters));
        bindRating();

        txtDescription.setText(
                aiDescription == null || aiDescription.trim().isEmpty()
                        ? "No description available."
                        : aiDescription.trim()
        );

        loadPhotoForPlace();
        setupFavorites();
    }

    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m away", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km away", meters / 1000.0);
        }
    }

    private void bindRating() {
        if (rating > 0 && ratingCount > 0) {
            txtRatingValue.setText(String.format(Locale.getDefault(), "%.1f", rating));
            ratingBar.setRating((float) rating);
            txtRatingCount.setText("(" + ratingCount + ")");
            layoutRating.setVisibility(View.VISIBLE);
        } else {
            txtRatingValue.setText("No ratings yet");
            ratingBar.setVisibility(View.GONE);
            txtRatingCount.setVisibility(View.GONE);
            layoutRating.setVisibility(View.VISIBLE);
        }
    }

    private void styleTypeChip(String key) {
        if (key == null) return;

        switch (key) {
            case "museum":
                txtType.setBackgroundResource(R.drawable.chip_museum);
                txtType.setTextColor(0xFF5A3E2B);
                break;
            case "art_gallery":
                txtType.setBackgroundResource(R.drawable.chip_art_gallery);
                txtType.setTextColor(0xFF0A3A5C);
                break;
            default:
                txtType.setBackgroundResource(R.drawable.chip_attraction);
                txtType.setTextColor(0xFF1F6B1F);
        }
    }

    private void loadPhotoForPlace() {
        imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

        if (placeId == null) return;

        FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                placeId,
                Arrays.asList(Place.Field.PHOTO_METADATAS)
        );

        placesClient.fetchPlace(req)
                .addOnSuccessListener(response -> {
                    if (response.getPlace().getPhotoMetadatas() == null ||
                            response.getPlace().getPhotoMetadatas().isEmpty()) return;

                    PhotoMetadata meta = response.getPlace().getPhotoMetadatas().get(0);

                    FetchPhotoRequest photoReq = FetchPhotoRequest.builder(meta)
                            .setMaxWidth(800)
                            .setMaxHeight(800)
                            .build();

                    placesClient.fetchPhoto(photoReq)
                            .addOnSuccessListener(photoResponse ->
                                    imgPhoto.setImageBitmap(photoResponse.getBitmap())
                            );
                });
    }

    private void setupFavorites() {
        String email = Session.currentUser != null ? Session.currentUser.getEmail() : null;

        if (email == null) {
            updateHeartIcon(false);
            return;
        }

        db.collection("users")
                .document(email)
                .collection("favorites")
                .document(placeId)
                .get()
                .addOnSuccessListener(doc -> updateHeartIcon(doc.exists()));

        imgFavorite.setOnClickListener(v -> {
            boolean fav = imgFavorite.getTag() instanceof Boolean && (Boolean) imgFavorite.getTag();
            boolean newFav = !fav;

            updateHeartIcon(newFav);

            if (newFav) {
                // ADD TO FIRESTORE
                FirestoreAttraction fa = new FirestoreAttraction(
                        placeId,
                        name,
                        0,
                        0,
                        distanceMeters,
                        typeLabel,
                        primaryTypeKey,
                        rating > 0 ? rating : null,
                        ratingCount > 0 ? ratingCount : null
                );

                db.collection("users")
                        .document(email)
                        .collection("favorites")
                        .document(placeId)
                        .set(fa)
                        .addOnSuccessListener(x ->
                                Toast.makeText(getContext(),
                                        "Added: " + name,
                                        Toast.LENGTH_SHORT).show()
                        );

            } else {
                // REMOVE FROM FIRESTORE
                db.collection("users")
                        .document(email)
                        .collection("favorites")
                        .document(placeId)
                        .delete()
                        .addOnSuccessListener(x ->
                                Toast.makeText(getContext(),
                                        "Removed: " + name,
                                        Toast.LENGTH_SHORT).show()
                        );
            }
        });

    }

    private void updateHeartIcon(boolean fav) {
        imgFavorite.setTag(fav);
        imgFavorite.setImageResource(
                fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );
    }
}
