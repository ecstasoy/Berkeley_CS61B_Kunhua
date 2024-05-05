package deque;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Comparator;

public class MaxArrayDequeTest {

    @Test
    /** Test MaxArrayDeque */
    public void testMADeque() {
        Comparator<Integer> c1 = Integer::compare;
        Comparator<String> c2 = String::compareTo;
        MaxArrayDeque<Integer> mad1 = new MaxArrayDeque<>(c1);
        mad1.addLast(1);
        mad1.addLast(2);
        mad1.addLast(3);
        mad1.addLast(4);
        assertEquals(4, (int) mad1.max());

        MaxArrayDeque<String> mad2 = new MaxArrayDeque<>(c2);
        mad2.addLast("a");
        mad2.addLast("b");
        mad2.addLast("c");
        mad2.addLast("d");
        assertEquals("d", mad2.max());
        };
}
