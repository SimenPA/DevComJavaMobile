package com.example.devcomjavamobile.network;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
            s = new Socket(ip, 9700);
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