package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Kunhua Huang
 */
public class Repository {
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File STAGE = join(GITLET_DIR, "stage");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File refs = join(GITLET_DIR, "refs");
    public static final File refsHeads = join(refs, "heads");
    public static final File remote = join(GITLET_DIR, "remote");
    public static final File remoteHeads = join(remote, "heads");

    public static Commit currentCommit;
    public static String currentBranch;
    public static StageArea stageArea;

    public Repository() {

    }

    /**
     * init command
     */
    public static void init() throws IOException {
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        STAGE.createNewFile();
        BLOBS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        refs.mkdir();
        refsHeads.mkdir();
        HEAD.createNewFile();

        initCommit();
        initHEAD();
        initRefsHeads();

        stageArea = new StageArea();
        stageArea.save();
    }

    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    public static void initCommit() {
        // Use an empty list to signify no parents for the initial commit
        List<String> noParents = new ArrayList<>();
        Map<String, Blob> noBlobs = new HashMap<>(); // No files are tracked in the initial commit

        // Creating the initial commit with no parents and no blobs
        Commit initCommit = new Commit("initial commit", noParents, noBlobs);
        currentCommit = initCommit;

        // Save the newly created commit to storage
        saveCommit(initCommit);
    }

    public static void initHEAD() {
        writeContents(HEAD, "master");
    }

    public static void initRefsHeads() {
        File master = join(refsHeads, "master");
        writeContents(master, currentCommit.getId());
    }

    /**
     * add command
     */
    public static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Blob blob = new Blob(file);
        currentCommit = getCurrentCommit();
        stageArea = StageArea.getInstance();
        String lastCommittedId = currentCommit.getBlobs().get(fileName);

        if (lastCommittedId != null && lastCommittedId.equals(blob.getId()) && !stageArea.isRemoved(fileName)) {
            return;
        }

        if (stageArea.isRemoved(fileName)) {
            stageArea.unmarkRemoved(fileName);
        } else {
            stageArea.stageFile(fileName, file);
            storeBlob(blob);
        }

