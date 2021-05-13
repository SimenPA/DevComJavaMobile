package com.example.devcomjavamobile.network;

import android.util.Log;

import com.example.devcomjavamobile.network.vpn.transport.ip.IPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPHeader;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.devcomjavamobile.network.Peer.PASSWORD_LENGTH;

public class P2P {

    private final static String TAG = P2P.class.getSimpleName();

    LinkedList<Peer> peers;
    String myFingerPrint;

    public P2P(LinkedList<Peer> peers, String myFingerPrint)
    {
        this.peers = peers;
        this.myFingerPrint =  myFingerPrint;
    }
    public void joinCommunity(String community, String fingerPrint, String physicalAddress) throws IOException {
        PeersHandler pHandler = new PeersHandler(peers);
        pHandler.addFingerPrint(fingerPrint);
        pHandler.addPhysicalAddress(fingerPrint, physicalAddress);

        Log.d(TAG, "PeersHandler has added community, supposedly");

        //TODO: save cache file method
        ControlTraffic controlTraffic = new ControlTraffic(peers, physicalAddress);
        controlTraffic.start();
        pHandler.addControlTraffic(fingerPrint, controlTraffic);

        sendControlJoin(controlTraffic, community, fingerPrint);
    }

    public void sendControlJoin(ControlTraffic ct, String commmunity, String fingerPrint)
    {
        PeersHandler pHandler =  new PeersHandler(peers);
        Peer peer = pHandler.getPeer(fingerPrint);

        // peer.setUdp(0);
        // 23 byte header + 1536 byte encrypted payload + 512 byte signature = 2071 byte packet
        byte[] controlPacket = new byte[2071];
        Log.d(TAG, "My fingerprint: " + myFingerPrint);
        newControlPacket(controlPacket, 'J', commmunity, myFingerPrint);

        byte packetType = controlPacket[0]; // "J", "P", "S" "T" "L" "D" "A"
        Log.i(TAG, "Packet Type: " + (char) packetType);

        char[] payload = new char[PASSWORD_LENGTH];
        generatePassword(payload, PASSWORD_LENGTH);
        Log.d(TAG, "Session key: " + payload.toString());
        // TODO: This is where I left off after 26.4. Method is send_control_join in control_traffic.c
        // addSessionKey()

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

    public native char[] control_packet_encrypt(char[] packet, char[] payload, String key_pair);
}
