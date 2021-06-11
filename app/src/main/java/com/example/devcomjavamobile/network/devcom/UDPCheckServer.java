package com.example.devcomjavamobile.network.devcom;


import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.MainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;


    public class UDPCheckServer implements Runnable {

        private final int DATA_TRAFFIC_PORT = 1337; // Thhis port +1 to check for UDP connection

        DatagramSocket ds;
        DatagramPacket packet;

        private Thread worker;


        private AtomicBoolean running = new AtomicBoolean(false);
        private AtomicBoolean stopped = new AtomicBoolean(true);

        private LinkedList<Peer> peers;

        private final String TAG = UDPCheckServer.class.getSimpleName();

        public UDPCheckServer() {
            this.peers = MainActivity.getPeers();
        }


        public void start() {
            worker = new Thread(this);
            worker.start();
        }

        public void interrupt() {
            if (isRunning()) {
                running.set(false);
                stopped.set(true);
                ds.close();
                Log.i(TAG, "UDP Check Server has started");
                worker.interrupt();
            } else {
                Log.i(TAG, "UDP Check Server has started");
            }
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
                byte[] lmessage = new byte[10];
                ds = new DatagramSocket(DATA_TRAFFIC_PORT + 1);
                ds.setReuseAddress(true);
                packet = new DatagramPacket(lmessage, lmessage.length);

                Log.i(TAG, "UDP Check Server has started");
                while (isRunning()) {
                    try {
                        ds.receive(packet);
                        Log.i(TAG, "UDP check packet received. Reading");
                        message = new String(lmessage, 0, packet.getLength());
                        String finalMessage = message;

                        Peer peer = null;

                        // Check if peer is among our known peers
                        for (Peer p : peers) {
                            for (String ipAddress : p.getPhysicalAddresses()) {
                                if (ipAddress.equals(packet.getAddress().toString())) peer = p;
                            }
                        }
                        if (peer == null) {
                            Log.d(TAG, "Unknown peer, aborting check");
                        } else {
                            switch (finalMessage) {
                                case "SYN": // Peer wants to know if we support UDP
                                {
                                    UDPCheckSender sender = new UDPCheckSender(packet.getAddress().toString(), "SYNACK");
                                    sender.run();
                                    break;
                                }
                                case "SYNACK": // Peer has responded to our syn, so he supports UDP and I do too. Acking synack to complete a three way handshake
                                {
                                    UDPCheckSender sender = new UDPCheckSender(packet.getAddress().toString(), "ACK");
                                    sender.run();
                                    peer.setUdp(1);
                                    break;
                                }
                                case "ACK": // Peer supports receiving UDP, and so do I, since this completes a three way handshake
                                    peer.setUdp(1);
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown check message: " + finalMessage);
                            }
                        }


                    } catch (SocketException e) {
                        if (e.getMessage().equals("Socket closed"))
                            Log.i("UDP Check Server", "Ignoring socket closed exception");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }