package unimelb.bitbox.service;

import unimelb.bitbox.util.ClientManager;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public interface CmdParser {

    public ClientManager parserCommandLine(String[] args);
}
