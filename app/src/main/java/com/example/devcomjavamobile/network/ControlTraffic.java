package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.network.security.RSAUtil;
import com.example.devcomjavamobile.network.vpn.socket.DataConst;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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

    private final BlockingDeque<byte[]> packetQueue = new LinkedBlockingDeque<>();

    private final static int PORT_CONTROL = 3283;

    public LinkedList<Peer> peers;
    public String physicalAddress = "";
    private Socket sock;
    private SocketChannel socketChannel;
    private ServerSocket serverSock;
    private DataOutputStream dos;
    private DataInputStream dis;
    private PrintWriter pw;
    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public ControlTraffic(LinkedList<Peer> peers, String physicalAddress, SocketChannel channel)
    {
        this.socketChannel =  channel;
        this.peers = peers;
        this.physicalAddress =  physicalAddress;
    }

    public void start() {
        Log.d(TAG, "Control Traffic is being started");
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
            if(sock != null) sock.close();
            Log.v(TAG, "TCP Control Socket has been opened for physical address: " + physicalAddress);
            worker.interrupt();
        }
        else {
            Log.v(TAG, "TCP Control Socket has been opened for physical address: " + physicalAddress);
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
            while (this.isRunning()) {
                Log.d(TAG, "Got incoming connection");
                ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
                int len;

                try {
                    do {
                        len = socketChannel.read(buffer);
                        Log.d(TAG, "Read this many bytes: " + len);
                        if (len > 0) {//-1 mean it reach the end of stream
                            buffer.flip();
                            handleTCPPacket(buffer, socketChannel);
                            Log.d(TAG, "Handed it over to handleTCPPacket");
                            buffer.clear();
                        } else if (len == -1) {
                            try {
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
                            catch (InterruptedException e) {
                            }
                        }
                    } while (len > 0);
                }catch(NotYetConnectedException e){
                    Log.e(TAG,"socket not connected");
                }catch(ClosedByInterruptException e){
                    Log.e(TAG,"ClosedByInterruptException reading SocketChannel: "+ e.getMessage());
                }catch(ClosedChannelException e){
                    Log.e(TAG,"ClosedChannelException reading SocketChannel: "+ e.getMessage());
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

        PeersHandler pHandler = new PeersHandler(peers);

        byte[] buf = new byte[packetData.remaining()];

        byte packetType = buf[0];

        byte[] regardingComByte = new byte[6]; // which community this packet belongs to
        byte[] regardingFingerprintByte = new byte[16]; // which device this packet belongs to

        System.arraycopy(buf, 1, regardingComByte, 0, 6);
        System.arraycopy(buf, 7, regardingComByte, 0, 16);

        StringBuilder comStrb = new StringBuilder();
        StringBuilder fingStrb = new StringBuilder();
        char[] regardingCommunity = new char[6];
        char[] regardingFingerprint = new char[16];
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
        boolean verified = false;
        try {
            verified = RSAUtil.verify(buf, pHandler.getPeer(fingStrb.toString()).getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(verified){
            pHandler.getPeer(fingStrb.toString()).setControlTraffic(this);
        } else {
            Log.i(TAG, "Couldn't verify peer signature, closing connnection");
            channel.close();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
