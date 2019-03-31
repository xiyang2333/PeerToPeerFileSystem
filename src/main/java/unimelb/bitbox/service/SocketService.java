package unimelb.bitbox.service;

import java.io.IOException;
import java.net.Socket;

public interface SocketService {
    public void send(Socket socket, String message) throws IOException;

}
