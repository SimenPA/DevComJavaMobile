package com.example.devcomjavamobile.ui.info;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devcomjavamobile.MainActivity;
import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.network.devcom.Peer;
import com.example.devcomjavamobile.network.security.Crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.Scanner;

import static android.content.Context.WIFI_SERVICE;
import static android.widget.Toast.makeText;

public class InfoFragment extends Fragment {

    private InfoViewModel infoViewModel;

    LinkedList<Peer> peers;

    EditText ipText, fingerprintText, devicesText;

    Button copyIpBtn, copyFingerprintBtn, copyPublicKeyButton;

    private final String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    // Begin RecycleView elements
    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected CustomAdapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2;

    // End RecycleView elements

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        infoViewModel =
                ViewModelProviders.of(this).get(InfoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_info, container, false);

        ipText = (EditText)root.findViewById(R.id.ipText);
        fingerprintText = (EditText)root.findViewById(R.id.fingerprintText);
        //devicesText = (EditText)root.findViewById(R.id.devicesText);

        peers = ((MainActivity)getActivity()).getPeers();

        try {
            ipText.setText(getLocalIp());
        } catch(UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            fingerprintText.setText(createFingerprint());
        } catch(Exception e) {
            e.printStackTrace();
        }

        copyIpBtn = (Button) root.findViewById(R.id.copyIpBtn);
        copyIpBtn.setOnClickListener(view -> {
            copyTextToClipboard(ipText.getText().toString(), "IP");
        });

        copyFingerprintBtn = (Button) root.findViewById(R.id.copyFingerprintBtn);
        copyFingerprintBtn.setOnClickListener(view -> {
            copyTextToClipboard(fingerprintText.getText().toString(), "fingerprint");
        });

        copyPublicKeyButton = (Button) root.findViewById(R.id.copyPublicKeyTextBtn);
        copyPublicKeyButton.setOnClickListener(view -> {
            copyTextToClipboard(getOwnPublicKeyString(), "public key");
        });

        // Begin manage card view RecycleView
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycleView);

        mLayoutManager = new LinearLayoutManager(getActivity());

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        String[] peersStringTable = new String[peers.indexOf(peers.peekLast()) + 1];
        String[] communitiesStringTable = new String[peers.indexOf(peers.peekLast()) + 1];
        int i = 0;
        for(Peer p : peers)
        {
            peersStringTable[i] =  p.getFingerPrint();
            if(!p.getCommunities().isEmpty()) communitiesStringTable[i] = p.getCommunities().getFirst();
            i++;
        }

        //String[] peersStringTable = new String[10];
        // for(int i = 0; i < peersStringTable.length; i++) { peersStringTable[i] = "Nummer " + i; }
        mAdapter = new CustomAdapter(peersStringTable, communitiesStringTable, getActivity());

        // Set CustomAdapter as the adapter for RecyclerView.
        mRecyclerView.setAdapter(mAdapter);
        // End manage card view RecycleView



        return root;
    }

    private String getLocalIp() throws UnknownHostException
    {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array())
                .getHostAddress();
    }

    private String createFingerprint() throws Exception {

        Crypto c = new Crypto();

        RSAPublicKey pk = c.readPublicKey(PUBLIC_KEY_PATH);
        BigInteger publicModulus = pk.getModulus();
        return publicModulus.toString(16).substring(0,16).toUpperCase();
    }

    private String getOwnPublicKeyString() {

        StringBuilder pubKeyStringBuilder =  new StringBuilder();
        try {
            File myObj = new File(PUBLIC_KEY_PATH);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                pubKeyStringBuilder.append(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return pubKeyStringBuilder.toString();
    }

    public void copyTextToClipboard(String text, String tag) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied to clipboard", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), "Copied " + tag + " to clipboard", Toast.LENGTH_SHORT).show();
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        switch (layoutManagerType) {
            case GRID_LAYOUT_MANAGER:
                mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
                mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
                break;
            case LINEAR_LAYOUT_MANAGER:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }
}