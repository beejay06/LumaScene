package com.beejay.lumascene;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.Supplier;

/** Menu and settings. */
public class MainActivity extends Activity {
    private Prefs prefs;
    private Music music;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        music = new Music(this);
        music.command("use/" + prefs.music());

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.CENTER_HORIZONTAL);
        list.setPadding(0, dp(24), 0, dp(24));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Theme.NIGHT);
        getWindow().setStatusBarColor(Theme.NIGHT);
        scroll.addView(list);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("LumaScene");
        title.setTextSize(36);
        title.setTextColor(Color.WHITE);
        list.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Plug in a USB stick with photos and videos.");
        hint.setTextColor(Color.LTGRAY);
        hint.setPadding(0, 0, 0, dp(12));
        list.addView(hint);

        Button start = button(() -> "Start Slideshow", () -> startActivity(new Intent(this, SlideshowActivity.class)));
        button(() -> "Open " + musicName() + " to pick music", () -> music.command("open/" + prefs.music()));
        button(() -> "Music app: " + musicName(), () -> {
            prefs.put("music", prefs.music().equals("spotify") ? "tidal" : "spotify");
            music.command("use/" + prefs.music());
        });
        button(() -> "Shuffle: " + onOff(prefs.shuffle()), () -> prefs.put("shuffle", !prefs.shuffle()));
        button(() -> "Transition: " + (prefs.dissolve() ? "Dissolve" : "Fade through black"),
            () -> prefs.put("dissolve", !prefs.dissolve()));
        button(() -> "Time per photo: " + prefs.seconds() + " seconds",
            () -> prefs.put("seconds", Prefs.after(Prefs.SECONDS, prefs.seconds())));
        button(() -> "Videos: " + (prefs.videoCap() == 0 ? "play all the way through" : "stop after " + prefs.videoCap() + " seconds"),
            () -> prefs.put("videoCap", Prefs.after(Prefs.VIDEO_CAPS, prefs.videoCap())));
        button(() -> "Clock: " + onOff(prefs.clock()), () -> prefs.put("clock", !prefs.clock()));
        button(() -> "Date taken: " + onOff(prefs.date()), () -> prefs.put("date", !prefs.date()));
        button(() -> "Song title: " + onOff(prefs.song()), () -> prefs.put("song", !prefs.song()));
        button(() -> "Allow song titles (one-time permission)", () -> music.command("access"));
        start.requestFocus();
        askForPhotos();
    }

    private Button button(Supplier<String> label, Runnable action) {
        Button b = new Button(this);
        Theme.style(b);
        b.setText(label.get());
        b.setOnClickListener(v -> {
            action.run();
            // Labels can depend on each other (music app name), so refresh them all.
            for (int i = 0; i < list.getChildCount(); i++) {
                Object tag = list.getChildAt(i).getTag();
                if (tag != null) ((Runnable) tag).run();
            }
        });
        b.setTag((Runnable) () -> b.setText(label.get()));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT);
        size.setMargins(0, dp(4), 0, dp(4));
        list.addView(b, size);
        return b;
    }

    private void askForPhotos() {
        String[] wanted = Build.VERSION.SDK_INT >= 33
            ? new String[] {"android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"}
            : new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (checkSelfPermission(wanted[0]) != PackageManager.PERMISSION_GRANTED) requestPermissions(wanted, 1);
    }

    private String musicName() { return prefs.music().equals("tidal") ? "Tidal" : "Spotify"; }
    private static String onOff(boolean on) { return on ? "On" : "Off"; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
