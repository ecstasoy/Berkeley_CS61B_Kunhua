package gitlet;

import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class does at a high level.
 *
 *
 *  @author Kunhua Huang
 */
public class Commit implements Serializable, Dumpable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    private String id;
    /** The message of this Commit. */
    private String message;
    private Date timestamp;
    private List<String> parents;
    private Map<String, Blob> tracked;

    // Constructor for normal commit
    public Commit(String message, String parent, Map<String, Blob> blobs) {
        this(message, Arrays.asList(parent), blobs);
    }

    // Constructor for merge commit
    public Commit(String message, List<String> parents, Map<String, Blob> blobs) {
        this.message = message;
        this.parents = new ArrayList<>(parents);
        this.tracked = new HashMap<>();
        tracked.putAll(blobs);
        this.timestamp = new Date();
        this.id = generateId();
    }

    public String generateId() {
        assert timestamp != null;
        return Utils.sha1(message + timestamp.toString());
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        // Setting the locale to US to ensure English names for days and months
        return String.format(Locale.US, "%ta %tb %td %tT %tY %tz", timestamp, timestamp, timestamp, timestamp, timestamp, timestamp);
    }

    public List<String> getParent() {
        return parents;
    }

    public void setParent(List<String> parents) {
        this.parents = parents;
    }

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
