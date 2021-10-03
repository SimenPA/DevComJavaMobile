package com.example.devcomjavamobile.network.devcom;

import java.util.LinkedList;

public class PeersHandler {


    LinkedList<Peer> peers;


    public static void addPhysicalAddress(String fingerPrint, String physicalAddress, LinkedList<Peer> peers)
    {

        for(Peer peer : peers)
        {
            if(peer.getFingerPrint().equals(fingerPrint))
            {
                peer.addPhysicalAddress(physicalAddress);
                return;
            }
        }

        // Only gets here if fingerprint doesn't exist
        Peer peer = addFingerPrint(fingerPrint, peers);
        peer.addPhysicalAddress(physicalAddress);
    }

    public static Peer addFingerPrint(String fingerPrint, LinkedList<Peer> peers)
    {
        Peer peer = new Peer();
        peer.setFingerPrint(fingerPrint);
        peers.add(peer);
        return peer;
    }

    public static void addControlTraffic(String fingerPrint, ControlTraffic controlTraffic, LinkedList<Peer> peers)
    {
        for(Peer p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                p.setControlTraffic(controlTraffic);
            }
        }
    }

    public static Peer getPeer(String fingerPrint, LinkedList<Peer> peers)
    {
        for(Peer p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                return p;
            }
        }
        return null;
    }
}
