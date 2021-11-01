package com.example.devcomjavamobile.network.security;

import android.util.Log;

import com.example.devcomjavamobile.network.devcom.Peer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    private final static String PRIVATE_KEY_PATH =  "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";
    private final static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";
    private final int KEY_SIZE =  4096;
    private final static int AES_BLOCK_SIZE =  16; // 16 bytes

    private final static String TAG = Crypto.class.getSimpleName();

    public Crypto()
    {
        Security.addProvider(new BouncyCastleProvider());
    }



    public static void genKeyPair() throws Exception
    {

        boolean privKeyExists = false;
        boolean pubKeyExists = false;

        File privateKey = new File(PRIVATE_KEY_PATH);
        if(privateKey.isFile())
        {
            privKeyExists = true;
            Log.i(TAG, "Private key already exists");
        }

        File publicKey = new File(PUBLIC_KEY_PATH);
        if(publicKey.isFile())
        {
            pubKeyExists = true;
            Log.i(TAG, "Public key already exists");
        }


        if(!privKeyExists || !pubKeyExists)
        {
            Log.d(TAG, "One of the keys does not exist, deleting any remaining key and creating a new key pair");
            deleteRSAKeys();

            KeyPair keyPair = generateRSAKeyPair();
            RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();

            writePemFile(priv, "RSA PRIVATE KEY", PRIVATE_KEY_PATH);
            writePemFile(pub, "RSA PUBLIC KEY", PUBLIC_KEY_PATH);

        } else {
            Log.d(TAG, "Key pair already exists, not creating new ones");
        }
    }

    private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);

        KeyPair keyPair = keyGen.generateKeyPair();
        Log.i(TAG, "RSA key pair generated.");
        return keyPair;
    }


    public static void deleteRSAKeys()
    {
        File privKey = new File(PRIVATE_KEY_PATH);
        if(privKey.delete())
        {
            Log.i(TAG, "Private key deleted");
        }

        File pubKey = new File(PRIVATE_KEY_PATH);
        if(pubKey.delete())
        {
            Log.i(TAG, "Public key deleted");
        }
    }

    private static void writePemFile(Key key, String description, String filename)
            throws FileNotFoundException, IOException {
        PemFile pemFile = new PemFile(key, description);
        pemFile.write(filename);
        Log.i(TAG, String.format("%s successfully written in file %s.", description, filename));
    }

    public static PrivateKey readPrivateKey(String filePath) throws Exception {

        String privateKeyContent = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filePath).toURI())));

        privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return kf.generatePrivate(keySpecPKCS8);
    }

    public static RSAPublicKey readPublicKey(String filePath) throws Exception {

        String publicKeyContent = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filePath).toURI())));

        publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PUBLIC KEY-----", "").replace("-----END RSA PUBLIC KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        return (RSAPublicKey) kf.generatePublic(pubKeySpec);
    }




    public static byte[] aes_encrypt(byte[] bytes, Cipher encryptCipher) throws Exception {
        byte[] encryptBlock = new byte[16 + bytes.length];
        System.arraycopy(bytes, 0, encryptBlock, 16, bytes.length);
        return encryptCipher.doFinal(encryptBlock);
    }
        /*
        int c_len = bytes.length + AES_BLOCK_SIZE;
        int buf_len = (int)(Math.ceil(c_len/16.0) * 16);
        Log.i(TAG, "Buf_len: " + buf_len);
        Log.i(TAG, "C_len: " + c_len);
        byte[] cipherText =  new byte[buf_len];
        encryptCipher.doFinal(bytes, 0, bytes.length, cipherText, 0);
        return cipherText;
         */

    public static byte[] aes_decrypt(byte[] encryptedBytes, Cipher decryptCipher) throws Exception {
        byte[] decryptedWithIV =  decryptCipher.doFinal(encryptedBytes);
        byte[] decrypted =  new byte[decryptedWithIV.length - 16];
        System.arraycopy(decryptedWithIV,16, decrypted, 0, decrypted.length);
        return decrypted;
    }

    public static void testEncryption() throws Exception {
        Peer peer = new Peer();

        char[] password = new char[32];
        generatePassword(password, 32);
        aesInit(password.toString(), peer);
        char[] message = "Heisann test".toCharArray();
        byte[] buf = new byte[message.length];
        for(int i = 0; i < message.length; i++)
        {
            buf[i] = (byte) message[i];
        }
        Log.i(TAG, "Data: " + Arrays.toString(buf));
        byte[] encrypted = aes_encrypt(buf, peer.getEncryptCipher());
        Log.i(TAG, "Encrypted data: " + Arrays.toString(encrypted));
        byte[] decrypted = aes_decrypt(encrypted, peer.getDecryptCipher());
        Log.i(TAG, "Decrypted data: " + Arrays.toString(decrypted));


    }


    public static void aesInit(String key, Peer peer) throws Exception {

        Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        int ivSize = 16;
        byte[] iv = new byte[ivSize];

        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Hashing key.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes("UTF-8"));
        byte[] keyBytes = new byte[32];
        // Hashing three times
        for(int i = 0; i > 3; i++)
        {
            System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
            digest.update(keyBytes);

        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Encrypt cipher init.
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        // Decrypt cipher init.
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        if(peer != null)
        {
            peer.setEncryptCipher(encryptCipher);
            peer.setDecryptCipher(decryptCipher);
        }


    }

    public static void generatePassword(char[] password, int length)
    {
        int i = 0;

        for(i = 0; i <= length - 1; i++) {
            int n = ThreadLocalRandom.current().nextInt() % 26;
            // int n = rand() % 26;
            char c = (char) (n + 65);
            password[i] = c;
        }
    }

    public static boolean encryptControlPacket(byte[] controlPacket, byte[] payload, PublicKey publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
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

    public static boolean decryptControlPacket(byte[] controlPacket, byte[] payload, PrivateKey privateKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);


        for(int i = 0; i < 3; i++)
        {
            byte[] toDecrypt =  new byte[512];
            System.arraycopy(payload,(i * 512), toDecrypt, 0, 512); //
            System.arraycopy(cipher.doFinal(toDecrypt), 0, controlPacket, (i*500) + 23, 500); // Destination (i * 500) + 23 due to decryption leaving 500 bytes and to skip the 23 byte header in control packet
        }
        return true;
    }

    public static int sign(byte[] controlPacket) throws Exception {

        byte[] data = new byte[1559];

        System.arraycopy(controlPacket, 0, data, 0, data.length);
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(readPrivateKey(PRIVATE_KEY_PATH));
        privateSignature.update(data);

        byte[] signatureBytes = privateSignature.sign();
        System.arraycopy(signatureBytes, 0, controlPacket, 1559, signatureBytes.length);
        return signatureBytes.length;
    }

    public static boolean verifyControlPacket(byte[] controlPacket, RSAPublicKey publicKey) throws Exception
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


}


