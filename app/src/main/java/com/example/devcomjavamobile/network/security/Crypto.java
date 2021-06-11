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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    private final String PRIVATE_KEY_PATH =  "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";
    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";
    private final int KEY_SIZE =  4096;

    private final String TAG = Crypto.class.getSimpleName();

    public Crypto()
    {
        Security.addProvider(new BouncyCastleProvider());
    }



    public void genKeyPair() throws Exception
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
            deleteKeys();

            KeyPair keyPair = generateRSAKeyPair();
            RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();

            writePemFile(priv, "RSA PRIVATE KEY", PRIVATE_KEY_PATH);
            writePemFile(pub, "RSA PUBLIC KEY", PUBLIC_KEY_PATH);

        } else {
            Log.d(TAG, "Key pair already exists, not creating new ones");
        }

        testKeys();
    }

    private  KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);

        KeyPair keyPair = keyGen.generateKeyPair();
        Log.i(TAG, "RSA key pair generated.");
        return keyPair;
    }

    private void writePemFile(Key key, String description, String filename)
            throws FileNotFoundException, IOException {
        PemFile pemFile = new PemFile(key, description);
        pemFile.write(filename);
        Log.i(TAG, String.format("%s successfully written in file %s.", description, filename));
    }

    public void deleteKeys()
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

    public PrivateKey readPrivateKey(String filePath) throws Exception {

        File file = new File(filePath);
        String privateKeyContent = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filePath).toURI())));

        privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return kf.generatePrivate(keySpecPKCS8);
    }

    public RSAPublicKey readPublicKey(String filePath) throws Exception {

        File file = new File(filePath);
        String publicKeyContent = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filePath).toURI())));

        publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PUBLIC KEY-----", "").replace("-----END RSA PUBLIC KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        return (RSAPublicKey) kf.generatePublic(pubKeySpec);
    }

    public void testKeys() throws Exception
    {
        PrivateKey privKey = readPrivateKey(PRIVATE_KEY_PATH);
        PublicKey pubKey = readPublicKey(PUBLIC_KEY_PATH);

        String testString = "RSA keys test";
        Log.d(TAG, "Unencrypted: " + testString);

        byte[] encrypted = aes_encrypt(testString, pubKey);
        Log.d(TAG, "Encrypted: " + encrypted);

        String decrypted = aes_decrypt(encrypted, privKey);
        Log.d(TAG, "Decrypted: " + decrypted);
    }



    public byte[] aes_encrypt(String plainText, Cipher encryptCipher) throws Exception {

        byte[] clean = plainText.getBytes();
        byte[] encrypted = encryptCipher.doFinal(clean);

        return encrypted;

    }

    public byte[] aes_decrypt(byte[] encryptedBytes, Cipher decryptCipher) throws Exception {
        return decryptCipher.doFinal(encryptedBytes);
    }


    public void aesInit(String key, Peer peer) throws Exception {

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
        byte[] keyBytes = new byte[16];
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

        peer.setEncryptCipher(encryptCipher);
        peer.setDecryptCipher(decryptCipher);

    }

    public void generatePassword(char[] password, int length)
    {
        int i = 0;

        for(i = 0; i <= length - 1; i++) {
            int n = ThreadLocalRandom.current().nextInt() % 26;
            // int n = rand() % 26;
            char c = (char) (n + 65);
            password[i] = c;
        }
    }

}


