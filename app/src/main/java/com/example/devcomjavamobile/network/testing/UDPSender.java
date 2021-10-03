package com.example.devcomjavamobile.network.testing;

import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPSender implements Runnable {

    private static final String TAG = UDPSender.class.getSimpleName();
    String ip, msg;
    byte[] buf;

    public UDPSender(String ipIn, String msgIn)
    {
        ip = ipIn;
        msg = msgIn;
        buf = msg.getBytes();


    }

    public void run() {

            try {
                InetAddress serverAddress = InetAddress.getByName(ip);
                DatagramSocket socket = new DatagramSocket();
                if (!socket.getBroadcast()) socket.setBroadcast(true);

                Log.i(TAG, "Sending transfer type");
                String transferType = "M"; // M = Message
                byte[] transferTypeBytes = transferType.getBytes();
                DatagramPacket transferTypePacket = new DatagramPacket(transferTypeBytes, transferTypeBytes.length , serverAddress, 2500);
                socket.send(transferTypePacket);

                DatagramPacket packet = new DatagramPacket(buf, buf.length,
                        serverAddress, 2500);
                Log.i("UDPSender", "Trying to send message: " + msg);
                socket.send(packet);
                socket.close();
            } catch (final UnknownHostException e) {
                e.printStackTrace();
            } catch (final SocketException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
    }
}
