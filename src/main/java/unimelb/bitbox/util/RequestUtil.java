package unimelb.bitbox.util;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xiyang on 2019/3/26
 */
public interface RequestUtil {
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

}
