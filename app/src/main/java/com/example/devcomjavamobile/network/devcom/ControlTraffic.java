package com.example.devcomjavamobile.network.devcom;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.security.RSAUtil;
import com.example.devcomjavamobile.network.vpn.socket.DataConst;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.widget.Toast.makeText;

public class ControlTraffic implements Runnable {

    public final String TAG = ControlTraffic.class.getSimpleName();

    private final String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";

    private final BlockingDeque<byte[]> packetQueue = new LinkedBlockingDeque<>();

    private final static int PORT_CONTROL = 3283;

    private final LinkedList<Peer> peers;
    private String physicalAddress = "";
    private Socket sock;
    private SocketChannel socketChannel;
    private ServerSocket serverSock;
    private DataOutputStream dos;
    private DataInputStream dis;
    private PrintWriter pw;
    private Thread worker;
    private Activity activity;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(true);

    public ControlTraffic(String physicalAddress, SocketChannel channel, Activity activity)
    {
        this.activity = activity;
        this.socketChannel =  channel;
        this.peers = MainActivity.getPeers();
        this.physicalAddress =  physicalAddress;
    }

    public void start() {
        Log.d(TAG, "Control Traffic is being started");
        worker = new Thread(this);
        worker.start();
        running.set(true);
        stopped.set(false);
    }

    public String getPhysicalAddress() { return physicalAddress; }

    public SocketChannel getSocketChannel() { return socketChannel; }

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
                activity.runOnUiThread(() -> Toast.makeText(activity, "Socket channel opened", Toast.LENGTH_SHORT).show());
                ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
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

            // Check if sender supports receiving UDP
            UDPCheckSender udpCheckSender = new UDPCheckSender(p.getPhysicalAddresses().getFirst(), "SYN");
            udpCheckSender.run();
        } else {
            Log.i(TAG,"Unable to decrypt control message and get session key. Aborting session");
            interrupt();
        }
    }


    public void handleControlPacket(byte[] data, SocketChannel channel, Peer p) throws Exception {

        // Handling this later
        /*
        Crypto c = new Crypto();

        byte[] encryptedPayload = new byte[1536];
        System.arraycopy(data, 23, encryptedPayload,0,1536);

        byte[] payLoad = RSAUtil.decrypt(encryptedPayload, c.readPrivateKey(PRIVATE_KEY_PATH));

        byte[] passwordBytes = new byte[32];
        System.arraycopy((payLoad,0, passwordBytes, 0, 32);

        char[] password = new char[32];
        for(int i = 0; i < passwordBytes.length; i++) {
            password[i] = (char)passwordBytes[i];
        }
        p.setPassword(password);
        Log.i(TAG, "Join message from device " + p.getFingerPrint() + " verified. Session has been opened.");

         */

    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
