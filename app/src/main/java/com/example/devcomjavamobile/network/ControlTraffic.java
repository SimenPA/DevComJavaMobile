package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControlTraffic implements Runnable {

    public final String TAG = ControlTraffic.class.getSimpleName();

    private final static int PORT_CONTROL = 7800;

    public LinkedList<Peer> peers;
    public String physicalAddress = "";
    private Socket sock;
    private DataOutputStream dos;
    PrintWriter pw;

    private Thread worker;


    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public ControlTraffic(LinkedList<Peer> peers, String physicalAddress)
    {
        this.peers = peers;
        this.physicalAddress =  physicalAddress;
    }

    public void start() {
        Log.d(TAG, "Control Traffic is being started");
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() throws IOException {
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            sock.close();
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

        String messsage = "Hei din kuk";
        InetAddress address = null;

        try {
            address = InetAddress.getByName(physicalAddress);
            Log.d(TAG, "Creating socket for address: " + physicalAddress);
            sock = new Socket(address, PORT_CONTROL);
            pw =  new PrintWriter(sock.getOutputStream());
            pw.write(messsage);
            pw.flush();
            pw.close();
            sock.close();
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
