package unimelb.bitbox.service;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.*;

import java.net.Socket;

public interface FileService {

    public Document requestGenerate(FileSystemEvent fileSystemEvent);

    public Document OperateAndResponseGenerate(Socket socket, Document request, FileSystemManager fileSystemManager);
    // we can combine these two methods later
    public Document newOperateAndResponseGenerate(Document request, FileSystemManager fileSystemManager);


}
