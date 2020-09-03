package messges;

import messges.AbstractMsg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileTransferMsg extends AbstractMsg {
    private String fileName;
    //    private String storage;
    private String path;
    private byte[] data;
    //private int size;

    public FileTransferMsg (Path filePaths) throws IOException {
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        this.data = Files.readAllBytes(filePaths);
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

