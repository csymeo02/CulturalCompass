package com.example.culturalcompass.ui.map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Attraction;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.model.Session;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.ViewHolder> {

    private List<Attraction> items;
    private PlacesClient placesClient;

    public NearbyAdapter(List<Attraction> items, PlacesClient placesClient) {
        this.items = items;
        this.placesClient = placesClient;
    }

    public void updateItems(List<Attraction> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attraction a = items.get(position);

        holder.txtName.setText(a.getName());
        holder.txtType.setText(a.getType());

        // --- Set chip color by type ---
        String typeKey = a.getPrimaryTypeKey();
        if (typeKey != null) {
            switch (typeKey) {
                case "museum":
                    holder.txtType.setBackgroundResource(R.drawable.chip_museum);
                    holder.txtType.setTextColor(0xFF5A3E2B);
                    break;

                case "art_gallery":
                    holder.txtType.setBackgroundResource(R.drawable.chip_art_gallery);
                    holder.txtType.setTextColor(0xFF0A3A5C);
                    break;

                case "tourist_attraction":
                default:
                    holder.txtType.setBackgroundResource(R.drawable.chip_attraction);
                    holder.txtType.setTextColor(0xFF1F6B1F);
                    break;
            }
        }

        // --- Distance ---
        double meters = a.getDistanceMeters();
        String distanceText;
        if (meters < 1000) {
            distanceText = String.format(Locale.getDefault(), "%.0f m away", meters);
        } else {
            distanceText = String.format(Locale.getDefault(), "%.1f km away", meters / 1000.0);
        }
        holder.txtDistance.setText(distanceText);

        // --- Rating ---
        Double rating = a.getRating();
        Integer ratingCount = a.getRatingCount();

        if (rating != null && ratingCount != null && ratingCount > 0) {
            holder.txtRatingValue.setText(String.format(Locale.getDefault(), "%.1f", rating));
            holder.ratingBar.setRating(rating.floatValue());
            holder.txtRatingCount.setText("(" + ratingCount + ")");
            holder.ratingBar.setVisibility(View.VISIBLE);
            holder.txtRatingCount.setVisibility(View.VISIBLE);
            holder.layoutRating.setVisibility(View.VISIBLE);
        } else {
            holder.txtRatingValue.setText("No ratings yet");
            holder.ratingBar.setVisibility(View.GONE);
            holder.txtRatingCount.setVisibility(View.GONE);
            holder.layoutRating.setVisibility(View.VISIBLE);
        }

        // --- Photo ---
        holder.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);
        PhotoMetadata meta = a.getPhotoMetadata();

        if (meta != null && placesClient != null) {
            int boundPos = holder.getBindingAdapterPosition();

            FetchPhotoRequest req = FetchPhotoRequest.builder(meta)
                    .setMaxWidth(400)
                    .setMaxHeight(400)
                    .build();

            placesClient.fetchPhoto(req)
                    .addOnSuccessListener(response -> {
                        if (holder.getBindingAdapterPosition() == boundPos) {
                            holder.imgPhoto.setImageBitmap(response.getBitmap());
                        }
                    });
        }

        // ======================================================
        // 🟩 REAL FAVORITES (Firestore)
        // ======================================================

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = Session.currentUser != null ? Session.currentUser.getEmail() : null;

        // A. Check if favorite
        if (email != null) {
            db.collection("users")
                    .document(email)
                    .collection("favorites")
                    .document(a.getPlaceId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        boolean isFav = doc.exists();
                        a.setFavorite(isFav);
                        updateHeartIcon(holder, isFav);
                    });
        }

        // B. Toggle and write to Firestore
        holder.imgFavorite.setOnClickListener(v -> {
            if (email == null) return;

            boolean newFav = !a.isFavorite();
            a.setFavorite(newFav);
            updateHeartIcon(holder, newFav);

            if (newFav) {
                // Add
                FirestoreAttraction fa = new FirestoreAttraction(
                        a.getPlaceId(),
                        a.getName(),
                        a.getLat(),
                        a.getLng(),
                        a.getDistanceMeters(),
                        a.getType(),
                        a.getPrimaryTypeKey(),
                        a.getRating(),
                        a.getRatingCount()
                );

                db.collection("users")
                        .document(email)
                        .collection("favorites")
                        .document(a.getPlaceId())
                        .set(fa);

            } else {
                // Remove
                db.collection("users")
                        .document(email)
                        .collection("favorites")
                        .document(a.getPlaceId())
                        .delete();
            }
        });
    }

    private void updateHeartIcon(ViewHolder holder, boolean favorite) {
        if (favorite) {
            holder.imgFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.imgFavorite.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPhoto;
        TextView txtName;
        TextView txtType;
        TextView txtDistance;
        ImageView imgFavorite;

        LinearLayout layoutRating;
        TextView txtRatingValue;
        TextView txtRatingCount;
        RatingBar ratingBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtType = itemView.findViewById(R.id.txtType);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgPhoto = itemView.findViewById(R.id.imgPhoto);

            layoutRating = itemView.findViewById(R.id.layoutRating);
            txtRatingValue = itemView.findViewById(R.id.txtRatingValue);
            txtRatingCount = itemView.findViewById(R.id.txtRatingCount);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
