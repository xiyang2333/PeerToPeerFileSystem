package unimelb.bitbox;

import unimelb.bitbox.service.CmdParser;
import unimelb.bitbox.service.CmdParserImpl;
import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.Document;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public class Client {


    private static Logger log = Logger.getLogger(Peer.class.getName());
    private static CmdParser ParserService = new CmdParserImpl();
    private static ClientMainService clientService = new ClientMainService();

    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("Client starting...");

        // parse args
        ClientManager clientManager = ParserService.parserCommandLine(args);
        // execute commandline
        clientService.commandLineRequest(clientManager);

    }



}
