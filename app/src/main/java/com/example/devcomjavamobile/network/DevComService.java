package com.example.devcomjavamobile.network;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class DevComService extends VpnService {

    private static final String TAG = "DevComJavaMobile";

    public static final String ACTION_TUNNEL = "com.example.javavpntest.START";
    public static final String ACTION_UNTUNNEL = "com.example.javavpntest.STOP";

    public ParcelFileDescriptor tunFd;

    private static class Tunnel extends Pair<Thread, ParcelFileDescriptor> {
        public Tunnel(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }

    private final AtomicReference<Thread> mTunnelThread = new AtomicReference<>();
    private final AtomicReference<Tunnel> mTunnel = new AtomicReference<>();

    private PendingIntent mConfigureIntent;

    @Override
    public void onCreate() {
        mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, DevComClient.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && ACTION_UNTUNNEL.equals(intent.getAction())) {
            untunnel();
            return START_NOT_STICKY;
        } else {
            tunnel();
            return START_STICKY;
        }
    }


    // Equivalent to connect()
    private void tunnel() {
        startTunnel(new TunnelService(this));
    }


    // Equivalent to startConnection()
    private void startTunnel(final TunnelService tunnel) {
        final Thread thread =  new Thread(tunnel, "DevComTunnelThread");
        setTunnelThread(thread);
        tunnel.setmConfigureIntent(mConfigureIntent);
        tunnel.setmOnEstablishListener(tunInterface -> {
            mTunnelThread.compareAndSet(thread, null);
            setTunnel(new Tunnel(thread, tunInterface));
        });
        thread.start();

    }

    private void setTunnelThread(final Thread thread) {
        final Thread oldThread = mTunnelThread.getAndSet(thread);
        if(oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setTunnel(final Tunnel tunnel) {
        final Tunnel oldTunnel = mTunnel.getAndSet(tunnel);
        if(oldTunnel != null) {
            try {
                oldTunnel.first.interrupt();
                oldTunnel.second.close();
            } catch (IOException e) {
                Log.d(TAG, "Closing VPN interface");
            }
        }
    }

    private void untunnel() {
        setTunnelThread(null);
        setTunnel(null);
        stopForeground(true);
    }

}
