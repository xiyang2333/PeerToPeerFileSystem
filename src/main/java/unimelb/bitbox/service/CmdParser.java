package unimelb.bitbox.service;

import unimelb.bitbox.util.ClientManager;
import unimelb.bitbox.util.Document;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 * @Purpose: XIGUANGL
 **/
public interface CmdParser {

    public ClientManager parserCommandLine(String[] args);
}
