package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        nextFirst = 0;
        nextLast = 1;
    }

    public int getItemsLength() {
        return items.length;
    }

    public int getNextFirst() {
        return nextFirst;
    }

    public int getNextLast() {
        return nextLast;
    }

    private void resize(int capacity) {
        T[] a = (T[]) new Object[capacity];
        System.arraycopy(items, nextLast, a, 0, size - nextLast);
        System.arraycopy(items, 0, a, size - nextLast, nextLast);
        items = a;
        nextLast = size;
        nextFirst = capacity - 1;
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
        int i;
        if (nextFirst == items.length - 1) {
            i = 0;
        } else {
            i = nextFirst + 1;
        }
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
        if (this == o) {
            return true;
        }
        Iterator<T> thisIter = this.iterator();
        if (o instanceof ArrayDeque) {
            Iterator<T> oIter = ((ArrayDeque<T>) o).iterator();
            if (this.size() != ((ArrayDeque<T>) o).size()) {
                return false;
            }
            while (thisIter.hasNext()) {
                if (!thisIter.next().equals(oIter.next())) {
                    return false;
                }
            }
        } else if (o instanceof LinkedListDeque) {
            Iterator<T> oIter = ((LinkedListDeque<T>) o).iterator();
            if (this.size() != ((LinkedListDeque<T>) o).size()) {
                return false;
            }
            while (thisIter.hasNext()) {
                if (!thisIter.next().equals(oIter.next())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    public Iterator<T> iterator() {
        return new ADequeIterator();
    }

    private class ADequeIterator implements Iterator<T> {
        private int wizPos;

        ADequeIterator() {
            wizPos = 0;
        }

        public boolean hasNext() {
            return wizPos < size;
        }

        public T next() {
            T returnItem = get(wizPos);
            wizPos += 1;
            return returnItem;
        }
    }

}
