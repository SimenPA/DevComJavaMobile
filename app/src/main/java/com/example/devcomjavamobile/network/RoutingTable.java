package com.example.devcomjavamobile.network;


import android.os.ParcelFileDescriptor;

import java.net.Socket;
import java.util.LinkedList;

public class RoutingTable {

    static final int MAX_ADDRESSES = 5;
    static final int MAX_COMMUNITIES = 5;
    static final int PASSWORD_LENGTH = 32; // 8 bits * 32 chars = 256 bit keys

    String fingerPrint;
    LinkedList<String> physicalAddresses;
    LinkedList<String> communities;
    String password;
    Socket controlSocket; // Socket of the TCP control channel associated with this device
    String key_pair; // Temporary variable, will find better later on
    int udp;

    public RoutingTable() {
        physicalAddresses = new LinkedList<>();
        communities = new LinkedList<>();
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public void setFingerPrint(String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }

    public LinkedList<String> getPhysicalAddresses() {
        return physicalAddresses;
    }

    public void addPhysicalAddress(String physicalAddress) {
        physicalAddresses.addFirst(physicalAddress);
        if((physicalAddresses.indexOf(physicalAddresses.peekLast()) >= MAX_ADDRESSES))
        {
            physicalAddresses.removeLast();
        }
    }

    public LinkedList<String> getCommunities() {
        return communities;
    }

    public void addCommunity(String community) {
        communities.add(community);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setControlSocket(Socket controlSocket) { this.controlSocket =  controlSocket; }

    public Socket getControlSocket() { return controlSocket; }

    public int getUdp() { return udp; }

    public void setUdp(int udp) { this.udp = udp; }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\nPeers:\n");
        str.append("Fingerprint: " +  getFingerPrint() + "\n");
        str.append("Physical addresses: \n");
        int i = 0;
        for(String b : getPhysicalAddresses())
        {
            i++;
            str.append(i + ". " + b + "\n");
        }
        str.append("Communities: \n");
        i = 0;
        for(String b : getCommunities())
        {
            i++;
            str.append(i + ". " + b + "\n");
        }
        return str.toString();
    }

}