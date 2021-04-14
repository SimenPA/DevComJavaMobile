package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.MainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPServer implements Runnable {

    DatagramSocket ds;
    DatagramPacket packet;

    private Thread worker;

    Activity activity;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public UDPServer(Activity activity) {
        this.activity = activity;
    }


    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() {
        running.set(false);
        stopped.set(true);
        ds.close();
        Toast.makeText(activity, "UDP Server has stopped", Toast.LENGTH_SHORT).show();
        worker.interrupt();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }


    @Override
    public void run() {

        running.set(true);
        stopped.set(false);

        try {
            String message;
            byte[] lmessage = new byte[100];
            ds =  new DatagramSocket(9700);
            packet = new DatagramPacket(lmessage, lmessage.length);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Server has started", Toast.LENGTH_SHORT).show();
                }
            });
            while (isRunning()) {
                try{
                    ds.receive(packet);
                } catch (SocketException e)
                {
                    if(e.getMessage().equals("Socket closed")) Log.i("UDPServer", "Ignoring socket closed exception");
                }
                Log.i("UDPServer", "Packet received. Reading");
                message = new String(lmessage, 0, packet.getLength());
                String finalMessage = message;
                Log.i("UDPServer", "Messager received: " + finalMessage);
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, "Message received from client: " + finalMessage, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }
}
