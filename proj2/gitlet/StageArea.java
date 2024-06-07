package gitlet;

import java.io.Serializable;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class StageArea implements Serializable, Dumpable {
    final static File stage = Repository.STAGE;
    private Map<String, Blob> stagedFiles;

    public StageArea() {
        stagedFiles = Utils.readObject(stage, StageArea.class).stagedFiles;
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

    public boolean isFileStaged(String fileName) {
        return stagedFiles.containsKey(fileName);
    }

    public Map<String, Blob> getStagedFiles() {
        return stagedFiles;
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
