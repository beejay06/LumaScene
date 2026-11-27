package com.beejay.lumascene;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Finds photos and videos on USB sticks. Nothing is copied to the Shield. */
final class Library {
    private Library() { }

    /** Key (stable if the stick is re-plugged) to item. */
    static Map<String, Item> scan(Context context) {
        Map<String, File> found = new LinkedHashMap<>();
        for (File root : usbRoots(context)) walk(root, root, found, 0);
        Map<String, Item> out = new LinkedHashMap<>();
        for (Map.Entry<String, File> e : found.entrySet())
            out.put(e.getKey(), Item.usb(e.getValue(), isVideo(e.getValue().getName())));
        return out;
    }

    private static List<File> usbRoots(Context context) {
        List<File> roots = new ArrayList<>();
        for (File dir : context.getExternalFilesDirs(null)) {
            if (dir == null || !Environment.isExternalStorageRemovable(dir)) continue;
            String path = dir.getAbsolutePath();
            int cut = path.indexOf("/Android/");
            if (cut > 0) roots.add(new File(path.substring(0, cut)));
        }
        // Some USB drives only show up here.
        File[] mounted = new File("/storage").listFiles();
        if (mounted != null) {
            for (File dir : mounted) {
                String name = dir.getName();
                if (!name.equals("emulated") && !name.equals("self") && dir.canRead() && !roots.contains(dir)) roots.add(dir);
            }
        }
        return roots;
    }

    private static void walk(File root, File dir, Map<String, File> out, int depth) {
        File[] files = dir.listFiles();
        if (files == null || depth > 12) return;
        List<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, files);
        Collections.sort(sorted);
        for (File f : sorted) {
            String name = f.getName();
            if (name.startsWith(".") || name.equals("Android")) continue;
            if (f.isDirectory()) walk(root, f, out, depth + 1);
            else if (isPhoto(name) || isVideo(name))
                out.put(f.getAbsolutePath().substring(root.getAbsolutePath().length()) + "|" + f.length(), f);
        }
    }

    static boolean isPhoto(String name) {
        return name.toLowerCase(Locale.US).matches(".*\\.(jpg|jpeg|png|webp|gif|bmp|heic|heif)");
    }

    static boolean isVideo(String name) {
        return name.toLowerCase(Locale.US).matches(".*\\.(mp4|m4v|mov|mkv|webm|3gp)");
    }
}
