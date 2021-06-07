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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.example.devcomjavamobile.Utility;
import com.example.devcomjavamobile.network.vpn.socket.IProtectSocket;
import com.example.devcomjavamobile.network.vpn.socket.SocketProtector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

public class TunnelService extends VpnService implements IProtectSocket {

    private final int MAX_PACKET_LEN = 1500;

    private static final String TAG = TunnelService.class.getSimpleName();

    public static final String START_TUNNEL = "com.example.devcomjavamobile.START";
    public static final String STOP_TUNNEL = "com.example.devcomjavamobile.STOP";

    private String fingerPrint;
    private String tunnelAddress;

    private static ParcelFileDescriptor tunnelInterface;
    private TunnelRunnable tunnelRunnable;
    private Thread tunnelRunnableThread;

    private TunnelService currentService = null;

    private PendingIntent mConfigureIntent;

    public LinkedList<Peer> peers;

    public static boolean isRunning() {
        return tunnelInterface != null;
    }

    public boolean isTunnelActive() {
        if(currentService == null) return false;
        else return currentService.isActive();
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        //mConfigureIntent = PendingIntent.getActivity(this, 0, new Intent(this, TunnelClient.class),
        // PendingIntent.FLAG_UPDATE_CURRENT);
        // getFilesDir(); // get key pair file
        currentService = this;

        try {
            fingerPrint = Utility.createFingerPrint();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tunnelAddress = createTunnelAddress();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        TunnelService currentService = null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        currentService = this;
        Log.d(TAG, "onStartCommand called");
        Log.d(TAG, intent.getAction() != null ? intent.getAction() : "no action");


        if(Objects.equals(intent.getAction(), START_TUNNEL))
        {
            boolean tunnelStarted = false;
            if(isActive()) tunnelStarted = restartTunnel(); else  tunnelStarted = startTunnel();

            if(tunnelStarted) return Service.START_REDELIVER_INTENT;
            else { stopTunnel(); }

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

        if(tunnelInterface != null) return false; // Already running
        ParcelFileDescriptor tunnelInterface = new Builder()
                .addAddress(tunnelAddress, 128)
                .allowFamily(AF_INET6)
                //.allowFamily(AF_INET)
                //.addRoute("0.0.0.0", 0) // All IPv4 addresses
                .addRoute("0000:0000:0000:0000:0000:0000:0000:0000", 0) // All IPv6 addresses
                .setMtu(MAX_PACKET_LEN)
                .setBlocking(true)
                .establish();
        if(tunnelInterface == null) {
            Log.d(TAG, "Tunnel interface NOT established");
            return false;
        } else {
            Log.d(TAG, "Tunnel interface established");
            this.tunnelInterface = tunnelInterface;
        }


        SocketProtector.getInstance().setProtector(this);

        try {
            Log.d(TAG, "Starting tunnelRunnable");
            tunnelRunnable = new TunnelRunnable(tunnelInterface, peers);
        } catch(IOException e) {
            Log.w("IOException", e);
        }

        Log.d(TAG, "Attempting to start Thread with TunnelRunnable");

        tunnelRunnableThread = new Thread(tunnelRunnable, "Tunnel thread");
        tunnelRunnableThread.start();

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
        return tunnelInterface != null;
    }


    private String createTunnelAddress()
    {
        StringBuilder str = new StringBuilder();
        str.append("fe80:0000:0000:0000:");
        str.append(fingerPrint.substring(0, 3).toLowerCase());
        str.append(":");
        str.append(fingerPrint.substring(4, 7).toLowerCase());
        str.append(":");
        str.append(fingerPrint.substring(8, 11).toLowerCase());
        str.append(":");
        str.append(fingerPrint.substring(12, 15).toLowerCase());
        return str.toString();
    }
}
