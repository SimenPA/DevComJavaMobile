package com.example.devcomjavamobile.network.devcom;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.security.RSAUtil;
import com.example.devcomjavamobile.network.vpn.ClientPacketWriter;

import java.io.File;
import java.math.BigInteger;
import java.net.Socket;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static com.example.devcomjavamobile.network.devcom.Peer.PASSWORD_LENGTH;

public class P2P {

    private final static String TAG = P2P.class.getSimpleName();

    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    LinkedList<Peer> peers;
    String myFingerPrint;
    Activity activity;
    ClientPacketWriter tunnelWriter;

    public P2P(Activity activity, ClientPacketWriter tunnelWriter)  {
        this.peers = MainActivity.getPeers();
        this.activity =  activity;
        this.tunnelWriter = tunnelWriter;
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
        Log.i(TAG, "My fingerprint: " + myFingerPrint.toUpperCase());
        PeersHandler.addFingerPrint(devFingerPrint);
        PeersHandler.addPhysicalAddress(devFingerPrint, devPhysicalAddress);

        Log.d(TAG, "PeersHandler has added community, supposedly");

        //TODO: save cache file method - .cache file, text file with fingerprint and known physical addresses
        ControlTraffic controlTraffic = new ControlTraffic(devPhysicalAddress, null, activity, tunnelWriter);
        controlTraffic.start();
        PeersHandler.addControlTraffic(devFingerPrint, controlTraffic);

        sendControlJoin(controlTraffic, community, devFingerPrint);

        // Poor hack to check whether the connection has been successfully established or not, but it works for now as a visual indicator
        TimeUnit.MILLISECONDS.sleep(500);
        if(!controlTraffic.isStopped()) {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Control channel to device " + devFingerPrint + " successfully established", Toast.LENGTH_SHORT).show());
        } else {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Unable to connect to device " + devFingerPrint, Toast.LENGTH_SHORT).show());
        }
    }

    public void sendControlJoin(ControlTraffic ct, String commmunity, String fingerPrint) throws Exception {
        Peer peer = PeersHandler.getPeer(fingerPrint);
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
                    RSAPublicKey pk = Crypto.readPublicKey(file);
                    peer.setPublicKey(pk);
                    hasPublicKey = true;
                }
            }
        } else { hasPublicKey = true; }

        if(hasPublicKey) {

            peer.setUdp(1); // This should be set to 0 here, and set to 1 if UDP check is successful, but this is always set to 1 here as I join from VM to physical,
            // and UDP only works from VM to physical in my testing case, so check will fail physical to VM, but other way will work

            // 23 byte header + 1536 byte encrypted payload + 512 byte signature = 2071 byte packet
            byte[] controlPacket = new byte[2071];
            Log.d(TAG, "My fingerprint: " + myFingerPrint);
            Log.i(TAG, "Community: " + commmunity);
            newControlPacket(controlPacket, 'J', commmunity, myFingerPrint);
            peer.addCommunity(commmunity);

            byte packetType = controlPacket[0]; // "J", "P", "S" "T" "L" "D" "A"
            Log.i(TAG, "Packet Type: " + (char) packetType);


            char[] key = new char[PASSWORD_LENGTH];
            Crypto.generatePassword(key, PASSWORD_LENGTH);
            Log.d(TAG, "Session key: " + String.valueOf(key));
            peer.setPassword(key); // adds session key

            // Initliaize AES encryption
            Crypto.aesInit(String.valueOf(key), peer);

            byte[] payload = new byte[32];
            for(int i = 0; i < key.length; i++)
                payload[i] = (byte) key[i];

            boolean successfulEncryption = Crypto.encryptControlPacket(controlPacket, payload, peer.getPublicKey()); // encrypt using RSA
            if(successfulEncryption)
            {
                int signatureLength = Crypto.sign(controlPacket); // sign with RSA public key
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

    // TODO: Add this method
    public void sendControlSync(Socket controlSocket, String commmunity, String fingerPrint)
    {
        //
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
        RSAPublicKey pk = Crypto.readPublicKey(PUBLIC_KEY_PATH);
        BigInteger publicModulus = pk.getModulus();
        return publicModulus.toString(16).substring(0,16).toUpperCase();
    }

}
