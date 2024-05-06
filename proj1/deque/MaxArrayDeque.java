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
        if (isEmpty()) {
            return null;
        }
        int idx = (getNextFirst() == getItemsLength() - 1) ? 0 : getNextFirst() + 1;
        T max = get(idx);
        while (idx != getNextLast()) {
            if (c.compare(get(idx), max) > 0) {
                max = get(idx);
            }
            idx = (idx + 1) % getItemsLength();
        }
        return max;
    }

}
