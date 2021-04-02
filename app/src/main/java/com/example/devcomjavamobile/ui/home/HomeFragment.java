package com.example.devcomjavamobile.ui.home;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
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
import com.example.devcomjavamobile.network.TunnelClient;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private static final String TAG = HomeFragment.class.getSimpleName();

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

        Button makeTunnel = (Button) root.findViewById(R.id.makeTunnel);
        makeTunnel.setOnClickListener(view -> {
            Log.d(TAG, "Attempting to create intent");
            Intent intent = new Intent(getActivity(), TunnelClient.class);
            Log.d(TAG, "Intent created, trying to start activity");
            startActivity(intent);

        });
        return root;
    }
}