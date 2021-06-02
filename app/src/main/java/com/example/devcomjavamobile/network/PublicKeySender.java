package com.example.devcomjavamobile.network;

import android.util.Log;

import com.example.devcomjavamobile.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/*
 * Based on File Send/Receive example by GitHub user absalomhr: https://gist.github.com/absalomhr/ce11c2e43df517b2571b1dfc9bc9b487
 * Simen Persch Andersen // 24.05.2021
 */

public class PublicKeySender implements Runnable {

    private final String TAG = PublicKeySender.class.getSimpleName();
    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    String serverRoute = "/data/data/com.example.devcomjavamobile/";

    int port;
    String host;
    String pubKeyFileName;

    public PublicKeySender(String host, int port, String pubKeyFileName) {
        this.host = host;
        this.port = port;
        this.pubKeyFileName = pubKeyFileName;
    }

    @Override
    public void run() {
        try {


            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);

            Log.i(TAG, "Sending transfer type");
            String transferType = "K"; // K = key
            byte[] transferTypeBytes = transferType.getBytes();
            DatagramPacket transferTypePacket = new DatagramPacket(transferTypeBytes, transferTypeBytes.length ,address, port);
            socket.send(transferTypePacket);

            Log.i(TAG, "Sending file name");
            File f = new File(PUBLIC_KEY_PATH);
            String fileName = Utility.createFingerPrint() + ".pem.tramp"; // FINGERPRINT.pem.tramp  --- like 99DE645C04C8C7B4.pem.tramp, NOT public_key.pem.tramp
            byte[] fileNameBytes = fileName.getBytes(); // File name as bytes to send it
            Log.i(TAG, "Sending file name to " + address.toString() + " at port " + port);
            DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port); // File name packet
            socket.send(fileStatPacket); // Sending the packet with the file name

            byte[] fileByteArray = readFileToByteArray(f); // Array of bytes the file is made of

            sendFile(socket, fileByteArray, address, port); // Entering the method to send the actual file
            receiveFile(socket, address, port);
            socket.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) throws Exception {

        Log.i(TAG, "Sending file");
        int sequenceNumber = 0; // For order
        boolean flag; // To see if we got to the end of the file
        int ackSequence = 0; // To see if the datagram was received correctly
        // Create message
        byte[] message = new byte[1024]; // First two bytes of the data are for control (datagram integrity and order)

        for (int i = 0; i < fileByteArray.length; i = i + 1019) {
            sequenceNumber += 1;

            // Create message

            if ((i + 1021) >= fileByteArray.length) { // Have we reached the end of file?
                message = new byte[fileByteArray.length + 5];
                flag = true;
                message[2] = (byte) (1); // We reached the end of the file (last datagram to be send)
            } else {
                flag = false;
                message[2] = (byte) (0); // We haven't reached the end of the file, still sending datagrams
            }

            message[0] = (byte) (sequenceNumber >> 8);
            message[1] = (byte) (sequenceNumber);

            int chunkSize;
            if(!flag) {
                chunkSize = 1019;
            } else {
                chunkSize = (fileByteArray.length - i);
            }
            message[3] = (byte) (chunkSize >> 8); // added these two bytes to contain int with the length of the next chunk to be written
            message[4] = (byte) (chunkSize);

            if (!flag) {
                Log.i(TAG, "This is not the last sequence");
                System.arraycopy(fileByteArray, i, message, 5, 1019);
            } else { // If it is the last datagram
                Log.i(TAG, "This IS the last sequence");
                System.arraycopy(fileByteArray, i, message, 5, fileByteArray.length - i);
            }

            Log.i(TAG, "Sending file to " + address.toString() + "at port " + port);
            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port); // The data to be sent
            socket.send(sendPacket); // Sending the data
            Log.i(TAG, "Sent: Sequence number = " + sequenceNumber);

            boolean ackRec; // Was the datagram received?
            int ttl = 0;

            while (true) {
                byte[] ack = new byte[2]; // Create another packet for datagram acknowledgement
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    socket.setSoTimeout(50); // Waiting for the server to send the ack
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff); // Figuring the sequence number
                    ackRec = true; // We received the ack
                } catch (SocketTimeoutException e) {
                    Log.i(TAG, "Socket timed out waiting for ack");
                    ackRec = false; // We did not receive an ack
                }

                // If the package was received correctly next packet can be sent
                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    ttl = 0;
                    Log.i(TAG, "Ack received: Sequence Number = " + ackSequence);
                    Log.i(TAG, "Received ack from address " + ackpack.getAddress().toString() + " at port " + ackpack.getPort());
                    break;
                } // Package was not received, so we resend it
                else {
                    ttl++;
                    if(ttl < 25)
                    {
                        socket.send(sendPacket);
                        Log.i(TAG, "Resending: Sequence Number = " + sequenceNumber);
                    }
                    else {
                        Log.i(TAG, "Transaction timed out, aborting");
                        break;
                    }
                }
            }
        }
    }

    private void receiveFile(DatagramSocket socket, InetAddress address, int port) throws IOException {
        // Send ack that we want to receive the recipients public key in order for NAT to let it through
        /*
        String fileRequestAck = "PK"; // FINGERPRINT.pem.tramp  --- like 99DE645C04C8C7B4.pem.tramp, NOT public_key.pem.tramp
        byte[] fileRequestAckBytes = fileRequestAck.getBytes(); // File name as bytes to send it
        Log.i(TAG, "Sending public key file request ack to " + address.toString() + " at port " + port);
        DatagramPacket fileRequestAckPacket = new DatagramPacket(fileRequestAckBytes, fileRequestAckBytes.length, address, port); // File name packet
        socket.send(fileRequestAckPacket);
         */
        boolean receivedFileName = false;
        int ttl = 0;

        byte[] receiveFileName = new byte[1024];
        DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length, address, port);

        while (!receivedFileName) {
            try {
                socket.setSoTimeout(50); // Waiting for the server to send the filename
                socket.receive(receiveFileNamePacket);
                receivedFileName = true;
            } catch (SocketTimeoutException e) {
                receivedFileName = false; // We did not receive file name
            }
            if (receivedFileName) {
                try {

                    byte[] data = receiveFileNamePacket.getData();
                    String fileName = new String(data, 0, receiveFileNamePacket.getLength());
                    Log.i(TAG, "Attempting to write filename: " + fileName + " to path: " + serverRoute + fileName);

                    File f = new File(serverRoute + "" + fileName); // Creating the file
                    FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we write the file content

                    Log.i(TAG, "Received file name");
                    boolean flag; // Have we reached end of file
                    int sequenceNumber = 0; // Order of sequences
                    int foundLast = 0; // The las sequence found

                    while (true) {
                        Log.i(TAG, "Receiving file");
                        byte[] message = new byte[1024]; // Where the data from the received datagram is stored
                        byte[] fileByteArray = new byte[1021]; // Where we store the data to be writen to the file

                        // Receive packet and retrieve the data
                        DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                        socket.receive(receivedPacket);
                        message = receivedPacket.getData(); // Data to be written to the file
                        Log.i(TAG, "Received data");

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
                            sendAck(foundLast, socket, address, port);
                        } else {
                            Log.i(TAG, "Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                            // Re send the acknowledgement
                            sendAck(foundLast, socket, address, port);
                        }
                        // Check for last datagram
                        if (flag) {
                            Log.i(TAG, "Received last datagram, ending.");
                            outToFile.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ttl++;
                if(ttl > 25)
                {
                    Log.i(TAG, "Socket timed out waiting for file name");
                    break;
                }
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

    public static byte[] readFileToByteArray(File file) {
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }
}