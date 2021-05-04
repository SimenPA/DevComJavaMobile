/* Based on HttpToolkit android open source code
* This class is based on the Kotlin code found on this GitHub page:
* https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/ProxyVpnRunnable.kt
* HttpToolkit is an open-source tool operated by Timothy Perry
* https://httptoolkit.tech/terms-of-service/
*
* Initial porting by Simen Persch Andersen started 24.03.2021
* TODO: Find out about licensing and include this here
 */


package com.example.devcomjavamobile.network;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.devcomjavamobile.network.vpn.ClientPacketWriter;
import com.example.devcomjavamobile.network.vpn.SessionHandler;
import com.example.devcomjavamobile.network.vpn.SessionManager;
import com.example.devcomjavamobile.network.vpn.socket.SocketNIODataService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;

public class TunnelRunnable implements Runnable {

    // Set on our Tunnel as the MTU, which should guarantee all packets fit this
    private final int MAX_PACKET_LEN = 1500;

    private boolean running = false;

    private final String TAG = TunnelRunnable.class.getSimpleName();

    // Packets from device apps downstream, heading upstream via this Tunnel
    private FileInputStream tunnelReadStream;

    // Packets from upstream servers, received by this VPN
    private FileOutputStream tunnelWriteStream;

    private ClientPacketWriter tunnelPacketWriter;
    private Thread tunnelPacketWriterThread;

    private SocketNIODataService nioService;

    private Thread dataServiceThread;

    private SessionManager manager;
    private SessionHandler handler;


    public TunnelRunnable(ParcelFileDescriptor tunnelInterface, LinkedList<Peer> peers) throws IOException {
        tunnelWriteStream = new FileOutputStream(Objects.requireNonNull(tunnelInterface).getFileDescriptor());
        tunnelReadStream =  new FileInputStream(Objects.requireNonNull(tunnelInterface).getFileDescriptor());
        tunnelPacketWriter = new ClientPacketWriter(tunnelWriteStream);
        tunnelPacketWriterThread = new Thread(tunnelPacketWriter);
        nioService = new SocketNIODataService(tunnelPacketWriter);
        dataServiceThread = new Thread(nioService, "Socket NIO thread");
        manager = new SessionManager();
        handler = new SessionHandler(manager, nioService, tunnelPacketWriter, peers);
    }

    // Allocate the buffer for a single packet.
    private ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);


    @Override
    public void run() {
        if(running) {
            Log.w(TAG, "Tunnel runnable attempted to start, but it's already running!");
            return;
        }
        Log.d(TAG, "Tunnel thread starting");

        dataServiceThread.start();
        tunnelPacketWriterThread.start();

        byte[] data;
        int length = 0;
        running = true;
        while (running) {
            try {
                data = packet.array();

                    try{
                        length = tunnelReadStream.read(data);
                    }
                    catch (IOException e) {
                        Log.i(TAG, "IOException in tunnelReadStream.read(data)",e);
                    }
                if (length > 0) {
                    // Log.i(TAG, "Packet inc");
                    try {
                        packet.limit(length);
                        handler.handlePacket(packet);
                    } catch (Exception e) {
                        String errorMessage = (e.getMessage() != null) ? e.getMessage() : e.toString();
                        Log.e(TAG, errorMessage);

                        /*
                        // Port this stuff later if needed
                        boolean isIgnorable =
                                ((e is ConnectException && errorMessage.equals("Permission denied")) ||
                        (e is ConnectException && errorMessage.equals("Network is unreachable")) ||
                        (e is PacketHeaderException && errorMessage.contains("IP version should be 4 but was 6")));

                        if (!isIgnorable) {
                            // Sentry.capture(e);
                        }
                        */
                    }

                    packet.clear();
                } else {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Sleep interrupted: " + e.getMessage());
            }
        }

        Log.i(TAG, "Tunnel thread shutting down");

    }

    void stop() {
        if(running) {
            running = false;
            nioService.shutdown();
            dataServiceThread.interrupt();

            tunnelPacketWriter.shutdown();
            tunnelPacketWriterThread.interrupt();
        } else {
            Log.w(TAG, "Tunnel runnable stopped, but it's not running");
        }
    }

}
