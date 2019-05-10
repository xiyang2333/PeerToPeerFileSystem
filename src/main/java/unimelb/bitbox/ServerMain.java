package unimelb.bitbox;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
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
    //connect and refused host port information
    public static List<HostPort> connectSocket = new Vector<>();
    private static List<HostPort> refusedSocket = new Vector<>();
    private static List<HostPort> inCome = new Vector<>();
    private HostPort localPort;
    private int socketCount = 0;

    private SocketService socketService = new SocketServiceImpl();
    private FileService fileService = new FileServiceImpl();

    protected FileSystemManager fileSystemManager;
    protected ServerMain serverMain;


    private CommunicationService comService = new CommunicationServiceImpl();
    private RsaUtil rsaUtil = new RsaUtilImpl();
    private AseUtil aseUtil = new AseUtilImpl();
    public static Key aseKey;
    private String ase128KeySeed = "cometGroup!@#$";


    public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        serverMain = this;
        //get configuration
        Map<String, String> configuration = Configuration.getConfiguration();
        HostPort local = new HostPort(configuration.get("advertisedName"), Integer.parseInt(configuration.get("port")));
        localPort = local;
        String mode = configuration.get("mode");

        if (mode.equals("tcp")){
            //tcp connection
            String[] peers = configuration.get("peers").split(",");
            tcpConnection(peers);




        }else if (mode.equals("udp")){
           //udp connection



        }else {

            log.warning("no specific connection mode");
        }


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

            //start Server

            try {

                ServerSocket serverSocket = new ServerSocket(local.port);
                while (true) {

                    Socket socket = serverSocket.accept();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // read a line of data from the stream
                                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
                                String data = in.readLine();
                                log.info(data);
                                Document document = Document.parse(data);
                                //the first data should be hand shake request
                                if (HANDSHAKE_REQUEST.equals(document.getString("command"))) {

                                    //if there are too many socket, return connect refuse
                                    if (socketCount >= Integer.parseInt(configuration.get("maximumIncommingConnections"))) {
                                        //if there are too many socket, return connect refuse
                                        Document connectionRefused = new Document();
                                        connectionRefused.append("command", CONNECTION_REFUSED);
                                        //get connect host port info
                                        ArrayList<Document> peers = new ArrayList<>();
                                        for (HostPort port : ServerMain.connectSocket) {
                                            peers.add(port.toDoc());
                                        }
                                        connectionRefused.append("peers", peers);
                                        socketService.send(socket, connectionRefused.toJson());
                                        //close socket
                                        socket.close();


                                    }else {
                                        //if success, return response and add to socket pool
                                        Document handshakeResponse = new Document();
                                        handshakeResponse.append("command", HANDSHAKE_RESPONSE);
                                        handshakeResponse.append("hostPort", new HostPort(local.host, local.port).toDoc());
                                        socketService.send(socket, handshakeResponse.toJson());
                                        //deal generalSync
                                        dealWithSync(socket, fileSystemManager.generateSyncEvents());
                                        Document newPort = (Document) document.get("hostPort");
                                        connectSocket.add(new HostPort(newPort));
                                        socketPool.add(socket);
                                        socketCount++;
                                        inCome.add(new HostPort(newPort));
                                        new SocketReceiveDealThread(fileSystemManager, socket, null).start();

                                    }



                                } else if (AUTH_REQUEST.equals(document.getString("command"))) {

                                    // deal with client request.

                                    // generate aseKey in order to decode the following encoded requests.
                                    // identify the public key which is used to encode aseKeyseed.
                                    String aseName = document.getString("identity");
                                    String publicKey = comService.getPublicKey(aseName,configuration.get("clientPort"));

                                    if (publicKey == null){
                                        //cannot find the public key
                                        Document authDoc = new Document();
                                        authDoc.append("command", AUTH_RESPONSE);
                                        authDoc.append("status",AUTH_STATES_FALSE);
                                        authDoc.append("message","public key not found");
                                        socketService.send(socket, authDoc.toJson());
                                        socket.close();

                                    }else {
                                        //find the public key
                                        aseKey = aseUtil.getKey(ase128KeySeed);
                                        Document authDoc = new Document();
                                        authDoc.append("command", AUTH_RESPONSE);
                                        authDoc.append("AES128", rsaUtil.encrypt(ase128KeySeed,publicKey));
                                        authDoc.append("status","true");
                                        authDoc.append("message","public key found");
                                        socketService.send(socket, authDoc.toJson());
                                        new ClientSocketReceivedDealThread(socket, null,serverMain).start();

                                    }


                                } else {

                                    Document invalid = new Document();
                                    invalid.append("command", AUTH_RESPONSE);
                                    invalid.append("status", "false");
                                    invalid.append("message", "wrong command");
                                    socketService.send(socket, invalid.toJson());
                                    //close socket
                                    socket.close();

                                }
                            } catch (Exception ex) {

                            }
                        }
                    }).start();
                    socketPool.removeIf(Objects::isNull);
                }
            } catch (IOException e) {
                log.warning(e.getMessage());
            } catch (Exception ex) {
                log.warning(ex.getMessage());
            }
