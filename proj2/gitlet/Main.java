package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Kunhua Huang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validArgs(args, 1);
                try {
                    Repository.init();
                } catch (IOException e) {
                    System.out.println("IOException occurred.");
                    System.exit(0);
                }
                break;
            case "add":
                validArgs(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                validArgs(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validArgs(args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                validArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                validArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validArgs(args, 1);
                Repository.status();
                break;
            case "checkout":
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutCommit(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                validArgs(args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validArgs(args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                validArgs(args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validArgs(args, 2);
                Repository.merge(args[1], null);
                break;
            case "add-remote":
                validArgs(args, 3);
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validArgs(args, 2);
                Repository.rmRemote(args[1]);
                break;
            case "push":
                validArgs(args, 3);
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                validArgs(args, 3);
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                validArgs(args, 3);
                Repository.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    /**
     * Check if the number of arguments is correct.
     *
     * @param args
     * @param numArgs
     */
    private static void validArgs(String[] args, int numArgs) {
        if (args.length != numArgs) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
