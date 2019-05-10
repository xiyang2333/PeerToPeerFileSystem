package unimelb.bitbox.service;

import org.apache.commons.cli.*;
import unimelb.bitbox.util.ClientManager;

import java.util.logging.Logger;
import unimelb.bitbox.util.*;


/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 * @Purpose: XIGUANGL
 **/
public class CmdParserImpl implements CmdParser{

    private static Logger log = Logger.getLogger(SocketReceiveDealThread.class.getName());

    @Override
    public ClientManager parserCommandLine(String[] args) {

        Options options = new Options();
        Option c = Option.builder("c")
                .required(true)
                .hasArg()
                .argName("command")
                .desc("command to execute")
                .build();

        Option s = Option.builder("s")
                .required(true)
                .hasArg()
                .argName("port")
                .desc("local port")
                .build();

        Option p = Option.builder("p")
                .required(false)
                .hasArg()
                .argName("port")
                .desc("target port")
                .build();

        options.addOption(c);
        options.addOption(s);
        options.addOption(p);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        ClientManager clientManager = new ClientManager();


        try {

            cmd = parser.parse(options, args);

            if (cmd.hasOption("c")){

                String requestType = cmd.getOptionValue("c");
                if (requestType.equals("list_peers")){
                    clientManager.requesttype = REQUESTTYPE.LIST_PEERS_REQUEST;

                }else if(requestType.equals("connect_peer")){
                    clientManager.requesttype = REQUESTTYPE.CONNECT_PEER_REQUEST;

                }else if(requestType.equals("disconnect_peer")){
                    clientManager.requesttype = REQUESTTYPE.DISCONNECT_PEER_REQUEST;

                }else {

                    log.warning("Unrecognized Command");
                }

            }

            if (cmd.hasOption("s")){
                String localPort = cmd.getOptionValue("s");
                clientManager.hostPort = new HostPort(localPort);


            }

            if (cmd.hasOption("p")){
                String targetPort = cmd.getOptionValue("p");
                clientManager.targetHostPort = new HostPort(targetPort);
            }


        }catch (ParseException e){

            log.warning(e.getMessage());


        }





       return clientManager;

    }
}
