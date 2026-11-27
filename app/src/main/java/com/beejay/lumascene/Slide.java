package com.beejay.lumascene;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;

/**
 * One full-screen photo or muted video over its blurred background.
 * Uses MediaPlayer directly: it never asks for audio focus, so music keeps playing.
 */
final class Slide extends FrameLayout {
    private MediaPlayer player;
    private Surface surface;

    Slide(Context context, Pictures pictures, File video, Runnable onDone) {
        super(context);
        setBackgroundColor(Color.BLACK);
        if (pictures.blurred != null) {
            ImageView back = new ImageView(context);
            back.setScaleType(ImageView.ScaleType.CENTER_CROP);
            back.setImageBitmap(pictures.blurred);
            back.setColorFilter(Color.argb(90, 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_ATOP);
            addView(back, new LayoutParams(-1, -1));
        }
        ImageView front = new ImageView(context);
        front.setScaleType(ImageView.ScaleType.FIT_CENTER);
        front.setImageBitmap(pictures.image);
        if (video == null) {
            addView(front, new LayoutParams(-1, -1));
            return;
        }
        TextureView screen = new TextureView(context);
        screen.setOpaque(false);
        addView(screen, new LayoutParams(-1, -1, Gravity.CENTER));
        addView(front, new LayoutParams(-1, -1)); // still frame until the video starts
        screen.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture texture, int w, int h) {
                try {
                    surface = new Surface(texture);
                    player = new MediaPlayer();
                    player.setDataSource(video.getPath());
                    player.setSurface(surface);
                    player.setVolume(0, 0);
                    player.setOnVideoSizeChangedListener((p, vw, vh) -> fit(screen, vw, vh));
                    player.setOnPreparedListener(p -> {
                        p.start();
                        front.postDelayed(() -> front.setVisibility(INVISIBLE), 200);
                    });
                    player.setOnCompletionListener(p -> onDone.run());
                    player.setOnErrorListener((p, what, extra) -> { onDone.run(); return true; });
                    player.prepareAsync();
                } catch (Exception e) {
                    post(onDone);
                }
            }

            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture t, int w, int h) { }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture t) { release(); return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture t) { }
        });
    }

    /** Keeps the video's shape inside the screen. */
    private static void fit(TextureView screen, int videoWidth, int videoHeight) {
        float w = screen.getWidth(), h = screen.getHeight();
        if (videoWidth == 0 || videoHeight == 0 || w == 0 || h == 0) return;
        float scale = Math.min(w / videoWidth, h / videoHeight);
        Matrix m = new Matrix();
        m.setScale(videoWidth * scale / w, videoHeight * scale / h, w / 2, h / 2);
        screen.setTransform(m);
    }

    void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }
}
