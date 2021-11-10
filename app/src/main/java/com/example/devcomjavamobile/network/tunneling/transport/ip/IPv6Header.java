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

package com.example.devcomjavamobile.network.tunneling.transport.ip;

import static com.example.devcomjavamobile.network.tunneling.util.PacketUtil.*;

/**
 * Original file:
 * Data structure for IPv4 header as defined in RFC 791.
 * @author Borey Sao
 * Date: May 8, 2014
 */

/*
* Modified to be IPv6 header.
* Data structure for IPv6 header as defined in RFC 2460
* Author: Simen Persch Andersen
* Date: 15.04.2021
 */
public class IPv6Header extends IPHeader {
    //IP packet is the four-bit version field. For IPv4, this has a value of 4 (hence the name IPv4).
    private byte ipVersion;

    //The traffic class of the header
    private byte trafficClass;

    // Flow label
    private short flowLabel;

    // length in bytes(?) of the payload that follows
    private short payloadLen = 0;


    // Point to the first extension header, which are hop-by-hop, routing, fragment, destination options, authenatican or encapsulating security payload headers
    // More info @
    private byte nextHdr;

    // Hop limit, same as TTL
    private byte hopLimit = 0;

    // Source IPv6 address
    private byte[] sourceIP = new byte[16];

    // Destination IPv6 address
    private byte[] destinationIP = new byte[16];

    /**
     * create a new IPv4 Header
     * @param ipVersion the first header field in an IP packet. It is four-bit. For IPv4, this has a value of 4.
     * @param sourceIP IPv4 address of sender.
     * @param destinationIP IPv4 address of receiver.
     */
    public IPv6Header(byte ipVersion, byte trafficClass,
                      short flowLabel, short payloadLen, byte nextHdr,
                      byte hopLimit, byte[] sourceIP, byte[] destinationIP){
        super(ipVersion);
        this.trafficClass = trafficClass;
        this.flowLabel = flowLabel;
        this.payloadLen = payloadLen;
        this.hopLimit = hopLimit;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
    }

    public byte getIpVersion() {
        return ipVersion;
    }

    public byte getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(byte trafficClass) {
        this.trafficClass = trafficClass;
    }

    public short getFlowLabel() {
        return flowLabel;
    }

    public short getPayloadLen() {
        return payloadLen;
    }

    public void setPayloadLen(short payloadLen) {
        this.payloadLen = payloadLen;
    }

    public byte getNextHdr() { return nextHdr; }

    public byte getHopLimit() {
        return hopLimit;
    }


    public byte[] getSourceIP() {
        return sourceIP;
    }

    public byte[] getDestinationIP() {
        return destinationIP;
    }

    public String getSourceIPString() {
        return byteArrayToIPv6Address(sourceIP);
    }

    public String getDestinationIPString() {
        return byteArrayToIPv6Address(destinationIP);
    }


    public void setSourceIP(byte[] sourceIP) {
        this.sourceIP = sourceIP;
    }

    public void setDestinationIP(byte[] destinationIP) {
        this.destinationIP = destinationIP;
    }
}