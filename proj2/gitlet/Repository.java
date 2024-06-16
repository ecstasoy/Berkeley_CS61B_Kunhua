package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  MARK: It's a good idea to give a description here of what else this Class
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
    public static final File REFS = join(GITLET_DIR, "REFS");
    public static final File REFS_HEADS = join(REFS, "heads");
    public static final File REMOTE = join(GITLET_DIR, "remotes");
    public static final File REMOTE_HEADS = join(REFS, "remotes");

    private static Commit currentCommit;
    private static String currentBranch;
    private static StageArea stageArea;

    /**
     * init command
     */
    public static void init() throws IOException {
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        STAGE.createNewFile();
        BLOBS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        REFS.mkdir();
        REFS_HEADS.mkdir();
        HEAD.createNewFile();
        REMOTE.mkdir();
        REMOTE_HEADS.mkdir();

        initCommit();
        initHEAD();
        initRefsHeads();

        stageArea = new StageArea();
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
        File master = join(REFS_HEADS, "master");
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
        String lastCommittedId = null;
        if (currentCommit.getBlobs().containsKey(fileName)) {
            lastCommittedId = currentCommit.getBlobs().get(fileName).getId();
        }

        if (lastCommittedId != null && lastCommittedId.equals(blob.getId())
                && !stageArea.isRemoved(fileName)) {
            return;
        }

        if (stageArea.isRemoved(fileName)) {
            stageArea.unmarkRemoved(fileName);
        } else {
            stageArea.stageFile(fileName, file);
            storeBlob(blob);
        }

    }

    private static void storeBlob(Blob blob) {
        File blobFile = join(BLOBS_DIR, blob.getId());
        writeObject(blobFile, blob);
    }

    private static Commit getCurrentCommit() {

        String currentHead = readContentsAsString(HEAD);

        if (currentCommit == null) {
            if (currentHead.contains("/")) {
                String remoteName = currentHead.split("/")[0];
                String branchName = currentHead.split("/")[1];
                File remoteBranch = join(REMOTE_HEADS, remoteName, branchName);
                currentCommit = getCommit(readContentsAsString(remoteBranch), remoteName);
            } else {
                File currentCommitPath = join(REFS_HEADS, currentHead);
                currentCommit = getCommit(readContentsAsString(currentCommitPath), null);
            }
        }
        return currentCommit;
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

        currentCommit = getCurrentCommit();
        stageArea = StageArea.getInstance();
        boolean isChanged = !stageArea.getStagedFiles().isEmpty()
                || !stageArea.getRemovedFiles().isEmpty();

        if (!isChanged) {
            System.out.println("No changes added to the commit.");
            return;
        }

        // Merge current commit's blobs with staged blobs to form the new commit's blobs
        Map<String, Blob> newCommitBlobs = new HashMap<>(currentCommit.getBlobs());
        for (Map.Entry<String, Blob> entry : stageArea.getStagedFiles().entrySet()) {
            Blob blob = entry.getValue();
            String blobId = blob.getId();

            // Only store the blob if it does not already exist to avoid redundancy
            File blobFile = join(BLOBS_DIR, blobId);
            if (!blobFile.exists()) {
                storeBlob(blob);
            }
            newCommitBlobs.put(entry.getKey(), blob);
        }

        // Remove blobs marked for removal in the staging area
        for (String removedFileName : stageArea.getRemovedFiles()) {
            newCommitBlobs.remove(removedFileName);
        }

        if (newCommitBlobs.equals(currentCommit.getBlobs())) {
            System.out.println("No changes detected.");
            return;
        }

        String parentCommitId = currentCommit.getId();
        Commit newCommit = createCommit(message, parentCommitId, newCommitBlobs);
        stageArea.clear();

        // Update HEAD and current branch to point to the new commit
        writeContents(join(REFS_HEADS, getCurrentBranch()), newCommit.getId());
        writeContents(HEAD, getCurrentBranch());
    }

    public static Commit createCommit(String message, String parentId,
                                      Map<String, Blob> blobs) {
        Commit newCommit = new Commit(message, parentId, blobs);
        newCommit.setParent(Collections.singletonList(parentId));
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

        if (!stageArea.isFileStaged(fileName)) {
            if (!isFileTracked(fileName)) {
                System.out.println("No reason to remove the file.");
                System.exit(0);
            } else {
                stageArea.markRemoved(fileName);
                Utils.restrictedDelete(fileName);
            }
        } else {
            stageArea.unstageFile(fileName);
        }

        if (currentCommit.getBlobs().containsKey(fileName)) {
            stageArea.markRemoved(fileName);
            if (join(CWD, fileName).exists()) {
                Utils.restrictedDelete(fileName);
            }
        }

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
                System.out.println("Merge: "
                        + currentCommit.getParent().get(0).substring(0, 7)
                        + " " + currentCommit.getParent().get(1).substring(0, 7));
            }

            System.out.println("Date: " + currentCommit.getFormattedTimestamp());
            System.out.println(currentCommit.getMessage() + "\n");

            currentCommit = getNextCommit(currentCommit);
        }
    }

    private static Commit getNextCommit(Commit current) {
        List<String> parents = current.getParent();
        if (parents != null && !parents.isEmpty()) {
            return getCommit(parents.get(0), null);
        }
        return null;  // No more parents to follow
    }

    private static Commit getCommit(String id, String remoteName) {

        File commitDir;

        if (remoteName != null) {
            commitDir = join(readContentsAsString(join(REMOTE, remoteName)), "commits");
            commitDir = join(commitDir, id.substring(0, 2));
        } else {
            commitDir = join(COMMITS_DIR, id.substring(0, 2));
        }

        if (id.length() < 6) {
            System.out.println("Commit id is too short.");
            System.exit(0);
        }
        if (!commitDir.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        for (File commitFile : Objects.requireNonNull(commitDir.listFiles())) {
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
            if (commitFolder == null) {
                return;
            }
            File[] commitFiles = commitFolder.listFiles();
            if (commitFiles == null) {
                continue;
            }
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
                System.out.println("Merge: "
                        + commit.getParent().get(0).substring(0, 7)
                        + " " + commit.getParent().get(1).substring(0, 7));
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
            if (commitFolder == null) {
                return;
            }
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

        currentCommit = getCurrentCommit();
        currentBranch = getCurrentBranch();
        stageArea = StageArea.getInstance();

        System.out.println("=== Branches ===");
        for (String branch : Objects.requireNonNull(plainFilenamesIn(REFS_HEADS))) {
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
        printModifiedFiles();
        System.out.println("\n=== Untracked Files ===");
        Set<String> untrackedFiles = getUntrackedFiles(currentCommit);
        for (String file : untrackedFiles) {
            if (!stageArea.getStagedFiles().containsKey(file)) {
                System.out.println(file);
            }
        }
    }

    private static void printModifiedFiles() {
        for (String fileName : currentCommit.getBlobs().keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !stageArea.isFileStaged(fileName)) {
                if (!sha1((Object) readContents(file)).equals(
                        currentCommit.getBlobs().get(fileName).getId())
                        && !isConflict(fileName)) {
                    System.out.println(fileName + " (modified)");
                }
            } else if (!stageArea.isFileStaged(fileName)) {
                if (!stageArea.isRemoved(fileName)) {
                    System.out.println(fileName + " (deleted)");
                }
            }
        }
    }

    private static boolean isConflict(String fileName) {
        File file = join(CWD, fileName);
        // read the first line of that file
        String firstLine = readContentsAsString(file).split("\n")[0];
        return firstLine.equals("<<<<<<< HEAD");
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
        isFileExistInCommit(fileName, file, currentCommit);
    }

    private static void isFileExistInCommit(String fileName, File file, Commit currCommit) {
        File blobFile = join(BLOBS_DIR, currCommit.getBlobs().get(fileName).getId());

        if (!currCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = readObject(blobFile, Blob.class);
        writeContents(file, (Object) blob.getContentBytes());
    }

    public static void checkoutCommit(String commitId, String fileName) {
        File file = join(CWD, fileName);
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Commit commit = getCommit(commitId, null);

        if (commit.getBlobs().get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        isFileExistInCommit(fileName, file, commit);
    }

    public static void checkoutBranch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        String remoteName = null;
        String branch = null;

        if (branchName.contains("/")) {
            remoteName = branchName.split("/")[0];
            branch = branchName.split("/")[1];
            if (!plainFilenamesIn(join(REMOTE_HEADS, remoteName)).contains(branch)) {
                System.out.println("A branch with that name does not exist on the remote.");
                System.exit(0);
            }
        } else {
            if (!Objects.requireNonNull(plainFilenamesIn(REFS_HEADS)).contains(branchName)) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
        }

        if (branchName.equals(readContentsAsString(HEAD))) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        currentCommit = getCurrentCommit();
        stageArea = StageArea.getInstance();

        String targetCommitId;
        Commit targetCommit;
        File blobsDir;

        if (branchName.contains("/")) {
            targetCommitId = readContentsAsString(join(REMOTE_HEADS, remoteName, branch));
            targetCommit = getCommit(targetCommitId, remoteName);
            blobsDir = join(readContentsAsString(join(REMOTE, remoteName)), "blobs");
        } else {
            targetCommitId = readContentsAsString(join(REFS_HEADS, branchName));
            targetCommit = getCommit(targetCommitId, null);
            blobsDir = BLOBS_DIR;
        }

        isFileUntracked(targetCommit);

        Map<String, Blob> finalBlobs = targetCommit.getBlobs();

        for (Map.Entry<String, Blob> entry : finalBlobs.entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue().getId();

            Blob blob = readObject(join(blobsDir, blobId), Blob.class);
            if (blobsDir != BLOBS_DIR) {
                storeBlob(blob);
            }
            File file = join(CWD, fileName);
            writeContents(file, (Object) blob.getContentBytes());
            // Only write the final state of each file
        }

        // Delete files that are present in the current
        // working directory but not in the target commit
        Set<String> currentFiles = new HashSet<>(Objects.requireNonNull(plainFilenamesIn(CWD)));
        // Assuming this method gives us current files
        for (String currentFile : currentFiles) {
            if (!finalBlobs.containsKey(currentFile)) {
                Utils.restrictedDelete(currentFile);
            }
        }

        stageArea.clear();
        writeContents(HEAD, branchName);
    }

    private static void isFileUntracked(Commit targetCommit) {
        for (String fileName : targetCommit.getBlobs().keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !currentCommit.getBlobs().containsKey(fileName)
                    && !isFileTracked(fileName)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    // Function to check if a file has ever been tracked in any commit
    private static boolean isFileTracked(String fileName) {
        Set<String> allCommits = getAllCommitIds();
        // Assume this function retrieves all commit IDs in the repo
        File file = join(CWD, fileName);
        if (file.exists()) {
            String currentFileSha1 = sha1((Object) readContents(file));
            for (String commitId : allCommits) {
                Commit commit = getCommit(commitId, null);
                if (commit != null && commit.getBlobs().containsKey(fileName)) {
                    String fileSha1 = commit.getBlobs().get(fileName).getId();
                    if (fileSha1.equals(currentFileSha1)) {
                        return true; // File was tracked in this commit
                    }
                }
            }
        } else {
            for (String commitId : allCommits) {
                Commit commit = getCommit(commitId, null);
                if (commit != null && commit.getBlobs().containsKey(fileName)) {
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
        if (Objects.requireNonNull(plainFilenamesIn(REFS_HEADS)).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        currentBranch = getCurrentBranch();
        currentCommit = getCurrentCommit();
        writeContents(join(REFS_HEADS, branchName), currentCommit.getId());
    }

    /**
     * rm-branch command
     */
    public static void rmBranch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!Objects.requireNonNull(plainFilenamesIn(REFS_HEADS)).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(readContentsAsString(HEAD))) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File branch = join(REFS_HEADS, branchName);
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

        Commit commit = getCommit(commitId, null);
        currentCommit = getCurrentCommit();

        isFileUntracked(commit);

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!commit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        for (String fileName : commit.getBlobs().keySet()) {
            Blob blob = readObject(join(BLOBS_DIR,
                    commit.getBlobs().get(fileName).getId()), Blob.class);
            storeBlob(blob);
            File file = join(CWD, fileName);
            writeContents(file, (Object) blob.getContentBytes());
        }

        stageArea = StageArea.getInstance();
        stageArea.clear();
        writeContents(join(REFS_HEADS, readContentsAsString(HEAD)), commitId);
        currentCommit = commit;
    }

    /** merge command */
    public static void merge(String branchName, String remoteName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        stageArea = StageArea.getInstance();

        if (!stageArea.getStagedFiles().isEmpty() || !stageArea.getRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        if (remoteName == null) {
            if (!Objects.requireNonNull(plainFilenamesIn(REFS_HEADS)).contains(branchName)) {
                System.out.println("A branch with that name does not exist.");
                System.exit(0);
            }
        } else {
            if (!Objects.requireNonNull(plainFilenamesIn(join(REMOTE_HEADS, remoteName)))
                    .contains(branchName)) {
                System.out.println("Remote branch does not exist.");
                System.exit(0);
            }

            if (!Objects.requireNonNull(plainFilenamesIn(join(REMOTE_HEADS, remoteName)))
                    .contains(branchName)) {
                System.out.println("A branch with that name does not exist on the remote.");
                System.exit(0);
            }
        }

        String branch = remoteName == null ? branchName : remoteName + "/" + branchName;
        if (branch.equals(readContentsAsString(HEAD))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        currentCommit = getCurrentCommit();
        Commit givenCommit = null;

        if (remoteName == null) {
            givenCommit = getCommit(readContentsAsString(join(REFS_HEADS, branchName)), null);
        } else {
            givenCommit = getCommit(readContentsAsString(join(REMOTE_HEADS,
                    remoteName, branchName)), remoteName);
        }

        Commit splitPoint = findSplitPoint(branchName, remoteName);

        if (splitPoint == null) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        List<String> ancestors = getAncestors(givenCommit);

        if (ancestors.contains(currentCommit.getId())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }

        Set<String> untrackedFiles = getUntrackedFiles(currentCommit);
        checkForUntrackedFiles(untrackedFiles, givenCommit);

        mergeHelper(givenCommit, splitPoint, branchName, remoteName);
    }

    private static void mergeHelper(Commit givenCommit, Commit splitPoint,
                                    String branchName, String remoteName) {
        Map<String, Blob> currentFiles = currentCommit.getBlobs();
        Map<String, Blob> givenFiles = givenCommit.getBlobs();
        Map<String, Blob> splitFiles = splitPoint.getBlobs();
        boolean conflict = false;

        // Determine actions for each file in the three commits
        Set<String> allFiles = new HashSet<>(currentFiles.keySet());
        allFiles.addAll(givenFiles.keySet());
        allFiles.addAll(splitFiles.keySet());

        Map<String, Blob> newBlobs = new HashMap<>(currentCommit.getBlobs());

        for (String file : allFiles) {
            boolean inCurrent = currentFiles.containsKey(file);
            boolean inGiven = givenFiles.containsKey(file);
            boolean inSplit = splitFiles.containsKey(file);

            String currentVersion = inCurrent ? currentFiles.get(file).getId() : null;
            String givenVersion = inGiven ? givenFiles.get(file).getId() : null;
            String splitVersion = inSplit ? splitFiles.get(file).getId() : null;

            if (inCurrent && inGiven && !givenVersion.equals(currentVersion)
                    && !givenVersion.equals(splitVersion)) {
                if (currentVersion.equals(splitVersion)) {
                    checkoutAndStageFile(file, givenCommit);
                    newBlobs.put(file, givenFiles.get(file));
                } else {
                    handleMergeConflict(file, givenCommit);
                    conflict = true;
                }
            } else if (inGiven && !inCurrent && !inSplit) {
                checkoutAndStageFile(file, givenCommit);
                newBlobs.put(file, givenFiles.get(file));
            } else if (!inGiven && inSplit && inCurrent) {
                if (currentVersion.equals(splitVersion)) {
                    Utils.restrictedDelete(join(CWD, file));
                    newBlobs.remove(file);
                    stageArea.unstageFile(file);
                } else {
                    handleMergeConflict(file, givenCommit);
                    conflict = true;
                }
            } else if (!inGiven && inSplit) {
                Utils.restrictedDelete(join(CWD, file));
                newBlobs.remove(file);
            }
        }

        String branch = remoteName == null ? branchName : remoteName + "/" + branchName;
        List<String> parents = Arrays.asList(currentCommit.getId(), givenCommit.getId());
        String message = "Merged " + branch + " into " + readContentsAsString(HEAD) + ".";
        createMergeCommit(message, parents, newBlobs);
        stageArea.clear();

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static void createMergeCommit(String message,
                                          List<String> parents, Map<String, Blob> blobs) {
        Commit mergeCommit = new Commit(message, parents, blobs);
        saveCommit(mergeCommit);
        writeContents(join(REFS_HEADS, readContentsAsString(HEAD)), mergeCommit.getId());
    }

    private static Commit findSplitPoint(String branchName, String remoteName) {

        String targetCommitId = null;
        if (remoteName == null) {
            targetCommitId = readContentsAsString(join(REFS_HEADS, branchName));
        } else {
            targetCommitId = readContentsAsString(join(REMOTE_HEADS, remoteName, branchName));
        }
        Commit targetCommit = getCommit(targetCommitId, null);
        currentCommit = getCurrentCommit();
        currentBranch = getCurrentBranch();

        List<String> currentAncestors = getAncestors(currentCommit);
        List<String> targetAncestors = getAncestors(targetCommit);

        for (String ancestor : currentAncestors) {
            if (targetAncestors.contains(ancestor)) {
                if (ancestor.equals(targetCommitId)) {
                    return null;
                }
                return getCommit(ancestor, null);
            }
        }
        return null;
    }

    private static List<String> getAncestors(Commit commit) {
        List<String> ancestors = new ArrayList<>();
        Stack<Commit> stack = new Stack<>();
        stack.push(commit);

        while (!stack.isEmpty()) {
            Commit current = stack.pop();
            ancestors.add(current.getId());

            for (String parentId : current.getParent()) {
                Commit parentCommit = getCommit(parentId, null);
                if (parentCommit != null) {
                    stack.push(parentCommit);
                }
            }
        }

        return ancestors;
    }

    private static void handleMergeConflict(String fileName, Commit givenCommit) {
        File file = join(CWD, fileName);
        Blob currentBlob = null;
        Blob givenBlob = null;

        if (currentCommit.getBlobs().containsKey(fileName)) {
            currentBlob = readObject(join(BLOBS_DIR,
                    currentCommit.getBlobs().get(fileName).getId()), Blob.class);
        }
        if (givenCommit.getBlobs().containsKey(fileName)) {
            givenBlob = readObject(join(BLOBS_DIR,
                    givenCommit.getBlobs().get(fileName).getId()), Blob.class);
        }

        String currentContents = (currentBlob != null)
                ? new String(currentBlob.getContentBytes())
                : "";
        String givenContents = (givenBlob != null)
                ? new String(givenBlob.getContentBytes())
                : "";

        String conflictContent = "<<<<<<< HEAD\n"
                + currentContents
                + "=======\n"
                + givenContents
                + ">>>>>>>\n";

        writeContents(file, conflictContent);
        add(fileName);
    }

    private static void checkoutAndStageFile(String fileName, Commit commit) {
        checkoutCommit(commit.getId(), fileName);
        stageArea.stageFile(fileName, join(CWD, fileName));
    }

    private static Set<String> getUntrackedFiles(Commit currCommit) {
        Set<String> trackedFiles = new HashSet<>(currCommit.getBlobs().keySet());
        Set<String> allFiles = new HashSet<>(Objects.requireNonNull(plainFilenamesIn(CWD)));
        allFiles.removeAll(trackedFiles);
        return allFiles;
    }

    private static void checkForUntrackedFiles(Set<String> untrackedFiles, Commit givenCommit) {
        for (String file : givenCommit.getBlobs().keySet()) {
            if (untrackedFiles.contains(file) && !isFileTracked(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** add-remote command */
    public static void addRemote(String remoteName, String remoteDir) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        if (join(REMOTE, remoteName).exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }

        writeContents(join(REMOTE, remoteName), remoteDir);
        join(REMOTE_HEADS, remoteName).mkdir();
    }

    /** rm-remote command */
    public static void rmRemote(String remoteName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        if (!join(REMOTE, remoteName).exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }

        join(REMOTE, remoteName).delete();
    }

    /** push command */
    public static void push(String remoteName, String remoteBranchName) {
        File remoteBranch = checkFetchPush(remoteName, remoteBranchName);
        File remoteFile = join(REMOTE, remoteName);
        String remoteDir = readContentsAsString(remoteFile);
        String remoteCommitId = readContentsAsString(remoteBranch);
        Commit remoteCommit = getCommit(remoteCommitId, remoteName);
        Commit localCommit = getCurrentCommit();

        if (!isAncestorOf(localCommit, remoteCommit)) {
            System.out.println("Please pull down remote changes before pushing.");
            System.exit(0);
        }

        List<Commit> commitsToPush = getCommitsToPush(localCommit, remoteCommit);
        for (Commit commit : commitsToPush) {
            saveCommitToRemote(commit, remoteDir);
        }

        writeContents(remoteBranch, localCommit.getId());
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        File remoteBranch = checkFetchPush(remoteName, remoteBranchName);
        createBranchIfNotExist(remoteName, remoteBranchName);
        Commit remoteHead = getCommit(readContentsAsString(remoteBranch), remoteName);

        List<Commit> newCommits = fetchNewCommits(remoteHead, remoteName);
        for (Commit commit : newCommits) {
            saveCommitLocally(commit);
            for (Blob blob : commit.getBlobs().values()) {
                storeBlob(blob);
            }
        }


        writeContents(join(REMOTE_HEADS, remoteName, remoteBranchName), remoteHead.getId());
    }

    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);

        merge(remoteBranchName, remoteName);
    }

    private static boolean isAncestorOf(Commit localCommit, Commit remoteCommit) {
        List<String> remoteAncestors = getAncestors(localCommit);
        return remoteAncestors.contains(remoteCommit.getId());
    }

    private static List<Commit> getCommitsToPush(Commit localCommit, Commit ancestorCommit) {
        // Collects all commits from localCommit up to, but not including, ancestorCommit
        List<Commit> commitsToPush = new ArrayList<>();
        Stack<Commit> stack = new Stack<>();
        stack.push(localCommit);

        while (!stack.isEmpty()) {
            Commit current = stack.pop();
            if (current.getId().equals(ancestorCommit.getId())) {
                break;
            }
            commitsToPush.add(current);
            for (String parentId : current.getParent()) {
                Commit parentCommit = getCommit(parentId, null);
                if (parentCommit != null) {
                    stack.push(parentCommit);
                }
            }
        }
        return commitsToPush;
    }

    private static void saveCommitToRemote(Commit commit, String remoteDir) {
        File remoteCommitsDir = join(remoteDir, "commits");
        File commitDir = join(remoteCommitsDir, commit.getId().substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commit.getId().substring(2));
        writeObject(commitFile, commit);
    }

    private static void createBranchIfNotExist(String remoteName, String branchName) {
        if (!plainFilenamesIn(join(REMOTE_HEADS, remoteName)).contains(branchName)) {
            writeContents(join(REMOTE_HEADS, remoteName, branchName), "");
        }
    }

    private static List<Commit> fetchNewCommits(Commit remoteHead, String remoteName) {
        List<Commit> newCommits = new ArrayList<>();
        Stack<Commit> stack = new Stack<>();
        stack.push(remoteHead);

        while (!stack.isEmpty()) {
            Commit current = stack.pop();
            if (!currentCommitExistsLocally(current)) {
                newCommits.add(current);
            }
            for (String parentId : current.getParent()) {
                Commit parentCommit = getCommit(parentId, remoteName);
                if (parentCommit != null) {
                    stack.push(parentCommit);
                }
            }
        }
        return newCommits;
    }

    private static boolean currentCommitExistsLocally(Commit commit) {
        File commitDir = join(COMMITS_DIR, commit.getId().substring(0, 2));
        if (!commitDir.exists()) {
            return false;
        }
        for (File commitFile : Objects.requireNonNull(commitDir.listFiles())) {
            if (commitFile.getName().equals(commit.getId().substring(2))) {
                return true;
            }
        }
        return false;
    }

    private static void saveCommitLocally(Commit commit) {
        File commitDir = join(COMMITS_DIR, commit.getId().substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commit.getId().substring(2));
        writeObject(commitFile, commit);
    }

    private static File checkFetchPush(String remoteName, String remoteBranchName) {
        if (!join(REMOTE, remoteName).exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }

        File remoteFile = join(REMOTE, remoteName);
        String remoteDir = readContentsAsString(remoteFile);
        if (!join(remoteDir).exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteBranch = join(remoteDir, "REFS", "heads", remoteBranchName);
        if (!remoteBranch.exists()) {
            System.out.println("Remote branch not found.");
            System.exit(0);
        }

        return remoteBranch;
    }
}
