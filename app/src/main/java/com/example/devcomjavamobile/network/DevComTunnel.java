package com.example.devcomjavamobile.network;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class DevComTunnel implements Runnable {

    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    private static final String TAG = DevComTunnel.class.getSimpleName();

    private final VpnService mService;

    private PendingIntent mConfigureIntent;
    private OnEstablishListener mOnEstablishListener;

    public void setmConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    public void setmOnEstablishListener(OnEstablishListener listener) {
        mOnEstablishListener = listener;
    }

    public DevComTunnel(final VpnService service)
    {
        mService = service;
    }

    @Override
    public void run() {
        runBoi();
    }

    private ParcelFileDescriptor runBoi() {
        return createTun();
    }

    private ParcelFileDescriptor createTun() {
        return configure();
    }

    private ParcelFileDescriptor configure() throws IllegalArgumentException {
        VpnService.Builder builder = mService.new Builder();

        //IPv6 address to give the TUN interface
        // Prefix: fe80, Dev community: dmms/'646d:6d73:0000', fingerprint 'c775:f615:9c29:fe06' from public key in res/keys/
        // Link-local
        builder.addAddress("fe80:646d:6d73:0000:c775:f615:9c29:fe06", 64);
        // Add route 0.0.0.0 to accept all traffic
        builder.addRoute("0.0.0.0", 0);
        //
        builder.setBlocking(true);
        final ParcelFileDescriptor tunInterface = builder.establish();
        if(tunInterface == null) {
            Log.d(TAG, "TUN File Descriptor was NOT established");
        } else  {
            Log.d(TAG, "TUN File Descriptor was established");
        }
        if (mOnEstablishListener != null) {
            mOnEstablishListener.onEstablish(tunInterface);
        }
        return tunInterface;
    }
}
