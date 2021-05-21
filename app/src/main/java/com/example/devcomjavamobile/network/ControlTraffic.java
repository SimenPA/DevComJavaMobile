package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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

    public ControlTraffic(LinkedList<Peer> peers, String physicalAddress)
    {
        this.peers = peers;
        this.physicalAddress =  physicalAddress;
    }

    public void start() throws IOException {
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

        try {
            socketChannel = SocketChannel.open(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(socketChannel != null)
        {
            while (this.isRunning()) {
                try {
                    byte[] data = this.packetQueue.take();
                    try {
                        this.socketChannel.write(ByteBuffer.wrap(data));
                        Log.d(TAG, "Sent " + data.length + " bytes");
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing " + data.length + " bytes to the VPN");
                        e.printStackTrace();

                        this.packetQueue.addFirst(data); // Put the data back, so it's recent
                        Thread.sleep(10); // Add an arbitrary tiny pause, in case that helps
                    }
                } catch (InterruptedException e) {
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

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
