package com.example.devcomjavamobile.network.vpn.transport.udp;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.network.vpn.transport.udp.UDPHeader;

import com.example.devcomjavamobile.network.vpn.transport.ip.IPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.transport.PacketHeaderException;
import com.example.devcomjavamobile.network.vpn.util.PacketUtil;

import java.nio.ByteBuffer;

/*
 * Imported by Simen Persch Andersen on 29.03.2021 from
 * https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/vpn/transport/udp/UDPPacketFactory.java
 */

public class UDPPacketFactory {

    public static UDPHeader createUDPHeader(@NonNull ByteBuffer stream) throws PacketHeaderException{
        if ((stream.remaining()) < 8){
            throw new PacketHeaderException("Minimum UDP header is 8 bytes.");
        }
        final int srcPort = stream.getShort() & 0xffff;
        final int destPort = stream.getShort() & 0xffff;
        final int length = stream.getShort() & 0xffff;
        final int checksum = stream.getShort();

        return new UDPHeader(srcPort, destPort, length, checksum);
    }

    public static UDPHeader createUDPHeader(@NonNull byte[] udpData) throws PacketHeaderException{
        if ((udpData.length) < 8){
            throw new PacketHeaderException("Minimum UDP header is 8 bytes.");
        }
        int srcPort = ((udpData[0] & 0xff) << 8) | (udpData[1] & 0xff);
        int destPort = ((udpData[2] & 0xff) << 8) | (udpData[3] & 0xff);
        int length = ((udpData[4] & 0xff) << 8) | (udpData[5] & 0xff);
        int checkSum = ((udpData[6] & 0xff) << 8) | (udpData[7] & 0xff);

        return new UDPHeader(srcPort, destPort, length, checkSum);
    }

    public static UDPHeader copyHeader(UDPHeader header){
        return new UDPHeader(header.getSourcePort(), header.getDestinationPort(),
                header.getLength(), header.getChecksum());
    }
    /**
     * create packet data for responding to vpn client
     * @param ip IPv4Header sent from VPN client, will be used as the template for response
     * @param udp UDPHeader sent from VPN client
     * @param packetData packet data to be sent to client
     * @return array of byte
     */
    public static byte[] createResponsePacket(IPv4Header ip, UDPHeader udp, byte[] packetData){
        byte[] buffer;
        int udpLen = 8;
        if(packetData != null){
            udpLen += packetData.length;
        }
        int srcPort = udp.getSourcePort();
        int destPort = udp.getDestinationPort();
        short checksum = 0;

        IPv4Header ipHeader = IPPacketFactory.copyIPv4Header(ip);

        int srcIp = ip.getSourceIP();
        int destIp = ip.getDestinationIP();
        ipHeader.setMayFragment(false);
        ipHeader.setSourceIP(srcIp);
        ipHeader.setDestinationIP(destIp);
        ipHeader.setIdentification(PacketUtil.getPacketId());

        //ip's length is the length of the entire packet => IP header length + UDP header length (8) + UDP body length
        int totalLength = ipHeader.getIPHeaderLength() + udpLen;

        ipHeader.setTotalLength(totalLength);
        buffer = new byte[totalLength];
        byte[] ipData = IPPacketFactory.createIPv4HeaderData(ipHeader);

        // clear IP checksum
        ipData[10] = ipData[11] = 0;

        //calculate checksum for IP header
        byte[] ipChecksum = PacketUtil.calculateChecksum(ipData, 0, ipData.length);
        //write result of checksum back to buffer
        System.arraycopy(ipChecksum, 0, ipData, 10, 2);
        System.arraycopy(ipData, 0, buffer, 0, ipData.length);

        //copy UDP header to buffer
        int start = ipData.length;
        byte[] intContainer = new byte[4];
        PacketUtil.writeIntToBytes(srcPort, intContainer, 0);
        //extract the last two bytes of int value
        System.arraycopy(intContainer,2,buffer,start,2);
        start += 2;

        PacketUtil.writeIntToBytes(destPort, intContainer, 0);
        System.arraycopy(intContainer, 2, buffer, start, 2);
        start += 2;

        PacketUtil.writeIntToBytes(udpLen, intContainer, 0);
        System.arraycopy(intContainer, 2, buffer, start, 2);
        start += 2;

        PacketUtil.writeIntToBytes(checksum, intContainer, 0);
        System.arraycopy(intContainer, 2, buffer, start, 2);
        start += 2;

        //now copy udp data
        if (packetData != null)
            System.arraycopy(packetData, 0, buffer, start, packetData.length);

        return buffer;
    }

}//end