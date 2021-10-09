package com.example.devcomjavamobile.network.devcom;

import com.example.devcomjavamobile.MainActivity;

import java.util.LinkedList;

public class PeersHandler {


    public static void addPhysicalAddress(String fingerPrint, String physicalAddress)
    {
        LinkedList<Peer> peers = MainActivity.getPeers();
        for(Peer peer : peers)
        {
            if(peer.getFingerPrint().equals(fingerPrint))
            {
                peer.addPhysicalAddress(physicalAddress);
                return;
            }
        }

        // Only gets here if fingerprint doesn't exist
        Peer peer = addFingerPrint(fingerPrint);
        peer.addPhysicalAddress(physicalAddress);
    }

    public static Peer addFingerPrint(String fingerPrint)
    {
        LinkedList<Peer> peers = MainActivity.getPeers();

        for(Peer peer : peers)
        {
            if(peer.getFingerPrint().equals(fingerPrint))
            {
                return peer;
            }
        }
        // Only gets here if fingerprint doesn't exist
        Peer peer = new Peer();
        peer.setFingerPrint(fingerPrint);
        peers.add(peer);
        return peer;
    }

    public static void addControlTraffic(String fingerPrint, ControlTraffic controlTraffic)
    {
        LinkedList<Peer> peers = MainActivity.getPeers();
        for(Peer p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                p.setControlTraffic(controlTraffic);
            }
        }
    }

    public static void clearStoppedControlTraffic() {
        LinkedList<Peer> peers = MainActivity.getPeers();
        for(Peer p : peers) {
            if(p.getControlTraffic() != null) {
                if (p.getControlTraffic().isStopped()) {
                    p.setControlTraffic(null);
                }
            }
        }
    }

    public static Peer getPeer(String fingerPrint)
    {
        LinkedList<Peer> peers = MainActivity.getPeers();
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
