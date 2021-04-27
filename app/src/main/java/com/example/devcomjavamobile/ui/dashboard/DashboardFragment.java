package com.example.devcomjavamobile.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.network.ControlTraffic;
import com.example.devcomjavamobile.network.P2P;
import com.example.devcomjavamobile.network.RoutingTable;
import com.example.devcomjavamobile.network.TCPServer;
import com.example.devcomjavamobile.network.UDPSender;
import com.example.devcomjavamobile.network.UDPServer;
import com.example.devcomjavamobile.ui.home.HomeFragment;
import com.example.devcomjavamobile.ui.home.HomeViewModel;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private static final String TAG = DashboardFragment.class.getSimpleName();

    EditText communityText, fingerPrintText, addressText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        TCPServer server = new TCPServer(getActivity());

        communityText = (EditText)root.findViewById(R.id.enterComEditText);
        fingerPrintText = (EditText)root.findViewById(R.id.enterFingerPrintEditText);
        addressText = (EditText)root.findViewById(R.id.enterAddressEditText);

        Button connectBtn = (Button) root.findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(view -> {
            LinkedList<RoutingTable> peers = ((MainActivity)getActivity()).getPeers();
            P2P p2p = new P2P(peers, ((MainActivity)getActivity()).createFingerprint());
            try {
                p2p.joinCommunity(communityText.getText().toString(), fingerPrintText.getText().toString(), addressText.getText().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        Button startTCPServerBtn = (Button) root.findViewById(R.id.startTCPButton);
        startTCPServerBtn.setOnClickListener(view -> {
            server.start();
        });

        Button stopTCPServerBtn = (Button) root.findViewById(R.id.stopTCPButton);
        stopTCPServerBtn.setOnClickListener(view -> {
            try {
                server.interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return root;
    }
}