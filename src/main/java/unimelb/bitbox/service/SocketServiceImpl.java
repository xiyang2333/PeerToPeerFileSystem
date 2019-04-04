package unimelb.bitbox.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class SocketServiceImpl implements SocketService {
    private static Logger log = Logger.getLogger(SocketServiceImpl.class.getName());

    @Override
    public void send(Socket socket, String message) throws IOException{
        log.info(message);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        writer.write(message + "\n");
        writer.flush();
    }
}
