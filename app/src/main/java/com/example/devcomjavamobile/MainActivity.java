package com.example.devcomjavamobile;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;

import com.example.devcomjavamobile.network.Peer;
import com.example.devcomjavamobile.network.TunnelService;
import com.example.devcomjavamobile.network.security.Crypto;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    final static int START_TUNNEL =  123;
    final static String TAG = MainActivity.class.getSimpleName();

    LinkedList<Peer> peers;

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
                R.id.navigation_home, R.id.navigation_tcp, R.id.navigation_home)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        //String fromC = stringFromJNI();
        //Log.i("MainActivity","Got the following string from C++: " + fromC );

        listItems();


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

    public void stopTunnel() {
        startService(getServiceIntent().setAction(TunnelService.STOP_TUNNEL));
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

    public void listItems()
    {
        File f = new File("/data/data/com.example.devcomjavamobile/");
        String[] fileList = f.list();
        for(String file : fileList) { Log.i(TAG, file); }
    }

    public LinkedList<Peer> getPeers() { return peers; }

    public native String stringFromJNI();

    public native void generateKeys();

    public native String createFingerprint();

}