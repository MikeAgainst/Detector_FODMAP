package com.detectorfodmap.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.detectorfodmap.app.data.Food;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra la foto del alimento. La obtiene de la API pública de Wikipedia y la
 * guarda en caché; si no hay red o no existe, deja el emoji como respaldo.
 */
public final class FoodImages {

    private static final String PREFS = "food_images";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, String> MEMORY = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = Collections.synchronizedSet(new HashSet<>());

    private FoodImages() {
    }

    public static void bind(ImageView image, TextView fallback, Food food) {
        Context context = image.getContext().getApplicationContext();
        image.setTag(food.getId());
        image.setVisibility(View.INVISIBLE);
        fallback.setVisibility(View.VISIBLE);
        fallback.setText(fallbackText(food));

        String cached = MEMORY.get(food.getId());
        if (cached == null) {
            SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (preferences.contains(food.getId())) {
                cached = preferences.getString(food.getId(), "");
                MEMORY.put(food.getId(), cached == null ? "" : cached);
            }
        }
        if (cached != null) {
            if (!cached.isEmpty()) {
                load(image, fallback, food, cached);
            }
            return;
        }

        if (!IN_FLIGHT.add(food.getId())) {
            return;
        }
        final String id = food.getId();
        EXECUTOR.execute(() -> {
            String url = fetchThumbnail(food);
            String value = url == null ? "" : url;
            MEMORY.put(id, value);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(id, value).apply();
            IN_FLIGHT.remove(id);
            MAIN.post(() -> {
                if (id.equals(image.getTag())) {
                    if (value.isEmpty()) {
                        image.setVisibility(View.INVISIBLE);
                        fallback.setVisibility(View.VISIBLE);
                    } else {
                        load(image, fallback, food, value);
                    }
                }
            });
        });
    }

    private static String fallbackText(Food food) {
        if (!food.getEmoji().isEmpty()) {
            return food.getEmoji();
        }
        String name = food.getName();
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static void load(ImageView image, TextView fallback, Food food, String url) {
        final String id = food.getId();
        Glide.with(image)
                .load(url)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException exception, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        image.setVisibility(View.INVISIBLE);
                        fallback.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        if (id.equals(image.getTag())) {
                            image.setVisibility(View.VISIBLE);
                            fallback.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .into(image);
    }

    @Nullable
    private static String fetchThumbnail(Food food) {
        HttpURLConnection connection = null;
        try {
            String title = Uri.encode(food.getWikiTitle());
            URL url = new URL("https://es.wikipedia.org/api/rest_v1/page/summary/" + title + "?redirect=true");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "DetectorFODMAP/1.2 (Android)");
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            JSONObject json = new JSONObject(body.toString());
            JSONObject thumbnail = json.optJSONObject("thumbnail");
            if (thumbnail == null) {
                return null;
            }
            String source = thumbnail.optString("source", "");
            return source.isEmpty() ? null : source;
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
