package com.example.devcomjavamobile.network.devcom;

import java.util.LinkedList;

public class PeersHandler {


    LinkedList<Peer> peers;

    public PeersHandler(LinkedList<Peer> peers)
    {
        this.peers = peers;
    }

    public Peer addPhysicalAddress(String fingerPrint, String physicalAddress)
    {

        boolean fingerPrintExists = false;
        for(Peer peer : peers)
        {
            if(peer.getFingerPrint().equals(fingerPrint))
            {
                peer.addPhysicalAddress(physicalAddress);
                return peer;
            }
        }

        // Only gets here if fingerprint doesn't exist
        Peer peer = addFingerPrint(fingerPrint);
        peer.addPhysicalAddress(physicalAddress);
        return peer;
    }

    public Peer addFingerPrint(String fingerPrint)
    {
        Peer peer = new Peer();
        peers.add(peer);
        return peer;
    }

    public Peer addControlTraffic(String fingerPrint, ControlTraffic controlTraffic)
    {
        boolean fingerPrintExists = false;
        for(Peer p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                p.setControlTraffic(controlTraffic);
                return p;
            }
        }
        return null;
    }

    public Peer getPeer(String fingerPrint)
    {
        boolean fingerPrintExists = false;
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
