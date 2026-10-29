package com.detectorfodmap.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.detectorfodmap.app.R;
import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.data.FoodRepository;
import com.detectorfodmap.app.databinding.ActivityDetailBinding;
import com.google.android.material.chip.Chip;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "food_id";

    private ActivityDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.edgeToEdge(this, binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        String id = getIntent().getStringExtra(EXTRA_FOOD_ID);
        Food food = id == null ? null : FoodRepository.get(this).byId(id);
        if (food == null) {
            finish();
            return;
        }

        binding.name.setText(food.getName());
        binding.category.setText(food.getCategory());
        LevelStyle.applyBadge(this, binding.badge, food.getLevel(), true);
        binding.portion.setText(food.getPortion());
        binding.note.setText(food.getNote().isEmpty() ? getString(R.string.detail_no_note) : food.getNote());
        binding.photo.setContentDescription(food.getName());

        binding.fodmapChips.removeAllViews();
        if (food.getFodmaps().isEmpty()) {
            binding.fodmapChips.addView(createChip(getString(R.string.detail_no_fodmaps)));
        } else {
            for (String fodmap : food.getFodmaps()) {
                binding.fodmapChips.addView(createChip(fodmap));
            }
        }

        FoodImages.bind(binding.photo, binding.emoji, food);
    }

    private Chip createChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setChipStrokeWidth(0f);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipBackgroundColorResource(R.color.surface_variant);
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        return chip;
    }
}
