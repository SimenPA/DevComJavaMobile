package com.example.devcomjavamobile.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.network.devcom.P2P;
import com.example.devcomjavamobile.network.devcom.TunnelRunnable;
import com.example.devcomjavamobile.network.testing.UDPFileServer;
import com.example.devcomjavamobile.network.testing.UDPServer;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private static final String TAG = HomeFragment.class.getSimpleName();

    Button makeTunnel, stopTunnel;

    EditText communityText, fingerPrintText, addressText;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);


        addressText = (EditText)root.findViewById(R.id.enterAddressEditText);
        communityText = (EditText)root.findViewById(R.id.enterComEditText);
        fingerPrintText = (EditText)root.findViewById(R.id.enterFingerPrintEditText);


        Button joinComBtn = (Button) root.findViewById(R.id.joinComBtn);
        joinComBtn.setOnClickListener(view -> {
            P2P p2p = new P2P(getActivity(), TunnelRunnable.getTunnelWriter());
            try {
                p2p.joinCommunity(communityText.getText().toString(), fingerPrintText.getText().toString(), addressText.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        makeTunnel = (Button) root.findViewById(R.id.makeTunnel);
        makeTunnel.setOnClickListener(view -> {
            if(!((MainActivity)getActivity()).isTunnelRunning())
            {
                ((MainActivity)getActivity()).startTunnel();
                Toast.makeText((MainActivity)getActivity(), "Tunnel Interface has been started", Toast.LENGTH_SHORT).show();
                makeTunnel.setTextColor(Color.GRAY);
                stopTunnel.setTextColor(Color.BLACK);
            } else {
                Toast.makeText((MainActivity)getActivity(), "Tunnel Interface is already active", Toast.LENGTH_SHORT).show();
            }
        });

        stopTunnel = (Button) root.findViewById(R.id.stopTunnel);
        stopTunnel.setOnClickListener(view -> {
            if(((MainActivity)getActivity()).isTunnelRunning())
            {
                ((MainActivity)getActivity()).stopTunnel();
                Toast.makeText((MainActivity)getActivity(), "Tunnel Interface has been stopped", Toast.LENGTH_SHORT).show();
                makeTunnel.setTextColor(Color.BLACK);
                stopTunnel.setTextColor(Color.GRAY);
            } else {
                Toast.makeText((MainActivity)getActivity(), "Tunnel Interface is not active", Toast.LENGTH_SHORT).show();
            }

        });

        if(((MainActivity)getActivity()).isTunnelRunning())
        {
            makeTunnel.setTextColor(Color.GRAY);
        } else { stopTunnel.setTextColor(Color.GRAY); }



        return root;
    }
}