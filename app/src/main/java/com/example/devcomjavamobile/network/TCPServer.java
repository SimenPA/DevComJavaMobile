package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import android.os.Handler;

import com.example.devcomjavamobile.MainActivity;

public class TCPServer implements Runnable {

    ServerSocket ss;
    Socket mySocket;
    String msg;
    DataInputStream dis;

    Activity activity;

    public TCPServer(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        try
        {
            ss =  new ServerSocket(9700);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Server has started", Toast.LENGTH_SHORT).show();
                }
            });
            while (true)
            {
                mySocket = ss.accept();
                dis =  new DataInputStream(mySocket.getInputStream());

                msg = dis.readUTF();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, "Message received from client: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }
}
