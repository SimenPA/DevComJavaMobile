package com.example.devcomjavamobile;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;

import com.example.devcomjavamobile.network.TunnelService;
import com.example.devcomjavamobile.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }
    private Intent getServiceIntent() {
        return new Intent(this, TunnelService.class);
    }

    public void stopTunnel() {
        startService(getServiceIntent().setAction(TunnelService.STOP_TUNNEL));
    }

    public void startTunnel() {
        VpnService.prepare(this);
        startService(getServiceIntent().setAction(TunnelService.START_TUNNEL));
    }
}