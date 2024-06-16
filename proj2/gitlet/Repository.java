package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  This class contains the main logic for the Gitlet version-control system.
 *  It provides methods to handle all the commands that the user can input.
 *  It also contains methods to interact with the filesystem to store and retrieve
 *  commit objects, blobs, and other data.
 *
 *  @author Kunhua Huang
 */
public class Repository {

    public static final File CWD = new File(System.getProperty("user.dir"));
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

    /** init command
     *  Creates a new Gitlet version-control system in the current directory.
     *
     *  @throws IOException if an I/O error occurs
     */
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
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

    /** isInitialized
     *  Checks if the current directory is a Gitlet repository.
     *  If not, prints an error message and exits the program.
     *  This method is used to ensure that the user is in a Gitlet repository
     *  before executing any commands.
     */
    public static void isInitialized() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /** initCommit
     *  Initializes the initial commit for the Gitlet repository.
     *  The initial commit has no parent commits and no files.
     *  It is the first commit in the repository.
     *  The HEAD file is updated to point to the initial commit.
     *  The master branch is created and points to the initial commit.
     *  The initial commit is saved to the .gitlet/commits directory.
     */
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

    /** initHEAD
     *  Initializes the HEAD file to point to the master branch.
     *  This method is called after the initial commit is created.
     *  The HEAD file is used to keep track of the current branch.
     *  The master branch is the default branch in Gitlet.
     *  The HEAD file is updated to point to the master branch.
     */
    public static void initHEAD() {
        writeContents(HEAD, "master");
    }

    /** initRefsHeads
     *  Initializes the master branch to point to the initial commit.
     *  This method is called after the initial commit is created.
     *  The master branch is the default branch in Gitlet.
     *  The master branch is created and points to the initial commit.
     */
    public static void initRefsHeads() {
        File master = join(REFS_HEADS, "master");
        writeContents(master, currentCommit.getId());
    }

