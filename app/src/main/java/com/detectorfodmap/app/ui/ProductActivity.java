package com.detectorfodmap.app.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.detectorfodmap.app.R;
import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.data.FoodRepository;
import com.detectorfodmap.app.data.IngredientAnalysis;
import com.detectorfodmap.app.data.OpenFoodFactsClient;
import com.detectorfodmap.app.data.Product;
import com.detectorfodmap.app.databinding.ActivityProductBinding;
import com.detectorfodmap.app.databinding.ItemIngredientBinding;
import com.detectorfodmap.app.util.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Muestra el producto leído por código de barras y lo cruza con la tabla FODMAP.
 */
public class ProductActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "barcode";

    private ActivityProductBinding binding;
    private FoodRepository repository;
    private String barcode;
    private String manualQuery = "";
    private Food match;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.edgeToEdge(this, binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repository = FoodRepository.get(this);
        barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode == null || barcode.trim().isEmpty()) {
            finish();
            return;
        }
        barcode = barcode.trim();
        binding.barcodeLabel.setText(getString(R.string.product_barcode_label, barcode));

        binding.retryButton.setOnClickListener(view -> load());
        binding.stateSearchButton.setOnClickListener(view -> openSearch(manualQuery));
        binding.searchButton.setOnClickListener(view -> openSearch(manualQuery));
        binding.detailButton.setOnClickListener(view -> openDetail());

        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.stateBox.setVisibility(View.GONE);

        OpenFoodFactsClient.fetch(barcode, new OpenFoodFactsClient.Callback() {
            @Override
            public void onProduct(Product product) {
                bindProduct(product);
            }

            @Override
            public void onNotFound(String value) {
                showState(R.string.product_not_found_title,
                        getString(R.string.product_not_found_body, value));
            }

            @Override
            public void onError(String value) {
                showState(R.string.product_error_title, getString(R.string.product_error_body));
            }
        });
    }

    private void bindProduct(Product product) {
        binding.progress.setVisibility(View.GONE);
        binding.stateBox.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.VISIBLE);

        binding.name.setText(product.getName());
        String brand = product.getBrand().trim();
        String quantity = product.getQuantity().trim();
        if (brand.isEmpty()) {
            brand = getString(R.string.product_brand_unknown);
        }
        binding.brand.setText(quantity.isEmpty() ? brand : brand + " · " + quantity);
        binding.barcodeLabel.setText(getString(R.string.product_barcode_label, product.getBarcode()));

        loadImage(product.getImageUrl());

        match = repository.findProductMatch(product.getName());
        manualQuery = match != null ? match.getName() : product.getName();

        IngredientAnalysis.Result analysis =
                IngredientAnalysis.analyze(repository.all(), product.getIngredientsText());
        List<IngredientAnalysis.Hit> warnings = analysis.getWarnings();

        binding.ingredientList.removeAllViews();
        if (analysis.isAvailable()) {
            binding.ingredientsCard.setVisibility(View.VISIBLE);
            binding.ingredientsText.setText(analysis.getText());
        } else {
            binding.ingredientsCard.setVisibility(View.GONE);
        }

        if (!warnings.isEmpty()) {
            bindWarnings(warnings);
        } else if (analysis.isAvailable()) {
            binding.levelBadge.setVisibility(View.VISIBLE);
            LevelStyle.applyClearBadge(this, binding.levelBadge);
            binding.matchName.setText(R.string.product_ingredients_none);
            binding.matchNote.setVisibility(View.VISIBLE);
            binding.matchNote.setText(R.string.product_ingredients_note);
        } else if (match != null) {
            binding.levelBadge.setVisibility(View.VISIBLE);
            LevelStyle.applyBadge(this, binding.levelBadge, match.getLevel(), true);
            binding.matchName.setText(match.getName());
            binding.matchNote.setVisibility(View.VISIBLE);
            binding.matchNote.setText(getString(R.string.product_match_note)
                    + "\n\n" + getString(R.string.product_ingredients_missing));
        } else {
            binding.levelBadge.setVisibility(View.GONE);
            binding.matchName.setText(R.string.product_no_match);
            binding.matchNote.setVisibility(View.VISIBLE);
            binding.matchNote.setText(R.string.product_ingredients_missing);
        }

        binding.detailButton.setVisibility(match != null ? View.VISIBLE : View.GONE);
        bindBaseFood(analysis.isAvailable(), warnings);
    }

    /** Pinta el veredicto a partir de los ingredientes y una fila por cada aviso. */
    private void bindWarnings(List<IngredientAnalysis.Hit> warnings) {
        binding.levelBadge.setVisibility(View.VISIBLE);
        // La lista viene ordenada: el primer aviso es el más grave.
        LevelStyle.applyBadge(this, binding.levelBadge, warnings.get(0).getLevel(), true);
        binding.matchName.setText(buildSummary(warnings));
        binding.matchNote.setVisibility(View.VISIBLE);
        binding.matchNote.setText(R.string.product_ingredients_note);

        for (IngredientAnalysis.Hit hit : warnings) {
            ItemIngredientBinding row = ItemIngredientBinding.inflate(
                    getLayoutInflater(), binding.ingredientList, false);

            Food food = hit.getFood();
            String emoji = food != null ? food.getEmoji() : "";
            row.emoji.setText(emoji == null || emoji.isEmpty() ? "⚠️" : emoji);
            row.name.setText(hit.getLabel());
            LevelStyle.applyBadge(this, row.badge, hit.getLevel(), false);

            String matched = hit.getMatched();
            if (!TextNormalizer.normalize(matched).equals(TextNormalizer.normalize(hit.getLabel()))) {
                row.matched.setVisibility(View.VISIBLE);
                row.matched.setText(getString(R.string.product_ingredient_matched, matched));
            }

            if (food != null) {
                row.getRoot().setOnClickListener(view -> openFood(food));
            } else {
                row.getRoot().setClickable(false);
                row.getRoot().setBackground(null);
            }
            binding.ingredientList.addView(row.getRoot());
        }
    }

    /** «Contiene cebolla, ajo y miel, que son altos en FODMAP.» */
    private String buildSummary(List<IngredientAnalysis.Hit> warnings) {
        StringBuilder summary = new StringBuilder();
        appendLevelSentence(summary, warnings, Food.LEVEL_HIGH,
                R.string.product_ingredients_high_one, R.string.product_ingredients_high_many);
        appendLevelSentence(summary, warnings, Food.LEVEL_MODERATE,
                R.string.product_ingredients_moderate_one,
                R.string.product_ingredients_moderate_many);
        return summary.toString();
    }

    private void appendLevelSentence(StringBuilder summary, List<IngredientAnalysis.Hit> warnings,
                                     String level, int oneRes, int manyRes) {
        List<String> labels = new ArrayList<>();
        for (IngredientAnalysis.Hit hit : warnings) {
            if (level.equals(hit.getLevel())) {
                labels.add(hit.getLabel());
            }
        }
        if (labels.isEmpty()) {
            return;
        }
        if (summary.length() > 0) {
            summary.append(' ');
        }
        summary.append(getString(labels.size() == 1 ? oneRes : manyRes, joinLabels(labels)));
    }

    /** Une con comas y «y» antes del último: «cebolla, ajo y miel». */
    private static String joinLabels(List<String> labels) {
        if (labels.size() == 1) {
            return labels.get(0);
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) {
                joined.append(i == labels.size() - 1 ? " y " : ", ");
            }
            joined.append(labels.get(i));
        }
        return joined.toString();
    }

    /** Muestra el alimento base si no es ya uno de los avisos. */
    private void bindBaseFood(boolean ingredientsAvailable, List<IngredientAnalysis.Hit> warnings) {
        boolean alreadyListed = false;
        if (match != null) {
            for (IngredientAnalysis.Hit hit : warnings) {
                if (hit.getFood() != null && hit.getFood().getId().equals(match.getId())) {
                    alreadyListed = true;
                    break;
                }
            }
        }
        if (!ingredientsAvailable || match == null || alreadyListed) {
            binding.baseFoodLabel.setVisibility(View.GONE);
            return;
        }
        binding.baseFoodLabel.setVisibility(View.VISIBLE);
        binding.baseFoodLabel.setText(getString(R.string.product_base_food, match.getName()));
    }

    private void loadImage(String url) {
        binding.image.setVisibility(View.INVISIBLE);
        binding.placeholder.setVisibility(View.VISIBLE);
        if (url.isEmpty()) {
            return;
        }
        Glide.with(this)
                .load(url)
                .centerInside()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException exception, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        binding.image.setVisibility(View.INVISIBLE);
                        binding.placeholder.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        binding.image.setVisibility(View.VISIBLE);
                        binding.placeholder.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(binding.image);
    }

    private void showState(int titleRes, String body) {
        binding.progress.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.stateBox.setVisibility(View.VISIBLE);
        binding.stateTitle.setText(titleRes);
        binding.stateBody.setText(body);
    }

    private void openDetail() {
        if (match == null) {
            return;
        }
        openFood(match);
    }

    private void openFood(Food food) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_FOOD_ID, food.getId());
        startActivity(intent);
    }

    private void openSearch(String query) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_QUERY, query == null ? "" : query);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
