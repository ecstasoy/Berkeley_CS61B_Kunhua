package deque;

import java.util.Comparator;
import java.util.Iterator;

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
        Iterator<T> iter = iterator();
        T max = iter.next();
        while (iter.hasNext()) {
            T current = iter.next();
            if (c.compare(current, max) > 0) {
                max = current;
            }
        }
        return max;
    }

}
