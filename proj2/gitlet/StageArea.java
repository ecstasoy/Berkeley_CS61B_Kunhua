package gitlet;

import java.io.Serializable;
import java.io.File;
import java.util.*;
import static gitlet.Utils.*;

/** Represents the stage area in gitlet.
 *
 * The stage area is where files are staged before they are committed. It
 * contains a map of file names to blobs that represent the files that are
 * staged. It also contains a set of file names that have been marked for
 * removal. The stage area is serialized and stored in the .gitlet directory.
 *
 * @author Kunhua Huang
 */
public class StageArea implements Serializable, Dumpable {
    private static final File STAGE = Repository.STAGE;
    private static StageArea instance;
    private final Map<String, Blob> stagedFiles;
    private final Set<String> removedFiles;

    /** Constructor for the stage area. */
    public StageArea() {
        stagedFiles = new HashMap<>();
        removedFiles = new HashSet<>();
        writeObject(STAGE, this);
    }

    /** getInstance
     * Returns the instance of the stage area. If the stage area has not been
     * initialized, it reads the stage area from the .gitlet directory.
     * @return the instance of the stage area
     *
     * @return a instance of the stage area
     */
    public static StageArea getInstance() {
        if (instance == null) {
            if (STAGE.exists()) {
                instance = readObject(STAGE, StageArea.class);
            } else {
                instance = new StageArea();
            }
        }
        return instance;
    }

    /** stageFile
     * Stages a file in the stage area.
     *
     * @param fileName the name of the file
     * @param file the file to be staged
     */
    public void stageFile(String fileName, File file) {
        Blob blob = new Blob(file);
        stagedFiles.put(fileName, blob);
        writeObject(STAGE, this);
    }

    /** unstageFile
     * Unstages a file in the stage area.
     *
     * @param fileName the name of the file
     */
    public void unstageFile(String fileName) {
        stagedFiles.remove(fileName);
        writeObject(STAGE, this);
    }

    /** markRemoved
     * Marks a file for removal in the stage area.
     *
     * @param fileName the name of the file
     */
    public void markRemoved(String fileName) {
        removedFiles.add(fileName);
        writeObject(STAGE, this);
    }

    /** unmarkRemoved
     * Unmarks a file for removal in the stage area.
     *
     * @param fileName the name of the file
     */
    public void unmarkRemoved(String fileName) {
        removedFiles.remove(fileName);
        writeObject(STAGE, this);
    }

    /** isRemoved
     * Checks if a file has been marked for removal.
     *
     * @param fileName the name of the file
     * @return true if the file has been marked for removal, false otherwise
     */
    public boolean isRemoved(String fileName) {
        return removedFiles.contains(fileName);
    }

    /** getRemovedFiles
     * Returns the set of files that have been marked for removal.
     *
     * @return the set of files that have been marked for removal
     */
    public Set<String> getRemovedFiles() {
        return removedFiles;
    }

    /** isFileStaged
     * Checks if a file has been staged.
     *
     * @param fileName the name of the file
     * @return true if the file has been staged, false otherwise
     */
    public boolean isFileStaged(String fileName) {
        return stagedFiles.containsKey(fileName);
    }

    /** getStagedFiles
     * Returns the map of files that have been staged.
     *
     * @return the map of files that have been staged
     */
    public Map<String, Blob> getStagedFiles() {
        return stagedFiles;
    }

    /** clear
     * Clears the stage area.
     */
    public void clear() {
        stagedFiles.clear();
        removedFiles.clear();
        writeObject(STAGE, this);
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
