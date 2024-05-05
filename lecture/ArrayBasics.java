public class ArrayBasics {
    public static void main(String[] args) {
        // Array creation
        int[] someArray = new int[3];
        someArray[0] = 3;
        someArray[1] = 2;
        someArray[2] = 1;

        // Array access
        System.out.println(someArray[0]);
        System.out.println(someArray[1]);
        System.out.println(someArray[2]);

        // Array length
        System.out.println(someArray.length);

        // Array iteration
        for (int i = 0; i < someArray.length; i++) {
            System.out.println(someArray[i]);
        }

        // Array initialization
        int[] someOtherArray = new int[]{3, 2, 1};

        // Array initialization shorthand
        int[] someOtherOtherArray = {3, 2, 1};

        // Array of arrays
        int[][] some2DArray = new int[3][];
        some2DArray[0] = new int[]{1, 2, 3};
        some2DArray[1] = new int[]{4, 5};
        some2DArray[2] = new int[]{6};

        // Array of arrays initialization
        int[][] someOther2DArray = new int[][]{
            {1, 2, 3},
            {4, 5},
            {6}
        };

        // Array of arrays initialization shorthand
        int[][] someOtherOther2DArray = {
            {1, 2, 3},
            {4, 5},
            {6}
        };

        // Array of arrays access
        System.out.println(some2DArray[0][0]);
        System.out.println(some2DArray[0][1]);
        System.out.println(some2DArray[0][2]);
        System.out.println(some2DArray[1][0]);
        System.out.println(some2DArray[1][1]);
        System.out.println(some2DArray[2][0]);
    }
}

/** Arrays vs. Classes
 * Arrays can be computed at runtime.
 * Class members must be declared at compile time.
 * Class member variable names CANNOT be computed and used at runtime.
 * The Java compiler does not treat text on either side of a dot as an expression, and thus does not evaluate it.
 */