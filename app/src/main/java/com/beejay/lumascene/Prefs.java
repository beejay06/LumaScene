package com.beejay.lumascene;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Settings and shuffle progress. No pictures. */
final class Prefs {
    static final int[] SECONDS = {5, 10, 15, 30, 60};
    static final int[] VIDEO_CAPS = {0, 15, 30, 60}; // 0 = play all the way through
    private final SharedPreferences p;

    Prefs(Context context) { p = context.getSharedPreferences("lumascene", Context.MODE_PRIVATE); }

    int seconds() { return p.getInt("seconds", 10); }
    int videoCap() { return p.getInt("videoCap", 0); }
    boolean shuffle() { return p.getBoolean("shuffle", true); }
    boolean dissolve() { return p.getBoolean("dissolve", true); }
    boolean clock() { return p.getBoolean("clock", true); }
    boolean date() { return p.getBoolean("date", true); }
    boolean song() { return p.getBoolean("song", true); }
    String music() { return p.getString("music", "spotify"); }
    Set<String> seen() { return new HashSet<>(p.getStringSet("seen", new HashSet<>())); }

    void put(String key, int value) { p.edit().putInt(key, value).apply(); }
    void put(String key, boolean value) { p.edit().putBoolean(key, value).apply(); }
    void put(String key, String value) { p.edit().putString(key, value).apply(); }
    void seen(Set<String> value) { p.edit().putStringSet("seen", value).apply(); }

    static int after(int[] choices, int now) {
        for (int i = 0; i < choices.length; i++) if (choices[i] == now) return choices[(i + 1) % choices.length];
        return choices[0];
    }
}
