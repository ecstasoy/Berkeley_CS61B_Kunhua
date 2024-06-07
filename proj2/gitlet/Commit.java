package gitlet;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

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
    private String parent;
    private Map<String, String> tracked;

    public Commit(String message, String parent, Map<String, Blob> blobs) {
        this.id = generateId();
        this.message = message;
        this.parent = parent;
        this.tracked = new HashMap<>();
        for (Map.Entry<String, Blob> entry : blobs.entrySet()) {
            tracked.put(entry.getKey(), entry.getValue().getId());
        }
        this.timestamp = new Date();
    }

    public String generateId() {
        assert timestamp != null;
        return Utils.sha1(message + timestamp.toString(), parent, tracked);
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

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<String, String> getBlobs() {
        return tracked;
    }


    @Override
    public void dump() {
        System.out.println("===");
        System.out.println("commit " + id);
        System.out.println("Date: " + timestamp.toString());
        System.out.println(message);
        System.out.println();
    }
}
