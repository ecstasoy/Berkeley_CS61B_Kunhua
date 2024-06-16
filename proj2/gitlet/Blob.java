package gitlet;

import java.io.Serializable;
import java.io.File;


/** Represents a gitlet blob object.
 *
 * This class represents a blob object in gitlet. It contains the content of the
 * file that is being tracked by gitlet. It also contains a unique id that is
 * generated based on the content of the file.
 *
 * @author Kunhua Huang
 */
public class Blob implements Serializable, Dumpable {
    private final byte[] contentBytes;
    private final String id;

    /** Constructor for the blob object.
     *
     * @param filePath the path of the file that is being tracked
     */
    public Blob(File filePath) {
        contentBytes = Utils.readContents(filePath);
        id = Utils.sha1((Object) contentBytes);
    }

    /** getContentBytes
     * Returns the content of the file that is being tracked.
     *
     * @return the content of the file that is being tracked
     */
    public byte[] getContentBytes() {
        return contentBytes;
    }

    /** getId
     * Returns the id of the blob object.
     *
     * @return the id of the blob object
     */
    public String getId() {
        return id;
    }

    @Override
    public boolean dump() {
        System.out.println("Blob ID: " + id);
        return false;
    }

}
