package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {
    private class IntNode {
        private T item;
        private IntNode prev;
        private IntNode next;

        IntNode(T i, IntNode p, IntNode n) {
            item = i;
            prev = p;
            next = n;
        }
    }

    private IntNode sentinel;
    private int size;

    public LinkedListDeque() {
        sentinel = new IntNode(null, null, null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    public void addFirst(T x) {
        sentinel.next = new IntNode(x, sentinel, sentinel.next);
        sentinel.next.next.prev = sentinel.next;
        size += 1;
    }

    public void addLast(T x) {
        sentinel.prev = new IntNode(x, sentinel.prev, sentinel);
        sentinel.prev.prev.next = sentinel.prev;
        size += 1;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        IntNode curr = sentinel.next;

        while (curr != sentinel) {
            System.out.println(curr.item + " ");
            curr = curr.next;
        }
    }

    public T removeFirst() {
        if (size != 0) {
            IntNode firstNode = sentinel.next;
            sentinel.next.next.prev = sentinel;
            sentinel.next = sentinel.next.next;
            size -= 1;
            return firstNode.item;
        } else {
            return null;
        }
    }

    public T removeLast() {
        if (size != 0) {
            IntNode lastNode = sentinel.prev;
            sentinel.prev.prev.next = sentinel;
            sentinel.prev = sentinel.prev.prev;
            size -= 1;
            return lastNode.item;
        } else {
            return null;
        }
    }

    public T get(int index) {
        IntNode curr = sentinel.next;
        for (int i = 0; i < index; i += 1) {
            curr = curr.next;
        }
        return curr.item;
    }

    public T getRecursive(int index) {
        return getRecursive(index, sentinel.next);
    }

    private T getRecursive(int index, IntNode p) {
        if (index == 0) {
            return p.item;
        }
        return getRecursive(index - 1, p.next);
    }

    public boolean equals(Object o) {
        if (!(o instanceof LinkedListDeque)) {
            return false;
        }
        LinkedListDeque<T> other = (LinkedListDeque<T>) o;
        if (size != other.size) {
            return false;
        }
        IntNode p1 = sentinel.next;
        IntNode p2 = other.sentinel.next;
        while (p1 != sentinel && p2 != other.sentinel) {
            if (!p1.item.equals(p2.item)) {
                return false;
            }
            p1 = p1.next;
            p2 = p2.next;
        }
        return true;
    }

    public Iterator<T> iterator() {
        return new LLDequeIterator();
    }

    private class LLDequeIterator implements Iterator<T> {
        private int wizPos;
        LLDequeIterator() {
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
