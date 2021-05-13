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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.MainActivity;
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

    private final static int PORT_CONTROL = 7800;

    ServerSocket ss;
    ServerSocketChannel serverSocketChannel;
    InetSocketAddress sockAddress;
    Socket mySocket;
    SocketChannel channel;
    String msg;
    InputStreamReader isr;
    BufferedReader br;

    private ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);

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
            sockAddress = new InetSocketAddress(PORT_CONTROL);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(sockAddress);
            activity.runOnUiThread(() -> makeText(activity, "TCP Server has started", Toast.LENGTH_SHORT).show());
            while (isRunning())
            {
                channel = serverSocketChannel.accept();
                if(channel != null)
                {
                    Log.d(TAG, "Got incoming connection");
                    ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
                    int len;

                    try {
                        do {
                            len = channel.read(buffer);
                            Log.d(TAG, "Read this many bytes: " + len);
                            if (len > 0) {//-1 mean it reach the end of stream
                                buffer.flip();
                                handleTCPPacket(buffer);
                                Log.d(TAG, "Handed it over to handleTCPPacket");
                                buffer.clear();
                            } else if (len == -1) {
                                /*
                                Log.d(TAG,"End of data from remote server, will send FIN to client");
                                Log.d(TAG,"End of data from remote server, will send FIN to client");
                                Log.d(TAG,"send FIN to: " + session);
                                sendFin(session);
                                session.setAbortingConnection(true);
                                 */
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
            makeText(activity, "TCP Server has stopped", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "TCP Server has stopped");
            worker.interrupt();
        }
        else {
            makeText(activity, "TCP Server is not running", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public void handleTCPPacket(@NonNull ByteBuffer packetData)  {

        byte packetType = packetData.get(); // "P", "S" "T" "L" "D" "A"
        Log.i(TAG, "Packet Type: " + (char) packetType);

        byte[] regardingComByte = new byte[6]; // which community this packet belongs to
        byte[] regardingFingerprintByte= new byte[16]; // which device this packet belongs to
        packetData.get(regardingComByte, 0, 6);
        packetData.get(regardingFingerprintByte, 0, 16);

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



        if(packetData.remaining() >= 36) {
            byte[] buf = new byte[packetData.remaining()];
            packetData.get(buf);
        }
    }

}
