package deque;

public class ArrayDeque<T> {
    T[] items;
    int size;
    int nextFirst;
    int nextLast;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        nextFirst = 0;
        nextLast = 1;
    }

    public ArrayDeque(T x) {
        items = (T[]) new Object[8];
        items[0] = x;
        size = 1;
        nextFirst = 7;
        nextLast = 1;
    }

    public void resize(int capacity) {
        T[] a = (T[]) new Object[capacity];
        System.arraycopy(items, nextLast, a, 0, size - nextLast);
        System.arraycopy(items, 0, a, size - nextLast, nextLast);
        items = a;
    }

    public void addFirst(T x) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextFirst] = x;
        if (nextFirst == 0) {
            nextFirst = items.length - 1;
        } else {
            nextFirst -= 1;
        }
        size += 1;
    }

    public void addLast(T x) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextLast] = x;
        if (nextLast == items.length - 1) {
            nextLast = 0;
        } else {
            nextLast += 1;
        }
        size += 1;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        int i = nextFirst + 1;
        while (i != nextLast) {
            System.out.println(items[i] + " ");
            if (i == items.length - 1) {
                i = 0;
            } else {
                i += 1;
            }
        }
    }

    public T removeFirst() {
        if (size == 0) {
            return null;
        }
        if (nextFirst == items.length - 1) {
            nextFirst = 0;
        } else {
            nextFirst += 1;
        }
        T x = items[nextFirst];
        items[nextFirst] = null;
        size -= 1;
        return x;
    }

    public T removeLast() {
        if (size == 0) {
            return null;
        }
        if (nextLast == 0) {
            nextLast = items.length - 1;
        } else {
            nextLast -= 1;
        }
        T x = items[nextLast];
        items[nextLast] = null;
        size -= 1;
        return x;
    }

    public T get(int index) {
        int i = nextFirst + 1;
        for (int j = 0; j < index; j += 1) {
            if (i == items.length - 1) {
                i = 0;
            } else {
                i += 1;
            }
        }
        return items[i];
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof ArrayDeque)) {
            return false;
        }
        ArrayDeque<T> other = (ArrayDeque<T>) o;
        if (size != other.size) {
            return false;
        }
        int i = nextFirst + 1;
        int j = other.nextFirst + 1;
        for (int k = 0; k < size; k += 1) {
            if (i == items.length - 1) {
                i = 0;
            } else {
                i += 1;
            }
            if (j == other.items.length - 1) {
                j = 0;
            } else {
                j += 1;
            }
            if (!items[i].equals(other.items[j])) {
                return false;
            }
        }
        return true;
    }

}
