package com.example.culturalcompass.ui.favorites;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public class PhotoCacheManager {

    private static final String DIR = "fav_photos"; // folder where we store cached images

    public static Bitmap load(Context ctx, String placeId) {
        // try to load the saved photo for this place
        File file = new File(ctx.getCacheDir(), DIR + "/" + placeId + ".jpg");
        if (!file.exists()) return null; // nothing saved yet
        return BitmapFactory.decodeFile(file.getAbsolutePath()); // return cached image
    }

    public static void save(Context ctx, String placeId, Bitmap bmp) {
        try {
            // make sure the cache folder exists
            File dir = new File(ctx.getCacheDir(), DIR);
            if (!dir.exists()) dir.mkdirs();

            // save the bitmap to a local jpg file
            File file = new File(dir, placeId + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out); // compress to keep the file small
            out.flush();
            out.close();
        } catch (Exception ignored) {
            // ignore errors, it's just a cache
        }
    }
}
