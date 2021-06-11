package com.example.devcomjavamobile.network.security;

import android.util.Log;

import com.example.devcomjavamobile.Utility;
import com.example.devcomjavamobile.network.devcom.P2P;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {
    private static String TAG = RSAUtil.class.getSimpleName();

    private static String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";
    private static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    public static PublicKey getPublicKey(String base64PublicKey){
        PublicKey publicKey = null;
        try{
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static PrivateKey getPrivateKey(String base64PrivateKey){
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey.getBytes()));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public static byte[] encrypt(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        return cipher.doFinal(data.getBytes());
    }

    public static boolean encrypt(byte[] controlPacket, byte[] payload, PublicKey publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] newPayload =  new byte[1500];
        if(payload.length <= 1500)
        {
            System.arraycopy(payload, 0, newPayload, 0, payload.length);
        } else {
            Log.e(TAG, "Payload length is longer than the maximum allowed 1500 bytes");
            return false;
        }


        for(int i = 0; i < 3; i++)
        {
            byte[] toEncrypt =  new byte[500];
            System.arraycopy(newPayload,(i * 500), toEncrypt, 0, 500); // - 12 due to PKCS1 Padding
            System.arraycopy(cipher.doFinal(toEncrypt), 0, controlPacket, (i*512) + 23, 512); // Destintation (i * 512) + 23 due to encryption leaving 512 bytes and to skip the 23 byte header in control packet
        }
        return true;
    }

    public static byte[] decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedPacket = new byte[data.length];
        for(int i = 0; i < data.length / 500; i++)
        {
            byte[] toDecrypt =  new byte[512];
            System.arraycopy(data,(i * 512) + 23, toDecrypt, 0, 512);
            System.arraycopy(cipher.doFinal(toDecrypt), 0, decryptedPacket, (i*512), 512);
        }

        return data;
    }

    public static int sign(byte[] controlPacket) throws Exception {
        Crypto c = new Crypto();

        byte[] data = new byte[1559];

        System.arraycopy(controlPacket, 0, data, 0, data.length);
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(c.readPrivateKey(PRIVATE_KEY_PATH));
        privateSignature.update(data);

        byte[] signatureBytes = privateSignature.sign();
        System.arraycopy(signatureBytes, 0, controlPacket, 1559, signatureBytes.length);
        return signatureBytes.length;
    }

    public static boolean verify(byte[] controlPacket, RSAPublicKey publicKey) throws Exception
    {
        byte[] signatureBytes = new byte[512];
        System.arraycopy(controlPacket, 1559, signatureBytes, 0, signatureBytes.length);
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(publicKey);

        byte[] data = new byte[1559];
        System.arraycopy(controlPacket, 0, data, 0, data.length);
        sign.update(data);

        return sign.verify(signatureBytes);
    }

    public static void testSignAndVerify() throws Exception
    {
        P2P p2p = new P2P(null);
        byte[] data = new byte[2071];

        String testString = "Sign/verify test string";

        p2p.newControlPacket(data, 'J', "family", Utility.createFingerPrint());
        byte[] testStringBytes = testString.getBytes();

        System.arraycopy(testStringBytes, 0, data, 23, testStringBytes.length);
        int signatureLength = sign(data);
        Log.i(TAG, "Signature length: " + signatureLength);
        Crypto c = new Crypto();
        RSAPublicKey publicKey = c.readPublicKey(PUBLIC_KEY_PATH);
        if(verify(data,publicKey)) { Log.i(TAG, "Sign/verify test successful"); } else { Log.i(TAG, "Sign/verify test failed"); }
    }
}