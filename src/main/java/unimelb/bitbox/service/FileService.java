package unimelb.bitbox.service;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.*;

import java.net.Socket;
import java.util.List;

public interface FileService {

    public Document requestGenerate(FileSystemEvent fileSystemEvent);

    public Document OperateAndResponseGenerate(Socket socket, Document request, FileSystemManager fileSystemManager);

}
