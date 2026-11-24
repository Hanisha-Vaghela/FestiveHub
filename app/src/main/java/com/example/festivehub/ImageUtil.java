package com.example.festivehub;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageUtil {

    /**
     * Loads the event banner into an ImageView.
     * 1) imageUrl (internet link) -> Glide
     * 2) imageName (drawable name) -> local drawable
     * 3) otherwise a placeholder
     */
    public static void loadEventImage(ImageView view, EventList event) {
        Context context = view.getContext();

        String url = event.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(view)
                    .load(url.trim())
                    .placeholder(R.drawable.splashscreen_logo)
                    .error(R.drawable.splashscreen_logo)
                    .into(view);
            return;
        }

        int resId = 0;
        String name = event.getImageName();
        if (name != null && !name.trim().isEmpty()) {
            resId = context.getResources().getIdentifier(name.trim(), "drawable", context.getPackageName());
        }
        view.setImageResource(resId != 0 ? resId : R.drawable.splashscreen_logo);
    }
}
