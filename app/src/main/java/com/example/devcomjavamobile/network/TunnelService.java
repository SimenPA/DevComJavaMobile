package com.example.devcomjavamobile.network;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.devcomjavamobile.R;

import java.io.IOException;


public class TunnelService extends VpnService {


    private static final String TAG = TunnelService.class.getSimpleName();

    public static final String START_TUNNEL = "com.example.javavpntest.START_TUNNEL";
    public static final String STOP_TUNNEL = "com.example.javavpntest.STOP_TUNNEL";

    private ParcelFileDescriptor tunnelInterface;
    private DevComTunnelRunnable tunnelRunnable;


    @Override
    public void onCreate()
    {
        super.onCreate();
        TunnelService currentService = this;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        TunnelService currentService = null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        currentService = this;
        Log.i(TAG, "onStartCommand called");
        Log.i(TAG, intent.getAction() != null ? intent.getAction() : "no action");


        if(intent.getAction() == START_TUNNEL) {
            boolean vpnStarted = if(isActive()) restartVpn()
        }
    }

    private Boolean startTunnel() {

        String[] packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        String[] allPackageNames = packages.map { pkg -> pkg.packageName }


        Boolean isGenymotion = allPackageNames.any {
            name -> name.startsWith("com.genymotion");
        }

        if(this.tunnelInterface != null) return false; // Already running
        ParcelFileDescriptor tunnelInterface = new Builder()
                .addAddress("fe80:646d:6d73:0000:c775:f615:9c29:fe06", 64)
                .addRoute("0.0.0.0", 0)
                .setSession(getString(R.string.app_name))
                .establish();
        if(tunnelInterface == null) {
            return false;
        } else {
            this.tunnelInterface = tunnelInterface;
        }

        // SocketProtector.getInstance(),setProtector(this);

        tunnelRunnable = new TunnelRunnable (
            tunnelInterface,
                )
    }

    private Boolean restartTunnel() {
        Log.i(TAG, "VPN stopping for a restart...");

        if(tunnelRunnable != null)
        {
            try
            {
                tunnelRunnable.stop();
            }
            catch(NullPointerException e) {
                Log.i(TAG, "tunnelRunnable null pointer exception", e);
            }
            tunnelRunnable = null;
        }
        try {
            tunnelInterface.close();
            tunnelRunnable = null;
        } catch (IOException e) {
            Log.i(TAG, "IOException", e);
        }

        stopForeground(true);
        return startTunnel();
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
