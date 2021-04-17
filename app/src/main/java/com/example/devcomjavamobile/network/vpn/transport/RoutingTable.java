package com.example.devcomjavamobile.network.vpn.transport;


import android.os.ParcelFileDescriptor;

import java.util.LinkedList;
import java.util.List;

public class RoutingTable {

    static final int MAX_ADDRESSES = 5;
    static final int MAX_COMMUNITIES = 5;
    static final int PASSWORD_LENGTH = 32; // 8 bits * 32 chars = 256 bit keys

    String fingerPrint;
    LinkedList<String> physicalAddresses;
    LinkedList<String> communities;
    String password;
    ParcelFileDescriptor fd_control; // File descriptor of the TCP control channel associated with this device
    String key_pair; // Temporary variable, will find better later on

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
        if(!(physicalAddresses.indexOf(physicalAddresses.peekLast()) >= MAX_ADDRESSES))
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
}