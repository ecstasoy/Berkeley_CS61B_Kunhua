package deque;

public class LinkedListDeque<T> {
    private class IntNode {
        public T item;
        public IntNode prev;
        public IntNode next;

        public IntNode(T i, IntNode p, IntNode n) {
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

    public LinkedListDeque(T x) {
        sentinel = new IntNode(null, null, null);
        sentinel.next = new IntNode(x, sentinel, sentinel);
        sentinel.prev = sentinel.next;
        size = 1;
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

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        IntNode curr = sentinel.next;

        while (curr != null) {
            System.out.println(curr.item + " ");
            curr = curr.next;
        }
    }

    public T removeFirst() {
        if (size != 0) {
            IntNode firstNode = sentinel.next;
            sentinel.next.next.prev = sentinel;
            sentinel.next = sentinel.next.next;
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

    public T getRecursive(int index, IntNode p) {
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

    public Iterator<T> iterator(){
        //TODO: The Deque objects we’ll make are iterable (i.e. Iterable<T>) so we must provide this method to return an iterator.
    }
}
