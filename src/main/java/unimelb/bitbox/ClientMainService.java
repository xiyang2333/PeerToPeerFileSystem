package unimelb.bitbox;

import unimelb.bitbox.service.*;
import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Key;
import java.util.logging.Logger;

import static unimelb.bitbox.util.RequestUtil.*;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 * @Purpose: XIGUANGL
 **/
public class ClientMainService {

    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    public static Key ase_128;
    // the socket between client and peer;
    public static Socket mainSocket;


    private SocketService socketService = new SocketServiceImpl();
    private CommunicationService comService = new CommunicationServiceImpl();
    private RsaUtil rsaUtil = new RsaUtilImpl();
    private AseUtil aseUtil = new AseUtilImpl();

    private String PRIVATE_KEY_FILE = "bitboxclient_rsa.pem";



    public void commandLineRequest(ClientManager clientManager) {


        boolean result = true;
        //check whether the secure channel has been established.
        if (mainSocket == null){

            result = askForAuthority(clientManager.hostPort,"lixiguang@xiguangl");

        }

        // secure channel has been established and continue to execute following command.
        if (result) {
            try {
                if (mainSocket != null && mainSocket.isConnected()) {
                    try {

                        // the info need to be encoded.
                        String encodeJson = comService.clientRequestGenerate(clientManager).toJson();
                        Document requestDoc = new Document();
                        // ASE_128 encode.
                        requestDoc.append("payload", aseUtil.encrypt(encodeJson, ase_128));
                        socketService.send(mainSocket, requestDoc.toJson());

                    } catch (Exception ex) {
                        log.warning(ex.getMessage());
                        mainSocket.close();

                    }
                }
            } catch (IOException e) {
                log.warning("io problem: " + e.getMessage());
            }
        }


    }



    private boolean askForAuthority(HostPort hostPort,String publicKeyName) {
        boolean result = true;

        try {

            Socket socket = new Socket(hostPort.host, hostPort.port);
            Document authRequest = new Document();
            authRequest.append("command", AUTH_REQUEST);
            authRequest.append("identity", publicKeyName);
            //send
            String requestJson = authRequest.toJson();
            socketService.send(socket, requestJson);
            // read a line of data from the stream
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
            String data = in.readLine();
            Document authRequestResponse = Document.parse(data);
            //if success, then use private key to decode.
            if (AUTH_STATES_TRUE.equals(authRequestResponse.getString("status"))) {

                //the AES128 info.
                String securityKey= rsaUtil.decrypt(authRequestResponse.getString("AES128"),PRIVATE_KEY_FILE);
                //generate AES128 key.
                ase_128 = aseUtil.getKey(securityKey);
                mainSocket = socket;
                new ClientSocketReceivedDealThread(mainSocket, in,null).start();

            }else {

                result = false;
                socket.close();
                log.warning(authRequestResponse.getString("message"));

            }


        } catch (Exception ex) {
            log.warning(ex.getMessage());

        }



        return result;

    }




    }
