package com.example.devcomjavamobile;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.devcomjavamobile.network.security.Crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.interfaces.RSAPublicKey;
import java.util.Scanner;

import static android.content.Context.WIFI_SERVICE;

public class Utility {

    private final static String PUBLIC_KEY_PATH = "/data/data/com.example.devcomjavamobile/public_key.pem.tramp";
    private final static String TAG = Utility.class.getSimpleName();

    public static String createFingerPrint() throws Exception {

        RSAPublicKey pk = Crypto.readPublicKey(PUBLIC_KEY_PATH);
        BigInteger publicModulus = pk.getModulus();
        return publicModulus.toString(16).substring(0,16).toUpperCase();
    }

    public static String convertCommunityStringToHex(String str)
    {
        char[] ch = str.toCharArray();

        StringBuilder strB =  new StringBuilder();
        int i = 0;
        for (char c : ch) {
            if(i == 6) break;
            if(i % 2 == 0 && i != 0)
            {
                strB.append(":");
            }
            strB.append(Integer.toHexString((int) c));
            i++;
        }
        return strB.toString();
    }

    public static String generateIpv6Address(String community, String fingerPrint)
    {
        char[] ch = community.toCharArray();

        // Community IPv6 format
        StringBuilder comStrB =  new StringBuilder();
        int i = 0;
        for (char c : ch) {
            if(i == 6) break;
            if(i % 2 == 0 && i != 0)
            {
                comStrB.append(":");
            }
            comStrB.append(Integer.toHexString((int) c));
            i++;
        }
        String comIpv6 = comStrB.toString();

        // Fingerprint IPv6 format;
        StringBuilder fingerprintStrB =  new StringBuilder();
        i = 0;
        for(char c: fingerPrint.toCharArray())
        {
            if(i % 4 == 0)
            {
                fingerprintStrB.append(":");
            }
            fingerprintStrB.append(c);
            i++;
        }

        String fingerPrintIPv6 = fingerprintStrB.toString().toLowerCase();

        return "fe80:" + comIpv6 + fingerPrintIPv6;
    }

    public static void printFile(String filePath)
    {
        try {
            File myObj = new File(filePath);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                Log.i(TAG, data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int ipv4toInt(String address)
    {
        int firstOct, secondOct, thirdOct, fourthOct;
        StringBuilder strB;
        int i = 0;
        for(char c : address.toCharArray())
        {
            if(c == ':')
            {
                i++;

            }
        }
        return 10;
    }

}
