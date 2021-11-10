package com.example.devcomjavamobile.network.tunneling.socket;

import  java.net.DatagramSocket;
import java.net.Socket;

/* Imported by Simen Persch Andersen on 29.03.2021 from
* https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/vpn/socket/IProtectSocket.java
 */

public interface IProtectSocket {
    boolean protect(Socket socket);
    boolean protect(DatagramSocket socket);
}