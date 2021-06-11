package com.example.devcomjavamobile.network.devcom;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPCheckSender implements Runnable {

    private final int DATA_TRAFFIC_PORT = 1337;

    String ip, msg;
    byte[] buf;

    public UDPCheckSender(String ipIn, String msgIn)
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
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    serverAddress, DATA_TRAFFIC_PORT + 1);
            Log.i("UDP Check Sender", "Trying to UDP Check message: " + msg);
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