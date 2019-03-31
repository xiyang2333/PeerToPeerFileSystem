package unimelb.bitbox.service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketServiceImpl implements SocketService {

    @Override
    public void send(Socket socket, String message) throws IOException{
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF(message);
        out.flush();
    }
}
