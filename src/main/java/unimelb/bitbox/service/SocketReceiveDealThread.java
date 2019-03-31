package unimelb.bitbox.service;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import static unimelb.bitbox.util.RequestUtil.*;

/**
 * Created by xiyang on 2019/3/26
 */
public class SocketReceiveDealThread extends Thread {
    private static Logger log = Logger.getLogger(SocketReceiveDealThread.class.getName());
    private SocketService socketService = new SocketServiceImpl();
    private FileService fileService = new FileServiceImpl();


    private FileSystemManager fileSystemManager;
    private Socket socket;

    public SocketReceiveDealThread(FileSystemManager manager, Socket socket) {
        this.fileSystemManager = manager;
        this.socket = socket;
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String data = in.readUTF();
                log.info(data);
                Document response = fileService.OperateAndResponseGenerate(socket, Document.parse(data), fileSystemManager);
                if(response != null){
                    socketService.send(socket, response.toJson());
                    if (INVALID_PROTOCOL.equals(response.getString("command"))) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            log.warning(e.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                log.warning(ex.getMessage());
                break;
            }
        }
    }
}
