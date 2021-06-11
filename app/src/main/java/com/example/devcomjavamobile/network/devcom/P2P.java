package com.example.devcomjavamobile.network.devcom;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.security.RSAUtil;

import java.io.File;
import java.math.BigInteger;
import java.net.Socket;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;

import javax.crypto.Cipher;

import static com.example.devcomjavamobile.network.devcom.Peer.PASSWORD_LENGTH;

public class P2P {

    private final static String TAG = P2P.class.getSimpleName();

    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    LinkedList<Peer> peers;
    String myFingerPrint;
    Activity activity;

    public P2P(Activity activity)  {
        this.peers = MainActivity.getPeers();
        this.activity =  activity;
        try
        {
            myFingerPrint =  createFingerprint();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        if(peers == null)
        {
            Log.i(TAG, "No peers");
        }
    }
    public void joinCommunity(String community, String devFingerPrint, String devPhysicalAddress) throws Exception {
        PeersHandler pHandler = new PeersHandler(peers);
        Log.i(TAG, "My fingerprint: " + myFingerPrint.toUpperCase());
        pHandler.addFingerPrint(devFingerPrint);
        pHandler.addPhysicalAddress(devFingerPrint, devPhysicalAddress);

        Log.d(TAG, "PeersHandler has added community, supposedly");

        //TODO: save cache file method - .cache file, text file with fingerprint and known physical addresses
        ControlTraffic controlTraffic = new ControlTraffic(devPhysicalAddress, null);
        controlTraffic.start();
        pHandler.addControlTraffic(devFingerPrint, controlTraffic);

        sendControlJoin(controlTraffic, community, devFingerPrint);
    }

    public void sendControlJoin(ControlTraffic ct, String commmunity, String fingerPrint) throws Exception {
        PeersHandler pHandler = new PeersHandler(peers);
        Peer peer = pHandler.getPeer(fingerPrint);
        if(peer == null)
        {
            Log.i(TAG, "No known device with fingerprint: " + fingerPrint);
            return;
        }
        boolean hasPublicKey = false;
        if(peer.getPublicKey() == null) // check for public key in files if
        {
            Log.i(TAG, "Public key file not in peer, checking for saved keys");
            File f = new File("/data/data/com.example.devcomjavamobile/");
            String[] fileList = f.list();
            for(String file : fileList) {
                if(file.equals(fingerPrint + "pem.tramp"))
                {
                    Crypto c = new Crypto();
                    RSAPublicKey pk = c.readPublicKey(file);
                    peer.setPublicKey(pk);
                    hasPublicKey = true;
                }
            }
        } else { hasPublicKey = true; }

        if(hasPublicKey) {
            Crypto crypto = new Crypto();

            peer.setUdp(0); // Not sure why this is necessary, but it is done in the DevCom C

            // 23 byte header + 1536 byte encrypted payload + 512 byte signature = 2071 byte packet
            byte[] controlPacket = new byte[2071];
            Log.d(TAG, "My fingerprint: " + myFingerPrint);
            Log.i(TAG, "Community: " + commmunity);
            newControlPacket(controlPacket, 'J', commmunity, myFingerPrint);

            byte packetType = controlPacket[0]; // "J", "P", "S" "T" "L" "D" "A"
            Log.i(TAG, "Packet Type: " + (char) packetType);


            char[] key = new char[PASSWORD_LENGTH];
            crypto.generatePassword(key, PASSWORD_LENGTH);
            Log.d(TAG, "Session key: " + key.toString());
            peer.setPassword(key); // adds session key

            Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            crypto.aesInit(key.toString(), encryptCipher, decryptCipher);

            byte[] payload = new byte[32];
            for(int i = 0; i < key.length; i++)
                payload[i] = (byte) key[i];

            boolean successfulEncryption = RSAUtil.encrypt(controlPacket, payload, peer.getPublicKey()); // encrypt using RSA
            if(successfulEncryption)
            {
                int signatureLength = RSAUtil.sign(controlPacket); // sign with RSA public key
                if(signatureLength == 512)
                {
                    Log.d(TAG, "Preparing to write control package");
                    ct.write(controlPacket);
                } else {
                    Log.e(TAG, "Package signing failed, aborting join");
                }
            } else { Log.e(TAG,"Encryption failed, aborting join"); }


        } else {
            Log.i(TAG, "Public key is unknown, unable to proceed");
            activity.runOnUiThread(() -> Toast.makeText(activity, "No public key known for fingerprint: " + fingerPrint, Toast.LENGTH_SHORT).show());
        }
    }

    public void sendControlSync(Socket controlSocket, String commmunity, String fingerPrint)
    {

    }

    public void newControlPacket(byte[] controlPacket, char type, String community, String fingerPrint) {
        controlPacket[0] = (byte)type;
        int i = 1;
        for(char c : community.toCharArray())
        {
            controlPacket[i] = (byte)c;
            i++;
        }
        i = 7;
        for(char c : fingerPrint.toCharArray())
        {
            controlPacket[i] = (byte)c;
            i++;
        }
    }



    public String createFingerprint() throws Exception {

        Crypto c = new Crypto();

        RSAPublicKey pk = c.readPublicKey(PUBLIC_KEY_PATH);
        BigInteger publicModulus = pk.getModulus();
        return publicModulus.toString(16).substring(0,16).toUpperCase();
    }

    public native char[] control_packet_encrypt(char[] packet, char[] payload, String key_pair);
}
