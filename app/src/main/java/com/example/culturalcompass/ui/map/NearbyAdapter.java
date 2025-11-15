package com.example.culturalcompass.ui.map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Attraction;

import java.util.List;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.ViewHolder> {

    private List<Attraction> items;

    public NearbyAdapter(List<Attraction> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDistance;

        public ViewHolder(View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
            txtDistance = v.findViewById(R.id.txtDistance);
        }
    }

    @NonNull
    @Override
    public NearbyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        Attraction a = items.get(pos);
        holder.txtName.setText(a.getName());
        holder.txtDistance.setText(String.format("%.2f km away", a.getDistanceMeters() / 1000));
    }

    @Override
    public int getItemCount() { return items.size(); }
}
