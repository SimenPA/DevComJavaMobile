package com.example.devcomjavamobile.network;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;

public class TunnelClient extends Activity {

    private final static String TAG = TunnelClient.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = VpnService.prepare(TunnelClient.this);

        Log.d(TAG, "Activity started");

        if(intent != null) {
            Log.d(TAG, "Intent != null");
            startActivityForResult(intent, 0);
        } else {
            Log.d(TAG, "Intent = null");
            onActivityResult(0, RESULT_OK, null);
        }

    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if(result == RESULT_OK) {
            Log.d(TAG, "Starting service");
            startService(getServiceIntent().setAction(TunnelService.START_TUNNEL));
        }
    }
    private Intent getServiceIntent() {
        return new Intent(this, TunnelService.class);
    }
}
