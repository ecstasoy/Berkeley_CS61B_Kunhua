package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Kunhua Huang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validArgs(args, 1);
                Repository.init();
                break;
            case "add":
                validArgs(args, 2);
                Repository.add(args[1]);
                break;
            // TODO: FILL THE REST IN
        }
    }

    private static void validArgs(String[] args, int numArgs) {
        if (args.length != numArgs) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
