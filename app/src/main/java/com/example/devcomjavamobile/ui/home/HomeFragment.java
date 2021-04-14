package com.example.devcomjavamobile.ui.home;

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
import com.example.devcomjavamobile.network.TCPServer;
import com.example.devcomjavamobile.network.UDPSender;
import com.example.devcomjavamobile.network.UDPServer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private static final String TAG = HomeFragment.class.getSimpleName();

    Button stopTunnelBtn;

    EditText ipText, msgText;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        /*
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        */

        UDPServer server = new UDPServer(getActivity());

        ipText = (EditText)root.findViewById(R.id.enterIPEditText);
        msgText = (EditText)root.findViewById(R.id.enterMsgEditText);

        Button sendMsgBtn = (Button) root.findViewById(R.id.sendMsgBtn);
        sendMsgBtn.setOnClickListener(view -> {

            Executor e = Executors.newCachedThreadPool();
            UDPSender b = new UDPSender(ipText.getText().toString(), msgText.getText().toString());
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
            server.start();
        });

        Button stopServerBtn = (Button) root.findViewById(R.id.stopServerBtn);
        stopServerBtn.setOnClickListener(view -> {
            server.interrupt();
        });

        return root;
    }
}