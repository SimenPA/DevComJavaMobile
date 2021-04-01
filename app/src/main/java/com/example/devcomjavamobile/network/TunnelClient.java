package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

public class TunnelClient extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = VpnService.prepare(TunnelClient.this);

        if(intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if(result == RESULT_OK) {
            startService(getServiceIntent().setAction(TunnelService.START_TUNNEL));
        }
    }
    private Intent getServiceIntent() {
        return new Intent(this, TunnelService.class);
    }
}
