package com.example.culturalcompass.ui.favorites;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.model.Session;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.Holder> {

    public interface OnFavoriteClickListener {
        void onFavoriteClicked(FirestoreAttraction attraction);
    }

    public interface OnEmptyStateListener {
        void onEmpty();
    }

    private List<FirestoreAttraction> items;
    private PlacesClient placesClient;

    private OnFavoriteClickListener clickListener;
    private OnEmptyStateListener emptyListener;

    public FavoritesAdapter(List<FirestoreAttraction> items, PlacesClient placesClient) {
        this.items = items;
        this.placesClient = placesClient;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.clickListener = listener;
    }

    public void setEmptyListener(OnEmptyStateListener listener) {
        this.emptyListener = listener;
    }

    public void update(List<FirestoreAttraction> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        FirestoreAttraction a = items.get(pos);

        h.txtName.setText(a.getName());
        h.txtType.setText(a.getTypeLabel());

        double meters = a.getDistanceMeters();
        if (meters < 1000) {
            h.txtDistance.setText(String.format(Locale.getDefault(), "%.0f m", meters));
        } else {
            h.txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", meters / 1000));
        }

        // Default placeholder
        h.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

        // Load photo
        if (placesClient != null && a.getId() != null) {
            List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS);

            FetchPlaceRequest req =
                    FetchPlaceRequest.builder(a.getId(), fields).build();

            placesClient.fetchPlace(req)
                    .addOnSuccessListener(response -> {
                        Place place = response.getPlace();
                        if (place.getPhotoMetadatas() != null &&
                                !place.getPhotoMetadatas().isEmpty()) {

                            PhotoMetadata meta = place.getPhotoMetadatas().get(0);

                            FetchPhotoRequest photoReq = FetchPhotoRequest.builder(meta)
                                    .setMaxWidth(400)
                                    .setMaxHeight(400)
                                    .build();

                            placesClient.fetchPhoto(photoReq)
                                    .addOnSuccessListener(photoResponse ->
                                            h.imgPhoto.setImageBitmap(photoResponse.getBitmap())
                                    );
                        }
                    });
        }

// Click -> DescriptionFragment
        h.itemView.setOnClickListener(v -> {

            // EXACT SAME LOGIC AS MAP FRAGMENT
            if (!isOnline(v)) {
                Toast.makeText(
                        v.getContext(),
                        "Failed to load description",
                        Toast.LENGTH_SHORT
                ).show();
                return; // ← STOP! Do NOT navigate.
            }

            // When online → request AI description + navigate
            if (clickListener != null) {
                clickListener.onFavoriteClicked(a);
            }
        });


        // Remove favorite
        String email = Session.currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        h.btnUnfavorite.setOnClickListener(v -> {
            int index = h.getBindingAdapterPosition();
            if (index == RecyclerView.NO_POSITION) return;

            db.collection("users")
                    .document(email)
                    .collection("favorites")
                    .document(a.getId())
                    .delete()
                    .addOnSuccessListener(x -> {
                        items.remove(index);
                        notifyItemRemoved(index);

                        if (items.isEmpty() && emptyListener != null) {
                            emptyListener.onEmpty();
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean isOnline(View v) {
        ConnectivityManager cm =
                (ConnectivityManager) v.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtName, txtType, txtDistance;
        ImageView imgPhoto, btnUnfavorite;

        Holder(@NonNull View v) {
            super(v);
            txtName = v.findViewById(R.id.txtFavName);
            txtType = v.findViewById(R.id.txtFavType);
            txtDistance = v.findViewById(R.id.txtFavDistance);
            imgPhoto = v.findViewById(R.id.imgFavPhoto);
            btnUnfavorite = v.findViewById(R.id.btnUnfavorite);
        }
    }
}
