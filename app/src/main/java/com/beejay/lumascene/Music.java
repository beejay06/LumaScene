package com.beejay.lumascene;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Opens and controls the music app picked on screen. */
class Music {
    private static final Map<String, String> APPS = new HashMap<>();
    static {
        APPS.put("spotify", "com.spotify.tv.android");
        APPS.put("tidal", "com.aspiro.tidal");
    }
    private final Activity activity;
    private String chosen;

    Music(Activity activity) { this.activity = activity; }

    /** Handles links such as open/spotify or next. */
    void command(String action) {
        if (action.startsWith("open/")) open(action.substring(5));
        else if (action.startsWith("use/")) chosen = APPS.get(action.substring(4));
        else if (action.equals("access")) access();
        else if (action.equals("prev")) key(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        else if (action.equals("playpause")) key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        else if (action.equals("next")) key(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    private void open(String name) {
        String app = APPS.get(name);
        if (app == null) return;
        chosen = app;
        Intent intent = activity.getPackageManager().getLeanbackLaunchIntentForPackage(app);
        if (intent == null) intent = activity.getPackageManager().getLaunchIntentForPackage(app);
        if (intent == null) intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app));
        try { activity.startActivity(intent); }
        catch (Exception e) { toast("That music app isn't available on this device."); }
    }

    private void access() {
        try { activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
        catch (Exception e) { toast("Open Settings, Apps, Special app access, Notification access, then allow LumaScene."); }
    }

    private void toast(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }

    /** The chosen app's session, or null. Throws SecurityException without access. */
    private MediaController session() {
        MediaSessionManager manager = (MediaSessionManager) activity.getSystemService(Activity.MEDIA_SESSION_SERVICE);
        List<MediaController> all = manager.getActiveSessions(new ComponentName(activity, MusicAccess.class));
        for (MediaController c : all) if (c.getPackageName().equals(chosen)) return c;
        return all.isEmpty() ? null : all.get(0);
    }

    private void key(int code) {
        KeyEvent down = new KeyEvent(KeyEvent.ACTION_DOWN, code), up = new KeyEvent(KeyEvent.ACTION_UP, code);
        try {
            MediaController c = session();
            if (c != null) { c.dispatchMediaButtonEvent(down); c.dispatchMediaButtonEvent(up); return; }
        } catch (SecurityException ignored) { }
        AudioManager audio = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
        audio.dispatchMediaKeyEvent(down);
        audio.dispatchMediaKeyEvent(up);
    }

    String nowPlaying() {
        JSONObject out = new JSONObject();
        try {
            try {
                MediaController c = session();
                out.put("access", true);
                MediaMetadata m = c == null ? null : c.getMetadata();
                if (m != null) {
                    out.put("title", m.getString(MediaMetadata.METADATA_KEY_TITLE));
                    out.put("artist", m.getString(MediaMetadata.METADATA_KEY_ARTIST));
                }
                PlaybackState p = c == null ? null : c.getPlaybackState();
                out.put("playing", p != null && p.getState() == PlaybackState.STATE_PLAYING);
            } catch (SecurityException e) {
                out.put("access", false);
            }
        } catch (JSONException ignored) { }
        return out.toString();
    }
}
