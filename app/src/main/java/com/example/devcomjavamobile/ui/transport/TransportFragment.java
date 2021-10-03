package com.example.devcomjavamobile.ui.transport;

import android.graphics.Color;
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
import com.example.devcomjavamobile.network.devcom.Peer;
import com.example.devcomjavamobile.network.testing.PublicKeySender;
import com.example.devcomjavamobile.network.testing.UDPSender;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransportFragment extends Fragment {

    private TransportViewModel TransportViewModel;
    private static final String TAG = TransportFragment.class.getSimpleName();

    EditText ipText, msgText;
    LinkedList<Peer> peers;

    Button startTCPServerBtn, stopTCPServerBtn, startUdpServerBtn, stopUdpServerBtn, sendPubKeyBtn;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {






        TransportViewModel =
                ViewModelProviders.of(this).get(TransportViewModel.class);
        View root = inflater.inflate(R.layout.fragment_transport, container, false);

        ipText = (EditText)root.findViewById(R.id.enterIPEditText);
        msgText = (EditText)root.findViewById(R.id.enterMsgEditText);

        peers = ((MainActivity)getActivity()).getPeers();

        String newString;
        if (savedInstanceState == null) {
            Bundle extras = getActivity().getIntent().getExtras();
            if(extras == null) {
                newString = null;
            } else {
                newString = extras.getString("ipv6String");
            }
        } else {
            newString= (String) savedInstanceState.getSerializable("ipv6String");
        }

        ipText.setText(newString);

        Button sendMsgBtn = (Button) root.findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {

            Executor e = Executors.newCachedThreadPool();
            UDPSender b = new UDPSender(ipText.getText().toString(), msgText.getText().toString());
            e.execute(b);

        });

        sendPubKeyBtn = (Button) root.findViewById(R.id.sendPubKeyBtn);
        sendPubKeyBtn.setOnClickListener(view -> {
                    String fingerPrint = "";
                    try {
                        fingerPrint = Utility.createFingerPrint();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Executor e = Executors.newCachedThreadPool();
                    PublicKeySender b = new PublicKeySender(ipText.getText().toString(), 2500, fingerPrint, peers);
                    e.execute(b);
                });

        startTCPServerBtn = (Button) root.findViewById(R.id.startTcpServerBtn);
        startTCPServerBtn.setOnClickListener(view -> {
            ((MainActivity)getActivity()).startTcpServer();
            if(((MainActivity)getActivity()).isTcpServerRunning())
            {
                startTCPServerBtn.setTextColor(Color.GRAY);
                stopTCPServerBtn.setTextColor(Color.BLACK);
            }
        });

        stopTCPServerBtn = (Button) root.findViewById(R.id.stopTcpServerBtn);
        stopTCPServerBtn.setOnClickListener(view -> {
            ((MainActivity)getActivity()).stopTcpServer();
            if(!((MainActivity)getActivity()).isTcpServerRunning())
            {
                startTCPServerBtn.setTextColor(Color.BLACK);
                stopTCPServerBtn.setTextColor(Color.GRAY);
            }
        });


        startUdpServerBtn = (Button) root.findViewById(R.id.startUdpServerBtn);
        startUdpServerBtn.setOnClickListener(view -> {
            ((MainActivity) getActivity()).startUdpServer();
            if(((MainActivity)getActivity()).isUdpServerRunning())
            {
                startUdpServerBtn.setTextColor(Color.GRAY);
                stopUdpServerBtn.setTextColor(Color.BLACK);
            }
        });

        stopUdpServerBtn = (Button) root.findViewById(R.id.stopUdpServerBtn);
        stopUdpServerBtn.setOnClickListener(view -> {
            ((MainActivity) getActivity()).stopUdpServer();
            if(!((MainActivity)getActivity()).isUdpServerRunning())
            {
                startUdpServerBtn.setTextColor(Color.BLACK);
                stopUdpServerBtn.setTextColor(Color.GRAY);
            }

        });

        if(((MainActivity)getActivity()).isTcpServerRunning())
        {
            startTCPServerBtn.setTextColor(Color.GRAY);
        } else { stopTCPServerBtn.setTextColor(Color.GRAY); }

        if(((MainActivity)getActivity()).isUdpServerRunning())
        {
            startUdpServerBtn.setTextColor(Color.GRAY);
        } else { stopUdpServerBtn.setTextColor(Color.GRAY); }


        return root;
    }
}