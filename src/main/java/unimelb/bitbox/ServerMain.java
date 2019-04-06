package unimelb.bitbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

import unimelb.bitbox.service.*;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import static unimelb.bitbox.util.RequestUtil.*;

public class ServerMain implements FileSystemObserver {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());

    public static List<Socket> socketPool = new Vector<>();
    private int socketCount = 0;

    private SocketService socketService = new SocketServiceImpl();
    private FileService fileService = new FileServiceImpl();
    protected FileSystemManager fileSystemManager;

    public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        //get configuration
        Map<String, String> configuration = Configuration.getConfiguration();

        //connect to other peers
        try {

            String[] peers = configuration.get("peers").split(",");
            ArrayList<HostPort> hostPorts = new ArrayList<>();
            ArrayList<Document> portsDoc = new ArrayList<>();
            HostPort local = new HostPort(configuration.get("advertisedName"), Integer.parseInt(configuration.get("port")));
            for (String peer : peers) {
                HostPort hostPort = new HostPort(peer);
                hostPorts.add(hostPort);
                portsDoc.add(hostPort.toDoc());
                connectToPeer(hostPort, local, fileSystemManager);
            }





            //start Server
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServerSocket serverSocket = new ServerSocket(local.port);
                        while (true) {
                            Socket socket = serverSocket.accept();
                            //if there are too many socket, return connect refuse
                            if (socketCount >= Integer.parseInt(configuration.get("maximumIncommingConnections"))) {
                                Document connectionRefused = new Document();

                                connectionRefused.append("command", CONNECTION_REFUSED);
                                connectionRefused.append("peers", portsDoc); 
                                socketService.send(socket, connectionRefused.toJson());
                                //close socket
                                socket.close();

                            } else {
                                //if success, return response and add to socket pool
                                Document handshakeResponse = new Document();
                                handshakeResponse.append("command", HANDSHAKE_RESPONSE);
                                handshakeResponse.append("hostPort", new HostPort(local.host, local.port).toDoc());
                                socketService.send(socket, handshakeResponse.toJson());
                                //deal generalSync
                                dealWithSync(socket, fileSystemManager.generateSyncEvents());
                                socketPool.add(socket);
                                socketCount++;
                                new SocketReceiveDealThread(fileSystemManager, socket, null).run();
                            }
                            socketPool.removeIf(Objects::isNull);
                        }
                    } catch (IOException e) {
                        log.warning(e.getMessage());
                    } catch (Exception ex) {
                        log.warning(ex.getMessage());
                    }
                }
            }).start();


            //start generateSync
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            List<FileSystemEvent> events = fileSystemManager.generateSyncEvents();
                            for (FileSystemEvent event : events) {
                                processFileSystemEvent(event);
                            }
                            Thread.sleep(Integer.parseInt(configuration.get("syncInterval")) * 1000);
                        }
                    } catch (InterruptedException e) {
                        log.warning("thread problem: " + e.getMessage());
                    } catch (Exception ex) {
                        log.warning(ex.getMessage());
                    }
                }
            }).start();
        }catch (Exception ex) {
            log.warning(ex.getMessage());
        }



    }



    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // TODO: process events03
        try {
            Document request = fileService.requestGenerate(fileSystemEvent);
            if(request != null) {
                int index = 0;
                for (Socket socket : socketPool) {
                    newPostThread(socket, request, index);
                }
                socketPool.removeIf(Objects::isNull);
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }


    private boolean connectToPeer(HostPort hostPort, HostPort local, FileSystemManager manager) {
        boolean result = true;
        try {
            Socket socket = new Socket(hostPort.host, hostPort.port);

            //generate request
            Document handshakeRequest = new Document();
            handshakeRequest.append("command", HANDSHAKE_REQUEST);
            handshakeRequest.append("hostPort", local.toDoc());
            //send
            String requestJson = handshakeRequest.toJson();
            socketService.send(socket, requestJson);
            // read a line of data from the stream
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
            String data = in.readLine();
            log.info(data);
            Document handshakeResponse = Document.parse(data);
            //if success, then put into socketPool
            if (HANDSHAKE_RESPONSE.equals(handshakeResponse.getString("command"))) {
                socketPool.add(socket);
                socketCount++;
                //deal generalSync
                dealWithSync(socket, fileSystemManager.generateSyncEvents());
                new SocketReceiveDealThread(manager, socket, in).start();
            } else if (CONNECTION_REFUSED.equals(handshakeResponse.getString("command"))) {
                socket.close();
                result = false;
                //close socket and try to connect another one
                List<Document> portDoc = (ArrayList<Document>) handshakeResponse.get("peers");
                for (Document nextHost : portDoc) {
                    HostPort port = new HostPort(nextHost);
                    result = connectToPeer(port, local, manager);
                    if (result) {
                        break;
                    }
                }
            } else {
                result = false;
                socket.close();
                log.warning("there are something wrong!");
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
//            if (socket != null) {
//                try {
//                    socket.close();
//                } catch (IOException ioE) {
//                    log.warning("io problem: " + ioE.getMessage() + String.format("host: %s, port: %d", hostPort.host, hostPort.port));
//                }
//            }
        }
        return result;
    }

    private void newPostThread(Socket socket, Document request, Integer index) {
        try {
            if (socket != null && socket.isConnected()) {
                try {
                    String requestJson = request.toJson();
                    log.info(requestJson);
                    //send
                    socketService.send(socket, requestJson);
                } catch (Exception ex) {
                    log.warning(ex.getMessage());
                    socket.close();
                }
            }
            if (socket != null && socket.isClosed()) {
                //if disconnect set it to null
                socketCount--;
                if(index != null){
                    socketPool.set(index, null);
                }
            }
        } catch (IOException e) {
            log.warning("io problem: " + e.getMessage());
        }
    }

    public void dealWithSync(Socket socket, List<FileSystemEvent> events) {
        for (FileSystemEvent event : events) {
            Document request = fileService.requestGenerate(event);
            if(request != null) {
                newPostThread(socket, request, null);
                socketPool.removeIf(Objects::isNull);
            }
        }
    }

}
