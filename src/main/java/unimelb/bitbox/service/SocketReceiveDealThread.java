package unimelb.bitbox.service;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.Socket;
import java.util.List;
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
    private BufferedReader in;

    public SocketReceiveDealThread(FileSystemManager manager, Socket socket, BufferedReader in) {
        this.fileSystemManager = manager;
        this.socket = socket;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            if(in == null){
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            while (socket.isConnected()) {
                String data = in.readLine();
                log.info(data);
                Document response = fileService.OperateAndResponseGenerate(socket, Document.parse(data), fileSystemManager);
                if (response != null) {
                    try {
                        writer.write(response.toJson() + "\n");
                        writer.flush();
                        if (INVALID_PROTOCOL.equals(response.getString("command"))) {
                            try {
                                socket.close();
                                if(in != null){
                                    in.close();
                                }
                                writer.close();
                            } catch (IOException e) {
                                log.warning(e.getMessage());
                            }
                        } else {
                            try {
                                List<Socket> socketList = ServerMain.socketPool;
                                if (REQUEST_LIST.contains(response.getString("command"))) {
                                    for (Socket socket : socketList) {
                                        //send
                                        if (socket != null && socket.isConnected()) {
                                            socketService.send(socket, data);
                                        }
                                    }
                                }
                            } catch (IOException e){
                                log.warning(e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        log.warning(e.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }
}
