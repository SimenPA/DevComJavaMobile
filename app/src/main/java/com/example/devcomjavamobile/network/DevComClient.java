package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import com.example.devcomjavamobile.R;

public class DevComClient extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Bunch of initalizing of UI elements here dood.
        findViewById(R.id.makeTunnel).setOnClickListener(v -> {
            Intent intent = VpnService.prepare(DevComClient.this);
            if(intent != null) {
                startActivityForResult(intent, 0);
            } else {
                onActivityResult(0, RESULT_OK, null);
            }
        });
         */
        Intent intent = VpnService.prepare(DevComClient.this);
        if(intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        if(result == RESULT_OK) {
            startService(getServiceIntent().setAction(com.example.devcomjavamobile.network.DevComService.ACTION_TUNNEL));
        }
    }
    private Intent getServiceIntent() {
        return new Intent(this, com.example.devcomjavamobile.network.DevComService.class);
    }
}
