package com.detectorfodmap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.detectorfodmap.app.R;
import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.data.FoodRepository;
import com.detectorfodmap.app.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "query";

    private ActivityMainBinding binding;
    private FoodRepository repository;
    private FoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.edgeToEdge(this, binding.getRoot());

        repository = FoodRepository.get(this);
        adapter = new FoodAdapter(this::openDetail);
        binding.foodList.setLayoutManager(new LinearLayoutManager(this));
        binding.foodList.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                showResults(text == null ? "" : text.toString());
            }
        });

        binding.scanButton.setOnClickListener(view ->
                startActivity(new Intent(this, ScanActivity.class)));

        String disclaimer = repository.getDisclaimer();
        binding.disclaimer.setText(disclaimer.isEmpty()
                ? getString(R.string.disclaimer)
                : disclaimer);

        applyQueryExtra(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyQueryExtra(intent);
    }

    private void applyQueryExtra(Intent intent) {
        String query = intent == null ? null : intent.getStringExtra(EXTRA_QUERY);
        if (query == null) {
            showResults("");
            return;
        }
        binding.searchInput.setText(query);
        binding.searchInput.setSelection(binding.searchInput.length());
        showResults(query);
    }

    private void showResults(String query) {
        List<Food> results = repository.search(query);
        adapter.submitList(results);

        boolean empty = results.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.foodList.setVisibility(empty ? View.GONE : View.VISIBLE);

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            binding.countLabel.setText(getString(R.string.count_all, results.size()));
        } else {
            binding.countLabel.setText(getString(R.string.count_results, results.size(), trimmed));
        }
        binding.emptyBody.setText(getString(R.string.empty_body, trimmed));
    }

    private void openDetail(Food food) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_FOOD_ID, food.getId());
        startActivity(intent);
    }
}
