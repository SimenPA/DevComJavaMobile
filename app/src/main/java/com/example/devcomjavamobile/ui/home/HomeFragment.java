package com.example.devcomjavamobile.ui.home;

import android.content.Intent;
import android.net.VpnService;
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

import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.network.DevComClient;
import com.example.devcomjavamobile.network.TCPConnection;
import com.example.devcomjavamobile.network.TCPServer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.devcomjavamobile.network.DevComService;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    EditText ipText, msgText;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });


        ipText = (EditText)root.findViewById(R.id.enterIPEditText);
        msgText = (EditText)root.findViewById(R.id.enterMsgEditText);
        /*

            Button sendMsgBtn = (Button) root.findViewById(R.id.makeTunnel);
            sendMsgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Executor e = Executors.newCachedThreadPool();
                    TCPConnection b = new TCPConnection(ipText.getText().toString(), msgText.getText().toString());
                    e.execute(b);
                }
            });

         */

        Button makeTunnel = (Button) root.findViewById(R.id.makeTunnel);
        makeTunnel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent  = new Intent(getActivity(), DevComClient.class);
                startActivity(intent);
            }
        });

        // Thread myThread =  new Thread(new TCPServer(getActivity()));
        // myThread.start();

        return root;
    }
}