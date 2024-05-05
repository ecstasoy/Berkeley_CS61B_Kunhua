/** An SLList is a list of integers, which hides the terrible truth
 * of the nakedness within. */

/** Invariants:
 * 1. The sentinel node is never null.
 * 2. The size variable is always accurate.
 * 3. The first item (if it exists) is at sentinel.next.
 * 4. The last item (if it exists) is at a node with null next.
 * */

/**
 * Java allows us to defer type decisions until runtime.
 * Use <> to indicate that the type is to be determined later.
 */
public class SLList {
    /** An IntNode is the building block of SLList.
     * We can declare it as static because it doesn't need to access
     * any instance variables of SLList.
     * */
    public static class IntNode {
        public int item;
        public IntNode next;

        public IntNode(int i, IntNode n) {
            item = i;
            next = n;
        }
    }

    /** Use private to hide the nakedness. */
    private IntNode first;
    /** Caching: store the size of the list. */
    private int size;
    /** The first item (if it exists) is at sentinel.next. */
    private IntNode sentinel;

    /** Creates an empty SLList. */
    public SLList() {
        sentinel = new IntNode(0, null);
        size = 0;
    }

    /** Creates an empty SLList. */
    public SLList(int x) {
        sentinel = new IntNode(0, null);
        sentinel.next = new IntNode(x, null);
        size = 1;
    }

    /** Adds x to the front of the list. */
    public void addFirst(int x) {
        sentinel.next = new IntNode(x, sentinel.next);
        size += 1;
    }

    /** Returns the first item in the list. */
    public int getFirst() {
        return sentinel.next.item;
    }

    /** Adds x to the end of the list. */
    public void addLast(int x) {
        IntNode p = sentinel;
        while (p.next != null) {
            p = p.next;
        }
        p.next = new IntNode(x, null);
        size += 1;
    }

    /** Returns the size of the list. */
    public int size() {
        return size;
    }
}