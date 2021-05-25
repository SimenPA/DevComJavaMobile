package com.example.devcomjavamobile;

import com.example.devcomjavamobile.network.security.Crypto;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

public class Utility {

    private final static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";

    public static String createFingerPrint() throws Exception {
        Crypto c = new Crypto();

        RSAPublicKey pk = c.readPublicKey(PUBLIC_KEY_PATH);
        BigInteger publicModulus = pk.getModulus();
        return publicModulus.toString(16).substring(0,16).toUpperCase();
    }
}
