package com.example.devcomjavamobile.network.devcom;

import android.app.Activity;
import android.util.Log;

import com.example.devcomjavamobile.network.tunneling.ClientPacketWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.widget.Toast.makeText;

public class TCPServer implements Runnable {

    private final static int MAX_PACKET_LEN = 1500;

    private final static String TAG = TCPServer.class.getSimpleName();

    private final static int PORT_CONTROL = 3283;

    ServerSocket ss;
    ServerSocketChannel serverSocketChannel;
    InetSocketAddress sockAddress;
    Socket mySocket;
    SocketChannel channel;
    String msg;
    InputStreamReader isr;
    BufferedReader br;


    private Activity mainActivity;

    private ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);

    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);
    ClientPacketWriter tunnelWriter;

    public TCPServer(Activity mainActivity) {
        this.mainActivity = mainActivity;
        this.tunnelWriter = tunnelWriter;
    }

    @Override
    public void run() {

        running.set(true);
        stopped.set(false);
        try
        {
            sockAddress = new InetSocketAddress(PORT_CONTROL);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(sockAddress);
            while (isRunning())
            {
                channel = serverSocketChannel.accept();
                if(channel != null)
                {
                    ControlTraffic ct = new ControlTraffic(channel.getRemoteAddress().toString(), channel, mainActivity);
                    ct.start();
                }
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
            serverSocketChannel.socket().close();
            Log.d(TAG, "TCP Server has stopped");
            worker.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }


}
