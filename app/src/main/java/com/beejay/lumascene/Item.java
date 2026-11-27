package com.beejay.lumascene;

import java.io.File;

/** One photo or video: either a file on a USB stick or a temporary Google link. */
final class Item {
    final File file;     // USB only
    final String url;    // Google only
    final String taken;  // Google only, yyyy-MM-dd or null
    final boolean video;

    private Item(File file, String url, String taken, boolean video) {
        this.file = file;
        this.url = url;
        this.taken = taken;
        this.video = video;
    }

    static Item usb(File file, boolean video) { return new Item(file, null, null, video); }
    static Item cloud(String url, String taken, boolean video) { return new Item(null, url, taken, video); }
}
