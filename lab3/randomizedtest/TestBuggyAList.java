package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    @Test
    public void testThreeAddThreeRemove() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        AListNoResizing<Integer> M = new AListNoResizing<>();
        L.addLast(5);
        M.addLast(5);
        L.addLast(10);
        M.addLast(10);
        L.addLast(15);
        M.addLast(15);
        assertEquals(L.getLast(), M.getLast());
        L.removeLast();
        M.removeLast();
        assertEquals(L.getLast(), M.getLast());
        L.removeLast();
        M.removeLast();
        assertEquals(L.getLast(), M.getLast());
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> M = new BuggyAList<>();
        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                M.addLast(randVal);
                System.out.println("addLast(" + randVal + ")");
            } else if (operationNumber == 1) {
                // size
                int LSize = L.size();
                int MSize = M.size();
                System.out.println("L Size: " + LSize);
                System.out.println("M Size: " + MSize);
            } else if (operationNumber == 2) {
                // getLast
                if (L.size() == 0 || M.size() == 0) {
                    continue;
                }
                int LLast = L.getLast();
                int MLast = M.getLast();
                System.out.println("getLast L: " + LLast);
                System.out.println("getLast M: " + MLast);
            } else {
                // removeLast
                if (L.size() == 0 || M.size() == 0) {
                    continue;
                }
                int LLast = L.removeLast();
                int MLast = M.removeLast();
                System.out.println("removeLast L: " + LLast);
                System.out.println("removeLast M: " + MLast);
            }
        }
    }
}