package com.beejay.lumascene;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Sign-in with the Shield's Google account, and the Google Photos Picker API. */
final class Google {
    static final int SIGN_IN = 40;
    private static final String API = "https://photospicker.googleapis.com/v1/";

    private Google() { }

    interface Token {
        void ok(String token);
        void problem(String message);
    }

    private static AuthorizationRequest request() {
        return AuthorizationRequest.builder()
            .setRequestedScopes(Collections.singletonList(
                new Scope("https://www.googleapis.com/auth/photospicker.mediaitems.readonly")))
            .build();
    }

    /** Asks for access. If Google needs to show a screen, the answer arrives in onActivityResult. */
    static void authorize(Activity activity, Token then) {
        Identity.getAuthorizationClient(activity).authorize(request())
            .addOnSuccessListener(activity, result -> {
                if (!result.hasResolution()) {
                    deliver(result, then);
                    return;
                }
                try {
                    activity.startIntentSenderForResult(result.getPendingIntent().getIntentSender(), SIGN_IN, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    then.problem("Couldn't open Google sign-in: " + e.getMessage());
                }
            })
            .addOnFailureListener(activity, e -> then.problem("Google sign-in failed: " + e.getMessage()));
    }

    static void signInResult(Activity activity, Intent data, Token then) {
        try {
            deliver(Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data), then);
        } catch (ApiException e) {
            then.problem("Google sign-in failed (code " + e.getStatusCode() + "): " + e.getMessage());
        }
    }

    private static void deliver(AuthorizationResult result, Token then) {
        if (result.getAccessToken() == null) then.problem("Google sign-in gave no access.");
        else then.ok(result.getAccessToken());
    }

    /** For refreshing during the show. Call off the main thread. */
    static String silentToken(Context context) throws Exception {
        AuthorizationResult result = Tasks.await(
            Identity.getAuthorizationClient(context).authorize(request()), 30, TimeUnit.SECONDS);
        if (result.getAccessToken() == null) throw new IOException("sign in again");
        return result.getAccessToken();
    }

    static JSONObject createSession(String token) throws Exception {
        return json(API + "sessions", "POST", token, "{}");
    }

    static JSONObject session(String id, String token) throws Exception {
        return json(API + "sessions/" + id, "GET", token, null);
    }

    /** Everything picked in a session. */
    static Map<String, Item> items(String session, String token) throws Exception {
        Map<String, Item> out = new LinkedHashMap<>();
        if (session.isEmpty()) return out;
        String page = "";
        do {
            JSONObject reply = json(API + "mediaItems?pageSize=100&sessionId=" + session
                + (page.isEmpty() ? "" : "&pageToken=" + URLEncoder.encode(page, "UTF-8")), "GET", token, null);
            JSONArray list = reply.optJSONArray("mediaItems");
            for (int i = 0; list != null && i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                String url = entry.getJSONObject("mediaFile").getString("baseUrl");
                String created = entry.optString("createTime", "");
                out.put("gp:" + entry.getString("id"), Item.cloud(url,
                    created.length() >= 10 ? created.substring(0, 10) : null, "VIDEO".equals(entry.optString("type"))));
            }
            page = reply.optString("nextPageToken", "");
        } while (!page.isEmpty());
        return out;
    }

    static byte[] bytes(String url, String token) throws IOException {
        HttpURLConnection c = open(url, token);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            return read(c.getInputStream());
        } finally {
            c.disconnect();
        }
    }

    private static JSONObject json(String url, String method, String token, String body) throws Exception {
        HttpURLConnection c = open(url, token);
        try {
            c.setRequestMethod(method);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            InputStream in = code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = in == null ? "" : new String(read(in), StandardCharsets.UTF_8);
            if (code >= 300) {
                String why = text;
                try { why = new JSONObject(text).getJSONObject("error").getString("message"); } catch (Exception ignored) { }
                throw new IOException("Google said " + code + ": " + why);
            }
            return new JSONObject(text);
        } finally {
            c.disconnect();
        }
    }

    private static HttpURLConnection open(String url, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Authorization", "Bearer " + token);
        return c;
    }

    private static byte[] read(InputStream in) throws IOException {
        try (InputStream closing = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            for (int n; (n = closing.read(buffer)) > 0; ) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }
}
