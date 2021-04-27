package com.example.devcomjavamobile.network;

import java.net.Socket;
import java.util.LinkedList;

public class PeersHandler {


    LinkedList<RoutingTable> peers;

    public PeersHandler(LinkedList<RoutingTable> peers)
    {
        this.peers = peers;
    }

    public RoutingTable addPhysicalAddress(String fingerPrint, String physicalAddress)
    {

        boolean fingerPrintExists = false;
        for(RoutingTable peer : peers)
        {
            if(peer.getFingerPrint().equals(fingerPrint))
            {
                peer.addPhysicalAddress(physicalAddress);
                return peer;
            }

        }

        // Only gets here if fingerprint doesn't exist
        RoutingTable peer = addFingerPrint(fingerPrint);
        peer.addPhysicalAddress(physicalAddress);
        return peer;
    }

    public RoutingTable addFingerPrint(String fingerPrint)
    {
        RoutingTable peer = new RoutingTable();
        peers.add(peer);
        return peer;
    }

    public RoutingTable addControlSocket(String fingerPrint, Socket controlSocket)
    {
        boolean fingerPrintExists = false;
        for(RoutingTable p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                p.setControlSocket(controlSocket);
                return p;
            }
        }
        return null;
    }

    public RoutingTable getPeer(String fingerPrint)
    {
        boolean fingerPrintExists = false;
        for(RoutingTable p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                return p;
            }
        }
        return null;
    }
}
