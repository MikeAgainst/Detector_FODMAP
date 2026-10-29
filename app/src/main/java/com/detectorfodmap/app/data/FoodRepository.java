package com.detectorfodmap.app.data;

import android.content.Context;
import android.util.Log;

import com.detectorfodmap.app.util.TextNormalizer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga la tabla FODMAP desde assets/foods.json y resuelve las búsquedas.
 */
public final class FoodRepository {

    private static final String TAG = "FoodRepository";
    private static final String ASSET = "foods.json";

    private static volatile FoodRepository instance;

    private final List<Food> foods = new ArrayList<>();
    private final Map<String, Food> byId = new HashMap<>();
    private String disclaimer = "";
    private String updated = "";

    private FoodRepository(Context context) {
        load(context);
    }

    public static FoodRepository get(Context context) {
        if (instance == null) {
            synchronized (FoodRepository.class) {
                if (instance == null) {
                    instance = new FoodRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private void load(Context context) {
        try (InputStream stream = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append('\n');
            }
            JSONObject root = new JSONObject(json.toString());
            disclaimer = root.optString("disclaimer", "");
            updated = root.optString("updated", "");
            JSONArray array = root.getJSONArray("foods");
            for (int i = 0; i < array.length(); i++) {
                Food food = parse(array.getJSONObject(i));
                if (food != null) {
                    foods.add(food);
                    byId.put(food.getId(), food);
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "No se pudo leer " + ASSET, exception);
        }
        Collections.sort(foods, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    private static Food parse(JSONObject object) {
        try {
            return new Food(
                    object.getString("id"),
                    object.getString("name"),
                    readList(object.optJSONArray("aliases")),
                    object.optString("category", "Otro"),
                    object.optString("level", Food.LEVEL_LOW),
                    readList(object.optJSONArray("fodmaps")),
                    object.optString("portion", ""),
                    object.optString("note", ""),
                    object.optString("emoji", ""),
                    object.optString("wiki", "")
            );
        } catch (Exception exception) {
            Log.e(TAG, "Alimento inválido en " + ASSET, exception);
            return null;
        }
    }

    private static List<String> readList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                values.add(array.optString(i));
            }
        }
        return values;
    }

    public List<Food> all() {
        return Collections.unmodifiableList(foods);
    }

    public Food byId(String id) {
        return byId.get(id);
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public String getUpdated() {
        return updated;
    }

    public List<Food> search(String query) {
        String normalized = TextNormalizer.normalize(query);
        if (normalized.isEmpty()) {
            return new ArrayList<>(foods);
        }
        List<Food> matches = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (Food food : foods) {
            int score = food.score(normalized);
            if (score > 0) {
                matches.add(food);
                scores.add(score);
            }
        }
        // Orden estable: primero por puntuación y, a igualdad, por nombre.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            order.add(i);
        }
        order.sort((left, right) -> {
            int byScore = Integer.compare(scores.get(right), scores.get(left));
            if (byScore != 0) {
                return byScore;
            }
            return matches.get(left).getName().compareToIgnoreCase(matches.get(right).getName());
        });
        List<Food> results = new ArrayList<>(order.size());
        for (Integer index : order) {
            results.add(matches.get(index));
        }
        return results;
    }

    /**
     * Busca el alimento que mejor encaja con el nombre de un producto envasado.
     * Devuelve null si la coincidencia es demasiado débil.
     */
    public Food findProductMatch(String productName) {
        String text = TextNormalizer.normalize(productName);
        if (text.isEmpty()) {
            return null;
        }
        Food best = null;
        int bestScore = 0;
        for (Food food : foods) {
            int score = food.matchInText(text);
            if (score > bestScore) {
                bestScore = score;
                best = food;
            }
        }
        return bestScore >= 700 ? best : null;
    }
}
