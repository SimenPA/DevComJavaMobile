package com.example.devcomjavamobile.network;

import android.util.Log;

import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPHeader;

import org.bouncycastle.jcajce.provider.symmetric.AES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.SecretKey;

import static com.example.devcomjavamobile.network.Peer.PASSWORD_LENGTH;

public class P2P {

    private final static String TAG = P2P.class.getSimpleName();

    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    LinkedList<Peer> peers;
    String myFingerPrint;

    public P2P(LinkedList<Peer> peers)  {
        this.peers = peers;
        try
        {
            myFingerPrint =  createFingerprint();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void joinCommunity(String community, String devFingerPrint, String devPhysicalAddress) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        PeersHandler pHandler = new PeersHandler(peers);
        Log.i(TAG, "My fingerprint: " + myFingerPrint.toUpperCase());
        pHandler.addFingerPrint(devFingerPrint);
        pHandler.addPhysicalAddress(devFingerPrint, devPhysicalAddress);

        Log.d(TAG, "PeersHandler has added community, supposedly");

        //TODO: save cache file method
        ControlTraffic controlTraffic = new ControlTraffic(peers, devPhysicalAddress);
        controlTraffic.start();
        pHandler.addControlTraffic(devFingerPrint, controlTraffic);

        sendControlJoin(controlTraffic, community, devFingerPrint);
    }

    public void sendControlJoin(ControlTraffic ct, String commmunity, String fingerPrint) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        PeersHandler pHandler =  new PeersHandler(peers);
        Peer peer = pHandler.getPeer(fingerPrint);

        Crypto crypto = new Crypto();

        // peer.setUdp(0);
        // 23 byte header + 1536 byte encrypted payload + 512 byte signature = 2071 byte packet
        byte[] controlPacket = new byte[2071];
        Log.d(TAG, "My fingerprint: " + myFingerPrint);
        newControlPacket(controlPacket, 'J', commmunity, myFingerPrint);

        byte packetType = controlPacket[0]; // "J", "P", "S" "T" "L" "D" "A"
        Log.i(TAG, "Packet Type: " + (char) packetType);

        char[] payload = new char[PASSWORD_LENGTH];
        crypto.generatePassword(payload, PASSWORD_LENGTH);
        Log.d(TAG, "Session key: " + payload.toString());
        // TODO: This is where I left off after 26.4. Method is send_control_join in control_traffic.c
        peer.setPassword(payload); // adds session key
        // SecretKey key = crypto.generateAESKey(payload);

        // aes_init(password, password length, password, encrypt_ctx, decrypt_ctx)

        // char[] encryptedPacket =  control_packet_encrypt(controlPacket, payload, peer.getPublicKeyFilePath());
        Log.d(TAG, "Preparing to write control package");
        ct.write(controlPacket);
    }

    public void sendControlSync(Socket controlSocket, String commmunity, String fingerPrint)
    {

    }

    public void newControlPacket(byte[] controlPacket, char type, String community, String fingerPrint)
    {
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
