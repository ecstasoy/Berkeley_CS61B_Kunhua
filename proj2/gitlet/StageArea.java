package gitlet;

import jdk.jshell.execution.Util;

import java.io.Serializable;
import java.io.File;
import java.util.*;

public class StageArea implements Serializable, Dumpable {
    final static File stage = Repository.STAGE;
    private final Map<String, Blob> stagedFiles;
    private final Set<String> removedFiles;

    public StageArea() {
        stagedFiles = Utils.readObject(stage, StageArea.class).stagedFiles;
        removedFiles = new HashSet<>();
    }

    public void stageFile(String fileName, File file) {
        Blob blob = new Blob(file);
        stagedFiles.put(fileName, blob);
        Utils.writeObject(stage, this);
    }

    public void unstageFile(String fileName) {
        stagedFiles.remove(fileName);
        Utils.writeObject(stage, this);
    }

    public void markRemoved(String fileName) {
        removedFiles.add(fileName);
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
        Utils.writeObject(stage, this);
    }

    @Override
    public void dump() {
        System.out.println("Stage Area: ");
        for (Map.Entry<String, Blob> entry : stagedFiles.entrySet()) {
            System.out.println("File Name: " + entry.getKey());
            entry.getValue().dump();
        }
    }
}
