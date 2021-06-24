package com.example.devcomjavamobile.network.testing;

import android.app.Activity;
import android.util.Log;

import com.example.devcomjavamobile.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/*
* Based on File Send/Receive example by GitHub user absalomhr: https://gist.github.com/absalomhr/ce11c2e43df517b2571b1dfc9bc9b487
* Simen Persch Andersen // 24.05.2021
 */

public class UDPFileServer implements Runnable {

    DatagramSocket ds;
    DatagramPacket packet;

    private final static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    public boolean isRunning() {
        return running.get();
    }
    public boolean isStopped() {
        return stopped.get();
    }

    int port =  2500; // random port, just not 1337 that is used for DevCom data
    String serverRoute = "/data/data/com.example.devcomjavamobile/family/";

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
        Log.i(TAG, "Interrupting UDP file server");
        if(isRunning())
        {
            running.set(false);
            stopped.set(true);
            ds.close();
            Log.i(TAG, "UDP File Server has stopped");
            worker.interrupt();
        }
    }

    public void run()
    {
        running.set(true);
        stopped.set(false);
        byte[] receiveTransferType = new byte[1];
        byte[] receiveFileName = new byte[1024];
        //InetAddress serverAddress = Inet4Address.getByName(getIpAddress());
        try
        {
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            Log.i(TAG, "UDP File Server has started");
        } catch(IOException e) {
            e.printStackTrace();
        }


        while(isRunning()) {
            try {
                DatagramPacket receiveTransferTypePacket = new DatagramPacket(receiveTransferType, receiveTransferType.length);
                ds.setSoTimeout(0);
                ds.receive(receiveTransferTypePacket);
                byte[] data = receiveTransferTypePacket.getData();
                String transferType = new String(data, 0, receiveTransferTypePacket.getLength());

                if (transferType.equals("K") || transferType.equals("F")) { // K = "key", "F" = "file"

                    if(transferType.equals("K"))
                    {
                        File familyDir = new File(serverRoute);
                        if(!familyDir.isDirectory())
                        {
                            if(familyDir.mkdir()) Log.i(TAG, "Created family directory");
                        }
                    }

                    DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
                    ds.receive(receiveFileNamePacket);

                    data = receiveFileNamePacket.getData();
                    String fileName = new String(data, 0, receiveFileNamePacket.getLength());
                    Log.i(TAG, "Attempting to write filename: " + fileName + " to path: " + serverRoute + fileName);

                    File f = new File(serverRoute + "" + fileName); // Creating the file
                    FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we write the file content

                    Log.i(TAG, "Receiving file");
                    boolean flag; // Have we reached end of file
                    int sequenceNumber = 0; // Order of sequences
                    int foundLast = 0; // The las sequence found
                    int chunkSize = 0; // Amount of bits in datagram containing file data

                    while (isRunning()) {
                        Log.i(TAG, "Receiving file");
                        byte[] message = new byte[1024]; // Where the data from the received datagram is stored
                        byte[] fileByteArray = new byte[1019]; // Where we store the data to be writen to the file

                        // Receive packet and retrieve the data
                        DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                        ds.receive(receivedPacket);
                        message = receivedPacket.getData(); // Data to be written to the file
                        Log.i(TAG, "Received data");
                        String text = new String(message, "UTF-8");
                        Log.i(TAG, "Text: " + text);

                        // Get port and address for sending acknowledgment
                        InetAddress address = receivedPacket.getAddress();
                        int port = receivedPacket.getPort();

                        // Retrieve sequence number
                        sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);

                        // Check if we reached last datagram (end of file)
                        flag = (message[2] & 0xff) == 1;

                        // Retrieve chunk size
                        chunkSize = ((message[3] & 0xff) << 8) + (message[4] & 0xff);

                        Log.i(TAG, "Sequence number: " + sequenceNumber);

                        // If sequence number is the last seen + 1, then it is correct
                        // We get the data from the message and write the ack that it has been received correctly
                        if (sequenceNumber == (foundLast + 1)) {

                            // set the last sequence number to be the one we just received
                            foundLast = sequenceNumber;

                            // Retrieve data from message
                            System.arraycopy(message, 5, fileByteArray, 0, 1019);

                            // Write the retrieved data to the file and print received data sequence number
                            outToFile.write(fileByteArray, 0, chunkSize);
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
                            Log.i(TAG, "Received last datagram, closing file and attempts to send back own key");
                            outToFile.close();
                            if(transferType.equals("K")) returnPublicKey(ds, address, port);
                            break;
                        }
                    }
                }
                else if(transferType.equals("M")) { // M = "message"
                    break; // to be developed
                }

            } catch(IOException e){
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
        Log.i(TAG, "Sending ack to " + address.toString() + " at port " + port);
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        Log.i(TAG, "Sent ack: Sequence Number = " + foundLast);
    }

    private void returnPublicKey(DatagramSocket socket, InetAddress address, int port) throws IOException {
        /*
        int ttl = 0;
        while(ttl < 100) {
            ttl++;
            byte[] fileRequestAck = new byte[1024];
            DatagramPacket fileRequestPacket = new DatagramPacket(fileRequestAck, fileRequestAck.length);
            ds.setSoTimeout(50);
            ds.receive(fileRequestPacket);

            byte[] data = fileRequestPacket.getData();
            String fileRequestAckString = new String(data, 0, fileRequestPacket.getLength());
            if (fileRequestAckString.equals("PK")) {
                Log.i(TAG, "Received file request ack");
                try {
                    Log.i(TAG, "Sending public key file name to " + address.toString() + " at port " + port);
                    String fileName;

                    File f = new File(PUBLIC_KEY_PATH);
                    fileName = Utility.createFingerPrint() + ".pem.tramp"; // FINGERPRINT.pem.tramp  --- like 99DE645C04C8C7B4.pem.tramp, NOT public_key.pem.tramp
                    byte[] fileNameBytes = fileName.getBytes(); // File name as bytes to send it
                    DatagramPacket fileNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port); // File name packet
                    socket.send(fileNamePacket); // Sending the packet with the file name

                    PublicKeySender sender = new PublicKeySender(address.toString(), port, Utility.createFingerPrint());
                    byte[] fileByteArray = PublicKeySender.readFileToByteArray(f); // Array of bytes the file is made of
                    sender.sendFile(socket, fileByteArray, address, port); // Entering the method to send the actual file
                    Log.i(TAG, "Done sending public key file");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
         */
        try {
            Log.i(TAG, "Sending public key file name to " + address.toString() + " at port " + port);
            String fileName;

            File f = new File(PUBLIC_KEY_PATH);
            fileName = Utility.createFingerPrint() + ".pem.tramp"; // FINGERPRINT.pem.tramp  --- like 99DE645C04C8C7B4.pem.tramp, NOT public_key.pem.tramp
            byte[] fileNameBytes = fileName.getBytes(); // File name as bytes to send it
            DatagramPacket fileNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port); // File name packet
            socket.send(fileNamePacket); // Sending the packet with the file name

            PublicKeySender sender = new PublicKeySender(address.toString(), port, Utility.createFingerPrint());
            byte[] fileByteArray = PublicKeySender.readFileToByteArray(f); // Array of bytes the file is made of
            sender.sendFile(socket, fileByteArray, address, port); // Entering the method to send the actual file
            Log.i(TAG, "Done sending public key file");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
