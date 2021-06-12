package com.example.devcomjavamobile.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.network.devcom.ControlTraffic;
import com.example.devcomjavamobile.network.devcom.Peer;
import com.example.devcomjavamobile.network.devcom.PeersHandler;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.security.RSAUtil;
import com.example.devcomjavamobile.network.vpn.ClientPacketWriter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

public class DataTrafficServer implements Runnable {

    public final String TAG = ControlTraffic.class.getSimpleName();

    private final String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";

    private final int AES_BLOCK_SIZE = 256;

    byte[] DatagramPacket = new byte[1500];

    private final BlockingDeque<DatagramPacket> packetQueue = new LinkedBlockingDeque<>();

    private final static int PORT_DATA = 1337;

    public LinkedList<Peer> peers;
    public String physicalAddress = "";
    private DatagramSocket datagramSocket;
    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    ClientPacketWriter tunnelWriter;

    public DataTrafficServer(ClientPacketWriter tunnelWriter)
    {
        this.tunnelWriter = tunnelWriter;
        this.peers = MainActivity.getPeers();
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
            if(datagramSocket != null) datagramSocket.close();
            Log.v(TAG, "UDP Data Socket has been closed for physical address: " + physicalAddress);
            worker.interrupt();
        }
        else {
            Log.v(TAG, "There is no UDP Data Socket open for physical address: " + physicalAddress);
        }
    }

    @Override
    public void run()
    {

        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        if(datagramSocket == null)
        {
            try {
                datagramSocket.bind(new InetSocketAddress(PORT_DATA));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(datagramSocket != null)
        {
            while (this.isRunning()) {
                Log.d(TAG, "Datagram socket opened");
                try {
                    do {
                        datagramSocket.receive(packet);
                        handleUDPPacket(packet);
                        Log.d(TAG, "Handed it over to handleUDPPacket");
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
                } catch (Exception e) {
                    e.printStackTrace();
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

    public void handleUDPPacket(@NonNull DatagramPacket packet) throws Exception {

        PeersHandler pHandler = new PeersHandler(peers);
        InetAddress address = packet.getAddress();
        byte[] encryptedData = packet.getData();

        Peer peer = null;

        for(Peer p: peers) {
            for(String ip: p.getPhysicalAddresses()) if(ip.equals(address.toString())) peer = p;
        }

        if(peer == null)
        {
            Log.d(TAG, "Received UDP message from unknown peer. Dismissing");
            return;
        }

        byte[] decryptedData = Crypto.aes_decrypt(encryptedData, peer.getDecryptCipher());
        tunnelWriter.write(decryptedData);
        // ClientPacketWriter tunnelWriter = new ClientPacketWriter()
    }

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

}
