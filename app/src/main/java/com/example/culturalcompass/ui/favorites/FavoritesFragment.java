package com.example.culturalcompass.ui.favorites;

import android.content.Context;                                     // used for connectivity check
import android.net.ConnectivityManager;                             // for isOnline()
import android.net.NetworkInfo;                                     // for isOnline()
import android.os.Bundle;                                           // fragment state
import android.view.LayoutInflater;                                 // inflate layout
import android.view.View;                                           // root views
import android.view.ViewGroup;                                      // container
import android.widget.TextView;                                     // empty text
import android.widget.Toast;                                        // toasts

import androidx.annotation.NonNull;                                 // annotations
import androidx.annotation.Nullable;                                // annotations
import androidx.fragment.app.Fragment;                              // base fragment
import androidx.recyclerview.widget.GridLayoutManager;              // grid layout
import androidx.recyclerview.widget.RecyclerView;                   // recycler view

import com.example.culturalcompass.MainActivity;                    // to use static placesClient
import com.example.culturalcompass.R;                               // resources
import com.example.culturalcompass.model.FirestoreAttraction;       // favorite model
import com.example.culturalcompass.ui.description.DescriptionFragment; // description screen
import com.google.firebase.auth.FirebaseAuth;                        // Firebase Auth
import com.google.firebase.auth.FirebaseUser;                        // current user
import com.google.firebase.firestore.DocumentSnapshot;              // Firestore docs
import com.google.firebase.firestore.FirebaseFirestore;             // Firestore

import org.json.JSONArray;                                         // JSON array
import org.json.JSONObject;                                        // JSON object

import java.io.IOException;                                        // network errors
import java.util.ArrayList;                                        // list impl
import java.util.List;                                             // list interface

import okhttp3.Callback;                                           // OkHttp async callback
import okhttp3.MediaType;                                          // request content type
import okhttp3.OkHttpClient;                                       // HTTP client
import okhttp3.Request;                                            // HTTP request
import okhttp3.RequestBody;                                        // HTTP body
import okhttp3.Response;                                           // HTTP response

public class FavoritesFragment extends Fragment {

    private RecyclerView recycler;                                  // grid of favorite cards
    private FavoritesAdapter adapter;                               // adapter for favorites
    private FirebaseFirestore db;                                   // Firestore instance
    private String email;                                           // current user's email

