/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.comm.bt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

/**
 *
 * @author jvanek
 */
public class BtOp {

    private static RemoteDevice foundDevice;
    private static ServiceRecord[] services;

    ;
    

    public static void main(String... args) throws BluetoothStateException, IOException {
        LocalDevice local = LocalDevice.getLocalDevice();
        DiscoveryAgent agent = local.getDiscoveryAgent();
// use inquiryStarted to make sure two inquiries aren't running at the same
// time.
        boolean inquiryStarted = agent.startInquiry(DiscoveryAgent.GIAC,
                new DiscoveryListener() {
                    @Override
                    public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
                        System.out.println("Found: " + device.getBluetoothAddress());
                        if ("000666C0AC62".equals(device.getBluetoothAddress())) {
                            foundDevice = device;
                        }
                    }

                    @Override
                    public void inquiryCompleted(int arg0) {
                        System.out.println("device search done");

                    }

                    @Override
                    public void serviceSearchCompleted(int arg0, int arg1) {
                        System.out.println("Services0 done");

                    }

                    @Override
                    public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
                        System.out.println("Service?");

                    }
                });
        while (foundDevice == null) {
            try {
                Thread.sleep(250);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        agent.cancelInquiry(new DiscoveryListener() {

            @Override
            public void deviceDiscovered(RemoteDevice rd, DeviceClass dc) {

            }

            @Override
            public void servicesDiscovered(int i, ServiceRecord[] srs) {

            }

            @Override
            public void serviceSearchCompleted(int i, int i1) {

            }

            @Override
            public void inquiryCompleted(int i) {

            }
        });

        UUID[] uuidSet = new UUID[1];
        /*
         [bluetooth]# info 00:06:66:C0:AC:62
         Device 00:06:66:C0:AC:62 (public)
         Name: RNBT-AC62
         Alias: RNBT-AC62
         Class: 0x00001f00
         Paired: yes
         Trusted: yes
         Blocked: no
         Connected: no
         LegacyPairing: no
         UUID: Serial Port               (00001101-0000-1000-8000-00805f9b34fb)
         */
        //serial port, see  abve
        uuidSet[0] = new UUID("0000110100001000800000805f9b34fb", false);
        agent.searchServices(new int[0], uuidSet, foundDevice, new DiscoveryListener() {

            @Override
            public void deviceDiscovered(RemoteDevice rd, DeviceClass dc) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void servicesDiscovered(int i, ServiceRecord[] srs) {
                System.out.println("Found: " + Arrays.toString(srs));
                services = srs;
            }

            @Override
            public void serviceSearchCompleted(int i, int i1) {
                System.out.println("Services done");
            }

            @Override
            public void inquiryCompleted(int i) {

            }
        });

        while (services == null) {
            try {
                Thread.sleep(250);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        String url = services[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true);
        //Connection conn = Connector.open(url);
        OutputStream out = Connector.openOutputStream(url);
        out.write("abcd".getBytes("ascii"));
        out.close();
        //conn.close();
    }

}
