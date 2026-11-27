package com.beejay.lumascene;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** The show. Left/Right change photos, OK shows music controls, Back returns to the menu. */
public class SlideshowActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable advance = () -> step(true);
    private final Runnable hideControls = () -> showControls(false);

    private Prefs prefs;
    private Music music;
    private Map<String, File> files;
    private ShuffleBag bag;
    private FrameLayout stage;
    private Slide current;
    private LinearLayout controls;
    private TextView dateText, songText;
    private Button playButton;
    private int request;      // newest load wins
    private int failures;
    private boolean visible;
    private boolean loading;   // presses wait, so nothing counts as seen without being shown

    private final Runnable songTick = new Runnable() {
        @Override
        public void run() {
            updateSong();
            main.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = new Prefs(this);
        music = new Music(this);
        music.command("use/" + prefs.music());

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        stage = new FrameLayout(this);
        root.addView(stage);

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        style(clock, 28);
        clock.setVisibility(prefs.clock() ? View.VISIBLE : View.GONE);
        root.addView(clock, corner(Gravity.TOP | Gravity.END));
        dateText = new TextView(this);
        style(dateText, 20);
        root.addView(dateText, corner(Gravity.BOTTOM | Gravity.START));
        songText = new TextView(this);
        style(songText, 20);
        songText.setMaxWidth(getResources().getDisplayMetrics().widthPixels / 2);
        songText.setGravity(Gravity.END);
        root.addView(songText, corner(Gravity.BOTTOM | Gravity.END));

        controls = new LinearLayout(this);
        control("Previous track", "prev");
        playButton = control("Play / Pause", "playpause");
        control("Next track", "next");
        controls.setVisibility(View.GONE);
        root.addView(controls, corner(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        setContentView(root);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        worker.execute(() -> {
            Map<String, File> found = Library.scan(this);
            main.post(() -> ready(found));
        });
    }

    private void ready(Map<String, File> found) {
        if (isDestroyed()) return;
        files = found;
        if (found.isEmpty()) {
            Toast.makeText(this, "No photos or videos found on a USB stick.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        bag = new ShuffleBag(new ArrayList<>(found.keySet()), prefs.seen(), prefs.shuffle(), new Random());
        if (visible) step(true);
    }

    private void step(boolean forward) {
        if (bag == null || isDestroyed()) return;
        main.removeCallbacks(advance);
        String key = forward ? bag.next() : bag.previous();
        File file = key == null ? null : files.get(key);
        if (file == null) return;
        int mine = ++request;
        loading = true;
        DisplayMetrics screen = getResources().getDisplayMetrics();
        worker.execute(() -> {
            if (mine != request) return;
            Pictures loaded = Pictures.load(file, screen.widthPixels, screen.heightPixels);
            main.post(() -> {
                if (mine != request || isDestroyed() || !visible) return;
                if (loaded == null) {
                    prefs.seen(bag.seen());
                    if (++failures >= 20) {
                        Toast.makeText(this, "Can't read these files. Check the USB stick.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        step(forward);
                    }
                    return;
                }
                failures = 0;
                loading = false;
                show(loaded, file);
                prefs.seen(bag.seen());
            });
        });
    }

    private void show(Pictures loaded, File file) {
        boolean video = Library.isVideo(file.getName());
        int mine = request;
        Runnable done = () -> { if (mine == request) step(true); };
        Slide next = new Slide(this, loaded, video ? file : null, done);
        Slide old = current;
        current = next;
        // Anything left over from a fast double-press goes now.
        for (int i = stage.getChildCount() - 1; i >= 0; i--) {
            View child = stage.getChildAt(i);
            if (child != old) remove((Slide) child);
        }
        next.setAlpha(0);
        stage.addView(next);
        if (old == null) {
            next.animate().alpha(1).setDuration(800);
        } else if (prefs.dissolve()) {
            next.animate().alpha(1).setStartDelay(0).setDuration(1200).withEndAction(() -> remove(old));
        } else {
            old.animate().alpha(0).setStartDelay(0).setDuration(700).withEndAction(() -> remove(old));
            next.animate().alpha(1).setStartDelay(700).setDuration(700);
        }
        dateText.setText(prefs.date() && loaded.date != null ? loaded.date : "");

        if (!video) main.postDelayed(advance, prefs.seconds() * 1000L);
        else if (prefs.videoCap() > 0) main.postDelayed(advance, prefs.videoCap() * 1000L);
    }

    private void remove(Slide slide) {
        slide.animate().cancel();
        slide.release();
        stage.removeView(slide);
    }

    private void updateSong() {
        try {
            JSONObject now = new JSONObject(music.nowPlaying());
            String title = now.optString("title", "");
            String artist = now.optString("artist", "");
            String line = artist.isEmpty() ? title : title + "  •  " + artist;
            songText.setText(prefs.song() && !title.isEmpty() && controls.getVisibility() != View.VISIBLE ? line : "");
            if (now.optBoolean("access")) playButton.setText(now.optBoolean("playing") ? "Pause" : "Play");
        } catch (Exception ignored) { }
    }

    private Button control(String label, String command) {
        Button b = new Button(this);
        Theme.style(b);
        b.setText(label);
        b.setOnClickListener(v -> {
            music.command(command);
            keepControls();
            main.postDelayed(this::updateSong, 400);
        });
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-2, -2);
        int gap = Math.round(6 * getResources().getDisplayMetrics().density);
        size.setMargins(gap, 0, gap, 0);
        controls.addView(b, size);
        return b;
    }

    private void showControls(boolean show) {
        controls.setVisibility(show ? View.VISIBLE : View.GONE);
        main.removeCallbacks(hideControls);
        if (show) {
            playButton.requestFocus();
            keepControls();
        }
        updateSong();
    }

    private void keepControls() {
        main.removeCallbacks(hideControls);
        main.postDelayed(hideControls, 6000);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER;
        if (controls.getVisibility() == View.VISIBLE) {
            if (key == KeyEvent.KEYCODE_BACK) {
                if (!down) showControls(false);
                return true;
            }
            if (down) keepControls();
            return super.dispatchKeyEvent(event);
        }
        if (ok) {
            if (!down) showControls(true); // on release, so the press doesn't also click a button
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (down && event.getRepeatCount() == 0 && !loading) step(key == KeyEvent.KEYCODE_DPAD_RIGHT);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible = true;
        main.post(songTick);
        if (bag != null) step(true);
    }

    @Override
    protected void onStop() {
        visible = false;
        loading = false;
        request++;
        main.removeCallbacksAndMessages(null);
        for (int i = stage.getChildCount() - 1; i >= 0; i--) remove((Slide) stage.getChildAt(i));
        current = null;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void style(TextView view, int size) {
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setShadowLayer(6, 0, 2, Color.BLACK);
    }

    private FrameLayout.LayoutParams corner(int gravity) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-2, -2, gravity);
        int margin = Math.round(32 * getResources().getDisplayMetrics().density);
        p.setMargins(margin, margin, margin, margin);
        return p;
    }
}
