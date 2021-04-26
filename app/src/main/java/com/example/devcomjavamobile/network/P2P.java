package com.example.devcomjavamobile.network;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.devcomjavamobile.network.RoutingTable.PASSWORD_LENGTH;

public class P2P {

    private final static String TAG = P2P.class.getSimpleName();

    LinkedList<RoutingTable> peers;
    String myFingerPrint

    public P2P(LinkedList<RoutingTable> peers, String myFingerPrint)
    {
        this.peers = peers;
        this.myFingerPrint =  myFingerPrint;
    }
    public void joinCommunity(String community, String fingerPrint, String physicalAddress) throws IOException {
        PeersHandler pHandler = new PeersHandler(peers);
        pHandler.addFingerPrint(fingerPrint);
        pHandler.addPhysicalAddress(fingerPrint, physicalAddress);

        //TODO: save cache file method
        ControlTraffic ct = new ControlTraffic(peers);
        Socket controlSocket = ct.connectTCPControlServer(physicalAddress);
        pHandler.addControlSocket(fingerPrint, controlSocket);


    }

    public void sendControlJoin(Socket controlSocket, String commmunity, String fingerPrint)
    {
        PeersHandler pHandler =  new PeersHandler(peers);
        RoutingTable peer = pHandler.getPeer(fingerPrint);

        peer.setUdp(0);
        // 23 byte header + 1536 byte encrypted payload + 512 byte signature = 2071 byte packet
        byte[] controlPacket = new byte[2071];
        newControlPacket(controlPacket, 'J', commmunity, myFingerPrint);

        char[] payload = new char[PASSWORD_LENGTH];
        generatePassword(payload, PASSWORD_LENGTH);
        Log.d(TAG, "Session key: " + payload.toString());
        // TODO: This is where I left off after 26.4. Method is send_control_join in control_traffic.c
        //addSessionKey()


    }

    public void sendControlSync(Socket controlSocket, String commmunity, String fingerPrint)
    {

    }

    public void newControlPacket(byte[] controlPacket, char type, String community, String fingerPrint)
    {

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
