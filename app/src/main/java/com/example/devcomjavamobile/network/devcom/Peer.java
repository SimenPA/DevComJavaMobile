package com.example.devcomjavamobile.network.devcom;


import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;

import javax.crypto.Cipher;

// Also known as routing_table in DevCom.c, but I(Simen Persch Andersen) prefer this name
public class Peer {

    static final int MAX_ADDRESSES = 5;
    static final int MAX_COMMUNITIES = 5;
    static final int PASSWORD_LENGTH = 32; // 8 bits * 32 chars = 256 bit keys

    String fingerPrint = "";
    LinkedList<String> physicalAddresses;
    LinkedList<String> communities;
    char[] password; // session key
    ControlTraffic controlTraffic = null; // Socket of the TCP control channel associated with this device
    Cipher encryptCipher;
    Cipher decryptCipher;
    int udp;

    public Cipher getEncryptCipher() {
        return encryptCipher;
    }

    public void setEncryptCipher(Cipher encryptCipher) {
        this.encryptCipher = encryptCipher;
    }


    public Cipher getDecryptCipher() {
        return decryptCipher;
    }

    public void setDecryptCipher(Cipher decryptCipher) {
        this.decryptCipher = decryptCipher;
    }


    public RSAPublicKey getPublicKey() {
        return rsaPublicKey;
    }

    public void setPublicKey(RSAPublicKey rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    RSAPublicKey rsaPublicKey;


    public Peer() {
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

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void setControlTraffic(ControlTraffic controlTraffic) { this.controlTraffic =  controlTraffic; }

    public ControlTraffic getControlTraffic() { return controlTraffic; }

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