        stageArea.save();
    }

    private static void storeBlob(Blob blob) {
        File blobFile = join(BLOBS_DIR, blob.getId());
        writeObject(blobFile, blob);
    }

    private static Commit getCurrentCommit() {
        if (currentCommit == null) {
            File currentCommitPath = join(refsHeads, readContentsAsString(HEAD));
            currentCommit = getCommit(readContentsAsString(currentCommitPath));
        }
        return currentCommit;
    }

    private static void clearCurrentCommitCache() {
        currentCommit = null;  // Call this method when a new commit is made
    }

    private static String getCurrentBranch() {
        if (currentBranch == null) {
            currentBranch = readContentsAsString(HEAD);
        }
        return currentBranch;
    }

    /**
     * commit command
     */
    public static void commit(String message) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Commit currentCommit = getCurrentCommit();
        StageArea stageArea = StageArea.getInstance();
        Boolean isChanged = !stageArea.getStagedFiles().isEmpty() || !stageArea.getRemovedFiles().isEmpty();

        if (!isChanged) {
            System.out.println("No changes added to the commit.");
            return;
        }

        String parentCommitId = currentCommit.getId();
        Map<String, Blob> stagedBlobs = new HashMap<>(stageArea.getStagedFiles());
        for (Blob blob : stagedBlobs.values()) {
            storeBlob(blob);
        }
        currentCommit = createCommit(message, parentCommitId, stagedBlobs);
        stageArea.clear();
        writeContents(HEAD, getCurrentBranch());
        writeContents(join(refsHeads, getCurrentBranch()), currentCommit.getId());
    }

    public static Commit createCommit(String commitMessage, String parentCommitId, Map<String, Blob> blobs) {
        Commit newCommit = new Commit(commitMessage, parentCommitId, blobs);
        newCommit.setParent(Collections.singletonList(parentCommitId));
        saveCommit(newCommit);
        return newCommit;
    }

    public static void saveCommit(Commit commit) {
        String commitId = commit.getId();
        File commitDir = join(COMMITS_DIR, commitId.substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commitId.substring(2));
        writeObject(commitFile, commit);
    }

    /**
     * rm command
     */
    public static void rm(String fileName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        currentCommit = getCurrentCommit();
        stageArea = StageArea.getInstance();

        if (!stageArea.isFileStaged(fileName) && !currentCommit.getBlobs().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stageArea.isFileStaged(fileName)) {
            stageArea.unstageFile(fileName);
        }
        if (currentCommit.getBlobs().containsKey(fileName)) {
            stageArea.markRemoved(fileName);
            if (join(CWD, fileName).exists()) {
                Utils.restrictedDelete(fileName);
            }
        }
        stageArea.save();
    }

    /**
     * log command
     */
    public static void log() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        currentCommit = getCurrentCommit();
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getId());

            // Check and print parents for merge commits
            if (currentCommit.getParent().size() > 1) {
                System.out.println("Merge: " +
                        currentCommit.getParent().get(0).substring(0, 7) + " " +
                        currentCommit.getParent().get(1).substring(0, 7));
            }

            System.out.println("Date: " + currentCommit.getFormattedTimestamp());
            System.out.println(currentCommit.getMessage() + "\n");
            if (currentCommit.getParent().size() == 1) {
                currentCommit = getCommit(currentCommit.getParent().get(0));
            } else {
                currentCommit = null;
            }
        }
    }

    public static Commit getCommit(String id) {
        File commitDir = join(COMMITS_DIR, id.substring(0, 2));
        if (id.length() < 6) {
            System.out.println("Commit id is too short.");
            System.exit(0);
        }
        if (!commitDir.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        for (File commitFile : commitDir.listFiles()) {
            Commit commit = readObject(commitFile, Commit.class);
            if (commit.getId().substring(2).startsWith(id.substring(2))) {
                return commit;
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }

    /**
     * global-log command
     */
    public static void globalLog() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        List<Commit> commits = new ArrayList<>();

        for (String commitFolder : plainFilenamesIn(COMMITS_DIR)) {
            File commitDir = join(COMMITS_DIR, commitFolder);
            for (String commitId : plainFilenamesIn(commitDir)) {
                Commit commit = readObject(join(commitDir, commitId), Commit.class);
                commits.add(commit);
            }
        }

        Collections.sort(commits, new Comparator<Commit>() {
            @Override
            public int compare(Commit c1, Commit c2) {
                return c1.getTimestamp().compareTo(c2.getTimestamp());
            }
        });

        for (Commit commit : commits) {
            System.out.println("===");
            System.out.println("commit " + commit.getId());
            if (commit.getParent().size() > 1) {
                System.out.println("Merge: " +
                        commit.getParent().get(0).substring(0, 7) + " " +
                        commit.getParent().get(1).substring(0, 7));
            }
            System.out.println("Date: " + commit.getFormattedTimestamp());
            System.out.println(commit.getMessage() + "\n");
        }

    }

    /**
     * find command
     */
    public static void find(String commitMessage) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        boolean found = false;
        for (String commitFile : plainFilenamesIn(COMMITS_DIR)) {
            Commit commit = getCommit(commitFile);
            if (commit.getMessage().equals(commitMessage)) {
                System.out.println(commit.getId());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /**
     * status command
     */
    public static void status() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        System.out.println("=== Branches ===");
        currentBranch = getCurrentBranch();
        stageArea = StageArea.getInstance();
        for (String branch : plainFilenamesIn(refsHeads)) {
            if (branch.equals(currentBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String fileName : stageArea.getStagedFiles().keySet()) {
            System.out.println(fileName);
        }

        System.out.println("\n=== Removed Files ===");
        for (String fileName : stageArea.getRemovedFiles()) {
            System.out.println(fileName);
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");
        // TODO: print modifications not staged for commit
        System.out.println("\n=== Untracked Files ===");
        // TODO: print untracked files
    }

    /**
     * checkout command
     */
    public static void checkoutFile(String fileName) {
        File file = join(CWD, fileName);

        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        currentCommit = getCurrentCommit();
        File blobFile = join(BLOBS_DIR, currentCommit.getBlobs().get(fileName));

        if (!currentCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = readObject(blobFile, Blob.class);
        writeContents(file, blob.getContentBytes());
    }

    public static void checkoutCommit(String commitId, String fileName) {
        File file = join(CWD, fileName);
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Commit commit = getCommit(commitId);
        File blobFile = join(BLOBS_DIR, commit.getBlobs().get(fileName));

        if (!commit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = readObject(blobFile, Blob.class);
        writeContents(file, blob.getContentBytes());
    }

    public static void checkoutBranch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        if (!plainFilenamesIn(refsHeads).contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(readContentsAsString(HEAD))) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        currentBranch = getCurrentBranch();
        currentCommit = getCurrentCommit();

        String targetCommitId = readContentsAsString(join(refsHeads, branchName));
        Commit targetCommit = getCommit(targetCommitId);

        for (String fileName : targetCommit.getBlobs().keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it or add it first.");
                System.exit(0);
            }
        }

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!targetCommit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
        for (String fileName : targetCommit.getBlobs().keySet()) {
            Blob blob = new Blob(join(BLOBS_DIR, targetCommit.getBlobs().get(fileName)));
            File file = join(CWD, fileName);
            writeContents(file, blob.getContentBytes());
        }
        writeContents(HEAD, branchName);
    }

    /**
     * branch command
     */
    public static void branch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (plainFilenamesIn(refsHeads).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        currentBranch = getCurrentBranch();
        String currentCommitId = readContentsAsString(join(refsHeads, currentBranch));
        writeContents(join(refsHeads, branchName), currentCommitId);
        writeContents(HEAD, branchName);
    }

    /**
     * rm-branch command
     */
    public static void rmBranch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!plainFilenamesIn(refsHeads).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(readContentsAsString(HEAD))) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File branch = join(refsHeads, branchName);
        branch.delete();
    }

    /**
     * reset command
     */
    public static void reset(String commitId) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Commit commit = getCommit(commitId);
        currentCommit = getCurrentCommit();

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!commit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
        for (String fileName : commit.getBlobs().keySet()) {
            Blob blob = new Blob(join(BLOBS_DIR, commit.getBlobs().get(fileName)));
            storeBlob(blob);
            File file = join(CWD, fileName);
            writeContents(file, blob.getContentBytes());
        }
        writeContents(join(refsHeads, readContentsAsString(HEAD)), commitId);
        currentCommit = commit;
    }

    /** merge command */
    public static void merge(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        if (!stageArea.getStagedFiles().isEmpty() || !stageArea.getRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        if (!plainFilenamesIn(refsHeads).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        if (branchName.equals(readContentsAsString(HEAD))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit splitPoint = findSplitPoint(branchName);

        if (splitPoint == null) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        Commit currentCommit = getCommit(readContentsAsString(join(refsHeads, readContentsAsString(HEAD))));
        Commit givenCommit = getCommit(readContentsAsString(join(refsHeads, branchName)));

        for (String fileName : splitPoint.getBlobs().keySet()) {
            if (!currentCommit.getBlobs().containsKey(fileName) && givenCommit.getBlobs().containsKey(fileName)) {
                checkoutCommit(givenCommit.getId(), fileName);
                stageArea.stageFile(fileName, join(CWD, fileName));
            }
        }

        for (String fileName : splitPoint.getBlobs().keySet()) {
            boolean isFileInCurrentCommit = currentCommit.getBlobs().containsKey(fileName);
            boolean isFileInGivenCommit = givenCommit.getBlobs().containsKey(fileName);
            boolean isFileModifiedInCurrentCommit = isFileInCurrentCommit
                    && !currentCommit.getBlobs().get(fileName).equals(splitPoint.getBlobs().get(fileName));
            boolean isFileModifiedInGivenCommit = isFileInGivenCommit
                    && !givenCommit.getBlobs().get(fileName).equals(splitPoint.getBlobs().get(fileName));

            if (!isFileModifiedInCurrentCommit && isFileModifiedInGivenCommit || isFileInCurrentCommit && !isFileInGivenCommit) {
                handleMergeConflict(fileName, currentCommit, givenCommit);
            } else if (isFileModifiedInCurrentCommit && isFileModifiedInGivenCommit) {
                if (currentCommit.getBlobs().get(fileName).equals(givenCommit.getBlobs().get(fileName))) {
                    checkoutAndStageFile(fileName, givenCommit);
                } else {
                    handleMergeConflict(fileName, currentCommit, givenCommit);
                }
            } else {
                continue;
            }
        }
    }

    private static Commit findSplitPoint(String branchName) {
        String currentBranch = readContentsAsString(HEAD);
        String currentCommitId = readContentsAsString(join(refsHeads, currentBranch));
        String targetCommitId = readContentsAsString(join(refsHeads, branchName));
        Commit currentCommit = getCommit(currentCommitId);
        Commit targetCommit = getCommit(targetCommitId);

        List<String> currentAncestors = getAncestors(currentCommit);
        List<String> targetAncestors = getAncestors(targetCommit);

        for (String ancestor : currentAncestors) {
            if (targetAncestors.contains(ancestor)) {
                return getCommit(ancestor);
            }
        }
        return null;
    }

    private static List<String> getAncestors(Commit commit) {
        List<String> ancestors = new ArrayList<>();
        while (commit != null) {
            ancestors.add(commit.getId());
            commit = getCommit(commit.getParent().get(0));
        }
        return ancestors;
    }

    private static void handleMergeConflict(String fileName, Commit currentCommit, Commit givenCommit) {
        File file = join(CWD, fileName);
        if (currentCommit.getBlobs().containsKey(fileName) && givenCommit.getBlobs().containsKey(fileName)) {
            Blob currentBlob = new Blob(join(BLOBS_DIR, currentCommit.getBlobs().get(fileName)));
            Blob givenBlob = new Blob(join(BLOBS_DIR, givenCommit.getBlobs().get(fileName)));
            writeContents(file, "<<<<<<< HEAD\n");
            writeContents(file, currentBlob.getContentBytes());
            writeContents(file, "\n=======\n");
            writeContents(file, givenBlob.getContentBytes());
            writeContents(file, "\n>>>>>>>\n");
        } else if (currentCommit.getBlobs().containsKey(fileName)) {
            Blob currentBlob = new Blob(join(BLOBS_DIR, currentCommit.getBlobs().get(fileName)));
            byte[] content = "<<<<<<< HEAD\n".getBytes();
            writeContents(file, "<<<<<<< HEAD\n");
            writeContents(file, currentBlob.getContentBytes());
            writeContents(file, "\n=======\n");
            writeContents(file, "\n>>>>>>>\n");
        } else {
            Blob givenBlob = new Blob(join(BLOBS_DIR, givenCommit.getBlobs().get(fileName)));
            byte[] content = "<<<<<<< HEAD\n".getBytes();
            writeContents(file, "<<<<<<< HEAD\n");
            writeContents(file, "\n=======\n");
            writeContents(file, givenBlob.getContentBytes());
            writeContents(file, "\n>>>>>>>\n");
        }
        stageArea.stageFile(fileName, file);
    }

    private static void checkoutAndStageFile(String fileName, Commit commit) {
        checkoutCommit(commit.getId(), fileName);
        stageArea.stageFile(fileName, join(CWD, fileName));
    }

}