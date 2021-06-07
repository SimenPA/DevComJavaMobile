package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.security.RSAUtil;
import com.example.devcomjavamobile.network.vpn.Session;
import com.example.devcomjavamobile.network.vpn.socket.DataConst;
import com.example.devcomjavamobile.network.vpn.transport.PacketHeaderException;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPPacketFactory;
import com.example.devcomjavamobile.network.vpn.transport.ip.IPv4Header;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPHeader;
import com.example.devcomjavamobile.network.vpn.transport.tcp.TCPPacketFactory;
import com.example.devcomjavamobile.network.vpn.util.PacketUtil;

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

    LinkedList<Peer> peers;

    private ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);

    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    public TCPServer(LinkedList<Peer> peers) {
        this.peers = peers;
    }

    @Override
    public void run() {

        String message = "";
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
                    ControlTraffic ct = new ControlTraffic(peers, channel.getRemoteAddress().toString(), channel);
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