    private View emptyContainer;                                    // layout for empty state
    private TextView txtEmpty;                                      // text inside empty state

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,                       // inflater
            @Nullable ViewGroup container,                          // parent container
            @Nullable Bundle savedInstanceState                     // saved state
    ) {
        View v = inflater.inflate(R.layout.fragment_favorites, container, false); // inflate UI

        recycler = v.findViewById(R.id.recyclerFavorites);          // find recycler
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2-column grid

        emptyContainer = v.findViewById(R.id.emptyContainer);       // empty state container
        txtEmpty = v.findViewById(R.id.txtEmpty);                   // empty state text

        // Use the static PlacesClient created in MainActivity
        adapter = new FavoritesAdapter(new ArrayList<>(), MainActivity.placesClient); // adapter with empty list
        recycler.setAdapter(adapter);                               // attach adapter

        // When the last item is removed -> show empty state
        adapter.setEmptyListener(() -> emptyContainer.setVisibility(View.VISIBLE));   // callback

        // When user taps a favorite card -> request AI description and open DescriptionFragment
        adapter.setOnFavoriteClickListener(this::requestAIDescriptionAndOpen);       // click callback

        db = FirebaseFirestore.getInstance();                       // init Firestore

        // -------- FirebaseAuth instead of Session.currentUser --------
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // get current user
        if (user == null) {                                         // if no user (safety)
            emptyContainer.setVisibility(View.VISIBLE);             // show empty state
            txtEmpty.setText("Please log in to see your favorites."); // optional message
            return v;                                               // stop here
        }

        email = user.getEmail();                                    // get email for Firestore
        if (email == null) {                                        // extra safety
            emptyContainer.setVisibility(View.VISIBLE);             // show empty state
            txtEmpty.setText("Could not load favorites (no email)."); // message
            return v;                                               // stop
        }

        loadFavorites();                                            // load favorites from Firestore

        return v;                                                   // return root view
    }

    // ------------------- Connectivity helper -------------------
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) requireContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE); // get connectivity service
        NetworkInfo netInfo = cm.getActiveNetworkInfo();                 // active network
        return netInfo != null && netInfo.isConnected();                 // true if connected
    }

    // ------------------- Load favorites list -------------------
    private void loadFavorites() {
        db.collection("users")                                        // users collection
                .document(email)                                      // current user doc
                .collection("favorites")                              // favorites subcollection
                .get()
                .addOnSuccessListener(snapshot -> {                   // when Firestore returns

                    List<FirestoreAttraction> list = new ArrayList<>(); // temp list

                    for (DocumentSnapshot doc : snapshot.getDocuments()) { // loop docs
                        FirestoreAttraction fa = doc.toObject(FirestoreAttraction.class); // map
                        if (fa != null) list.add(fa);                  // add if not null
                    }

                    // -------- Sort as in their version (rating + count) --------
                    list.sort((a, b) -> {

                        int countA = a.getRatingCount() != null ? a.getRatingCount() : 0; // reviews A
                        int countB = b.getRatingCount() != null ? b.getRatingCount() : 0; // reviews B

                        boolean aValid = countA >= 2;                 // A has enough reviews?
                        boolean bValid = countB >= 2;                 // B has enough reviews?

                        // Those with >=2 reviews go first
                        if (aValid && !bValid) return -1;
                        if (!aValid && bValid) return 1;

                        double ratingA = a.getRating() != null ? a.getRating() : 0.0; // rating A
                        double ratingB = b.getRating() != null ? b.getRating() : 0.0; // rating B

                        int cmp = Double.compare(ratingB, ratingA);   // higher rating first
                        if (cmp != 0) return cmp;                     // if different, done

                        return Integer.compare(countB, countA);       // else by review count
                    });

                    adapter.update(list);                              // push into adapter

                    // show / hide empty layout
                    emptyContainer.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ------------------- AI description + navigation -------------------
    private void requestAIDescriptionAndOpen(FirestoreAttraction a) {

        // 1) If no internet -> just open Description without AI text
        if (!isOnline()) {
            Toast.makeText(requireContext(),
                    "Failed to load description",
                    Toast.LENGTH_SHORT
            ).show();

            openDescriptionFragment(a, "");                           // open with empty AI text
            return;                                                  // stop here
        }

        // 2) Build Gemini prompt
        String prompt =
                "Give me a short friendly explanation (3–5 sentences, no markdown) about: "
                        + a.getName()
                        + ". The type is " + a.getTypeLabel() + "."; // simple prompt

        JSONObject body = new JSONObject();                          // root JSON
        try {
            JSONArray contents = new JSONArray();                    // contents array

            JSONObject userObj = new JSONObject();                   // single user message
            userObj.put("role", "user");                             // role = user

            JSONArray parts = new JSONArray();                       // parts array
            JSONObject text = new JSONObject();                      // text object
            text.put("text", prompt);                                // put prompt text
            parts.put(text);                                         // add to parts

            userObj.put("parts", parts);                             // attach parts to user obj
            contents.put(userObj);                                   // add user obj to contents

            body.put("contents", contents);                          // set contents in body

        } catch (Exception e) {
            e.printStackTrace();                                     // log error
        }

        RequestBody reqBody = RequestBody.create(                    // OkHttp request body
                body.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + "gemini-2.0-flash:generateContent?key="
                        + getString(R.string.gemini_api_key);        // full Gemini endpoint

        Request request = new Request.Builder()                       // build HTTP request
                .url(url)
                .post(reqBody)
                .build();

        OkHttpClient client = new OkHttpClient();                    // HTTP client

        client.newCall(request).enqueue(new Callback() {             // async call

            @Override
            public void onFailure(@NonNull okhttp3.Call call,
                                  @NonNull IOException e) {
                if (!isAdded()) return;                              // fragment detached?

                requireActivity().runOnUiThread(() ->                // back to UI thread
                        Toast.makeText(requireContext(),
                                "Failed to load description",
                                Toast.LENGTH_SHORT
                        ).show()
                );

                openDescriptionFragment(a, "");                      // open without AI text
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call,
                                   @NonNull Response response) throws IOException {

                String json = response.body().string();              // read body
                String description = parseGeminiDescription(json);   // parse AI text

                if (!isAdded()) return;                              // fragment detached?

                requireActivity().runOnUiThread(() ->                // back to UI
                        openDescriptionFragment(a, description)      // open with AI text
                );
            }
        });
    }

    // Parse AI response JSON
    private String parseGeminiDescription(String json) {
        try {
            JSONObject obj = new JSONObject(json);                   // root
            JSONArray candidates = obj.getJSONArray("candidates");   // candidates array
            JSONObject content = candidates.getJSONObject(0)
                    .getJSONObject("content");                       // first content
            JSONArray parts = content.getJSONArray("parts");         // parts
            return parts.getJSONObject(0).getString("text").trim();  // first text
        } catch (Exception e) {
            return "No description available.";                      // fallback
        }
    }

    // Open DescriptionFragment with all info
    private void openDescriptionFragment(FirestoreAttraction a, String aiText) {

        DescriptionFragment fragment = DescriptionFragment.newInstance(
                a.getId(),                                          // placeId
                a.getName(),                                        // name
                a.getTypeLabel(),                                   // type label
                a.getPrimaryTypeKey(),                              // primary type key
                a.getDistanceMeters(),                              // distance
                a.getRating(),                                      // rating
                a.getRatingCount(),                                 // rating count
                aiText                                              // AI description
        );

        requireActivity().getSupportFragmentManager()               // navigate
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .addToBackStack(null)
                .commit();
    }
}
