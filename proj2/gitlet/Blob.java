package gitlet;

import java.io.Serializable;
import java.io.File;
import java.nio.file.Path;
import java.io.IOException;

public class Blob implements Serializable, Dumpable {
    private byte[] contentBytes;
    private String id;

    public Blob(File filePath) {
        contentBytes = Utils.readContents(filePath);
        id = Utils.sha1(contentBytes);
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public String getId() {
        return id;
    }

    @Override
    public void dump() {
        System.out.println("Blob ID: " + id);
    }

}
