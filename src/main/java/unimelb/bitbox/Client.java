package unimelb.bitbox;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
//import unimelb.bitbox.service.CmdParser;
//import unimelb.bitbox.service.CmdParserImpl;
import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.CmdLineArgs;
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
//    private static CmdParser ParserService = new CmdParserImpl();
    private static ClientMainService clientService = new ClientMainService();

    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("Client starting...");


        CmdLineArgs argsBean = new CmdLineArgs();
        //Parser provided by args4j
        CmdLineParser parser = new CmdLineParser(argsBean);
        try {

            //Parse the arguments
            parser.parseArgument(args);
            System.out.printf("c: %s; s: %s; p: %s", argsBean.getCommand(), argsBean.getServer(), argsBean.getPeer());

            clientService.commandLineRequest(argsBean);
        } catch (CmdLineException e) {
            log.warning(e.getMessage());
        }

//        // parse args
//        ClientManager clientManager = ParserService.parserCommandLine(args);
//        // execute commandline
//        clientService.commandLineRequest(clientManager);

    }



}
