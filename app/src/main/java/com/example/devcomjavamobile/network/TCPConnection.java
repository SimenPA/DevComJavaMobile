package com.example.devcomjavamobile.network;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

public class TCPConnection implements Runnable
{
    Socket s;
    DataOutputStream dos;
    String ip, msg;

    public TCPConnection(String ipIn, String msgIn)
    {
        ip = ipIn;
        msg = msgIn;
    }

    @Override
    public void run() {
        try {
            /*
            try
            {
                InetAddress address = InetAddress.getByName(ip);

                boolean isIPv6 = address instanceof Inet6Address;
            }

             */
            InetAddress address = InetAddress.getByName(ip);
            s = new Socket(address, 9700);
            dos =  new DataOutputStream(s.getOutputStream());
            dos.writeUTF(msg);

            dos.close();

            s.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}