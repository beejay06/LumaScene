package com.beejay.lumascene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Picks what to show next. Nothing repeats until everything in the list has been shown.
 * Stepping back and forward through history does not count as a new showing.
 */
public class ShuffleBag {
    private final List<String> items;
    private final Set<String> seen;
    private final List<String> history = new ArrayList<>();
    private final Random random;
    private final boolean shuffle;
    private int at = -1;

    public ShuffleBag(List<String> items, Set<String> seen, boolean shuffle, Random random) {
        this.items = new ArrayList<>(new LinkedHashSet<>(items));
        this.seen = new HashSet<>(seen);
        this.shuffle = shuffle;
        this.random = random;
    }

    public String next() {
        if (at < history.size() - 1) return history.get(++at);
        if (items.isEmpty()) return null;
        List<String> unseen = unseen();
        String last = at >= 0 ? history.get(at) : null;
        if (unseen.isEmpty()) {
            // New round. Entries for missing files stay, in case that USB stick comes back.
            seen.removeAll(items);
            unseen = unseen();
            if (unseen.size() > 1) unseen.remove(last); // no back-to-back repeat between rounds
        }
        String pick = unseen.get(shuffle ? random.nextInt(unseen.size()) : 0);
        seen.add(pick);
        history.add(pick);
        if (history.size() > 500) history.remove(0);
        at = history.size() - 1;
        return pick;
    }

    public String previous() {
        if (at > 0) at--;
        return at < 0 ? null : history.get(at);
    }

    public Set<String> seen() { return new HashSet<>(seen); }

    private List<String> unseen() {
        List<String> out = new ArrayList<>();
        for (String item : items) if (!seen.contains(item)) out.add(item);
        return out;
    }
}