    /** add command
     *  Adds a copy of the file as it currently exists to the staging area.
     *  The file is added to the staging area with the same name it has in the
     *  working directory.
     *
     *  @param fileName the name of the file to add
     */
    public static void add(String fileName) {
        isInitialized();

        File file = join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
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

    /** storeBlob
     *  Stores the blob object in the .gitlet/blobs directory.
     *  The blob object is stored as a file with the name of its unique id.
     *
     *  @param blob the blob object to store
     */
    private static void storeBlob(Blob blob) {
        File blobFile = join(BLOBS_DIR, blob.getId());
        writeObject(blobFile, blob);
    }

    /** getCurrentCommit
     *  Returns the current commit object.
     *  The current commit is determined by the HEAD file.
     *  If the HEAD file points to a branch, the current commit is the commit
     *  that the branch points to.
     *  If the HEAD file points to a commit, the current commit is the commit
     *  that the HEAD file points to.
     *
     *  @return the current commit object
     */
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

    /** getCurrentBranch
     *  Returns the current branch.
     *  The current branch is determined by the HEAD file.
     *  If the HEAD file points to a branch, the current branch is the branch
     *  that the HEAD file points to.
     *
     *  @return the current branch
     */
    private static String getCurrentBranch() {
        if (currentBranch == null) {
            currentBranch = readContentsAsString(HEAD);
        }
        return currentBranch;
    }

    /** commit command
     *  Saves a snapshot of certain files in the current commit and staging area.
     *  The commit is created with the given message.
     *
     *  @param message the commit message
     */
    public static void commit(String message) {
        isInitialized();

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

    /** createCommit
     *  Creates a new commit object with the given message, parent commit id,
     *  and blobs.
     *  The commit is saved to the .gitlet/commits directory.
     *
     *  @param message the commit message
     *  @param parentId the parent commit id
     *  @param blobs the blobs to store in the commit
     *  @return the new commit object
     */
    public static Commit createCommit(String message, String parentId,
                                      Map<String, Blob> blobs) {
        Commit newCommit = new Commit(message, parentId, blobs);
        newCommit.setParent(Collections.singletonList(parentId));
        saveCommit(newCommit);
        return newCommit;
    }

    /** saveCommit
     *  Saves the commit object to the .gitlet/commits directory.
     *
     *  @param commit the commit object to save
     */
    public static void saveCommit(Commit commit) {
        String commitId = commit.getId();
        File commitDir = join(COMMITS_DIR, commitId.substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commitId.substring(2));
        writeObject(commitFile, commit);
    }

    /** rm command
     *  Unstages the file if it is currently staged for addition.
     *  If the file is tracked in the current commit, marks it to indicate
     *  that it is not to be included in the next commit.
     *
     *  @param fileName the name of the file to remove
     */
    public static void rm(String fileName) {
        isInitialized();

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

    /** log command
     *  Prints information about each commit in the current branch.
     */
    public static void log() {
        isInitialized();
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

    /** getNextCommit
     *  Returns the next commit to follow in the log.
     *  The next commit is determined by the current commit's parent.
     *
     *  @param current the current commit
     *  @return the next commit to follow
     */
    private static Commit getNextCommit(Commit current) {
        List<String> parents = current.getParent();
        if (parents != null && !parents.isEmpty()) {
            return getCommit(parents.get(0), null);
        }
        return null;  // No more parents to follow
    }

    /** getCommit
     *  Returns the commit object with the given id.
     *  The commit object is retrieved from the .gitlet/commits directory.
     *
     *  @param id the id of the commit to retrieve
     *  @param remoteName the name of the remote repository
     *  @return the commit object with the given id
     */
    private static Commit getCommit(String id, String remoteName) {

        File commitDir = remoteName == null ? join(COMMITS_DIR, id.substring(0, 2))
                : join(readContentsAsString(join(REMOTE, remoteName)),
                "commits", id.substring(0, 2));

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

    /** global-log command
     *  Prints information about all commits in the repository.
     */
    public static void globalLog() {
        isInitialized();

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

    /** find command
     *  Prints the commit id of the commit with the given message.
     *
     *  @param commitMessage the message of the commit to find
     */
    public static void find(String commitMessage) {
        isInitialized();
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

    /** status command
     *  Prints the status of the repository.
     */
    public static void status() {
        isInitialized();

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

    /** printModifiedFiles
     *  Prints the files that have been modified or deleted since the last commit.
     */
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

    /** isConflict
     *  Checks if the file has a conflict.
     *
     *  @param fileName the name of the file to check
     *  @return true if the file has a conflict, false otherwise
     */
    private static boolean isConflict(String fileName) {
        File file = join(CWD, fileName);
        // read the first line of that file
        String firstLine = readContentsAsString(file).split("\n")[0];
        return firstLine.equals("<<<<<<< HEAD");
    }

    /** checkoutFile command
     *  Checks out the file from the current commit.
     *
     *  @param fileName the name of the file to checkout
     */
    public static void checkoutFile(String fileName) {
        isInitialized();
        File file = join(CWD, fileName);
        currentCommit = getCurrentCommit();
        isFileExistInCommit(fileName, file, currentCommit);
    }

    /** isFileExistInCommit
     *  Checks if the file exists in the given commit.
     *  If the file does not exist, prints an error message and exits the program.
     *
     *  @param fileName the name of the file to check
     *  @param file the file to write to
     *  @param currCommit the commit to check
     */
    private static void isFileExistInCommit(String fileName, File file, Commit currCommit) {
        File blobFile = join(BLOBS_DIR, currCommit.getBlobs().get(fileName).getId());

        if (!currCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = readObject(blobFile, Blob.class);
        writeContents(file, (Object) blob.getContentBytes());
    }

    /** checkoutCommit command
     *  Checks out the file from the given commit.
     *
     *  @param commitId the id of the commit to check out
     *  @param fileName the name of the file to check out
     */
    public static void checkoutCommit(String commitId, String fileName) {
        isInitialized();
        File file = join(CWD, fileName);
        Commit commit = getCommit(commitId, null);

        if (commit.getBlobs().get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        isFileExistInCommit(fileName, file, commit);
    }

    /** checkoutBranch command
     *  Checks out the given branch.
     *
     *  @param branchName the name of the branch to check out
     */
    public static void checkoutBranch(String branchName) {
        isInitialized();

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

    /** isFileUntracked
     *  Checks if there are untracked files in the way of the checkout.
     *
     *  @param targetCommit the commit to check
     */
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

    /** isFileTracked
     *  Checks if the file is tracked in any commit.
     *
     *  @param fileName the name of the file to check
     *  @return true if the file is tracked, false otherwise
     */
    private static boolean isFileTracked(String fileName) {
        Set<String> allCommits = getAllCommitIds();
        File file = join(CWD, fileName);
        String currentFileSha1 = file.exists() ? sha1((Object) readContents(file)) : null;

        for (String commitId : allCommits) {
            if (isFileInCommit(fileName, commitId, currentFileSha1)) {
                return true;
            }
        }

        return false;
    }

    /** isFileInCommit
     *  Checks if the file is in the given commit.
     *
     *  @param fileName the name of the file to check
     *  @param commitId the id of the commit to check
     *  @param currentFileSha1 the sha1 of the current file
     *  @return true if the file is in the commit, false otherwise
     */
    private static boolean isFileInCommit(String fileName, String commitId, String currentFileSha1) {
        Commit commit = getCommit(commitId, null);
        if (commit != null && commit.getBlobs().containsKey(fileName)) {
            String fileSha1 = commit.getBlobs().get(fileName).getId();
            return !fileSha1.equals(currentFileSha1);
        }
        return false;
    }

    /** getAllCommitIds
     *  Returns a set of all commit ids in the repository.
     *
     *  @return a set of all commit ids in the repository
     */
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

    /** branch command
     *  Creates a new branch with the given name.
     *
     *  @param branchName the name of the branch to create
     */
    public static void branch(String branchName) {
        isInitialized();
        if (Objects.requireNonNull(plainFilenamesIn(REFS_HEADS)).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        currentBranch = getCurrentBranch();
        currentCommit = getCurrentCommit();
        writeContents(join(REFS_HEADS, branchName), currentCommit.getId());
    }

    /** rm-branch command
     *  Deletes the branch with the given name.
     *
     *  @param branchName the name of the branch to delete
     */
    public static void rmBranch(String branchName) {
        isInitialized();
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

    /** reset command
     *  Resets the current branch to the commit with the given id.
     *
     *  @param commitId the id of the commit to reset to
     */
    public static void reset(String commitId) {
        isInitialized();

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

    /** merge command
     *  Merges the given branch into the current branch.
     *
     *  @param branchName the name of the branch to merge
     *  @param remoteName the name of the remote repository if applicable
     */
    public static void merge(String branchName, String remoteName) {
        isInitialized();

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

    /** mergeHelper
     *  Helper method for the merge command.
     *  Merges the given branch into the current branch.
     *
     *  @param givenCommit the commit to merge
     *  @param splitPoint the split point commit
     *  @param branchName the name of the branch to merge
     *  @param remoteName the name of the remote repository if applicable
     */
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

    /** createMergeCommit
     *  Creates a merge commit with the given message, parents, and blobs.
     *
     *  @param message the commit message
     *  @param parents the parent commit ids
     *  @param blobs the blobs to store in the commit
     */
    private static void createMergeCommit(String message,
                                          List<String> parents, Map<String, Blob> blobs) {
        Commit mergeCommit = new Commit(message, parents, blobs);
        saveCommit(mergeCommit);
        writeContents(join(REFS_HEADS, readContentsAsString(HEAD)), mergeCommit.getId());
    }

    /** findSplitPoint
     *  Finds the split point commit between the current commit and the given commit.
     *
     *  @param branchName the name of the branch to merge
     *  @param remoteName the name of the remote repository if applicable
     *  @return the split point commit
     */
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

    /** getAncestors
     *  Returns a list of all ancestor commit ids of the given commit.
     *
     *  @param commit the commit to get ancestors of
     *  @return a list of all ancestor commit ids
     */
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

    /** handleMergeConflict
     *  Handles a merge conflict by writing the conflict content to the file.
     *
     *  @param fileName the name of the file to handle
     *  @param givenCommit the commit to merge
     */
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

    /** checkout command
     *  Checks out the file from the given commit or branch.
     *
     *  @param commit the id of the commit to check out
     *  @param fileName the name of the file to check out
     */
    private static void checkoutAndStageFile(String fileName, Commit commit) {
        checkoutCommit(commit.getId(), fileName);
        stageArea.stageFile(fileName, join(CWD, fileName));
    }

    /** getUntrackedFiles
     *  Returns a set of all untracked files in the working directory.
     *
     *  @param currCommit the current commit
     *  @return a set of all untracked files in the working directory
     */
    private static Set<String> getUntrackedFiles(Commit currCommit) {
        Set<String> trackedFiles = new HashSet<>(currCommit.getBlobs().keySet());
        Set<String> allFiles = new HashSet<>(Objects.requireNonNull(plainFilenamesIn(CWD)));
        allFiles.removeAll(trackedFiles);
        return allFiles;
    }

    /** checkForUntrackedFiles
     *  Checks if there are untracked files in the way of the merge.
     *
     *  @param untrackedFiles the set of untracked files
     *  @param givenCommit the commit to merge
     */
    private static void checkForUntrackedFiles(Set<String> untrackedFiles, Commit givenCommit) {
        for (String file : givenCommit.getBlobs().keySet()) {
            if (untrackedFiles.contains(file) && !isFileTracked(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** add-remote command
     * Adds a remote repository with the given name and directory.
     *
     * @param remoteName the name of the remote repository
     * @param remoteDir the directory of the remote repository
     */
    public static void addRemote(String remoteName, String remoteDir) {
        isInitialized();

        if (join(REMOTE, remoteName).exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }

        writeContents(join(REMOTE, remoteName), remoteDir);
        join(REMOTE_HEADS, remoteName).mkdir();
    }

    /** rm-remote command
     * Removes the remote repository with the given name.
     *
     * @param remoteName the name of the remote repository
     */
    public static void rmRemote(String remoteName) {
        isInitialized();

        if (!join(REMOTE, remoteName).exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }

        join(REMOTE, remoteName).delete();
    }

    /** push command
     * Pushes the current branch to the given remote branch.
     *
     * @param remoteName the name of the remote repository
     * @param remoteBranchName the name of the remote branch
     */
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

    /** fetch command
     * Fetches the given remote branch to the local repository.
     *
     * @param remoteName the name of the remote repository
     * @param remoteBranchName the name of the remote branch
     */
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

    /** pull command
     * Pulls the given remote branch to the local repository.
     *
     * @param remoteName the name of the remote repository
     * @param remoteBranchName the name of the remote branch
     */
    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);

        merge(remoteBranchName, remoteName);
    }

    /** isAncestorOf
     * Checks if the local commit is an ancestor of the remote commit.
     *
     * @param localCommit the local commit
     * @param remoteCommit the remote commit
     * @return true if the local commit is an ancestor of the remote commit, false otherwise
     */
    private static boolean isAncestorOf(Commit localCommit, Commit remoteCommit) {
        List<String> remoteAncestors = getAncestors(localCommit);
        return remoteAncestors.contains(remoteCommit.getId());
    }

    /** getCommitsToPush
     * Returns a list of all commits to push to the remote repository.
     *
     * @param localCommit the local commit
     * @param ancestorCommit the ancestor commit
     * @return a list of all commits to push to the remote repository
     */
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

    /** saveCommitToRemote
     * Saves the given commit to the remote repository.
     *
     * @param commit the commit to save
     * @param remoteDir the directory of the remote repository
     */
    private static void saveCommitToRemote(Commit commit, String remoteDir) {
        File remoteCommitsDir = join(remoteDir, "commits");
        File commitDir = join(remoteCommitsDir, commit.getId().substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commit.getId().substring(2));
        writeObject(commitFile, commit);
    }

    /** createBranchIfNotExist
     * Creates the remote branch if it does not exist.
     *
     * @param remoteName the name of the remote repository
     * @param branchName the name of the remote branch
     */
    private static void createBranchIfNotExist(String remoteName, String branchName) {
        if (!plainFilenamesIn(join(REMOTE_HEADS, remoteName)).contains(branchName)) {
            writeContents(join(REMOTE_HEADS, remoteName, branchName), "");
        }
    }

    /** fetchNewCommits
     * Returns a list of all new commits to fetch from the remote repository.
     *
     * @param remoteHead the head commit of the remote repository
     * @param remoteName the name of the remote repository
     * @return a list of all new commits to fetch from the remote repository
     */
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

    /** currentCommitExistsLocally
     * Checks if the current commit exists locally.
     *
     * @param commit the commit to check
     * @return true if the current commit exists locally, false otherwise
     */
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

    /** saveCommitLocally
     * Saves the given commit locally.
     *
     * @param commit the commit to save
     */
    private static void saveCommitLocally(Commit commit) {
        File commitDir = join(COMMITS_DIR, commit.getId().substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, commit.getId().substring(2));
        writeObject(commitFile, commit);
    }

    /** checkFetchPush
     * Checks if the remote repository and branch exist.
     *
     * @param remoteName the name of the remote repository
     * @param remoteBranchName the name of the remote branch
     * @return the remote branch file
     */
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
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }

        return remoteBranch;
    }
}
