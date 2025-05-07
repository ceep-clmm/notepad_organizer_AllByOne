package com.example.allbyone.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class DrawableUtils {
    public static Drawable getDrawableFromUri(Context context, Uri uri) throws Exception {
        return Drawable.createFromStream(context.getContentResolver().openInputStream(uri), uri.toString());
    }
}
