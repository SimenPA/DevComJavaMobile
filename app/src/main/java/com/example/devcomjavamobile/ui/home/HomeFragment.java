package com.example.devcomjavamobile.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.Utility;
import com.example.devcomjavamobile.network.PublicKeySender;
import com.example.devcomjavamobile.network.UDPFileServer;
import com.example.devcomjavamobile.network.UDPSender;
import com.example.devcomjavamobile.network.UDPServer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private static final String TAG = HomeFragment.class.getSimpleName();

    EditText ipText, msgText;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);


        UDPServer msgServer = new UDPServer(getActivity());
        UDPFileServer fileServer = new UDPFileServer(getActivity());

        ipText = (EditText)root.findViewById(R.id.enterIPEditText);
        msgText = (EditText)root.findViewById(R.id.enterComEditText);

        Button sendMsgBtn = (Button) root.findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {

            Executor e = Executors.newCachedThreadPool();
            UDPSender b = new UDPSender(ipText.getText().toString(), msgText.getText().toString());
            e.execute(b);

        });

        Button sendPubKeyBtn = (Button) root.findViewById(R.id.sendPubKeyBtn);
        sendPubKeyBtn.setOnClickListener(view -> {
            String fingerPrint = "";
            try{
                fingerPrint = Utility.createFingerPrint();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            Executor e = Executors.newCachedThreadPool();
            PublicKeySender b = new PublicKeySender(ipText.getText().toString(), 1337, fingerPrint);
            e.execute(b);

        });

        Button makeTunnel = (Button) root.findViewById(R.id.makeTunnel);
        makeTunnel.setOnClickListener(view -> {
            ((MainActivity)getActivity()).startTunnel();
        });

        Button stopTunnelBtn = (Button) root.findViewById(R.id.stopTunnel);
        stopTunnelBtn.setOnClickListener(view -> {
            ((MainActivity)getActivity()).stopTunnel();
        });

        Button startServerBtn = (Button) root.findViewById(R.id.startServerBtn);
        startServerBtn.setOnClickListener(view -> {
            msgServer.start();
        });

        Button stopServerBtn = (Button) root.findViewById(R.id.stopServerBtn);
        stopServerBtn.setOnClickListener(view -> {
            msgServer.interrupt();
        });


        Button startFileServerBtn = (Button) root.findViewById(R.id.startFileServerBtn);
        startFileServerBtn.setOnClickListener(view -> {
            fileServer.start();
        });

        Button stopFileServerBtn = (Button) root.findViewById(R.id.stopFileServerBtn);
        stopFileServerBtn.setOnClickListener(view -> {
            fileServer.interrupt();
        });
        return root;
    }
}