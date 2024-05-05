package deque;

import org.junit.Test;
import static org.junit.Assert.*;


public class ArrayDequeTest {
    @Test
    /** Adds a few things to the list, checking isEmpty() and size() are correct,
     * finally printing the results.
     *
     * && is the "and" operation. */
    public void addIsEmptySizeTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        assertEquals(0, ad1.size());
        assertTrue("A newly initialized ArrayDeque should be empty", ad1.isEmpty());
        ad1.addLast(99);
        assertEquals(1, ad1.size());
        ad1.addFirst(36);
        assertEquals(2, ad1.size());
        assertTrue("ad1 should now contain 2 items", !ad1.isEmpty());
    }

    @Test
    public void printTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        ad1.addLast(100);
        ad1.addLast(50);
        ad1.addLast(1);
        ad1.addFirst(2);
        ad1.addFirst(3);
        ad1.printDeque();
    }

    @Test
    /** Adds an item, then removes an item, and ensures that dll is empty afterwards. */
    public void removeEmptyTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        ad1.addFirst(3);

        ad1.removeLast();
        ad1.removeFirst();
        ad1.removeLast();

        assertEquals(0, ad1.size());
    }

    @Test
    /** Checks if the ArrayDeque can handle multiple types */
    public void multipleParamsTest() {
        ArrayDeque<String> ad1 = new ArrayDeque<String>();
        ArrayDeque<Double> ad2 = new ArrayDeque<Double>();
        ArrayDeque<Integer> ad3 = new ArrayDeque<Integer>();

        ad1.addFirst("Hello");
        ad1.addLast("World");
        ad1.addFirst("Java");

        ad2.addFirst(3.14);
        ad2.addLast(2.71);
        ad2.addFirst(1.0);

        ad3.addFirst(1);
        ad3.addLast(2);
        ad3.addFirst(3);

        String s = ad1.removeFirst();
        double d = ad2.removeFirst();
        int i = ad3.removeFirst();
    }

    @Test
    /** Check if null is returned when removing from an empty ArrayDeque */
    public void emptyNullReturnTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        assertEquals(null, ad1.removeFirst());
        assertEquals(null, ad1.removeLast());
    }

    @Test
    public void bigTest() {
        ArrayDeque<Integer> ad1 = new ArrayDeque<Integer>();
        int N = 10;
        for (int i = 0; i < N; i += 1) {
            ad1.addLast(i);
        }

        for (int i = 0; i < N; i += 1) {
            ad1.addLast(ad1.get(i));
        }

        for (int i = 0; i < N; i += 1) {
            assertEquals(i, (int) ad1.get(i));
        }
    }
}
