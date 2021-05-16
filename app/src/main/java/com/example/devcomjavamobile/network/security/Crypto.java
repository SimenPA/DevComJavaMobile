package com.example.devcomjavamobile.network.security;

import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Crypto {
    private final String PRIVATE_KEY_PATH =  "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";
    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";
    private final int KEY_SIZE =  4096;

    private final String TAG = Crypto.class.getSimpleName();

    public void Crypto()
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

    public PublicKey readPublicKey(String filePath) throws Exception {

        File file = new File(filePath);
        String publicKeyContent = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filePath).toURI())));

        publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PUBLIC KEY-----", "").replace("-----END RSA PUBLIC KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        return kf.generatePublic(pubKeySpec);
    }

    public byte[] encrypt(String data, PublicKey publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data.getBytes());
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(data));
    }

    public void testKeys() throws Exception
    {
        PrivateKey privKey = readPrivateKey(PRIVATE_KEY_PATH);
        PublicKey pubKey = readPublicKey(PUBLIC_KEY_PATH);

        String testString = "hei hei";
        Log.d(TAG, "Unencrypted: " + testString);

        byte[] encrypted = encrypt(testString, pubKey);
        Log.d(TAG, "Encrypted: " + encrypted);

        String decrypted = decrypt(encrypted, privKey);
        Log.d(TAG, "Decrypted: " + decrypted);
    }
}


