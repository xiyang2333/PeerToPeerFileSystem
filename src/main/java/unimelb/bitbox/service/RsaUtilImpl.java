package unimelb.bitbox.service;

/**
 * @Author: XIGUANG LI <xiguangl@student.unimelb.edu.au>
 **/

import javax.crypto.Cipher;
import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;


public class RsaUtilImpl implements RsaUtil{

    private static String ALGORITHM = "RSA";
    private static String AL_PADDING = "RSA/ECB/PKCS1Padding";

    private static Logger log = Logger.getLogger(SocketReceiveDealThread.class.getName());

    @Override
    public String encrypt(String plainText, String publicKey) throws Exception{

        String cipherText = "";

        try {

            Cipher cipher = Cipher.getInstance(AL_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
            byte[] plaintextBytes = plainText.getBytes("UTF-8");
            cipherText = Base64.getEncoder().encodeToString(cipher.doFinal(plaintextBytes));

        }catch (UnsupportedEncodingException e){

            log.warning(e.getMessage());
        }

        return cipherText;

    }


    // String to PublicKey
    public static PublicKey getPublicKey(String key) throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    // String to PrivateKey
    public static PrivateKey getPrivateKey(String key) throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    @Override
    public String decrypt(String cipherText, String privateKeyFile) throws Exception{


        byte[] dectyptedText = null;
        try {

            Cipher cipher = Cipher.getInstance(AL_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(initPrivateKey(privateKeyFile)));
            byte[] text = Base64.getDecoder().decode(cipherText);
            dectyptedText = cipher.doFinal(text);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return new String(dectyptedText);
    }



    private String initPrivateKey(String privateKeyFile) {

        String privateKey = "";

        try {

            BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
            String s = br.readLine();
            s = br.readLine();
            while (s.charAt(0) != '-') {
                privateKey += s ;
                s = br.readLine();
            }

        } catch (Exception e) {

            log.warning(e.getMessage());
        }

        return privateKey;


    }



}
