package com.example.devcomjavamobile.network;

import android.util.Log;

import com.example.devcomjavamobile.Utility;

import java.io.File;
import java.io.FileInputStream;
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
            Log.i(TAG, "Sending file name");
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);
            String fileName;

            File f = new File(PUBLIC_KEY_PATH);
            fileName = Utility.createFingerPrint() + ".pem.tramp"; // FINGERPRINT.pem.tramp  --- like 99DE645C04C8C7B4.pem.tramp, NOT public_key.pem.tramp
            byte[] fileNameBytes = fileName.getBytes(); // File name as bytes to send it
            DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port); // File name packet
            socket.send(fileStatPacket); // Sending the packet with the file name

            byte[] fileByteArray = readFileToByteArray(f); // Array of bytes the file is made of
            sendFile(socket, fileByteArray, address, port); // Entering the method to send the actual file
            socket.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) throws IOException {
        Log.i(TAG, "Sending file");
        int sequenceNumber = 0; // For order
        boolean flag; // To see if we got to the end of the file
        int ackSequence = 0; // To see if the datagram was received correctly

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            sequenceNumber += 1;

            // Create message
            byte[] message = new byte[1024]; // First two bytes of the data are for control (datagram integrity and order)
            message[0] = (byte) (sequenceNumber >> 8);
            message[1] = (byte) (sequenceNumber);

            if ((i + 1021) >= fileByteArray.length) { // Have we reached the end of file?
                flag = true;
                message[2] = (byte) (1); // We reached the end of the file (last datagram to be send)
            } else {
                flag = false;
                message[2] = (byte) (0); // We haven't reached the end of the file, still sending datagrams
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
            } else { // If it is the last datagram
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
            }

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port); // The data to be sent
            socket.send(sendPacket); // Sending the data
            Log.i(TAG, "Sent: Sequence number = " + sequenceNumber);

            boolean ackRec; // Was the datagram received?
            int ttl = 0;

            while (true) {
                byte[] ack = new byte[2]; // Create another packet for datagram ackknowledgement
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

    private static byte[] readFileToByteArray(File file) {
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