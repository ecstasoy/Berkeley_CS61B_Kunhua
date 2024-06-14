package gitlet;

import java.io.Serializable;
import java.io.File;

public class Blob implements Serializable, Dumpable {
    private final byte[] contentBytes;
    private final String id;

    public Blob(File filePath) {
        contentBytes = Utils.readContents(filePath);
        id = Utils.sha1((Object) contentBytes);
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean dump() {
        System.out.println("Blob ID: " + id);
        return false;
    }

}
