package com.example.devcomjavamobile.network.security;

import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/*
TODO: DELETE THIS FILE, NOT IN USE
 */

public class RSAKeyPairGenerator {

    private final String TAG = RSAKeyPairGenerator.class.getSimpleName();

    public RSAKeyPairGenerator()
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        return keyGen.generateKeyPair();
    }

    public PrivateKey loadPrivFromPath(String filePath) throws Exception
    {
        File file = new File(filePath);
        if(file.isFile()) {
            Log.d(TAG, "Private key file found, reading");
            String privateKeyContent = new String(Files.readAllBytes(Paths.get(filePath)));
            privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");
            Log.d(TAG, privateKeyContent);

            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpecPKCS8);
        } else
        {
            Log.d(TAG, "Private key file not found");
            return null;
        }
    }

    public PublicKey loadPubFromPath(String filePath) throws Exception
    {
        return null;
    }

    public void writeToFile(String path, byte[] key) throws IOException
    {
        File f = new File(path);
        f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(key);
        fos.flush();
        fos.close();
    }

    /*
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
        keyPairGenerator.writeToFile("RSA/publicKey", keyPairGenerator.getPublicKey().getEncoded());
        keyPairGenerator.writeToFile("RSA/privateKey", keyPairGenerator.getPrivateKey().getEncoded());
        System.out.println(Base64.getEncoder().encodeToString(keyPairGenerator.getPublicKey().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPairGenerator.getPrivateKey().getEncoded()));
    }

     */
}