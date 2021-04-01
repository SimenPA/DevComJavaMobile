/* Based on HttpToolkit android open source code
 * This class is based on the Kotlin code found on this GitHub page:
 * https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/ProxyVpnService.kt
 * HttpToolkit is an open-source tool operated by Timothy Perry
 * https://httptoolkit.tech/terms-of-service/
 *
 * Initial porting by Simen Persch Andersen started 24.03.2021
 * TODO: Find out about licensing and include this here
 */

package com.example.devcomjavamobile.network;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.network.vpn.socket.IProtectSocket;
import com.example.devcomjavamobile.network.vpn.socket.SocketProtector;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


public class TunnelService extends VpnService implements IProtectSocket {

    private final int MAX_PACKET_LEN = 1500;

    private static final String TAG = TunnelService.class.getSimpleName();

    public static final String START_TUNNEL = "com.example.devcomjavamobile.START_TUNNEL";
    public static final String STOP_TUNNEL = "com.example.devcomjavamobile.STOP_TUNNEL";

    private ParcelFileDescriptor tunnelInterface;
    private TunnelRunnable tunnelRunnable;

    private TunnelService currentService = null;

    public boolean isTunnelActive() {
        if(currentService == null) return false;
        else return currentService.isActive();
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        currentService = this;
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


        if(Objects.equals(intent.getAction(), START_TUNNEL))
        {
            boolean tunnelStarted = false;
            if(isActive()) tunnelStarted = restartTunnel(); else  tunnelStarted = startTunnel();

            if(tunnelStarted) return Service.START_REDELIVER_INTENT;

        } else if(Objects.equals(intent.getAction(), STOP_TUNNEL)) stopTunnel();

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Log.i(TAG, "onRevoke called");
        stopTunnel();
    }

    private Boolean startTunnel() {

        /*

        // Coding this later if need be
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        Stream<Object> allPackageNames = packages.stream().map(pkg -> pkg.packageName);

        // Basically checks whether it's running on the Android emulator called Genymotion
        // Apparently, the whole device crashes when intercepting the whole system using
        // genymotion, so each app needs to be explicitly allowed.
        // I do not use Genymotion however, so I'm not sure if I need to do the same and will therefore
        // leave it as is and intercept every app for now
        Boolean isGenymotion = allPackageNames.any {
            name -> name.startsWith("com.genymotion");
        }
         */

        if(this.tunnelInterface != null) return false; // Already running
        ParcelFileDescriptor tunnelInterface = new Builder()
                .addAddress("fe80:646d:6d73:0000:c775:f615:9c29:fe06", 64)
                .addRoute("0.0.0.0", 0)
                .setSession(getString(R.string.app_name))
                .setMtu(MAX_PACKET_LEN)
                .setBlocking(true)
                .establish();
        if(tunnelInterface == null) {
            return false;
        } else {
            this.tunnelInterface = tunnelInterface;
        }


        // BroadcastManager.sendBroadcast(blah blah blah)

        SocketProtector.getInstance().setProtector(this);

        try {
            TunnelRunnable tunnelRunnable = new TunnelRunnable(tunnelInterface);
        } catch(IOException e) {
            Log.w("IOException", e);
        }

        new Thread(tunnelRunnable, "Tunnel thread").start();

        return true;
    }


    private boolean restartTunnel() {
        Log.i(TAG, "Tunnel stopping for a restart...");

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

    private void stopTunnel() {
        Log.i(TAG, "Tunnel stopping...");
        if(tunnelRunnable != null) {
            tunnelRunnable.stop();
            tunnelRunnable = null;
        }

        try {
            if(tunnelInterface != null) tunnelInterface.close();
            tunnelInterface = null;
        } catch(IOException e) {
            // Sentry.capture(e);
        }

        stopForeground(true);
        stopSelf();

        currentService = null;
    }

    private boolean isActive() {
        return this.tunnelInterface != null;
    }
}
