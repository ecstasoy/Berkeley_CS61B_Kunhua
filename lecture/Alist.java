public class Alist {
    private int[] items;
    private int size;

    public Alist() {
        items = new int[100];
        size = 0;
    }

    public void addLast(int x) {
        if (size == items.length) {
            int[] a = new int[size + 1];
            System.arraycopy(items, 0, a, 0, size);
            items = a;
        }
        items[size] = x;
        size += 1;
    }

    public int getLast() {
        return items[size - 1];
    }

    public int get(int i) {
        return items[i];
    }

    public int size() {
        return size;
    }

    public int removeLast() {
        int last = getLast();
        size -= 1;
        return last;
    }
}