//                }
//            }).start();


    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // TODO: process events03
        try {
            Document request = fileService.requestGenerate(fileSystemEvent);
            if (request != null) {
                int index = 0;
                for (Socket socket : socketPool) {
                    newPostThread(socket, request, index);
                    index++;
                }
                //delete null node
                socketPool.removeIf(Objects::isNull);
                connectSocket.removeIf(Objects::isNull);
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }

    private boolean connectToPeer(HostPort hostPort, HostPort local, FileSystemManager manager) {
        // eliminate duplicate connections
        if (connectSocket.contains(hostPort)||refusedSocket.contains(hostPort)){
            return false;
        }
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
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
            String data = in.readLine();
            log.info(data);
            Document handshakeResponse = Document.parse(data);
            //if success, then put into socketPool
            if (HANDSHAKE_RESPONSE.equals(handshakeResponse.getString("command"))) {
                connectSocket.add(hostPort);
                socketPool.add(socket);
                //deal generalSync
                dealWithSync(socket, fileSystemManager.generateSyncEvents());
                new SocketReceiveDealThread(manager, socket, in).start();
            } else if (CONNECTION_REFUSED.equals(handshakeResponse.getString("command"))) {
                socket.close();
                result = false;
                refusedSocket.add(hostPort);
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
                if (index != null) {
                    HostPort hostPort = connectSocket.get(index);
                    if(inCome.contains(socketCount)){
                        socketCount--;
                        inCome.remove(hostPort);
                    }
                    socketPool.set(index, null);
                    connectSocket.set(index,null);
                }
            }
        } catch (IOException e) {
            log.warning("io problem: " + e.getMessage());
        }
    }

    public void dealWithSync(Socket socket, List<FileSystemEvent> events) {
        for (FileSystemEvent event : events) {
            Document request = fileService.requestGenerate(event);
            if (request != null) {
                newPostThread(socket, request, null);
                //delete null node
                socketPool.removeIf(Objects::isNull);
                connectSocket.removeIf(Objects::isNull);
            }
        }
    }



    //peer response
    public boolean clientCommand(ClientManager clientManager){

         if (clientManager.requesttype == REQUESTTYPE.CONNECT_PEER_REQUEST){

             if(connectSocket.contains(clientManager.targetHostPort)){
                 return true;
             }
             return connectToPeer(clientManager.targetHostPort,localPort,fileSystemManager);

         }else if(clientManager.requesttype == REQUESTTYPE.DISCONNECT_PEER_REQUEST){

             if(connectSocket.contains(clientManager.targetHostPort)){
                 //disconnect to a peer


                 try {

                     int index = 0;
                     for (Socket socket : socketPool) {
                         HostPort p = connectSocket.get(index);
                         if (p == clientManager.targetHostPort){
                             socket.close();
                             socketPool.set(index, null);
                             connectSocket.set(index,null);
                             break;
                         }
                         index++;
                     }

                 }catch (IOException e){

                     log.warning(e.getMessage());
                 }


                 return true;
             }

             return false;
         }


         return false;
    }




    private void tcpConnection(String[] peers){

        //connect to other peers
        try {

            for (String peer : peers) {
                HostPort hostPort = new HostPort(peer);
                //try connect to the other peer
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectToPeer(hostPort, localPort, fileSystemManager);
                    }
                }).start();
            }
        }catch (Exception ex) {
            log.warning(ex.getMessage());

        }

        
    }

}
