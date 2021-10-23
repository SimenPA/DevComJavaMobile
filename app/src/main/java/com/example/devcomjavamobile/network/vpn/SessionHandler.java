/*
 *  Copyright 2014 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.devcomjavamobile.network.vpn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.Utility;
import com.example.devcomjavamobile.network.DataTrafficSender;
import com.example.devcomjavamobile.network.devcom.ControlTraffic;
import com.example.devcomjavamobile.network.devcom.P2P;
import com.example.devcomjavamobile.network.devcom.Peer;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.socket.SocketNIODataService;
import com.example.devcomjavamobile.network.vpn.transport.PacketHeaderException;
import com.example.devcomjavamobile.network.vpn.transport.icmp.ICMPPacket;
import com.example.devcomjavamobile.network.vpn.transport.icmp.ICMPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv6Header;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPHeader;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.udp.UDPHeader;
import com.example.devcomjavamobile.network.vpn.transport.udp.UDPPacketFactory;
import com.example.devcomjavamobile.network.vpn.util.PacketUtil;

import androidx.annotation.NonNull;

import android.util.Log;

import static java.net.InetAddress.*;


/**
 * handle VPN client request and response. it create a new session for each VPN client.
 * @author Borey Sao
 * Date: May 22, 2014
 */

/*
 * Imported by Simen Persch Andersen on 30.03.21 from
 * https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/vpn/SessionHandler.java
 */

public class SessionHandler {
    private final String TAG = SessionHandler.class.getSimpleName();

    private final int PORT_DATA_TRAFFIC =  1337;

    private final SessionManager manager;
    private final SocketNIODataService nioService;
    private final ClientPacketWriter writer;

    private final ExecutorService pingThreadpool;


    public SessionHandler(SessionManager manager, SocketNIODataService nioService, ClientPacketWriter writer) {
        this.manager = manager;
        this.nioService = nioService;
        this.writer = writer;

        // Checking if peers actually have peers hers


        // Pool of threads to synchronously proxy ICMP ping requests in the background. We need to
        // carefully limit these, or a ping flood can cause us big big problems.
        this.pingThreadpool = new ThreadPoolExecutor(
                1, 20, // 1 - 20 parallel pings max
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy() // Replace running pings if there's too many
        );
    }

    /**
     * Handle unknown raw IP packet data
     *
     * @param stream ByteBuffer to be read
     */
    public void handlePacket(@NonNull ByteBuffer stream) throws Exception {
        final byte[] rawPacket = new byte[stream.limit()];
        stream.get(rawPacket, 0, stream.limit());
        stream.rewind();

        final Object IPHeader = IPPacketFactory.createIPHeader(stream);
        IPv4Header ipv4Header;
        IPv6Header ipv6Header;
        if(IPHeader instanceof IPv4Header)
        {
            ipv4Header = (IPv4Header) IPHeader;
            if (ipv4Header.getProtocol() == 6) {
                handleTCPPacket(stream, ipv4Header);
            } else if (ipv4Header.getProtocol() == 17) {
                handleUDPPacket(stream, ipv4Header);
            } else if (ipv4Header.getProtocol() == 1) {
                handleICMPPacket(stream, ipv4Header);
            } else {
                Log.w(TAG, "Unsupported IP protocol: " + ipv4Header.getProtocol());
            }
        }
        // IPv6 stuff
        else if(IPHeader instanceof IPv6Header) {
            ipv6Header = (IPv6Header) IPHeader;
            handleIPv6Packet(stream, ipv6Header);
        }
        else {
            Log.w(TAG, "Unsupported IP header");
        }


    }

