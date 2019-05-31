package unimelb.bitbox;

import java.io.*;
import java.net.*;
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
    private String mode;
    private int blockSize;

    private int socketCount = 0;

    private SocketService socketService = new SocketServiceImpl();
    private FileService fileService = new FileServiceImpl();

    protected FileSystemManager fileSystemManager;
    protected ServerMain serverMain;

    private CommunicationService comService = new CommunicationServiceImpl();
    private static ClientManager clientManager = new ClientManager();
    private RsaUtil rsaUtil = new RsaUtilImpl();
    private AseUtil aseUtil = new AseUtilImpl();
    public static Key aseKey;
    private String ase128KeySeed = "cometGroup!@#$";
    Map<String, String> configuration = Configuration.getConfiguration();

    List<WaitingTask> waitingTasks = new Vector<>();


    public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        serverMain = this;
        //get configuration
        HostPort local = new HostPort(configuration.get("advertisedName"), Integer.parseInt(configuration.get("port")));
        localPort = local;
        mode = configuration.get("mode");
        blockSize = Integer.parseInt(configuration.get("updblockSize"));

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
                        //add deal waiting list
                        if ("udp".equals(mode) && waitingTasks.size() > 0) {
                            int index = 0;
                            for (WaitingTask task : waitingTasks) {
                                int tryTimes = task.getRetryTimes();
                                if (tryTimes < 5) {
                                    if (udpSendAndResponse(task.getRequest(), task.getHostPort())) {
                                        waitingTasks.set(index, null);
                                    } else {
                                        task.setRetryTimes(tryTimes + 1);
                                    }
                                } else {
                                    if (connectSocket.contains(task.getHostPort())) {
                                        int i = 0;
                                        for (HostPort port : connectSocket) {
                                            if (port.equals(task.getHostPort())) {
                                                connectSocket.set(i, null);
                                                socketCount--;
                                                break;
                                            }
                                            i++;
                                        }
                                        connectSocket.removeIf(Objects::isNull);
                                    }
                                    waitingTasks.set(index, null);
                                }
                            }

                            waitingTasks.removeIf(Objects::isNull);
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

        //still leave it here to support client connection request.
//        startTcpServer();
        //start client server
        new Thread(new Runnable() {
            @Override
            public void run() {
                startClientServer();
            }
        }).start();

        if (mode.equals("tcp")) {
            //tcp connection
            String[] peers = configuration.get("peers").split(",");
            tcpConnection(peers);

            //still leave it here to support client connection request.
            startTcpServer();
        } else if (mode.equals("udp")) {
            String[] peers = configuration.get("peers").split(",");
            udpConnection(peers);
            //don't need to establish connections, and just start udp server.
            startUdpServer();
        } else {
            log.warning("no specific connection mode");
        }

    }

    private void udpConnection(String[] peers) {
        try {

            for (String peer : peers) {
                HostPort hostPort = new HostPort(peer);
                //try connect to the other peer
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Document handshakeRequest = new Document();
                            handshakeRequest.append("command", HANDSHAKE_REQUEST);
                            handshakeRequest.append("hostPort", localPort.toDoc());
                            udpSendAndResponse(handshakeRequest, hostPort);
                        } catch (Exception e) {
                            log.warning(e.getMessage());
                        }
                    }
                }).start();
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());

        }
    }

    private void startUdpServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                log.info("updServer start......");
                try {

                    byte[] buf = new byte[blockSize];
                    DatagramSocket socket = new DatagramSocket(localPort.port);
                    DatagramPacket receivePacket = new DatagramPacket(buf, blockSize);
                    while (true) {
                        socket.receive(receivePacket);
                        //do some action based on the requset.
                        String request = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");

                        log.info(request);
                        //do handshake only
                        Document requestDoc = Document.parse(request);
                        Document response = null;
                        boolean handRequestFlag = false;
                        if (HANDSHAKE_REQUEST.equals(requestDoc.getString("command"))) {
                            if (socketCount >= Integer.parseInt(configuration.get("maximumIncommingConnections"))) {
                                //if there are too many socket, return connect refuse
                                response = new Document();
                                response.append("command", CONNECTION_REFUSED);
                                response.append("message", "connection limit reached");
                                //get connect host port info
                                ArrayList<Document> peers = new ArrayList<>();
                                for (HostPort port : ServerMain.connectSocket) {
                                    peers.add(port.toDoc());
                                }
                                response.append("peers", peers);
                            } else {
                                //if success, return response and add to socket pool
                                response = new Document();
                                response.append("command", HANDSHAKE_RESPONSE);
                                response.append("hostPort", new HostPort(localPort.host, localPort.port).toDoc());
                                Document newPort = (Document) requestDoc.get("hostPort");
                                HostPort newHost = new HostPort(newPort);
                                if (!connectSocket.contains(newHost)) {
                                    connectSocket.add(new HostPort(newPort));
                                    if (!inCome.contains(newHost)) {
                                        socketCount++;
                                        inCome.add(new HostPort(newPort));
                                    }
                                }
                                handRequestFlag = true;
                            }
                        } else {
                            System.out.println(receivePacket.getAddress().toString());
                            System.out.println(receivePacket.getPort());
                            response = fileService.newOperateAndResponseGenerate(requestDoc, fileSystemManager);
                        }

                        //still need to do some work here.
                        if (response != null) {
                            log.info(response.toJson());
                            byte[] message = response.toJson().getBytes("UTF-8");
                            DatagramPacket send = new DatagramPacket(message, message.length, receivePacket.getAddress(), receivePacket.getPort());
                            socket.send(send);
                        } else {

                        }
                        receivePacket.setLength(blockSize);

                        if (handRequestFlag) {
                            //deal first sync
                            dealUdpSync(new HostPort((Document) requestDoc.get("hostPort")), fileSystemManager.generateSyncEvents());
                        }
                    }

                } catch (IOException i) {
                    log.warning(i.getMessage());
                }
            }
        }).start();
    }

    private void startClientServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(configuration.get("clientPort")));
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
                            String data = in.readLine();
                            log.info(data);
                            Document document = Document.parse(data);
                            if (AUTH_REQUEST.equals(document.getString("command"))) {
                                // deal with client request.

                                // generate aseKey in order to decode the following encoded requests.
                                // identify the public key which is used to encode aseKeyseed.
                                String aseName = document.getString("identity");
                                String publicKey = comService.getPublicKey(aseName, configuration.get("authorized_keys"));
                                if (publicKey == null) {
                                    //cannot find the public key
                                    Document authDoc = new Document();
                                    authDoc.append("command", AUTH_RESPONSE);
                                    authDoc.append("status", AUTH_STATES_FALSE);
                                    authDoc.append("message", "public key not found");
                                    socketService.send(socket, authDoc.toJson());
                                    socket.close();
                                } else {
                                    //find the public key
                                    aseKey = aseUtil.getKey(ase128KeySeed);
                                    Document authDoc = new Document();
                                    authDoc.append("command", AUTH_RESPONSE);
                                    authDoc.append("AES128", rsaUtil.encrypt(ase128KeySeed, publicKey));
                                    authDoc.append("status", "true");
                                    authDoc.append("message", "public key found");
                                    socketService.send(socket, authDoc.toJson());

                                    // if success there will be one more request
                                    data = in.readLine();
                                    log.info(data);
                                    //deal with the communication with client request
                                    Document payload = comService.clientAndPeerResponse(clientManager.decodePayload(Document.parse(data)), serverMain);

                                    //peer receives request, and gives responses.
                                    if (PEER_RESPONSE_LIST.contains(payload.getString("command"))) {
                                        String cipherText = aseUtil.encrypt(payload.toJson(), ServerMain.aseKey);
                                        Document response = new Document();
                                        response.append("payload", cipherText);
                                        socketService.send(socket, response.toJson());
                                    }

                                    //finish and close the socket
                                    socket.close();
//                                    new ClientSocketReceivedDealThread(socket, in, serverMain).start();
                                }
                            }
                        } catch (IOException e) {
                            log.warning(e.getMessage());
                        } catch (Exception ex) {
                            log.warning(ex.getMessage());
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            log.warning(e.getMessage());
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }

    private void startTcpServer() {

        //start Server
        try {

            ServerSocket serverSocket = new ServerSocket(localPort.port);
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
                                    connectionRefused.append("message", "connection limit reached");
                                    //get connect host port info
                                    ArrayList<Document> peers = new ArrayList<>();
                                    for (HostPort port : ServerMain.connectSocket) {
                                        peers.add(port.toDoc());
                                    }
                                    connectionRefused.append("peers", peers);
                                    socketService.send(socket, connectionRefused.toJson());
                                    //close socket
                                    socket.close();
                                } else {
                                    //if success, return response and add to socket pool
                                    Document handshakeResponse = new Document();
                                    handshakeResponse.append("command", HANDSHAKE_RESPONSE);
                                    handshakeResponse.append("hostPort", new HostPort(localPort.host, localPort.port).toDoc());
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
//                            } else if (AUTH_REQUEST.equals(document.getString("command"))) {
//                                // deal with client request.
//
//                                // generate aseKey in order to decode the following encoded requests.
//                                // identify the public key which is used to encode aseKeyseed.
//                                String aseName = document.getString("identity");
//                                String publicKey = comService.getPublicKey(aseName,configuration.get("clientPort"));
//                                if (publicKey == null){
//                                    //cannot find the public key
//                                    Document authDoc = new Document();
//                                    authDoc.append("command", AUTH_RESPONSE);
//                                    authDoc.append("status",AUTH_STATES_FALSE);
//                                    authDoc.append("message","public key not found");
//                                    socketService.send(socket, authDoc.toJson());
//                                    socket.close();
//                                }else {
//                                    //find the public key
//                                    aseKey = aseUtil.getKey(ase128KeySeed);
//                                    Document authDoc = new Document();
//                                    authDoc.append("command", AUTH_RESPONSE);
//                                    authDoc.append("AES128", rsaUtil.encrypt(ase128KeySeed,publicKey));
//                                    authDoc.append("status","true");
//                                    authDoc.append("message","public key found");
//                                    socketService.send(socket, authDoc.toJson());
//                                    new ClientSocketReceivedDealThread(socket, null,serverMain).start();
//                                }
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


    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // TODO: process events03
        try {
            Document request = fileService.requestGenerate(fileSystemEvent);
            if (request != null) {
                if (mode.equals("tcp")) {
                    tcpProcess(request);

                } else if (mode.equals("udp")) {
                    log.info(request.toJson());
                    udpProcess(request, connectSocket);

                } else {

                    log.warning("Connection mode is not set");

                }

            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }

    private void tcpProcess(Document request) {
        int index = 0;
        for (Socket socket : socketPool) {
            newPostThread(socket, request, index);
            index++;
        }
        //delete null node
        socketPool.removeIf(Objects::isNull);
        connectSocket.removeIf(Objects::isNull);
    }

    private void udpProcess(Document request, List<HostPort> hostPorts) {


//        String[] peers = configuration.get("udpPort").split(",");

        for (HostPort hostPort : connectSocket) {
//            HostPort hostPort = new HostPort(peer);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    udpSendAndResponse(request, hostPort);
                }
            }).start();
        }
    }

    private boolean udpSendAndResponse(Document request, HostPort hostPort) {

        int TIMEOUT = 5000;
        String retry = configuration.get("retryNumber");
        int MAXNUM = retry != null && !"".equals(retry) ? Integer.parseInt(retry) : 5;
        DatagramSocket socket = null;
        try {

            byte[] buf = new byte[blockSize];
            socket = new DatagramSocket();
            log.info(request.toJson());
            byte[] message = request.toJson().getBytes("UTF-8");
            InetAddress host = InetAddress.getByName(hostPort.host);
            DatagramPacket sendPacket = new DatagramPacket(message, message.length, host, hostPort.port);
            DatagramPacket receivePacket = new DatagramPacket(buf, blockSize);
            //set timeout
            socket.setSoTimeout(TIMEOUT);
            // retry times.
            int tries = 0;
            boolean receivedResponse = false;
            while (!receivedResponse && tries < MAXNUM) {
                if (! HANDSHAKE_REQUEST.equals(request.getString("command")) || !refusedSocket.contains(hostPort)) {
                    socket.send(sendPacket);
                } else {
                    return false;
                }

                try {
                    socket.receive(receivePacket);

                    if (!receivePacket.getAddress().equals(host)) {
                        throw new IOException("Packet from an umknown source");
                    } else {
                        if (HANDSHAKE_REQUEST.equals(request.getString("command"))) {
                            String ServerMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                            log.info(ServerMessage);
                            Document response = Document.parse(ServerMessage);
                            if (HANDSHAKE_RESPONSE.equals(response.getString("command"))) {
                                Document newPort = (Document) response.get("hostPort");
                                HostPort newHost = new HostPort(newPort);
                                if (!connectSocket.contains(newHost)) {
                                    connectSocket.add(new HostPort(newPort));
                                }
                                dealUdpSync(new HostPort(newPort), fileSystemManager.generateSyncEvents());
                                receivedResponse = true;
                            } else if (CONNECTION_REFUSED.equals(response.getString("command"))) {
                                receivedResponse = true;
                                refusedSocket.add(hostPort);
                                List<Document> portDoc = (ArrayList<Document>) response.get("peers");
                                for (Document nextHost : portDoc) {
                                    HostPort port = new HostPort(nextHost);
                                    udpSendAndResponse(request, port);
                                }
                            }
                        } else {
                            // do some action
                            String ServerMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                            log.info(ServerMessage);
                            Document response = fileService.newOperateAndResponseGenerate(Document.parse(ServerMessage), fileSystemManager);
                            //do some work here.
                            if (response != null) {
                                //continue use this DatagramSocket to send message to guarantee the order of packet.
                                tries = 0;
                                receivedResponse = false;
                                receivePacket.setLength(blockSize);
                                byte[] responseMessage = response.toJson().getBytes("UTF-8");
                                sendPacket = new DatagramPacket(responseMessage, responseMessage.length, host, hostPort.port);
                            } else {
                                // no need to make extra communication and exit the loop.
                                receivedResponse = true;
                            }
                        }
                    }
                } catch (InterruptedIOException e) {
                    tries += 1;
                    log.warning("Time out," + (MAXNUM - tries) + " more tries...");
                }
            }

            //when try 5 times, add to waiting list and deal in sync generate
            if (tries == 5) {
                waitingTasks.add(new WaitingTask(hostPort, request, 0));
//                if (connectSocket.contains(hostPort)) {
//                    int i = 0;
//                    for (HostPort port : connectSocket) {
//                        if (port.equals(hostPort)) {
//                            connectSocket.set(i, null);
//                            break;
//                        }
//                        i++;
//                    }
//                    connectSocket.removeIf(Objects::isNull);
//                }
            }

            if (receivedResponse) {
                receivePacket.setLength(blockSize);
            } else {
                log.warning("No response to " + hostPort.toString());
            }
            socket.close();

            return receivedResponse;
        } catch (SocketException s) {
            log.warning(s.getMessage());
        } catch (UnknownHostException u) {
            log.warning(u.getMessage());
        } catch (IOException e) {
            log.warning(e.getMessage());
        }

        return false;
    }


    private boolean connectToPeer(HostPort hostPort, HostPort local, FileSystemManager manager) {
        // eliminate duplicate connections
        if (connectSocket.contains(hostPort) || refusedSocket.contains(hostPort)) {
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
                    if (inCome.contains(hostPort)) {
                        socketCount--;
                        inCome.remove(hostPort);
                    }
                    socketPool.set(index, null);
                    connectSocket.set(index, null);
                }
            }
        } catch (IOException e) {
            log.warning("io problem: " + e.getMessage());
        }
    }

    private void dealUdpSync(HostPort hostPort, List<FileSystemEvent> events) {
        for (FileSystemEvent event : events) {
            Document request = fileService.requestGenerate(event);
            if (request != null) {
                udpSendAndResponse(request, hostPort);
            }
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


    private void tcpConnection(String[] peers) {

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
        } catch (Exception ex) {
            log.warning(ex.getMessage());

        }


    }


    //peer response
    public boolean clientCommand(ClientManager clientManager) {
//
        if (mode.equals("udp")) {

//             log.warning("unsupported connection mode");
//             return false;

            // add udp connect

            if (clientManager.requesttype == REQUESTTYPE.CONNECT_PEER_REQUEST) {
                if (connectSocket.contains(clientManager.targetHostPort)) {
                    return true;
                }
                Document handshakeRequest = new Document();
                handshakeRequest.append("command", HANDSHAKE_REQUEST);
                handshakeRequest.append("hostPort", localPort.toDoc());
                return udpSendAndResponse(handshakeRequest, clientManager.targetHostPort);
            } else if (clientManager.requesttype == REQUESTTYPE.DISCONNECT_PEER_REQUEST) {
                if (connectSocket.contains(clientManager.targetHostPort)) {
                    //disconnect to a peer
                    try {
                        int index = 0;
                        for (HostPort port : connectSocket) {
                            if (port.equals(clientManager.targetHostPort)) {
                                connectSocket.set(index, null);
                                break;
                            }
                            index++;
                        }
                        connectSocket.removeIf(Objects::isNull);
                    } catch (Exception e) {
                        log.warning(e.getMessage());
                    }
                    return true;
                }
            } else {
                return false;
            }
        }

        if (clientManager.requesttype == REQUESTTYPE.CONNECT_PEER_REQUEST) {
            if (connectSocket.contains(clientManager.targetHostPort)) {
                return true;
            }
            return connectToPeer(clientManager.targetHostPort, localPort, fileSystemManager);
        } else if (clientManager.requesttype == REQUESTTYPE.DISCONNECT_PEER_REQUEST) {

            if (connectSocket.contains(clientManager.targetHostPort)) {
                //disconnect to a peer
                try {
                    int index = 0;
                    for (Socket socket : socketPool) {
                        HostPort p = connectSocket.get(index);
                        if (p.equals(clientManager.targetHostPort)) {
                            socket.close();
                            socketPool.set(index, null);
                            connectSocket.set(index, null);
                            break;
                        }
                        index++;
                    }
                    socketPool.removeIf(Objects::isNull);
                    connectSocket.removeIf(Objects::isNull);
                } catch (IOException e) {
                    log.warning(e.getMessage());
                }
                return true;
            }
            return false;
        }
        return false;
    }

}
