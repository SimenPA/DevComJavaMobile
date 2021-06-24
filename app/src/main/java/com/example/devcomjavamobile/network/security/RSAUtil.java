package com.example.devcomjavamobile.network.security;

import android.util.Log;

import com.example.devcomjavamobile.Utility;
import com.example.devcomjavamobile.network.devcom.P2P;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;

public class RSAUtil {
    private static String TAG = RSAUtil.class.getSimpleName();

    private static String PRIVATE_KEY_PATH = "/data/data/com.example.devcomjavamobile/private_key.pem.tramp";
    private static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";





}