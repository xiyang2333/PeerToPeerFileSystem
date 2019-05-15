package unimelb.bitbox.service;


import java.security.Key;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/
public interface AseUtil {

    public String encrypt(String plainText,  Key secretKey);
    public String decrypt(String cipherText, Key secretKey);
    public Key getKey(String key);


}
