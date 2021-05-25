package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/*
* Based on File Send/Receive example by GitHub user absalomhr: https://gist.github.com/absalomhr/ce11c2e43df517b2571b1dfc9bc9b487
* Simen Persch Andersen // 24.05.2021
 */

public class UDPFileServer implements Runnable {

    DatagramSocket ds;
    DatagramPacket packet;

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

    int port =  1337;
    String serverRoute = "/data/data/com.example.devcomjavamobile/";

    private Thread worker;

    Activity activity;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(true);

    private final String TAG = UDPFileServer.class.getSimpleName();

    public UDPFileServer(Activity activity) {
        this.activity = activity;
    }


    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() {
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            ds.close();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "UDP File Server has stopped", Toast.LENGTH_SHORT).show();
                }
            });
            Log.i("UDPServer", "UDP File Server has stopped");
            worker.interrupt();
        }
        else {
            Toast.makeText(activity, "UDP Server is not running", Toast.LENGTH_SHORT).show();
        }
    }

    public void run()
    {
        running.set(true);
        stopped.set(false);
        byte[] receiveFileName = new byte[1024];
        //InetAddress serverAddress = Inet4Address.getByName(getIpAddress());
        try
        {
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "UDP File Server has started", Toast.LENGTH_SHORT).show();
                }
            });
            Log.i(TAG, "UDP File Server has started");
        } catch(IOException e) {
            e.printStackTrace();
        }


        while(isRunning()) {
            try {

                DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
                ds.receive(receiveFileNamePacket);

                byte[] data = receiveFileNamePacket.getData();
                String fileName = new String(data, 0, receiveFileNamePacket.getLength());
                Log.i(TAG, "Attempting to write filename: " + fileName + " to path: " + serverRoute + fileName);

                File f = new File(serverRoute + "" + fileName); // Creating the file
                FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we write the file content

                Log.i(TAG, "Receiving file");
                boolean flag; // Have we reached end of file
                int sequenceNumber = 0; // Order of sequences
                int foundLast = 0; // The las sequence found

                while (isRunning()) {
                    Log.i(TAG, "Receiving file");
                    byte[] message = new byte[1024]; // Where the data from the received datagram is stored
                    byte[] fileByteArray = new byte[1021]; // Where we store the data to be writen to the file

                    // Receive packet and retrieve the data
                    DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                    ds.receive(receivedPacket);
                    message = receivedPacket.getData(); // Data to be written to the file
                    Log.i(TAG, "Received data");

                    // Get port and address for sending acknowledgment
                    InetAddress address = receivedPacket.getAddress();
                    int port = receivedPacket.getPort();

                    // Retrieve sequence number
                    sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
                    // Check if we reached last datagram (end of file)
                    flag = (message[2] & 0xff) == 1;
                    Log.i(TAG, "Sequence number: " + sequenceNumber);

                    // If sequence number is the last seen + 1, then it is correct
                    // We get the data from the message and write the ack that it has been received correctly
                    if (sequenceNumber == (foundLast + 1)) {

                        // set the last sequence number to be the one we just received
                        foundLast = sequenceNumber;

                        // Retrieve data from message
                        System.arraycopy(message, 3, fileByteArray, 0, 1021);

                        // Write the retrieved data to the file and print received data sequence number
                        outToFile.write(fileByteArray);
                        Log.i(TAG, "Received: Sequence number:" + foundLast);

                        // Send acknowledgement
                        sendAck(foundLast, ds, address, port);
                    } else {
                        Log.i(TAG, "Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                        // Re send the acknowledgement
                        sendAck(foundLast, ds, address, port);
                    }
                    // Check for last datagram
                    if (flag) {
                        Log.i(TAG, "Received last datagram, closing server/socket");
                        outToFile.close();
                        interrupt();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (foundLast >> 8);
        ackPacket[1] = (byte) (foundLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        Log.i(TAG, "Sent ack: Sequence Number = " + foundLast);
    }
}
