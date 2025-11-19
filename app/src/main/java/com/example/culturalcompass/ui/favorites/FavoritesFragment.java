package com.example.culturalcompass.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.model.Session;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recycler;
    private FavoritesAdapter adapter;
    private FirebaseFirestore db;
    private String email;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_favorites, container, false);

        recycler = v.findViewById(R.id.recyclerFavorites);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        PlacesClient placesClient = Places.createClient(requireContext());
        adapter = new FavoritesAdapter(new ArrayList<>(), placesClient);
        recycler.setAdapter(adapter);


        db = FirebaseFirestore.getInstance();
        email = Session.currentUser.getEmail();

        loadFavorites();
        return v;
    }

    private void loadFavorites() {
        db.collection("users")
                .document(email)
                .collection("favorites")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<FirestoreAttraction> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        FirestoreAttraction fa = doc.toObject(FirestoreAttraction.class);
                        if (fa != null) list.add(fa);
                    }

                    adapter.update(list);
                });
    }
}
