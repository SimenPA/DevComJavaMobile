package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

import com.example.devcomjavamobile.MainActivity;

public class TCPServer implements Runnable {

    private final static String TAG = TCPServer.class.getSimpleName();

    private final static int PORT_CONTROL = 7800;

    ServerSocket ss;
    Socket mySocket;
    String msg;
    InputStreamReader isr;
    BufferedReader br;

    Activity activity;

    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public TCPServer(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {

        String message = "";
        running.set(true);
        stopped.set(false);
        try
        {
            ss =  new ServerSocket(PORT_CONTROL);
            ss.setReuseAddress(true);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Server has started", Toast.LENGTH_SHORT).show();
                }
            });
            while (true)
            {
                mySocket = ss.accept();
                Log.d(TAG, "Got an incoming connection");

                mySocket = ss.accept();
                isr =  new InputStreamReader(mySocket.getInputStream());
                br = new BufferedReader(isr);

                message = br.readLine();

                String finalMessage = message;
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

    public void start() {
        Log.d(TAG, "TCP Server has started");
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() throws IOException {
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            ss.close();
            Toast.makeText(activity, "TCP Server has stopped", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "TCP Server has stopped");
            worker.interrupt();
        }
        else {
            Toast.makeText(activity, "TCP Server is not running", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }

}
