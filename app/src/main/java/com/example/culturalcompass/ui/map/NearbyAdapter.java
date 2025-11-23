package com.example.culturalcompass.ui.map;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Attraction;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.ui.favorites.PhotoCacheManager;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.ViewHolder> {

    private List<Attraction> items;
    private PlacesClient placesClient;
    private OnAttractionClickListener listener;

    public interface OnAttractionClickListener {
        void onAttractionClicked(Attraction attraction);
    }

    public void setOnAttractionClickListener(OnAttractionClickListener l) {
        this.listener = l;
    }

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
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        Attraction a = items.get(position);

        // Main click → open description
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAttractionClicked(a);
        });

        h.txtName.setText(a.getName());
        h.txtType.setText(a.getType());

        // Type chip styling
        String typeKey = a.getPrimaryTypeKey();
        if (typeKey != null) {
            switch (typeKey) {
                case "museum":
                    h.txtType.setBackgroundResource(R.drawable.chip_museum);
                    h.txtType.setTextColor(0xFF5A3E2B);
                    break;

                case "art_gallery":
                    h.txtType.setBackgroundResource(R.drawable.chip_art_gallery);
                    h.txtType.setTextColor(0xFF0A3A5C);
                    break;

                default:
                    h.txtType.setBackgroundResource(R.drawable.chip_attraction);
                    h.txtType.setTextColor(0xFF1F6B1F);
                    break;
            }
        }

        // Distance text
        double m = a.getDistanceMeters();
        h.txtDistance.setText(
                (m < 1000)
                        ? String.format(Locale.getDefault(), "%.0f m away", m)
                        : String.format(Locale.getDefault(), "%.1f km away", m / 1000.0)
        );

        // Rating section
        Double r = a.getRating();
        Integer rc = a.getRatingCount();

        if (r != null && rc != null && rc > 0) {
            h.txtRatingValue.setText(String.format(Locale.getDefault(), "%.1f", r));
            h.ratingBar.setRating(r.floatValue());
            h.txtRatingCount.setText("(" + rc + ")");
            h.ratingBar.setVisibility(View.VISIBLE);
            h.txtRatingCount.setVisibility(View.VISIBLE);
            h.layoutRating.setVisibility(View.VISIBLE);
        } else {
            h.txtRatingValue.setText("No ratings yet");
            h.ratingBar.setVisibility(View.GONE);
            h.txtRatingCount.setVisibility(View.GONE);
            h.layoutRating.setVisibility(View.VISIBLE);
        }

        // photo loading
        Bitmap cached = PhotoCacheManager.load(h.itemView.getContext(), a.getPlaceId());
        if (cached != null) {
            h.imgPhoto.setImageBitmap(cached);
        } else {
            h.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

            PhotoMetadata meta = a.getPhotoMetadata();
            if (meta != null) {
                FetchPhotoRequest photoReq = FetchPhotoRequest.builder(meta)
                        .setMaxWidth(400)
                        .setMaxHeight(400)
                        .build();

                placesClient.fetchPhoto(photoReq).addOnSuccessListener(resp -> {
                    Bitmap bmp = resp.getBitmap();
                    PhotoCacheManager.save(h.itemView.getContext(), a.getPlaceId(), bmp);
                    h.imgPhoto.setImageBitmap(bmp);
                });
            }
        }

        // favorite toogle
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        updateHeartIcon(h, a.isFavorite());

        h.imgFavorite.setOnClickListener(v -> {
            if (email == null) return;

            boolean newFav = !a.isFavorite();
            a.setFavorite(newFav);
            updateHeartIcon(h, newFav);

            // Refresh row immediately
            notifyItemChanged(h.getBindingAdapterPosition());

            if (newFav) {
                // Add to Firestore
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
                        .set(fa)
                        .addOnSuccessListener(x ->
                                Toast.makeText(v.getContext(),
                                        "Added: " + a.getName(),
                                        Toast.LENGTH_SHORT).show()
                        );

            } else {
                // Remove from Firestore
                db.collection("users")
                        .document(email)
                        .collection("favorites")
                        .document(a.getPlaceId())
                        .delete()
                        .addOnSuccessListener(x ->
                                Toast.makeText(v.getContext(),
                                        "Removed: " + a.getName(),
                                        Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }

    private void updateHeartIcon(ViewHolder h, boolean fav) {
        h.imgFavorite.setImageResource(
                fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPhoto, imgFavorite;
        TextView txtName, txtType, txtDistance;
        LinearLayout layoutRating;
        TextView txtRatingValue, txtRatingCount;
        RatingBar ratingBar;

        ViewHolder(@NonNull View item) {
            super(item);
            txtName = item.findViewById(R.id.txtName);
            txtType = item.findViewById(R.id.txtType);
            txtDistance = item.findViewById(R.id.txtDistance);
            imgFavorite = item.findViewById(R.id.imgFavorite);
            imgPhoto = item.findViewById(R.id.imgPhoto);

            layoutRating = item.findViewById(R.id.layoutRating);
            txtRatingValue = item.findViewById(R.id.txtRatingValue);
            txtRatingCount = item.findViewById(R.id.txtRatingCount);
            ratingBar = item.findViewById(R.id.ratingBar);
        }
    }
}
