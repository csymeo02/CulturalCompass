package com.example.culturalcompass.ui.favorites;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public class PhotoCacheManager {

    private static final String DIR = "fav_photos";

    public static Bitmap load(Context ctx, String placeId) {
        File file = new File(ctx.getCacheDir(), DIR + "/" + placeId + ".jpg");
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static void save(Context ctx, String placeId, Bitmap bmp) {
        try {
            File dir = new File(ctx.getCacheDir(), DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, placeId + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();
        } catch (Exception ignored) {}
    }
}
