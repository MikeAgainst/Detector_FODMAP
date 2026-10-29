package com.detectorfodmap.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.detectorfodmap.app.R;
import com.detectorfodmap.app.data.Food;

/** Aplica el color y el texto del nivel FODMAP a una insignia. */
public final class LevelStyle {

    private LevelStyle() {
    }

    public static void applyBadge(Context context, TextView view, String level, boolean longLabel) {
        boolean high = Food.LEVEL_HIGH.equals(level);
        boolean moderate = Food.LEVEL_MODERATE.equals(level);

        int background = high ? R.color.high_bg : moderate ? R.color.moderate_bg : R.color.low_bg;
        int foreground = high ? R.color.high_fg : moderate ? R.color.moderate_fg : R.color.low_fg;
        int shortLabel = high ? R.string.level_high : moderate ? R.string.level_moderate : R.string.level_low;
        int longText = high ? R.string.level_high_long : moderate ? R.string.level_moderate_long : R.string.level_low_long;

        view.setText(context.getString(longLabel ? longText : shortLabel));
        view.setTextColor(ContextCompat.getColor(context, foreground));
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
    }

    /** Insignia verde para un producto del que no hay nada que avisar. */
    public static void applyClearBadge(Context context, TextView view) {
        view.setText(R.string.product_no_alerts);
        view.setTextColor(ContextCompat.getColor(context, R.color.low_fg));
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.low_bg)));
    }
}
