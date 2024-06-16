package gitlet;

import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *
 *  This class represents a commit object in gitlet. It contains the message,
 *  timestamp, parent commit id, and the blobs that are tracked by this commit.
 *  It also contains a unique id that is generated based on the message and
 *  timestamp of the commit.
 *
 *  @author Kunhua Huang
 */
public class Commit implements Serializable, Dumpable {

    private final String id;
    private final String message;
    private final Date timestamp;
    private List<String> parents;
    private final Map<String, Blob> tracked;

    /** Constructor for initial commit.
     *
     * @param message The message of the commit.
     * @param blobs The blobs that are tracked by this commit.
     */
    public Commit(String message, String parent, Map<String, Blob> blobs) {
        this(message, Collections.singletonList(parent), blobs);
    }

    /** Constructor for non-initial commit.
     *
     * @param message The message of the commit.
     * @param parents The parent commit ids.
     * @param blobs The blobs that are tracked by this commit.
     */
    public Commit(String message, List<String> parents, Map<String, Blob> blobs) {
        this.message = message;
        this.parents = new ArrayList<>(parents);
        this.tracked = new HashMap<>();
        tracked.putAll(blobs);
        this.timestamp = new Date();
        this.id = generateId();
    }

    /** Generate the unique id for this commit.
     *
     * @return The unique id of this commit.
     */
    public String generateId() {
        assert timestamp != null;
        return Utils.sha1(message + timestamp);
    }

    /** Get the id of this commit.
     *
     * @return The id of this commit.
     */
    public String getId() {
        return id;
    }

    /** Get the message of this commit.
     *
     * @return The message of this commit.
     */
    public String getMessage() {
        return message;
    }

    /** Get the timestamp of this commit.
     *
     * @return The timestamp of this commit.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Get the formatted timestamp of this commit.
     *
     * @return The formatted timestamp of this commit.
     */
    public String getFormattedTimestamp() {
        // Setting the locale to US to ensure English names for days and months
        return String.format(Locale.US, "%ta %tb %td %tT %tY %tz",
                timestamp, timestamp, timestamp, timestamp, timestamp, timestamp);
    }

    /** Get the parent commit id.
     *
     * @return The parent commit id.
     */
    public List<String> getParent() {
        return parents;
    }

    /** Set the parent commit id.
     *
     * @param newParents The new parent commit id.
     */
    public void setParent(List<String> newParents) {
        this.parents = newParents;
    }

    /** Get the blobs that are tracked by this commit.
     *  The key is the file name and the value is the blob object.
     *
     * @return The blobs that are tracked by this commit.
     */
    public Map<String, Blob> getBlobs() {
        return tracked;
    }


    @Override
    public boolean dump() {
        System.out.println("===");
        System.out.println("commit " + id);
        System.out.println("Date: " + timestamp.toString());
        System.out.println(message);
        System.out.println();
        return false;
    }
}
