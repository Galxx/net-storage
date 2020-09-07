package messages;

import java.io.IOException;
import java.nio.file.Path;


public class FileTransferMsg extends AbstractMsg {
    private String fileName;
    //    private String storage;
    private String path;
    private byte[] data;
    //private long size;
    private boolean islast;
    private boolean isFirst;


    public FileTransferMsg (Path filePaths,byte[] data, boolean islast, boolean isFirst) throws IOException {
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        //this.data = Files.readAllBytes(filePaths);
        this.data = data;
        this.islast = islast;
        this.isFirst = isFirst;
    }

    public boolean getislast() {
        return islast;
    }

    public boolean getisFirst() {
        return isFirst;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

}

