package com.example.devcomjavamobile.network;

import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class ControlTraffic {

    private final static int PORT_CONTROL = 3283;

    public LinkedList<RoutingTable> peers;

    public ControlTraffic(LinkedList<RoutingTable> peers)
    {
        this.peers = peers;
    }

    public Socket connectTCPControlServer(String physicalAddress) throws IOException {
        InetAddress address = InetAddress.getByName(physicalAddress);
        Socket s = new Socket(address, PORT_CONTROL);
        s.setTcpNoDelay(true);
        return s;
    }

}
