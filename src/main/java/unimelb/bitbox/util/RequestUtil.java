package unimelb.bitbox.util;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xiyang on 2019/3/26
 */
public interface RequestUtil {
    //Client
    public final static String AUTH_REQUEST = "AUTH_REQUEST";
    public final static String AUTH_RESPONSE = "AUTH_RESPONSE";
    public final static String AUTH_STATES_TRUE = "true";
    public final static String AUTH_STATES_FALSE = "false";
    public final static String LIST_PEERS_REQUEST = "LIST_PEERS_REQUEST";
    public final static String LIST_PEERS_RESPONSE = "LIST_PEERS_RESPONSE";

    public final static String CONNECT_PEER_REQUEST = "CONNECT_PEER_REQUEST";
    public final static String CONNECT_PEER_RESPONSE = "CONNECT_PEER_RESPONSE";

    public final static String DISCONNECT_PEER_REQUEST = "DISCONNECT_PEER_REQUEST";
    public final static String DISCONNECT_PEER_RESPONSE = "DISCONNECT_PEER_RESPONSE";




    //Peer
    public final static String INVALID_PROTOCOL = "INVALID_PROTOCOL";
    public final static String CONNECTION_REFUSED = "CONNECTION_REFUSED";
    public final static String HANDSHAKE_REQUEST = "HANDSHAKE_REQUEST";
    public final static String HANDSHAKE_RESPONSE = "HANDSHAKE_RESPONSE";
    public final static String FILE_CREATE_REQUEST = "FILE_CREATE_REQUEST";
    public final static String FILE_CREATE_RESPONSE = "FILE_CREATE_RESPONSE";
    public final static String FILE_BYTES_REQUEST = "FILE_BYTES_REQUEST";
    public final static String FILE_BYTES_RESPONSE = "FILE_BYTES_RESPONSE";
    public final static String FILE_DELETE_REQUEST = "FILE_DELETE_REQUEST";
    public final static String FILE_DELETE_RESPONSE = "FILE_DELETE_RESPONSE";
    public final static String FILE_MODIFY_REQUEST = "FILE_MODIFY_REQUEST";
    public final static String FILE_MODIFY_RESPONSE = "FILE_MODIFY_RESPONSE";
    public final static String DIRECTORY_CREATE_REQUEST = "DIRECTORY_CREATE_REQUEST";
    public final static String DIRECTORY_CREATE_RESPONSE = "DIRECTORY_CREATE_RESPONSE";
    public final static String DIRECTORY_DELETE_REQUEST = "DIRECTORY_DELETE_REQUEST";
    public final static String DIRECTORY_DELETE_RESPONSE = "DIRECTORY_DELETE_RESPONSE";

    public final static List<String> REQUEST_LIST = Arrays.asList(FILE_CREATE_REQUEST, FILE_DELETE_REQUEST,
            FILE_MODIFY_REQUEST, DIRECTORY_CREATE_REQUEST, DIRECTORY_DELETE_REQUEST);

    public final static List<String> PEER_RESPONSE_LIST = Arrays.asList(LIST_PEERS_RESPONSE, CONNECT_PEER_RESPONSE,
            DISCONNECT_PEER_RESPONSE);

}
