package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

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
        File commitFile = join(COMMITS_DIR, commit.getId());
        writeObject(commitFile, commit);
    }

    public static Commit getCommit(String id) {
        File commitFile = join(COMMITS_DIR, id);
        return readObject(commitFile, Commit.class);
    }

}
