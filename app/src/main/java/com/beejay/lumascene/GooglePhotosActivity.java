package com.beejay.lumascene;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Signs in, shows a QR code, and waits while photos are picked on the phone. */
public class GooglePhotosActivity extends Activity implements Google.Token {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView message;
    private ImageView code;
    private String token;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(Theme.NIGHT);
        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(20);
        message.setGravity(Gravity.CENTER);
        message.setPadding(48, 24, 48, 24);
        code = new ImageView(this);
        page.addView(code, new LinearLayout.LayoutParams(-2, -2));
        page.addView(message);
        setContentView(page);
        message.setText("Signing in to Google…");
        Google.authorize(this, this);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == Google.SIGN_IN) Google.signInResult(this, data, this);
    }

    @Override
    public void ok(String value) {
        token = value;
        message.setText("Starting Google Photos…");
        worker.execute(() -> {
            try {
                JSONObject session = Google.createSession(token);
                String id = session.getString("id");
                // "/autoclose" closes the phone page when picking is finished.
                Bitmap qr = qr(session.getString("pickerUri") + "/autoclose");
                main.post(() -> {
                    code.setImageBitmap(qr);
                    message.setText("Scan with your phone, using the same Google account as this Shield.\n"
                        + "Pick your photos, then tap Done.");
                });
                waitForPicks(id);
            } catch (Exception e) {
                problem(String.valueOf(e.getMessage()));
            }
        });
    }

    private void waitForPicks(String id) throws Exception {
        long end = System.currentTimeMillis() + 30 * 60 * 1000L;
        while (!isDestroyed() && System.currentTimeMillis() < end) {
            Thread.sleep(5000);
            if (!Google.session(id, token).optBoolean("mediaItemsSet")) continue;
            int count = Google.items(id, token).size();
            Prefs prefs = new Prefs(this);
            prefs.put("session", id);
            prefs.put("source", "google");
            main.post(() -> {
                code.setImageBitmap(null);
                message.setText(count + " items ready. Press Back, then Start Slideshow.");
            });
            return;
        }
        if (!isDestroyed()) problem("Timed out waiting. Press Back and try again.");
    }

    @Override
    public void problem(String text) {
        main.post(() -> {
            code.setImageBitmap(null);
            message.setText(text + "\n\nTake a photo of this screen, then press Back.");
        });
    }

    private static Bitmap qr(String text) throws Exception {
        int size = 420;
        BitMatrix bits = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++) out.setPixel(x, y, bits.get(x, y) ? Color.BLACK : Color.WHITE);
        return out;
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
