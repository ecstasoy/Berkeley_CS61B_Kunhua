package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {
    private Comparator<T> comparator;

    public MaxArrayDeque(Comparator<T> c) {
        super();
        if (c == null) {
            throw new IllegalArgumentException("Comparator cannot be null");
        }
        comparator = c;
    }

    public T max() {
        return max(comparator);
    }

    public T max(Comparator<T> c) {
        if (c == null) {
            throw new IllegalArgumentException("Comparator cannot be null");
        }
        if (size == 0) {
            return null;
        }
        int idx = (nextFirst == items.length - 1) ? 0 : nextFirst + 1;
        T max = items[idx];
        while (idx != nextLast) {
            if (c.compare(items[idx], max) > 0) {
                max = items[idx];
            }
            idx = (idx + 1) % items.length;
        }
        return max;
    }

}