    private void handleIPv6Packet(ByteBuffer clientPacketData, IPv6Header iPv6Header) throws Exception {

        LinkedList<Peer> peers = MainActivity.getPeers();
        Log.d(TAG, "Got IPv6 package inc");

        String destinationIP = iPv6Header.getDestinationIPString();
        if(destinationIP.substring(0, 4).equals("fe80")) // Link-local
        {
            Log.d(TAG, "This IPv6 package is link-local(fe80)");
            String community = destinationIP.substring(5, 19).replace(":", "").toUpperCase();
            String fingerPrint = destinationIP.substring(20, 39).replace(":", "").toUpperCase();
            Log.d(TAG, "Fingerprint: " + fingerPrint);
            boolean deviceFound = false;
            for(Peer p : peers) {
                Log.d(TAG, "Checking in peer if fingerprint matches");
                if (p.getFingerPrint().equals(fingerPrint)) {
                    deviceFound = true;
                    Log.d(TAG, "Device found, trying to send");

                    byte[] buf = new byte[clientPacketData.remaining()];
                    clientPacketData.get(buf);

                    Log.i(TAG, "Unencrypted data length: " + buf.length);
                    Log.i(TAG, "Data: " + Arrays.toString(buf));
                    byte[] encryptedBuffer = Crypto.aes_encrypt(buf, p.getEncryptCipher());
                    Log.i(TAG, "Encrypted data length: " + encryptedBuffer.length);
                    Log.i(TAG, "Encrypted data: " + Arrays.toString(encryptedBuffer));

                    byte[] decryptedBuffer = Crypto.aes_decrypt(encryptedBuffer, p.getDecryptCipher());
                    Log.i(TAG, "Decrypted data: " + Arrays.toString(decryptedBuffer));

                    // Send with UDP if available
                    if(p.getUdp() == 1)
                    {
                        if(p.getControlTraffic() != null)
                        {
                            ControlTraffic ct = p.getControlTraffic();
                            String ipAddress = ct.getPhysicalAddress();
                            DatagramPacket datagramPacket = new DatagramPacket(encryptedBuffer, encryptedBuffer.length, InetAddress.getByName(ipAddress), PORT_DATA_TRAFFIC);

                            DataTrafficSender dataTrafficSender = new DataTrafficSender(datagramPacket);
                            dataTrafficSender.start();
                        }


                    }
                    // Send with TCP as fallback
                    else if(p.getControlTraffic() != null) {

                        byte[] controlPacket = new byte[2017];
                        ControlTraffic ct = p.getControlTraffic();
                        P2P p2p = new P2P(null);

                        p2p.newControlPacket(controlPacket, 'P', community, Utility.createFingerPrint());

                        System.arraycopy(encryptedBuffer, 0, controlPacket, 23, encryptedBuffer.length);
                        Crypto.sign(controlPacket);
                        ct.write(controlPacket);

                    } else {
                        Log.d(TAG, "Peer not connected, dismissing packet");
                    }

                    Log.d(TAG, "Package sent");
                }
            }
            if(!deviceFound) { Log.d(TAG, "Device wasn't found"); }
            //
            /*
            * TODO: Handle link-local IPv6 packets where device hasn't been found
            *
            if(!deviceFound)
            {
                // unknown device, pass on
            }
             */
        }
        /*
        * TODO: Pass on
         *
        else {
            // not link-local address, we don't care about this and pass it on
        }
        */

    }
    private void handleUDPPacket(ByteBuffer clientPacketData, IPv4Header ipHeader) throws PacketHeaderException, IOException {
        UDPHeader udpheader = UDPPacketFactory.createUDPHeader(clientPacketData);

        Session session = manager.getSession(
                ipHeader.getDestinationIP(), udpheader.getDestinationPort(),
                ipHeader.getSourceIP(), udpheader.getSourcePort()
        );

        boolean newSession = session == null;

        if (session == null) {
            session = manager.createNewUDPSession(
                    ipHeader.getDestinationIP(), udpheader.getDestinationPort(),
                    ipHeader.getSourceIP(), udpheader.getSourcePort()
            );
        }

        synchronized (session) {
            session.setLastIpHeader(ipHeader);
            session.setLastUdpHeader(udpheader);
            manager.addClientData(clientPacketData, session);
            session.setDataForSendingReady(true);

            // We don't register the session until it's fully populated (as above)
            if (newSession) nioService.registerSession(session);

            // Ping the NIO thread to write this, when the session is next writable
            session.subscribeKey(SelectionKey.OP_WRITE);
            nioService.refreshSelect(session);
        }
        manager.keepSessionAlive(session);
    }

