package com.example.devcomjavamobile.network;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataTrafficSender implements Runnable {
    DatagramPacket packet;

    private final String TAG = DataTrafficSender.class.getSimpleName();

    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public DataTrafficSender(DatagramPacket packet)
    {
        this.packet =  packet;
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
            worker.interrupt();
        }
    }

    public void run() {

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            interrupt();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
