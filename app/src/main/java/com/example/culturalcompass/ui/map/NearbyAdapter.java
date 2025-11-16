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

import java.util.List;
import java.util.Locale;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;



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

        // --- Name & type ---
        holder.txtName.setText(a.getName());
        holder.txtType.setText(a.getType());

        // --- Distance formatting ---
        double meters = a.getDistanceMeters();
        String distanceText;
        if (meters < 1000) {
            distanceText = String.format(Locale.getDefault(), "%.0f m away", meters);
        } else {
            distanceText = String.format(Locale.getDefault(), "%.1f km away", meters / 1000.0);
        }
        holder.txtDistance.setText(distanceText);

        // --- Rating chip (4,4 ★★★★☆ (88)) ---
        Double rating = a.getRating();
        Integer ratingCount = a.getRatingCount();

        if (rating != null && ratingCount != null && ratingCount > 0) {
            String ratingStr = String.format(Locale.getDefault(), "%.1f", rating);
            holder.txtRatingValue.setText(ratingStr);
            holder.ratingBar.setRating(rating.floatValue());

            String countStr = "(" + ratingCount + ")";
            holder.txtRatingCount.setText(countStr);

            holder.layoutRating.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRating.setVisibility(View.GONE);
        }

        // --- Landmark photo ---
        holder.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

        PhotoMetadata meta = a.getPhotoMetadata();

        if (meta != null && placesClient != null) {
            FetchPhotoRequest request = FetchPhotoRequest.builder(meta)
                    .setMaxWidth(400)
                    .setMaxHeight(400)
                    .build();

            int boundPosition = holder.getBindingAdapterPosition();

            placesClient.fetchPhoto(request)
                    .addOnSuccessListener(response -> {
                        if (holder.getBindingAdapterPosition() == boundPosition) {
                            holder.imgPhoto.setImageBitmap(response.getBitmap());
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }

        // --- Heart toggle ---
        updateHeartIcon(holder, a.isFavorite());
        holder.imgFavorite.setOnClickListener(v -> {
            boolean newFav = !a.isFavorite();
            a.setFavorite(newFav);
            updateHeartIcon(holder, newFav);
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

