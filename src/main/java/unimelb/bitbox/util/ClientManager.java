package unimelb.bitbox.util;

import unimelb.bitbox.ClientMainService;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.service.AseUtil;
import unimelb.bitbox.service.AseUtilImpl;

import java.security.Key;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public class ClientManager {

    private AseUtil aseUtil = new AseUtilImpl();


    public REQUESTTYPE requesttype;

    public HostPort hostPort;

    public HostPort targetHostPort;

    public String identity;




    public Document decodePayload(Document payLoad){
        Key aseKey;
        if (ServerMain.aseKey != null){
            aseKey=ServerMain.aseKey;
        }else{
            aseKey= ClientMainService.ase_128;
        }
        Document response;
        String payload = payLoad.getString("payload");
        String decodedPayload = aseUtil.decrypt(payload, aseKey);
        return Document.parse(decodedPayload);

    }

}
