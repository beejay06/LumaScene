import com.beejay.lumascene.ShuffleBag;
import java.util.*;

public class ShuffleBagTest {
    static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }

    static List<String> names(int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add("p" + i);
        return out;
    }

    public static void main(String[] args) {
        for (int seed = 0; seed < 200; seed++) {
            ShuffleBag bag = new ShuffleBag(names(7), new HashSet<>(), true, new Random(seed));
            String last = null;
            for (int round = 0; round < 4; round++) {
                Set<String> got = new HashSet<>();
                for (int i = 0; i < 7; i++) {
                    String p = bag.next();
                    check(got.add(p), "repeat inside a round");
                    check(!p.equals(last), "back-to-back repeat");
                    last = p;
                }
            }
        }
        // Progress survives a restart, and new items join the unseen ones.
        ShuffleBag first = new ShuffleBag(names(5), new HashSet<>(), true, new Random(1));
        Set<String> shown = new HashSet<>(Arrays.asList(first.next(), first.next(), first.next()));
        ShuffleBag second = new ShuffleBag(names(8), first.seen(), true, new Random(2));
        for (int i = 0; i < 5; i++) check(shown.add(second.next()), "repeat after restart");
        check(shown.size() == 8, "not everything shown");
        // Back then forward replays history without using up unseen items.
        ShuffleBag b = new ShuffleBag(names(4), new HashSet<>(), true, new Random(3));
        String a = b.next(), c = b.next();
        check(b.previous().equals(a), "previous");
        check(b.next().equals(c), "forward again");
        check(b.seen().size() == 2, "history changed progress");
        // One item and no items.
        ShuffleBag one = new ShuffleBag(names(1), new HashSet<>(), true, new Random(4));
        check(one.next().equals("p0") && one.next().equals("p0"), "single item");
        check(new ShuffleBag(names(0), new HashSet<>(), true, new Random()).next() == null, "empty");
        // Shuffle off keeps list order.
        ShuffleBag ordered = new ShuffleBag(names(3), new HashSet<>(), false, new Random());
        check(ordered.next().equals("p0") && ordered.next().equals("p1"), "order");
        System.out.println("shuffle tests passed");
    }
}
