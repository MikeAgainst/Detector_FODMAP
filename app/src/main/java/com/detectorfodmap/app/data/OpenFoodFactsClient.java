package com.detectorfodmap.app.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consulta la API pública de Open Food Facts por código de barras.
 * Gratuita, sin clave y con datos abiertos (CC BY-SA).
 */
public final class OpenFoodFactsClient {

    public interface Callback {
        void onProduct(Product product);

        void onNotFound(String barcode);

        void onError(String barcode);
    }

    private static final String TAG = "OpenFoodFacts";
    private static final String BASE = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String FIELDS =
            "product_name,product_name_es,generic_name,generic_name_es,brands,quantity,"
                    + "image_front_url,image_front_small_url,"
                    + "ingredients_text,ingredients_text_es,ingredients";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private OpenFoodFactsClient() {
    }

    public static void fetch(String barcode, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                Product product = request(barcode);
                MAIN.post(() -> {
                    if (product == null) {
                        callback.onNotFound(barcode);
                    } else {
                        callback.onProduct(product);
                    }
                });
            } catch (Exception exception) {
                Log.w(TAG, "No se pudo consultar el código " + barcode, exception);
                MAIN.post(() -> callback.onError(barcode));
            }
        });
    }

    private static Product request(String barcode) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE + barcode + ".json?fields=" + FIELDS);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "DetectorFODMAP/1.2 (Android)");
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
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
            JSONObject product = json.optJSONObject("product");
            if (product == null) {
                return null;
            }

            String name = firstNonEmpty(
                    product.optString("product_name_es"),
                    product.optString("product_name"),
                    product.optString("generic_name_es"),
                    product.optString("generic_name"));
            if (name.isEmpty()) {
                return null;
            }

            String image = firstNonEmpty(
                    product.optString("image_front_url"),
                    product.optString("image_front_small_url"));

            String ingredients = firstNonEmpty(
                    product.optString("ingredients_text_es"),
                    product.optString("ingredients_text"),
                    joinIngredients(product.optJSONArray("ingredients")));

            return new Product(
                    barcode,
                    name,
                    product.optString("brands"),
                    product.optString("quantity"),
                    image,
                    ingredients);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Une la lista estructurada de ingredientes cuando falta el texto libre. */
    private static String joinIngredients(JSONArray array) {
        if (array == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject ingredient = array.optJSONObject(i);
            if (ingredient == null) {
                continue;
            }
            String text = ingredient.optString("text").trim();
            if (text.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
