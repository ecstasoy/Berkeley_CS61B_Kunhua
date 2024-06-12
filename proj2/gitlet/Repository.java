package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Kunhua Huang
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
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
    public static StageArea stageArea;

    public Repository() {

    }

    /** init command */
    public static void init() throws IOException {
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        STAGE.mkdir();
        BLOBS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        refs.mkdir();
        refsHeads.mkdir();
        HEAD.createNewFile();

        initCommit();
        initHEAD();
        initRefsHeads();

        stageArea = new StageArea();
    }

    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    public static void initCommit() {
        Commit initCommit = new Commit("initial commit", null, new HashMap<>());
        currentCommit = initCommit;
        saveCommit(initCommit);
    }

    public static void initHEAD() {
        writeContents(HEAD, "master");
    }

    public static void initRefsHeads() {
        File master = join(refsHeads, "master");
        writeContents(master, currentCommit.getId());
    }

    /** add command */
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
        String lastCommittedId = currentCommit.getBlobs().get(fileName);
        if (lastCommittedId != null && lastCommittedId.equals(blob.getId())) {
            stageArea.unstageFile(fileName);
            System.out.println("File has not been modified since the last commit.");
            System.exit(0);
        }
        stageArea.stageFile(fileName, file);
        storeBlob(blob);
    }

    public static void storeBlob(Blob blob) {
        File blobFile = join(BLOBS_DIR, blob.getId());
        writeObject(blobFile, blob);
    }

    /** commit command */
    public static void commit(String message) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (stageArea.getStagedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        String parentCommitId = currentCommit.getId();
        Map<String, Path> stagedFiles = new HashMap<>();
        for (Map.Entry<String, Blob> entry : stageArea.getStagedFiles().entrySet()) {
            stagedFiles.put(entry.getKey(), join(BLOBS_DIR, entry.getValue().getId()).toPath());
        }
        Commit newCommit = createCommit(message, parentCommitId, stagedFiles);
        stageArea.clear();
        currentCommit = newCommit;
        writeContents(HEAD, currentCommit.getId());
        writeContents(join(refsHeads, "master"), currentCommit.getId());
    }

    public static Commit createCommit(String commitMessage, String parentCommitId, Map<String, Path> stagedFiles) {
        Map<String, Blob> blobs = new HashMap<>();
        for (Map.Entry<String, Path> entry : stagedFiles.entrySet()) {
            Blob blob = new Blob(entry.getValue().toFile());
            blobs.put(entry.getKey(), blob);
        }
        Commit newCommit = new Commit(commitMessage, parentCommitId, blobs);
        newCommit.setParent(parentCommitId);
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

    /** rm command */
    public static void rm(String fileName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!stageArea.isFileStaged(fileName) && !currentCommit.getBlobs().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stageArea.isFileStaged(fileName)) {
            stageArea.unstageFile(fileName);
        }
        if (currentCommit.getBlobs().containsKey(fileName)) {
            stageArea.markRemoved(fileName);
            Utils.restrictedDelete(fileName);
        }
    }

    /** log command */
    public static void log() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Commit current = currentCommit;
        while (current != null) {
            System.out.println("===");
            System.out.println("commit " + current.getId());

            // TODO: print merge commit

            System.out.println("Date: " + current.getTimestamp().toString());
            System.out.println(current.getMessage());
            current = getCommit(current.getParent());
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

    /** global-log command */
    public static void globalLog() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        for (String commitFile : plainFilenamesIn(COMMITS_DIR)) {
            Commit commit = getCommit(commitFile);
            System.out.println("===");
            System.out.println("commit " + commit.getId());

            // TODO: print merge commit

            System.out.println("Date: " + commit.getTimestamp().toString());
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }

    /** find command */
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

    /** status command */
    public static void status() {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        System.out.println("=== Branches ===");
        String currentBranch = readContentsAsString(HEAD);
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

    /** checkout command */
    public static void checkoutFile(String fileName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        String currentBranch = readContentsAsString(HEAD);
        String headCommitId = readContentsAsString(join(refsHeads, currentBranch));
        Commit headCommit = getCommit(headCommitId);

        if (!currentCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = new Blob(join(BLOBS_DIR, headCommit.getBlobs().get(fileName)));
        File file = join(CWD, fileName);
        writeContents(file, blob.getContentBytes());
    }

    public static void checkoutCommit(String commitId, String fileName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Commit commit = getCommit(commitId);

        if (!commit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = new Blob(join(BLOBS_DIR, commit.getBlobs().get(fileName)));
        File file = join(CWD, fileName);
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

        String currentBranch = readContentsAsString(HEAD);
        String currentCommitId = readContentsAsString(join(refsHeads, currentBranch));
        Commit currentCommit = getCommit(currentCommitId);

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

    /** branch command */
    public static void branch(String branchName) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (plainFilenamesIn(refsHeads).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String currentBranch = readContentsAsString(HEAD);
        String currentCommitId = readContentsAsString(join(refsHeads, currentBranch));
        writeContents(join(refsHeads, branchName), currentCommitId);
    }

    /** rm-branch command */
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

    /** reset command */
    public static void reset(String commitId) {
        if (!isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Commit commit = getCommit(commitId);

        for (String fileName : currentCommit.getBlobs().keySet()) {
            if (!commit.getBlobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
        for (String fileName : commit.getBlobs().keySet()) {
            Blob blob = new Blob(join(BLOBS_DIR, commit.getBlobs().get(fileName)));
            File file = join(CWD, fileName);
            writeContents(file, blob.getContentBytes());
        }
        writeContents(join(refsHeads, readContentsAsString(HEAD)), commitId);
        currentCommit = commit;
    }
}
