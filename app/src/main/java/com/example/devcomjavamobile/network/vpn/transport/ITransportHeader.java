package com.example.devcomjavamobile.network.vpn.transport;

/**
 * Created by Lipi on 2017. 3. 27..
 */

/*
* Imported by Simen Persch Andersen on 29.3.2021 from
* https://github.com/httptoolkit/httptoolkit-android/blob/master/app/src/main/java/tech/httptoolkit/android/vpn/transport/ITransportHeader.java
 */

public interface ITransportHeader {
    int getSourcePort();
    int getDestinationPort();
}