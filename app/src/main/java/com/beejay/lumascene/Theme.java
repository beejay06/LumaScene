package com.beejay.lumascene;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;

/** The app's colors and button look. */
final class Theme {
    static final int NIGHT = Color.parseColor("#10121E");
    static final int SLATE = Color.parseColor("#2F334C");
    static final int CORAL = Color.parseColor("#FA4A56");
    static final int PURPLE = Color.parseColor("#9562AF");

    private Theme() { }

    /** Slate card that lights up coral-to-purple when the remote is on it. */
    static void style(Button button) {
        float d = button.getResources().getDisplayMetrics().density;
        GradientDrawable lit = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {CORAL, PURPLE});
        lit.setCornerRadius(14 * d);
        GradientDrawable plain = new GradientDrawable();
        plain.setColor(SLATE);
        plain.setCornerRadius(14 * d);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, lit);
        states.addState(new int[] {android.R.attr.state_pressed}, lit);
        states.addState(new int[0], plain);
        button.setBackground(states);
        button.setStateListAnimator(null);
        button.setTextColor(ColorStateList.valueOf(Color.WHITE));
        button.setAllCaps(false);
        button.setPadding((int) (20 * d), (int) (10 * d), (int) (20 * d), (int) (10 * d));
    }
}
