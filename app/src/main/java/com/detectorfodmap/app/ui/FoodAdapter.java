package com.detectorfodmap.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.databinding.ItemFoodBinding;

public class FoodAdapter extends ListAdapter<Food, FoodAdapter.FoodViewHolder> {

    public interface OnFoodClickListener {
        void onFoodClick(Food food);
    }

    private static final DiffUtil.ItemCallback<Food> DIFF_CALLBACK = new DiffUtil.ItemCallback<Food>() {
        @Override
        public boolean areItemsTheSame(@NonNull Food oldItem, @NonNull Food newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Food oldItem, @NonNull Food newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getLevel().equals(newItem.getLevel());
        }
    };

    private final OnFoodClickListener listener;

    public FoodAdapter(OnFoodClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodBinding binding = ItemFoodBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FoodViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    final class FoodViewHolder extends RecyclerView.ViewHolder {

        private final ItemFoodBinding binding;

        FoodViewHolder(ItemFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Food food) {
            binding.name.setText(food.getName());
            binding.category.setText(food.getCategory());
            LevelStyle.applyBadge(binding.getRoot().getContext(), binding.badge, food.getLevel(), false);
            FoodImages.bind(binding.thumb, binding.emoji, food);
            binding.getRoot().setOnClickListener(view -> listener.onFoodClick(food));
        }
    }
}
