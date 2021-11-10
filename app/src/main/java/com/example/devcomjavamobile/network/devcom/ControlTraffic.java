package com.example.devcomjavamobile.network.devcom;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.tunneling.ClientPacketWriter;
import com.example.devcomjavamobile.network.tunneling.socket.DataConst;
import com.example.devcomjavamobile.network.tunneling.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.tunneling.transport.udp.UDPHeader;
import com.example.devcomjavamobile.network.tunneling.transport.udp.UDPPacketFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.widget.Toast.makeText;

public class ControlTraffic implements Runnable {

    public final String TAG = ControlTraffic.class.getSimpleName();

    private final String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";

    private final BlockingDeque<byte[]> packetQueue = new LinkedBlockingDeque<>();

    private final static int PORT_CONTROL = 3283;

    private String physicalAddress = "";
    private Socket sock;
    private SocketChannel socketChannel;
    private ServerSocket serverSock;
    private DataOutputStream dos;
    private DataInputStream dis;
    private PrintWriter pw;
    private Thread worker;
    private final Activity activity;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(true);

    public ControlTraffic(String physicalAddress, SocketChannel channel, Activity activity)
    {
        this.activity = activity;
        this.socketChannel = channel;
        this.physicalAddress = physicalAddress;
    }

    public void start() {
        Log.d(TAG, "Control Traffic is being started");
        worker = new Thread(this);
        worker.start();
        running.set(true);
        stopped.set(false);
    }

    public String getPhysicalAddress() { return physicalAddress; }

