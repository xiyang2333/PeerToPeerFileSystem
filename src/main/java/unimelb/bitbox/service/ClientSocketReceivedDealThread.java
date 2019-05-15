package unimelb.bitbox.service;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.Document;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

import static unimelb.bitbox.util.RequestUtil.PEER_RESPONSE_LIST;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public class ClientSocketReceivedDealThread extends Thread {

    private static Logger log = Logger.getLogger(SocketReceiveDealThread.class.getName());
    private ClientManager clientManager = new ClientManager();
    private CommunicationServiceImpl comService = new CommunicationServiceImpl();
    private AseUtil aseUtil = new AseUtilImpl();


    private Socket socket;
    private BufferedReader in;
    private ServerMain serverMain;

    public ClientSocketReceivedDealThread(Socket socket, BufferedReader in,ServerMain serverMain) {
        this.socket = socket;
        this.in = in;
        this.serverMain = serverMain;
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
                //deal with the communication with client request
                Document payload = comService.clientAndPeerResponse(clientManager.decodePayload(Document.parse(data)),serverMain);
                if (payload != null) {

                    if (serverMain == null){

                        //client receives responses, and just print then close socket .
                        log.info("client" + payload.toJson());
                        //At the completion of the command the client disconnects.
                        socket.close();


                    }else {


                        //peer receives request, and gives responses.
                        if (PEER_RESPONSE_LIST.contains(payload.getString("command"))) {

                            String cipherText = aseUtil.encrypt(payload.toJson(), ServerMain.aseKey);
                            Document response = new Document();
                            response.append("payload", cipherText);
                            writer.write(response.toJson() + "\n");
                            writer.flush();


                        }
                    }


                }

            }

        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }
}
