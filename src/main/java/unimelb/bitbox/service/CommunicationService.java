package unimelb.bitbox.service;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.Document;

import java.net.Socket;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 * @Purpose: XIGUANGL
 **/
public interface CommunicationService {

    public Document clientAndPeerResponse(Document request, ServerMain serverMain);

    public Document clientRequestGenerate(ClientManager clientManager);

    public String getPublicKey(String keyIndentiy, String clientPortConfig);
}