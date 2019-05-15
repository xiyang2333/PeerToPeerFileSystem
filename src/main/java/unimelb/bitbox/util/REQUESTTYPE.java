package unimelb.bitbox.util;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public enum REQUESTTYPE {

    /**
     * List all the known/connected peers.
     */
    LIST_PEERS_REQUEST,
    /**
     * Connect to a peer.
     */
    CONNECT_PEER_REQUEST,
    /**
     * Disconnect to a peer.
     */
    DISCONNECT_PEER_REQUEST,
}
