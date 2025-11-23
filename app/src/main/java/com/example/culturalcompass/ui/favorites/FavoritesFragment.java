package com.example.culturalcompass.ui.favorites;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.MainActivity;
import com.example.culturalcompass.R;
import com.example.culturalcompass.model.FirestoreAttraction;
import com.example.culturalcompass.ui.description.DescriptionFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FavoritesFragment extends Fragment {

    private RecyclerView recycler;
    private FavoritesAdapter adapter;
    private FirebaseFirestore db;
    private String email;

    private View emptyContainer;
    private TextView txtEmpty;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_favorites, container, false);

        recycler = v.findViewById(R.id.recyclerFavorites);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        emptyContainer = v.findViewById(R.id.emptyContainer);
        txtEmpty = v.findViewById(R.id.txtEmpty);

        adapter = new FavoritesAdapter(new ArrayList<>(), MainActivity.placesClient);
        recycler.setAdapter(adapter);

        adapter.setEmptyListener(() -> emptyContainer.setVisibility(View.VISIBLE));
        adapter.setOnFavoriteClickListener(this::requestAIDescriptionAndOpen);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            emptyContainer.setVisibility(View.VISIBLE);
            txtEmpty.setText("Please log in to see your favorites.");
            return v;
        }

        email = user.getEmail();
        if (email == null) {
            emptyContainer.setVisibility(View.VISIBLE);
            txtEmpty.setText("Could not load favorites.");
            return v;
        }

        loadFavorites();
        return v;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
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

                    list.sort((a, b) -> {
                        int countA = a.getRatingCount() != null ? a.getRatingCount() : 0;
                        int countB = b.getRatingCount() != null ? b.getRatingCount() : 0;

                        boolean aValid = countA >= 2;
                        boolean bValid = countB >= 2;

                        if (aValid && !bValid) return -1;
                        if (!aValid && bValid) return 1;

                        double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                        double ratingB = b.getRating() != null ? b.getRating() : 0.0;

                        int cmp = Double.compare(ratingB, ratingA);
                        if (cmp != 0) return cmp;

                        return Integer.compare(countB, countA);
                    });

                    adapter.update(list);
                    emptyContainer.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void requestAIDescriptionAndOpen(FirestoreAttraction a) {

        if (!isOnline()) {
            Toast.makeText(requireContext(), "Failed to load description", Toast.LENGTH_SHORT).show();
            openDescriptionFragment(a, "");
            return;
        }

        String prompt =
                "Give me a short friendly explanation (3–5 sentences, no markdown) "
                        + "about this place: " + a.getName() + ". "
                        + "It is a " + a.getTypeLabel() + ". "
                        + "If well-known, include background. "
                        + "If small, keep it simple. "
                        + "No coordinates or country names.";

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

        RequestBody reqBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + "gemini-2.0-flash:generateContent?key="
                        + getString(R.string.gemini_api_key);

        Request request = new Request.Builder()
                .url(url)
                .post(reqBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load description", Toast.LENGTH_SHORT).show());
                openDescriptionFragment(a, "");
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call,
                                   @NonNull Response response) throws IOException {

                String json = response.body().string();
                String desc = parseGeminiDescription(json);

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() ->
                        openDescriptionFragment(a, desc));
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

    private void openDescriptionFragment(FirestoreAttraction a, String aiText) {
        DescriptionFragment fragment = DescriptionFragment.newInstance(
                a.getId(),
                a.getName(),
                a.getTypeLabel(),
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
}