    private void handleTCPPacket(ByteBuffer clientPacketData, IPv4Header ipHeader) throws PacketHeaderException, IOException {
        TCPHeader tcpheader = TCPPacketFactory.createTCPHeader(clientPacketData);
        int dataLength = clientPacketData.limit() - clientPacketData.position();
        int sourceIP = ipHeader.getSourceIP();
        int destinationIP = ipHeader.getDestinationIP();
        int sourcePort = tcpheader.getSourcePort();
        int destinationPort = tcpheader.getDestinationPort();

        if (tcpheader.isSYN()) {
            // 3-way handshake + create new session
            replySynAck(ipHeader,tcpheader);
        } else if(tcpheader.isACK()) {
            String key = Session.getSessionKey(destinationIP, destinationPort, sourceIP, sourcePort);
            Session session = manager.getSessionByKey(key);

            if (session == null) {
                Log.w(TAG, "Ack for unknown session: " + key);
                if (tcpheader.isFIN()) {
                    sendLastAck(ipHeader, tcpheader);
                } else if (!tcpheader.isRST()) {
                    sendRstPacket(ipHeader, tcpheader, dataLength);
                }
                return;
            }

            synchronized (session) {
                session.setLastIpHeader(ipHeader);
                session.setLastTcpHeader(tcpheader);

                //any data from client?
                if (dataLength > 0) {
                    //accumulate data from client
                    if (session.getRecSequence() == 0 || tcpheader.getSequenceNumber() >= session.getRecSequence()) {
                        int addedLength = manager.addClientData(clientPacketData, session);
                        //send ack to client only if new data was added
                        sendAck(ipHeader, tcpheader, addedLength, session);
                    } else {
                        sendAckForDisorder(ipHeader, tcpheader, dataLength);
                    }
                } else {
                    //an ack from client for previously sent data
                    acceptAck(tcpheader, session);

                    if (session.isClosingConnection()) {
                        sendFinAck(ipHeader, tcpheader, session);
                    } else if (session.isAckedToFin() && !tcpheader.isFIN()) {
                        //the last ACK from client after FIN-ACK flag was sent
                        manager.closeSession(destinationIP, destinationPort, sourceIP, sourcePort);
                        Log.d(TAG, "got last ACK after FIN, session is now closed.");
                    }
                }
                //received the last segment of data from vpn client
                if (tcpheader.isPSH()) {
                    // Tell the NIO thread to immediately send data to the destination
                    pushDataToDestination(session, tcpheader);
                } else if (tcpheader.isFIN()) {
                    //fin from vpn client is the last packet
                    //ack it
                    Log.d(TAG, "FIN from vpn client, will ack it.");
                    ackFinAck(ipHeader, tcpheader, session);
                } else if (tcpheader.isRST()) {
                    resetConnection(ipHeader, tcpheader);
                }

                if (!session.isAbortingConnection()) {
                    manager.keepSessionAlive(session);
                }
            }
        } else if(tcpheader.isFIN()){
            //case client sent FIN without ACK
            Session session = manager.getSession(destinationIP, destinationPort, sourceIP, sourcePort);
            if(session == null)
                ackFinAck(ipHeader, tcpheader, null);
            else
                manager.keepSessionAlive(session);

        } else if(tcpheader.isRST()){
            resetConnection(ipHeader, tcpheader);
        } else {
            Log.d(TAG,"unknown TCP flag");
            String str1 = PacketUtil.getOutput(ipHeader, tcpheader, clientPacketData.array());
            Log.d(TAG,">>>>>>>> Received from client <<<<<<<<<<");
            Log.d(TAG,str1);
            Log.d(TAG,">>>>>>>>>>>>>>>>>>>end receiving from client>>>>>>>>>>>>>>>>>>>>>");
        }
    }

    private void sendRstPacket(IPv4Header ip, TCPHeader tcp, int dataLength){
        byte[] data = TCPPacketFactory.createRstData(ip, tcp, dataLength);

        writer.write(data);
        Log.d(TAG,"Sent RST Packet to client with dest => " +
                PacketUtil.intToIPAddress(ip.getDestinationIP()) + ":" +
                tcp.getDestinationPort());
    }

    private void sendLastAck(IPv4Header ip, TCPHeader tcp){
        byte[] data = TCPPacketFactory.createResponseAckData(ip, tcp, tcp.getSequenceNumber()+1);

        writer.write(data);
        Log.d(TAG,"Sent last ACK Packet to client with dest => " +
                PacketUtil.intToIPAddress(ip.getDestinationIP()) + ":" +
                tcp.getDestinationPort());
    }

