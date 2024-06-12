package gitlet;

import java.io.Serializable;
import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

public class StageArea implements Serializable, Dumpable {
    private final static File stage = Repository.STAGE;
    private static StageArea instance;
    private final Map<String, Blob> stagedFiles;
    private final Set<String> removedFiles;

    public StageArea() {
        stagedFiles = new HashMap<>();
        removedFiles = new HashSet<>();
    }

    public static StageArea getInstance() {
        if (instance == null) {
            if (stage.exists()) {
                instance = readObject(stage, StageArea.class);
            } else {
                instance = new StageArea();
            }
        }
        return instance;
    }

    public void save() {
        writeObject(Repository.STAGE, this);
    }

    public void stageFile(String fileName, File file) {
        Blob blob = new Blob(file);
        stagedFiles.put(fileName, blob);
        writeObject(stage, this);
    }

    public void unstageFile(String fileName) {
        stagedFiles.remove(fileName);
        writeObject(stage, this);
    }

    public void markRemoved(String fileName) {
        removedFiles.add(fileName);
    }

    public void unmarkRemoved(String fileName) {
        removedFiles.remove(fileName);
    }

    public boolean isRemoved(String fileName) {
        return removedFiles.contains(fileName);
    }

    public Set<String> getRemovedFiles() {
        return removedFiles;
    }

    public boolean isFileStaged(String fileName) {
        return stagedFiles.containsKey(fileName);
    }

    public Map<String, Blob> getStagedFiles() {
        return stagedFiles;
    }

    public void clear() {
        stagedFiles.clear();
        removedFiles.clear();
        writeObject(stage, this);
    }

    @Override
    public boolean dump() {
        System.out.println("Stage Area: ");
        for (Map.Entry<String, Blob> entry : stagedFiles.entrySet()) {
            System.out.println("File Name: " + entry.getKey());
            entry.getValue().dump();
        }
        return false;
    }
}
