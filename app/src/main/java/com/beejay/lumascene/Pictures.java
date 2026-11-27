package com.beejay.lumascene;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Loads one item into memory for display. */
final class Pictures {
    Bitmap image;      // the photo, or a video's still frame
    Bitmap blurred;    // soft background copy
    String date;       // date taken, or null if the file doesn't say

    /** Returns null if the file can't be read. Call off the main thread. */
    static Pictures load(File file, int width, int height) {
        try {
            Pictures out = new Pictures();
            if (Library.isVideo(file.getName())) out.video(file);
            else out.photo(file, width, height);
            if (out.image != null) out.blurred = blur(out.image);
            else if (!Library.isVideo(file.getName())) return null;
            return out;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private void photo(File file, int width, int height) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        int sample = 1;
        // Twice the screen size is plenty, and keeps huge photos from running out of memory.
        while (options.outWidth / sample > width * 2 || options.outHeight / sample > height * 2) sample *= 2;
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap == null) return;

        ExifInterface exif = new ExifInterface(file.getPath());
        Matrix turn = new Matrix();
        switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            case ExifInterface.ORIENTATION_ROTATE_90: turn.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: turn.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: turn.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: turn.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: turn.postScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: turn.postRotate(90); turn.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: turn.postRotate(270); turn.postScale(-1, 1); break;
            default: break;
        }
        image = turn.isIdentity() ? bitmap
            : Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), turn, true);

        String taken = exif.getAttribute("DateTimeOriginal");
        if (taken == null) taken = exif.getAttribute(ExifInterface.TAG_DATETIME);
        date = pretty(taken, "yyyy:MM:dd HH:mm:ss", null);
    }

    private void video(File file) throws Exception {
        MediaMetadataRetriever info = new MediaMetadataRetriever();
        try {
            info.setDataSource(file.getPath());
            image = info.getFrameAtTime(0);
            String taken = info.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            if (taken != null && taken.length() >= 15 && !taken.startsWith("1904"))
                date = pretty(taken.substring(0, 15), "yyyyMMdd'T'HHmmss", TimeZone.getTimeZone("UTC"));
        } finally {
            info.release();
        }
    }

    private static String pretty(String value, String pattern, TimeZone zone) {
        if (value == null) return null;
        try {
            SimpleDateFormat in = new SimpleDateFormat(pattern, Locale.US);
            if (zone != null) in.setTimeZone(zone);
            in.setLenient(false);
            Date d = in.parse(value);
            return new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(d);
        } catch (ParseException e) {
            return null;
        }
    }

    /** Shrinking hard and letting the screen stretch it back up gives a soft blur. */
    private static Bitmap blur(Bitmap source) {
        int w = 48, h = Math.max(1, Math.round(48f * source.getHeight() / source.getWidth()));
        Bitmap half = Bitmap.createScaledBitmap(source, w * 4, h * 4, true);
        Bitmap small = Bitmap.createScaledBitmap(half, w, h, true);
        return Bitmap.createScaledBitmap(small, w * 4, h * 4, true);
    }
}
