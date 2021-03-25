package com.example.devcomjavamobile.network;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class TunnelRunnable  implements Runnable {

    private final int MAX_PACKET_LEN = 1500;

    private boolean running = false;

    private final String TAG = TunnelRunnable.class.getSimpleName();

    // Packets from device apps downstream, heading upstream via this Tunnel
    private FileInputStream tunnelReadStream;

    private ParcelFileDescriptor tunnelInterface;
    private FileOutputStream tunnelWriteStream;

    public TunnelRunnable(ParcelFileDescriptor tunnelInterface) {
        this.tunnelInterface =  tunnelInterface;
        tunnelWriteStream = new FileOutputStream(Objects.requireNonNull(tunnelInterface).getFileDescriptor());
        tunnelReadStream =  new FileInputStream(Objects.requireNonNull(tunnelInterface).getFileDescriptor());
    }

    // private ClientPacketWriter tunnelPacketWriter = new ClientPacketWriter(tunnelWriteStream);
    // private Thread clientPacketWriterThread = new Thread(tunnelPacketWriter);

    // private SocketNIODataService nioService = new SocketNIODataService(vpnPacketWriter);
    // private Thread dataServiceThread = new Thread(nioService, "Socket NIO thread");

    // private SessionManager manager = new SessionManager();
    // private SessionHandler handler = new SessionHandler(manager, nioservice, tunnelPacketWriter);

    private ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);

    // private

    @Override
    public void run()
    {
        if(running) {
            Log.w(TAG, "Tunnel runnable attempted to start, but it's already running!");
            return;
        }
        Log.i(TAG, "Tunnel thread starting");

        // manager.setTcpPortRedirections(portRedirections);
        // dataServiceThread.start();
        // tunnelPacketWriterThread.start();

        byte[] data;
        int length;
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
                    try {
                        packet.limit(length);
                        // handler.handlePacket(packet);
                    } catch (Exception e) {
                        String errorMessage = (e.getMessage() != null) ? e.getMessage() : e.toString();
                        Log.e(TAG, errorMessage);

                        boolean isIgnorable =
                                ((e is ConnectException && errorMessage.equals("Permission denied")) ||
                        (e is ConnectException && errorMessage.equals("Network is unreachable")) ||
                        (e is PacketHeaderException && errorMessage.contains("IP version should be 4 but was 6")));

                        if (!isIgnorable) {
                            // Sentry.capture(e);
                        }
                    }

                    packet.clear();
                } else {
                    Thread.sleep(10);
                }
            } catch (e catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOExction e) {
                e.printStacepkTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }: InterruptedException) {
                Log.i(TAG, "Sleep interrupted: " + e.message)
            } catch (e:InterruptedIOException) {
                Log.i(TAG, "Read interrupted: " + e.message)
            }
        }

        Log.i(TAG, "Tunnel thread shutting down")

    }

}