    private void ackFinAck(IPv4Header ip, TCPHeader tcp, Session session){
        long ack = tcp.getSequenceNumber() + 1;
        long seq = tcp.getAckNumber();
        byte[] data = TCPPacketFactory.createFinAckData(ip, tcp, ack, seq, true, true);

        writer.write(data);
        if(session != null){
            session.cancelKey();
            manager.closeSession(session);
            Log.d(TAG,"ACK to client's FIN and close session => "+PacketUtil.intToIPAddress(ip.getDestinationIP())+":"+tcp.getDestinationPort()
                    +"-"+PacketUtil.intToIPAddress(ip.getSourceIP())+":"+tcp.getSourcePort());
        }
    }
    private void sendFinAck(IPv4Header ip, TCPHeader tcp, Session session){
        final long ack = tcp.getSequenceNumber();
        final long seq = tcp.getAckNumber();
        final byte[] data = TCPPacketFactory.createFinAckData(ip, tcp, ack, seq,true,false);
        final ByteBuffer stream = ByteBuffer.wrap(data);

        writer.write(data);
        Log.d(TAG,"00000000000 FIN-ACK packet data to vpn client 000000000000");
        IPv4Header vpnip = null;
        try {
            Object obj = IPPacketFactory.createIPHeader(stream);
            vpnip = (IPv4Header) obj;
        } catch (PacketHeaderException e) {
            e.printStackTrace();
        }

        TCPHeader vpntcp = null;
        try {
            if (vpnip != null)
                vpntcp = TCPPacketFactory.createTCPHeader(stream);
        } catch (PacketHeaderException e) {
            e.printStackTrace();
        }

        if(vpnip != null && vpntcp != null){
            String sout = PacketUtil.getOutput(vpnip, vpntcp, data);
            Log.d(TAG,sout);
        }
        Log.d(TAG,"0000000000000 finished sending FIN-ACK packet to vpn client 000000000000");

        session.setSendNext(seq + 1);
        //avoid re-sending it, from here client should take care the rest
        session.setClosingConnection(false);
    }

    private void pushDataToDestination(Session session, TCPHeader tcp){
        session.setDataForSendingReady(true);
        session.setTimestampReplyto(tcp.getTimeStampSender());
        session.setTimestampSender((int)System.currentTimeMillis());

        // Ping the NIO thread to write this, when the session is next writable
        session.subscribeKey(SelectionKey.OP_WRITE);
        nioService.refreshSelect(session);
    }

    /**
     * send acknowledgment packet to VPN client
     * @param ipheader IP Header
     * @param tcpheader TCP Header
     * @param acceptedDataLength Data Length
     * @param session Session
     */
    private void sendAck(IPv4Header ipheader, TCPHeader tcpheader, int acceptedDataLength, Session session){
        long acknumber = session.getRecSequence() + acceptedDataLength;
        session.setRecSequence(acknumber);
        byte[] data = TCPPacketFactory.createResponseAckData(ipheader, tcpheader, acknumber);

        writer.write(data);
    }

    /**
     * resend the last acknowledgment packet to VPN client, e.g. when an unexpected out of order
     * packet arrives.
     * @param session Session
     */
    private void resendAck(Session session){
        byte[] data = TCPPacketFactory.createResponseAckData(
                session.getLastIpHeader(),
                session.getLastTcpHeader(),
                session.getRecSequence()
        );
        writer.write(data);
    }

    private void sendAckForDisorder(IPv4Header ipHeader, TCPHeader tcpheader, int acceptedDataLength) {
        long ackNumber = tcpheader.getSequenceNumber() + acceptedDataLength;
        Log.d(TAG,"sent disorder ack, ack# " + tcpheader.getSequenceNumber() +
                " + " + acceptedDataLength + " = " + ackNumber);
        byte[] data = TCPPacketFactory.createResponseAckData(ipHeader, tcpheader, ackNumber);

        writer.write(data);
    }

