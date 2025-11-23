package com.example.culturalcompass.ui.favorites;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
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
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.Holder> {

    public interface OnFavoriteClickListener {
        void onFavoriteClicked(FirestoreAttraction a);
    }

    public interface OnEmptyStateListener {
        void onEmpty();
    }

    private OnFavoriteClickListener clickListener;
    private OnEmptyStateListener emptyListener;

    private List<FirestoreAttraction> items;
    private PlacesClient placesClient;

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

    @SuppressLint("NotifyDataSetChanged")
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

        double m = a.getDistanceMeters();
        h.txtDistance.setText(m < 1000
                ? String.format(Locale.getDefault(), "%.0f m", m)
                : String.format(Locale.getDefault(), "%.1f km", m / 1000));

        // new logic -> load photo from phone cache and if does not exist call the API
        h.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

        Bitmap cached = PhotoCacheManager.load(h.itemView.getContext(), a.getId());
        if (cached != null) {
            h.imgPhoto.setImageBitmap(cached);
        } else {
            h.imgPhoto.setImageResource(R.drawable.ic_landmark_placeholder);

            if (placesClient != null && a.getId() != null) {

                FetchPlaceRequest req = FetchPlaceRequest.builder(
                        a.getId(),
                        Arrays.asList(Place.Field.PHOTO_METADATAS)
                ).build();

                placesClient.fetchPlace(req).addOnSuccessListener(response -> {

                    List<PhotoMetadata> metaList = response.getPlace().getPhotoMetadatas();
                    if (metaList == null || metaList.isEmpty()) return;

                    PhotoMetadata meta = metaList.get(0);

                    FetchPhotoRequest photoReq = FetchPhotoRequest.builder(meta)
                            .setMaxWidth(400)
                            .setMaxHeight(400)
                            .build();

                    placesClient.fetchPhoto(photoReq).addOnSuccessListener(photoResponse -> {
                        Bitmap bmp = photoResponse.getBitmap();
                        PhotoCacheManager.save(h.itemView.getContext(), a.getId(), bmp);
                        h.imgPhoto.setImageBitmap(bmp);
                    });
                });
            }
        }


        // open description
        h.itemView.setOnClickListener(v -> {
            if (!isOnline(v)) {
                Toast.makeText(v.getContext(), "Failed to load description", Toast.LENGTH_SHORT).show();
                return;
            }
            if (clickListener != null) clickListener.onFavoriteClicked(a);
        });

        // unfavorite
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
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
                        Toast.makeText(v.getContext(),
                                "Removed: " + a.getName(),
                                Toast.LENGTH_SHORT).show();

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
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
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
