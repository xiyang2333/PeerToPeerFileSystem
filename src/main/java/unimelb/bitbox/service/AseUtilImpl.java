package unimelb.bitbox.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 * @Purpose: XIGUANGL
 **/
public class AseUtilImpl implements AseUtil{

    private static Logger log = Logger.getLogger(SocketReceiveDealThread.class.getName());

    @Override
    public String encrypt(String plainText, Key secretKey) {

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] p = plainText.getBytes("UTF-8");
            String encoded = Base64.getEncoder().encodeToString(cipher.doFinal(p));
            return encoded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String decrypt(String cipherText, Key secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] c = Base64.getDecoder().decode(cipherText);
            byte[] result = cipher.doFinal(c);
            String plainText = new String(result, "UTF-8");
            return plainText;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public Key getKey(String keyString) {

        if (keyString == null) {
            keyString = "lxg1230@!#$%";
        }

        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(keyString.getBytes());
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128,secureRandom);
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
