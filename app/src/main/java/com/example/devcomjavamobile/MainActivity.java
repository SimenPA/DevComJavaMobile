package com.example.devcomjavamobile;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.network.Peer;
import com.example.devcomjavamobile.network.PeersHandler;
import com.example.devcomjavamobile.network.TCPServer;
import com.example.devcomjavamobile.network.TunnelService;
import com.example.devcomjavamobile.network.UDPFileServer;
import com.example.devcomjavamobile.network.UDPServer;
import com.example.devcomjavamobile.network.security.Crypto;
import com.example.devcomjavamobile.network.security.RSAUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    final static int START_TUNNEL =  123;
    final static String TAG = MainActivity.class.getSimpleName();

    LinkedList<Peer> peers;

    TCPServer tcpServer;
    UDPFileServer udpServer;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_transport, R.id.navigation_home)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        //String fromC = stringFromJNI();
        //Log.i("MainActivity","Got the following string from C++: " + fromC );

        listItems();
        try{
            RSAUtil.testSignAndVerify();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Crypto crypto = new Crypto();

        try {
            crypto.testEncryption();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            crypto.genKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        peers = new LinkedList<>();
        try{
            setPeers();
        } catch(Exception e) {
            e.printStackTrace();
        }

        udpServer = new UDPFileServer(this);
        tcpServer = new TCPServer(peers);

        startTcpServer();
        startUdpServer();


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            startService(getServiceIntent().setAction(TunnelService.START_TUNNEL));
        }


    }
    private Intent getServiceIntent() {
        return new Intent(this, TunnelService.class);
    }

    public void startTcpServer() {
        if(!isTcpServerRunning())
        {
            tcpServer.start();
            Toast.makeText(this, "TCP Server is now running", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "TCP Server is already running", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTcpServer() {
        if(isTcpServerRunning())
        {
            try{
                tcpServer.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "TCP Server has now been stopped", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "TCP Server is not running", Toast.LENGTH_SHORT).show();
        }

    }

    public void startUdpServer(){
        if(!isUdpServerRunning())
        {
            udpServer.start();
            Toast.makeText(this, "UDP Server is now running", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "UDP Server is already running", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopUdpServer() {
        if(isUdpServerRunning())
        {
            udpServer.interrupt();
            Toast.makeText(this, "UDP Server has now been stopped", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "UDP Server is not running", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTunnel() {
        startService(getServiceIntent().setAction(TunnelService.STOP_TUNNEL));
    }

    public boolean isTunnelRunning() {
        return TunnelService.isRunning();
    }

    public boolean isUdpServerRunning()
    {
        return udpServer.isRunning();
    }

    public boolean isTcpServerRunning()
    {
        return tcpServer.isRunning();
    }


    public void startTunnel() {

        Intent vpnIntent = VpnService.prepare(this);
        boolean vpnNotConfigured = vpnIntent != null;
        if (vpnNotConfigured) {
            startActivityForResult(vpnIntent, START_TUNNEL);
        } else {
            onActivityResult(START_TUNNEL, RESULT_OK, null);
        }
    }

    // Lists items for testings purposes. Deletes pem files outside community directories that is not part of clients key pair
    public void listItems()
    {
        File f = new File("/data/data/com.example.devcomjavamobile/");
        String[] fileList = f.list();
        for(String file : fileList) {
            Log.i(TAG, file);
            if(file.endsWith(".pem.tramp") && !file.equals("private_key.pem.tramp") && !file.equals("public_key.pem.tramp"))
            {
                File deleteFile = new File("/data/data/com.example.devcomjavamobile/" + file);
                if(deleteFile.delete()) { Log.i(TAG, "Successfully deleted file " + deleteFile); }
            }
        }
    }

    public void setPeers() throws Exception {
        PeersHandler pHandler = new PeersHandler(peers);

        Crypto c = new Crypto();
        File appDir = new File("/data/data/com.example.devcomjavamobile/");
        String[] directories = appDir.list((current, name) -> new File(current, name).isDirectory());
        for(String community : directories) {
            File dir = new File("/data/data/com.example.devcomjavamobile/" + community);
            String[] fileList = dir.list();
            for (String file : fileList) {
                if (file.endsWith(".pem.tramp") && !file.equals("private_key.pem.tramp") && !file.equals("public_key.pem.tramp")) {
                    String fingerPrint = file.substring(0, file.length() - 10).toUpperCase();
                    Peer p = pHandler.getPeer(fingerPrint);
                    if (p == null) {
                        Log.i(TAG, "Adding peer " + fingerPrint);
                        p = new Peer();
                        p.setFingerPrint(fingerPrint);
                        p.setPublicKey(c.readPublicKey("/data/data/com.example.devcomjavamobile/" + community + "/" + file));
                        peers.add(p);
                    } else {
                        Log.i(TAG, "Peer " + fingerPrint + " already exists, adding community");
                    }
                    p.addCommunity(community);
                }
            }
        }
    }

    public LinkedList<Peer> getPeers() { return peers; }

    public native String stringFromJNI();

    public native void generateKeys();

    public native String createFingerprint();

}