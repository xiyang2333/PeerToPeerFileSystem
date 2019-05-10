package unimelb.bitbox.service;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.*;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import static unimelb.bitbox.util.RequestUtil.*;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public class CommunicationServiceImpl implements CommunicationService{



    private static Logger log = Logger.getLogger(ServerMain.class.getName());




    @Override
    public Document clientAndPeerResponse(Document request,ServerMain serverMain){

        Map<String, String> configuration = Configuration.getConfiguration();

        Document payLoad = new Document();

        try {

            if (LIST_PEERS_REQUEST.equals(request.getString("command"))) {
                //list connect peers
                payLoad.append("command", LIST_PEERS_RESPONSE);
                //get connect host port info
                ArrayList<Document> peers = new ArrayList<>();
                for (HostPort port : ServerMain.connectSocket) {
                    peers.add(port.toDoc());
                }
                payLoad.append("peers", peers);


            }else if (CONNECT_PEER_REQUEST.equals(request.getString("command"))){
                // let the peer connect to another peer
                ClientManager clientManager = new ClientManager();
                clientManager.requesttype = REQUESTTYPE.CONNECT_PEER_REQUEST;


                HostPort hostPort = new HostPort(request.getString("host"),(int)request.getLong("port"));
                clientManager.targetHostPort = hostPort;
                payLoad.append("command",CONNECT_PEER_RESPONSE);
                payLoad.append("host",hostPort.host);
                payLoad.append("port",hostPort.port);

                // execute the connection command.
                if (serverMain.clientCommand(clientManager)){

                    payLoad.append("status",AUTH_STATES_TRUE);
                    payLoad.append("message","connected to peer");

                }else {
                    payLoad.append("status",AUTH_STATES_FALSE);
                    payLoad.append("message","connection failed");

                }



            }else if (DISCONNECT_PEER_REQUEST.equals(request.getString("command"))){
                // let the peer disconnect to another peer
                ClientManager clientManager = new ClientManager();
                clientManager.requesttype = REQUESTTYPE.DISCONNECT_PEER_REQUEST;
                HostPort hostPort = new HostPort(request.getString("host"),Integer.parseInt(request.getString("port")));
                clientManager.targetHostPort = hostPort;
                serverMain.clientCommand(clientManager);
                payLoad.append("command",DISCONNECT_PEER_RESPONSE);
                payLoad.append("host",hostPort.host);
                payLoad.append("port",hostPort.port);

                // execute the disconnection command.
                if (serverMain.clientCommand(clientManager)){

                    payLoad.append("status",AUTH_STATES_TRUE);
                    payLoad.append("message","disconnected from peer");

                }else {

                    payLoad.append("status",AUTH_STATES_FALSE);
                    payLoad.append("message","connection not active");

                }


            }else {

                payLoad = request;
            }


        }catch (Exception e){

            log.warning(e.getMessage());

        }



        return payLoad;
    }

    @Override
    public Document clientRequestGenerate(ClientManager clientManager) {

        Document request = new Document();
        if (clientManager.requesttype == REQUESTTYPE.LIST_PEERS_REQUEST){
            request.append("command", LIST_PEERS_REQUEST);
        }else if (clientManager.requesttype == REQUESTTYPE.CONNECT_PEER_REQUEST){
            request.append("command", CONNECT_PEER_REQUEST);
            request.append("host", clientManager.targetHostPort.host);
            request.append("port", clientManager.targetHostPort.port);

        }else if (clientManager.requesttype == REQUESTTYPE.DISCONNECT_PEER_REQUEST){
            request.append("command", DISCONNECT_PEER_REQUEST);
            request.append("host", clientManager.targetHostPort.host);
            request.append("port", clientManager.targetHostPort.port);
        }else {
            return null;
        }

        return request;


    }


    @Override
    public String getPublicKey(String keyIndentiy, String clientPortConfig) {

        String publicKey = null;
        String[] publicKeyList = clientPortConfig.split(",");
        for (int i = 1; i< publicKeyList.length;i++){
            String element = publicKeyList[i];
            String[] temp = element.split(" ");
            if (keyIndentiy.equals(temp[1])){
                publicKey = temp[0];
            }
        }

        return publicKey;
    }
}