    /**
     * acknowledge a packet.
     * @param tcpHeader TCP Header
     * @param session Session
     */
    private void acceptAck(TCPHeader tcpHeader, Session session){
        boolean isCorrupted = PacketUtil.isPacketCorrupted(tcpHeader);

        session.setPacketCorrupted(isCorrupted);
        if (isCorrupted) {
            Log.e(TAG,"prev packet was corrupted, last ack# " + tcpHeader.getAckNumber());
        }

        if (
                tcpHeader.getAckNumber() > session.getSendUnack() ||
                        tcpHeader.getAckNumber() == session.getSendNext()
        ) {
            session.setAcked(true);

            session.setSendUnack(tcpHeader.getAckNumber());
            session.setRecSequence(tcpHeader.getSequenceNumber());
            session.setTimestampReplyto(tcpHeader.getTimeStampSender());
            session.setTimestampSender((int) System.currentTimeMillis());
        } else {
            Log.d(TAG,"Not Accepting ack# "+tcpHeader.getAckNumber() +" , it should be: "+session.getSendNext());
            Log.d(TAG,"Prev sendUnack: "+session.getSendUnack());
            session.setAcked(false);
        }
    }

    /**
     * set connection as aborting so that background worker will close it.
     * @param ip IP
     * @param tcp TCP
     */
    private void resetConnection(IPv4Header ip, TCPHeader tcp){
        Session session = manager.getSession(
                ip.getDestinationIP(), tcp.getDestinationPort(),
                ip.getSourceIP(), tcp.getSourcePort()
        );
        if(session != null){
            synchronized (session) {
                session.setAbortingConnection(true);
            }
        }
    }

    /**
     * create a new client's session and SYN-ACK packet data to respond to client
     * @param ip IP
     * @param tcp TCP
     */
    private void replySynAck(IPv4Header ip, TCPHeader tcp) throws IOException {
        ip.setIdentification(0);
        tech.httptoolkit.android.vpn.Packet packet = TCPPacketFactory.createSynAckPacketData(ip, tcp);

        TCPHeader tcpheader = (TCPHeader) packet.getTransportHeader();

        Session session = manager.createNewTCPSession(
                ip.getDestinationIP(), tcp.getDestinationPort(),
                ip.getSourceIP(), tcp.getSourcePort()
        );

        if (session.getLastIpHeader() != null) {
            // We have an existing session for this connection! We've somehow received a SYN
            // for an existing socket (or some kind of other race). We resend the last ACK
            // for this session, rejecting this SYN. Not clear why this happens, but it can.
            resendAck(session);
            return;
        }

        synchronized (session) {
            session.setMaxSegmentSize(tcpheader.getMaxSegmentSize());
            session.setSendUnack(tcpheader.getSequenceNumber());
            session.setSendNext(tcpheader.getSequenceNumber() + 1);
            //client initial sequence has been incremented by 1 and set to ack
            session.setRecSequence(tcpheader.getAckNumber());

            session.setLastIpHeader(ip);
            session.setLastTcpHeader(tcp);

            nioService.registerSession(session);

            writer.write(packet.getBuffer());
            Log.d(TAG,"Send SYN-ACK to client");
        }
    }

    private void handleICMPPacket(
            ByteBuffer clientPacketData,
            final IPv4Header ipHeader
    ) throws PacketHeaderException {
        final ICMPPacket requestPacket = ICMPPacketFactory.parseICMPPacket(clientPacketData);
        Log.d(TAG, "Got an ICMP ping packet, type " + requestPacket.toString());

        pingThreadpool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isReachable(PacketUtil.intToIPAddress(ipHeader.getDestinationIP()))) {
                        Log.d(TAG, "Failed ping, ignoring");
                        return;
                    }

                    ICMPPacket response = ICMPPacketFactory.buildSuccessPacket(requestPacket);

                    // Flip the address
                    int destination = ipHeader.getDestinationIP();
                    int source = ipHeader.getSourceIP();
                    ipHeader.setSourceIP(destination);
                    ipHeader.setDestinationIP(source);

                    byte[] responseData = ICMPPacketFactory.packetToBuffer(ipHeader, response);

                    Log.d(TAG, "Successful ping response");
                    writer.write(responseData);
                } catch (PacketHeaderException e) {
                    Log.w(TAG, "Handling ICMP failed with " + e.getMessage());
                    return;
                }
            }

            private boolean isReachable(String ipAddress) {
                try {
                    return getByName(ipAddress).isReachable(10000);
                } catch (IOException e) {
                    return false;
                }
            }
        });
    }
}