    public void interrupt() throws IOException {
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            if(sock != null) sock.close();
            Log.v(TAG, "TCP Control Socket has been closed for physical address: " + physicalAddress);
            worker.interrupt();
            PeersHandler.clearStoppedControlTraffic();
        }
        else {
            Log.v(TAG, "There is no TCP Control Socket open for physical address: " + physicalAddress);
        }
    }

    @Override
    public void run()
    {
        InetSocketAddress address = null;

        address = new InetSocketAddress(physicalAddress, PORT_CONTROL);
        if(socketChannel == null)
        {
            try {
                socketChannel = SocketChannel.open(address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socketChannel != null)
        {
            try
            {
                socketChannel.configureBlocking(false);
            } catch(IOException e)
            {
                e.printStackTrace();
            }
            while (this.isRunning()) {
                Log.d(TAG, "Socket channel opened");
                ByteBuffer buffer = ByteBuffer.allocate(2071);
                int len;

                try {
                    do {
                        len = socketChannel.read(buffer);
                        if (len > 0) {
                            if(len < 2071) {// sometimes doesn't read all bytes from stream, typically 1448 bytes. Read the rest of the bytes if there are any.
                                while(len < 2071)
                                    len = len + socketChannel.read(buffer);
                            }
                            Log.d(TAG, "Read this many bytes: " + len);
                            buffer.flip();
                            handleTCPPacket(buffer, socketChannel);
                            Log.d(TAG, "Handed it over to handleTCPPacket");
                            buffer.clear();
                        } else if (len == 0) {
                            try {
                                if(!packetQueue.isEmpty())
                                {
                                byte[] data = this.packetQueue.take();
                                    try {
                                        this.socketChannel.write(ByteBuffer.wrap(data));
                                        Log.d(TAG, "Sent " + data.length + " bytes");
                                    } catch (IOException e) {
                                        Log.e(TAG, "Error writing " + data.length + " bytes over control channel");
                                        e.printStackTrace();

                                        this.packetQueue.addFirst(data); // Put the data back, so it's recent
                                        Thread.sleep(10); // Add an arbitrary tiny pause, in case that helps
                                    }
                                }

                            }
                            catch (InterruptedException e) {
                            }
                        }
                        else if (len < 0) { // length -1 means end of stream has been reached, i.e. connection closed on other end
                            try {
                                this.interrupt();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
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

    public void write(byte[] data) {
        Log.d(TAG, "Writing control package");
        Log.d(TAG, "Package type : " + (char) data[0]);
        if (data.length > 30000) throw new Error("Packet too large");
        packetQueue.addLast(data);
        Log.d(TAG, "Written control package");
    }

    public void handleTCPPacket(@NonNull ByteBuffer packetData, SocketChannel channel) throws IOException {


        byte[] buf = new byte[packetData.remaining()];
        packetData.get(buf);

        char packetType = (char)buf[0];

        byte[] regardingComByte = new byte[6]; // which community this packet belongs to
        byte[] regardingFingerprintByte = new byte[16]; // which device this packet belongs to

        System.arraycopy(buf, 1, regardingComByte, 0, 6);
        System.arraycopy(buf, 7, regardingFingerprintByte, 0, 16);

        StringBuilder comStrb = new StringBuilder();
        StringBuilder fingStrb = new StringBuilder();
        for(int i = 0; i < 6 ; i++)
        {
            if(regardingComByte[i] != 0x00)
            {
                comStrb.append((char)regardingComByte[i]);
            }
        }
        for(int i = 0; i < 16 ; i++)
        {
            fingStrb.append((char)regardingFingerprintByte[i]);
        }
        Log.d(TAG, "Community: " + comStrb.toString());
        Log.d(TAG, "Fingerprint: " + fingStrb.toString());
        Peer p = PeersHandler.getPeer(fingStrb.toString());
        if(p != null) {
            boolean verified = false;
            try {
                verified = Crypto.verifyControlPacket(buf, p.getPublicKey());
                if (verified) {

                    if(packetType == 'J') { // J = join
                        handleControlJoin(buf, channel, p, comStrb.toString());
                    } else if(packetType == 'P') { // P = packet, i.e. data packet that is normally sent over UDP with TCP as fallback
                        handleControlPacket(buf, channel, p);
                    }

                } else {
                    Log.i(TAG, "Couldn't verify peer signature, closing connnection");
                    this.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Device with fingerprint " + fingStrb.toString() + " is unknown, closing connection");
            this.interrupt();
        }
    }

    public void handleControlJoin(byte[] data, SocketChannel channel, Peer p, String community) throws Exception
    {


        byte[] encryptedPayload = new byte[1536];
        System.arraycopy(data, 23, encryptedPayload,0,1536);

        boolean successfulDecryption = Crypto.decryptControlPacket(data, encryptedPayload, Crypto.readPrivateKey(PRIVATE_KEY_PATH));
        if(successfulDecryption)
        {
            byte[] passwordBytes = new byte[32];
            System.arraycopy(data,23, passwordBytes, 0, 32);

            char[] password = new char[32];
            for(int i = 0; i < passwordBytes.length; i++) {
                password[i] = (char)passwordBytes[i];
            }
            p.setPassword(password);
            Log.i(TAG, "Password for session has been set. Password: " + String.valueOf(p.getPassword()));
            Log.i(TAG, "Join message from device " + p.getFingerPrint() + " verified. Session has been opened.");
            Crypto.aesInit(String.valueOf(p.getPassword()), p);

            p.addCommunity(community);

            p.setControlTraffic(this);
            InetSocketAddress remoteIpAddresss =  (InetSocketAddress)channel.getRemoteAddress();
            p.addPhysicalAddress(remoteIpAddresss.getAddress().getHostAddress());

            activity.runOnUiThread(() -> Toast.makeText(activity, "Incoming control channel from device " + p.getFingerPrint() + " successfully established", Toast.LENGTH_SHORT).show());

            // Check if sender supports receiving UDP.
            // Haven't been able to test if this works as I only have one device where I'm able to open up UDP, i.e. one physical android device.
            // The other test device is virtual android emulator that sits behind a NAT running by the computer that I'm not able to configure. (or at least haven't prioritized figuring out how to)

            // UDPCheckSender udpCheckSender = new UDPCheckSender(p.getPhysicalAddresses().getFirst(), "SYN");
            // udpCheckSender.run();
        } else {
            Log.i(TAG,"Unable to decrypt control message and get session key. Aborting session");
            interrupt();
        }
    }

    // TODO: Handle TCP packets in control packets
    public void handleControlPacket(byte[] data, SocketChannel channel, Peer p) throws Exception {

        InetSocketAddress destSockAddr = (InetSocketAddress)channel.getLocalAddress();
        InetAddress destInetAddress = destSockAddr.getAddress();

        InetSocketAddress srcSockAddr = (InetSocketAddress)channel.getRemoteAddress();
        InetAddress srcInetAddress = srcSockAddr.getAddress();


        // Extract encrypted data length
        byte [] encDataLength = new byte[4];
        System.arraycopy(data, 23, encDataLength,0,4);
        ByteBuffer bb = ByteBuffer.wrap(encDataLength);
        int encDataLengthInt = bb.getInt();

        // Encrypted data starts at position 27. The first 23 bytes are DevCom headers and the next 4 is encrypted data length in int
        byte[] encryptedData = new byte[encDataLengthInt];
        System.arraycopy(data, 27, encryptedData,0,encryptedData.length);

        byte[] decryptedData = Crypto.aes_decrypt(encryptedData, p.getDecryptCipher());

        Log.i(TAG, "Unencrypted data length: " + decryptedData.length);
        Log.i(TAG, "Unencrypted data: " + Arrays.toString(decryptedData));

        int totalLength = 20 + decryptedData.length;
        IPv4Header iPv4Header = new IPv4Header((byte)0x04, (byte)0x05, (byte)0x00, (byte)0x00, totalLength, 0, false, false, (short)0x00, (byte)0x01, (byte)0x11, (byte)0x00, srcInetAddress.hashCode(), destInetAddress.hashCode());
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
        ClientPacketWriter tunnelWriter = TunnelRunnable.getTunnelWriter();
        tunnelWriter.write(tunnelPacket);
        Log.i(TAG, "Written to tunnel");

    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
