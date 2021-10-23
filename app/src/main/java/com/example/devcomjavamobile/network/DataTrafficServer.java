package com.example.devcomjavamobile.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.devcom.Peer;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.vpn.ClientPacketWriter;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.transport.udp.UDPHeader;
import com.example.devcomjavamobile.network.vpn.transport.udp.UDPPacketFactory;
import com.example.devcomjavamobile.network.vpn.util.PacketUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.widget.Toast.makeText;

public class DataTrafficServer implements Runnable {

    public final String TAG = DataTrafficServer.class.getSimpleName();

    private final String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";

    private final int AES_BLOCK_SIZE = 256;

    private final BlockingDeque<DatagramPacket> packetQueue = new LinkedBlockingDeque<>();

    private final static int PORT_DATA = 1337;

    public LinkedList<Peer> peers;
    public String physicalAddress = "";
    private DatagramSocket datagramSocket;
    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    ClientPacketWriter tunnelWriter;

    public DataTrafficServer(ClientPacketWriter tunnelWriter)
    {
        this.tunnelWriter = tunnelWriter;
        this.peers = MainActivity.getPeers();
    }

    public void start() {
        Log.d(TAG, "Data traffic server is being started");
        worker = new Thread(this);
        worker.start();
        running.set(true);
        stopped.set(false);
    }

    public void interrupt() throws IOException {
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            if(datagramSocket != null) datagramSocket.close();
            Log.v(TAG, "UDP Data Socket has been closed for physical address: " + physicalAddress);
            worker.interrupt();
        }
        else {
            Log.v(TAG, "There is no UDP Data Socket open for physical address: " + physicalAddress);
        }
    }

    @Override
    public void run()
    {

        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        if(datagramSocket == null)
        {
            try {
                datagramSocket = new DatagramSocket(PORT_DATA);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        if(datagramSocket != null)
        {
            while (this.isRunning()) {
                Log.d(TAG, "Datagram socket opened");
                try {
                    do {
                        datagramSocket.receive(packet);
                        handleUDPPacket(packet);
                    } while (this.isRunning());
                }catch(NotYetConnectedException e){
                    Log.e(TAG,"Socket not connected");
                }catch(ClosedByInterruptException e){
                    Log.e(TAG,"ClosedByInterruptException reading SocketChannel: "+ e.getMessage());
                }catch(ClosedChannelException e){
                    Log.e(TAG,"ClosedChannelException reading SocketChannel: "+ e.getMessage());
                    try {
                        this.interrupt();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (IOException e) {
                    Log.e(TAG,"Error reading data from SocketChannel: "+ e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
        else
        {
            try {
                interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Write from queue. Done due to operating in different thread

    }

    public void handleUDPPacket(@NonNull DatagramPacket packet) throws Exception {

        InetAddress sourceAddress = packet.getAddress();
        InetAddress destinationAddress = InetAddress.getByName(PacketUtil.getLocalIpAddress());
        packet.getOffset();
        Log.i(TAG, "Address in incoming UDP packet: " + sourceAddress.toString().replace("/", ""));
        byte[] buffer = packet.getData();
        int packetLength = packet.getLength();
        byte[] encryptedData = new byte[packetLength];
        System.arraycopy(buffer, 0, encryptedData, 0, packetLength);

        Peer peer = null;

        for(Peer p: peers) {
            for(String ip : p.getPhysicalAddresses()) if(ip.equals(sourceAddress.toString().replace("/", ""))) peer = p;
        }

        if(peer == null)
        {
            Log.d(TAG, "Received UDP message from unknown peer. Dismissing");
            return;
        }
        if(peer.getControlTraffic() != null )
        {
            Log.d(TAG, "Received UDP message from peer " + peer.getFingerPrint());
            Log.d(TAG, "Encrypted data length: " + encryptedData.length);
            Log.d(TAG, "Encrypted data: " + Arrays.toString(encryptedData));
            byte[] decryptedData = Crypto.aes_decrypt(encryptedData, peer.getDecryptCipher());
            Log.i(TAG, "Unencrypted data length: " + decryptedData.length);
            Log.i(TAG, "Unencrypted data: " + Arrays.toString(decryptedData));

            int totalLength = 20 + decryptedData.length;
            IPv4Header iPv4Header = new IPv4Header((byte)0x04, (byte)0x05, (byte)0x00, (byte)0x00, totalLength, 0, false, false, (short)0x00, (byte)0x01, (byte)0x11, (byte)0x00, sourceAddress.hashCode(), destinationAddress.hashCode());
            Log.i(TAG, "Source address in int: " + iPv4Header.getSourceIP());
            byte[] srcBytes = BigInteger.valueOf(iPv4Header.getSourceIP()).toByteArray();
            InetAddress sourceAddr = InetAddress.getByAddress(srcBytes);
            Log.i(TAG, "Source address: " + sourceAddr.getHostAddress());
            Log.i(TAG, "Destination address int: " + iPv4Header.getDestinationIP());
            byte[] destBytes = BigInteger.valueOf(iPv4Header.getDestinationIP()).toByteArray();
            InetAddress destAddr = InetAddress.getByAddress(destBytes);
            UDPHeader udpHeader = UDPPacketFactory.createUDPHeader(decryptedData);

            Log.i(TAG, "Source Port: " + udpHeader.getSourcePort());
            Log.i(TAG, "Destination Port: " + udpHeader.getDestinationPort());
            Log.i(TAG, "Length: " + udpHeader.getLength());
            Log.i(TAG, "Checksum: " + udpHeader.getChecksum());

            byte[] packetData = new byte[decryptedData.length - 8];
            System.arraycopy(decryptedData, 8, packetData, 0, packetData.length);

            byte[] tunnelPacket = UDPPacketFactory.createResponsePacket(iPv4Header, udpHeader, packetData);

            Log.i(TAG, "Writing to tunnel");
            // tunnelPacket.flip()
            tunnelWriter.write(tunnelPacket);
            Log.i(TAG, "Written to tunnel");

        } else {
            Log.i(TAG, "No active session with sending peer. Dismissing packet");
        }

    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
