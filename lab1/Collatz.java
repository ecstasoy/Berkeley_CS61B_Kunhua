/** Class that prints the Collatz sequence starting from a given number.
 *  @author Kunhua Huang
 */
public class Collatz {

    /**
     * Return the next number of the Collatz sequence
     *
     * @param n The previous number in the sequence
     * @return The next number in the sequence
     * */
    public static int nextNumber(int n) {
        if (n % 2 == 0) {
            return n / 2;
        } else {
            return 3 * n + 1;
        }
    }

    public static void main(String[] args) {
        int n = 5;
        System.out.print(n + " ");
        while (n != 1) {
            n = nextNumber(n);
            System.out.print(n + " ");
        }
        System.out.println();
    }
}

