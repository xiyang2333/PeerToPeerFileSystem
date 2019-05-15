package unimelb.bitbox.service;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public interface RsaUtil {

    public String encrypt(String plainText, String publicKey) throws Exception;
    public String decrypt(String cipherText, String privateKey) throws Exception;


}
