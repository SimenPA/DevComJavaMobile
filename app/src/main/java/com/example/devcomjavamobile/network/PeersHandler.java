package com.example.devcomjavamobile.network;

import java.net.Socket;
import java.util.LinkedList;

public class PeersHandler {


    LinkedList<RoutingTable> peers;


    public PeersHandler(LinkedList<RoutingTable> peers)
    {
        this.peers = peers;
    }

    public LinkedList<RoutingTable> createPeersTable()
    {
        return new LinkedList<>();
    }

    public RoutingTable addPhysicalAddress(String fingerPrint, String physicalAddress)
    {
        boolean fingerPrintExists = false;
        for(RoutingTable p : peers)
        {
            if(p.getFingerPrint().equals(fingerPrint))
            {
                p.addPhysicalAddress(physicalAddress);
                fingerPrintExists = true;
            }
        }
        if(!fingerPrintExists)
        {
            RoutingTable peer = addFingerPrint(fingerPrint);
            peer.addPhysicalAddress(physicalAddress);
        }
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
