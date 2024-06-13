package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            List<String> parents = currentCommit.getParent();
            if (parents.size() > 1) {
                System.out.println("Merge: " +
                        currentCommit.getParent().get(0).substring(0, 7) + " " +
                        currentCommit.getParent().get(1).substring(0, 7));
            }

            System.out.println("Date: " + currentCommit.getFormattedTimestamp());
            System.out.println(currentCommit.getMessage() + "\n");

            currentCommit = getNextCommit(currentCommit);
        }
    }

    private static Commit getNextCommit(Commit current) {
        List<String> parents = current.getParent();
        if (parents != null && !parents.isEmpty()) {
            return getCommit(parents.get(0));  // Always follow the first parent in a log (like git does)
        }
        return null;  // No more parents to follow
    }

    private static Commit getCommit(String id) {
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

        for (File commitFolder : Objects.requireNonNull(COMMITS_DIR.listFiles())) {
            if (commitFolder == null) return;
            File[] commitFiles = commitFolder.listFiles();
            if (commitFiles == null) continue;
            for (File commitFile : commitFiles) {
                try {
                    Commit commit = readObject(commitFile, Commit.class);
                    commits.add(commit);
                } catch (IllegalArgumentException e) {
                    System.out.println("Error reading commit file.");
                    System.exit(0);
                }
            }
        }

        commits.sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()));

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

        for (File commitFolder : Objects.requireNonNull(COMMITS_DIR.listFiles())) {
            if (commitFolder == null) return;
            for (File commitFile : Objects.requireNonNull(commitFolder.listFiles())) {
                Commit commit = readObject(commitFile, Commit.class);
                if (commit.getMessage().equals(commitMessage)) {
                    System.out.println(commit.getId());
                    found = true;
                }
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

        if (commit.getBlobs().get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

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

        currentCommit = getCurrentCommit();

        String targetCommitId = readContentsAsString(join(refsHeads, branchName));
        Commit targetCommit = getCommit(targetCommitId);

        for (String fileName : targetCommit.getBlobs().keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.getBlobs().containsKey(fileName) && !isFileTracked(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!targetCommit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
        for (String fileName : targetCommit.getBlobs().keySet()) {
            Blob blob = readObject(join(BLOBS_DIR, targetCommit.getBlobs().get(fileName)), Blob.class);
            File file = join(CWD, fileName);
            writeContents(file, blob.getContentBytes());
        }
        writeContents(HEAD, branchName);
    }

    // Function to check if a file has ever been tracked in any commit
    private static boolean isFileTracked(String fileName) {
        Set<String> allCommits = getAllCommitIds(); // Assume this function retrieves all commit IDs in the repo
        File file = join(CWD, fileName);
        String currentFileSha1 = sha1(readContents(file));
        for (String commitId : allCommits) {
            Commit commit = getCommit(commitId);
            if (commit != null && commit.getBlobs().containsKey(fileName)) {
                String fileSha1 = commit.getBlobs().get(fileName);
                if (fileSha1.equals(currentFileSha1)) {
                    return true; // File was tracked in this commit
                }
            }
        }
        return false; // File was never tracked
    }

    // Function to get all commit IDs in the repository
    private static Set<String> getAllCommitIds() {
        Set<String> commitIds = new HashSet<>();
        // Example: Traverse all commit objects stored in the commits directory
        File[] commitFolders = COMMITS_DIR.listFiles();
        if (commitFolders != null) {
            for (File folder : commitFolders) {
                File[] commits = folder.listFiles();
                if (commits != null) {
                    for (File commitFile : commits) {
                        commitIds.add(folder.getName() + commitFile.getName());
                    }
                }
            }
        }
        return commitIds;
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
        currentCommit = getCurrentCommit();
        writeContents(join(refsHeads, branchName), currentCommit.getId());
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

        for (String fileName : commit.getBlobs().keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.getBlobs().containsKey(fileName) && !isFileTracked(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!commit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        for (String fileName : commit.getBlobs().keySet()) {
            Blob blob = readObject(join(BLOBS_DIR, commit.getBlobs().get(fileName)), Blob.class);
            storeBlob(blob);
            File file = join(CWD, fileName);
            writeContents(file, blob.getContentBytes());
        }

        stageArea = StageArea.getInstance();
        stageArea.clear();
        writeContents(join(refsHeads, readContentsAsString(HEAD)), commitId);
        currentCommit = commit;
    }

    /** merge command */
    public static void merge(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        stageArea = StageArea.getInstance();

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

        currentCommit = getCurrentCommit();
        Commit givenCommit = getCommit(readContentsAsString(join(refsHeads, branchName)));
        Commit splitPoint = findSplitPoint(branchName);
        Boolean conflict = false;

        if (splitPoint == null) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        Map<String, String> currentFiles = currentCommit.getBlobs();
        Map<String, String> givenFiles = givenCommit.getBlobs();
        Map<String, String> splitFiles = splitPoint.getBlobs();

        // Determine actions for each file in the three commits
        Set<String> allFiles = new HashSet<>(currentFiles.keySet());
        allFiles.addAll(givenFiles.keySet());
        allFiles.addAll(splitFiles.keySet());

        for (String file : allFiles) {
            boolean inCurrent = currentFiles.containsKey(file);
            boolean inGiven = givenFiles.containsKey(file);
            boolean inSplit = splitFiles.containsKey(file);

            String currentVersion = inCurrent ? currentFiles.get(file) : null;
            String givenVersion = inGiven ? givenFiles.get(file) : null;
            String splitVersion = inSplit ? splitFiles.get(file) : null;

            if (inCurrent && inGiven && !givenVersion.equals(currentVersion) && !givenVersion.equals(splitVersion) && !currentVersion.equals(splitVersion)) {
                handleMergeConflict(file, currentCommit, givenCommit);
                conflict = true;
            } else if (inGiven && (!inCurrent || !givenVersion.equals(splitVersion))) {
                checkoutAndStageFile(file, givenCommit);
            } else if (!inGiven && inSplit && inCurrent && currentVersion.equals(splitVersion)) {
                Utils.restrictedDelete(join(CWD, file));
                stageArea.unstageFile(file);
                stageArea.save();
            } else if (!inGiven && inSplit && !inCurrent) {
                Utils.restrictedDelete(join(CWD, file));
            }
        }

        // Creating a merge commit
        List<String> parents = Arrays.asList(currentCommit.getId(), givenCommit.getId());
        String message = "Merged " + branchName + " into " + readContentsAsString(HEAD) + ".";
        Commit mergeCommit = new Commit(message, parents, stageArea.getStagedFiles());
        saveCommit(mergeCommit);
        stageArea.clear();
        writeContents(join(refsHeads, readContentsAsString(HEAD)), mergeCommit.getId());

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }

        System.out.println("DEBUG: Final conflict file content:");
        System.out.println(readContentsAsString(join(CWD, "f.txt")));

    }

    private static Commit findSplitPoint(String branchName) {
        String targetCommitId = readContentsAsString(join(refsHeads, branchName));
        Commit targetCommit = getCommit(targetCommitId);
        currentCommit = getCurrentCommit();
        currentBranch = getCurrentBranch();

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
            List<String> parent = commit.getParent();
            if (parent.isEmpty()) {
                break;
            } else {
                commit = getCommit(parent.get(0));
            }
        }
        return ancestors;
    }

    private static void handleMergeConflict(String fileName, Commit currentCommit, Commit givenCommit) {
        File file = join(CWD, fileName);
        Blob currentBlob = null;
        Blob givenBlob = null;

        if (currentCommit.getBlobs().containsKey(fileName)) {
            currentBlob = readObject(join(BLOBS_DIR, currentCommit.getBlobs().get(fileName)), Blob.class);
        }
        if (givenCommit.getBlobs().containsKey(fileName)) {
            givenBlob = readObject(join(BLOBS_DIR, givenCommit.getBlobs().get(fileName)), Blob.class);
        }

        String currentContents = (currentBlob != null) ? new String(currentBlob.getContentBytes()) : "";
        String givenContents = (givenBlob != null) ? new String(givenBlob.getContentBytes()) : "";

        String conflictContent = "<<<<<<< HEAD\n" +
                currentContents +
                "=======\n" +
                givenContents +
                ">>>>>>>";

        writeContents(file, conflictContent);
        stageArea.stageFile(fileName, file);
        stageArea.save();
    }

    private static void checkoutAndStageFile(String fileName, Commit commit) {
        checkoutCommit(commit.getId(), fileName);
        stageArea = StageArea.getInstance();
        stageArea.stageFile(fileName, join(CWD, fileName));
        stageArea.save();
    }

}