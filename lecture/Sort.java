public class Sort {
    /** Sorts strings destructively. */
    public static void sort(String[] x) {
        // Find the smallest item.
        // Move it to the front.
        // Selection sort the rest (using recursion, perhaps).
        sort(x, 0);
    }

    /** Sort x starting at position start. */
    private static void sort(String[] x, int start) {
        if (start == x.length) {
            return;
        }
        int smallest = findSmallest(x, start);
         swap(x, start, smallest);
        sort(x, start + 1);
    }

    /** Returns the smallest string in x. */
    public static int findSmallest(String[] x, int start) {
        int smallestIndex = start;
        for (int i = start; i < x.length; i += 1) {
            int cmp = x[i].compareTo(x[smallestIndex]);
            if (cmp < 0) {
                smallestIndex = i;
            }
        }
        return smallestIndex;
    }

    /** Swap item a with item b. */
    public static void swap(String[] x, int a, int b) {
        String temp = x[a];
        x[a] = x[b];
        x[b] = temp;
    }
}
