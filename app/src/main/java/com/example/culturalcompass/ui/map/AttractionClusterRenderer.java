package com.example.culturalcompass.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.AttractionClusterItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class AttractionClusterRenderer
        extends DefaultClusterRenderer<AttractionClusterItem> {

    private final Context context;

    public AttractionClusterRenderer(
            Context context,
            GoogleMap map,
            ClusterManager<AttractionClusterItem> clusterManager
    ) {
        super(context, map, clusterManager);
        this.context = context;
    }

    @Override
    protected void onBeforeClusterItemRendered(
            AttractionClusterItem item,
            MarkerOptions markerOptions
    ) {
        String primaryTypeKey = item.getAttraction().getPrimaryTypeKey();

        BitmapDescriptor icon = getMarkerIcon(primaryTypeKey);
        markerOptions.icon(icon).title(item.getTitle());
    }

    // Optional: customize cluster icon if you want later
    @Override
    protected boolean shouldRenderAsCluster(Cluster<AttractionClusterItem> cluster) {
        // Cluster when 2 or more items overlap
        return cluster.getSize() > 1;
    }

    private BitmapDescriptor getMarkerIcon(String primaryTypeKey) {
        int iconRes;

        if ("museum".equals(primaryTypeKey)) {
            iconRes = R.drawable.ic_museum;
        } else if ("art_gallery".equals(primaryTypeKey)) {
            iconRes = R.drawable.ic_art_gallery;
        } else if ("tourist_attraction".equals(primaryTypeKey)) {
            iconRes = R.drawable.ic_tourist_attraction;
        } else {
            iconRes = R.drawable.ic_tourist_attraction; // fallback
        }

        Drawable drawable = ContextCompat.getDrawable(context, iconRes);
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 120, 120, false);
        return BitmapDescriptorFactory.fromBitmap(scaled);
    }
}
