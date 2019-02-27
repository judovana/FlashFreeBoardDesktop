/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.comm.bt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.ListAndWrite;

/**
 *
 * @author jvanek
 */
public class BtOp implements ListAndWrite {

    @Override
    public void writeToDevice(String id, byte[]... b) {
        try {
            writeTo(id, b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ConnectionID[] listDevices() {
        return list();
    }

    //serial port
    //our single desired service
    private static final UUID[] uuidSet = new UUID[]{new UUID("0000110100001000800000805f9b34fb", false)};
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
    private static boolean inquiryRunning = false;

    private static ConnectionID[] list() {
        try {
            List<ServiceRecord> services = listServices();
            ConnectionID[] r = new ConnectionID[services.size()];
            for (int i = 0; i < services.size(); i++) {
                ServiceRecord s = services.get(i);
                try {
                    String name = s.getHostDevice().getBluetoothAddress() + " (" + s.getHostDevice().getFriendlyName(false) + ")";
                    r[i] = new ConnectionID(s.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true), name);
                } catch (Exception ex) {
                    String name = s.getHostDevice().getBluetoothAddress();
                    r[i] = new ConnectionID(s.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true), name);
                }
            }
            return r;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<ServiceRecord> listServices() throws BluetoothStateException {
        if (inquiryRunning) {
            throw new RuntimeException("Inquiry running!");
        }
        LocalDevice local = LocalDevice.getLocalDevice();
        DiscoveryAgent agent = local.getDiscoveryAgent();
        SearchContoller sc = new SearchContoller(agent);
        // use inquiryRunning to make sure two inquiries aren't running at the sametime.
        inquiryRunning = agent.startInquiry(DiscoveryAgent.GIAC, sc);

        while (inquiryRunning) {
            try {
                Thread.sleep(250);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return sc.services;
    }

    public static void main(String... args) throws BluetoothStateException, IOException {
        ConnectionID[] services = list();
        for (int i = 0; i < services.length; i++) {
            ConnectionID service = services[i];
            System.out.println(service);

        }
        String url = services[0].getId();
        byte[] testData = new byte[]{
            (byte) 5, 0, 0,
            (byte) 5, 0, 0,
            (byte) 0, 5, 0,
            (byte) 0, 5, 0,
            (byte) 0, 0, 5,
            (byte) 0, 0, 5,
            (byte) 5, 5, 0,
            (byte) 5, 5, 0,
            (byte) 5, 0, 5,
            (byte) 5, 0, 5,
            (byte) 0, 5, 5,
            (byte) 0, 5, 5,
            (byte) 5, 5, 5
        };
        writeTo(url, testData);
    }

    private static Map<String, OutputStream> ocache = new HashMap<>();

    private static void writeTo(String url, byte[]... b) throws IOException {
        OutputStream os = ocache.get(url);
        if (os == null) {
            os = Connector.openOutputStream(url);
            ocache.put(url, os);
        }
        try {
            for (byte[] byteArray : b) {
                os.write(byteArray);
                os.flush();
                System.out.println("written -  " + byteArray.length);
                Thread.sleep(10);
            }
            System.out.println("written - end - " + b.length);
            Thread.sleep(500);//time to fullyconsume? HYPER CRITICAL!
        } catch (Exception e) {
            os.close();
            ocache.remove(url);
            e.printStackTrace();
        }

    }

    private static class SearchContoller implements DiscoveryListener {

        private final DiscoveryAgent agent;
        private final List<ServiceRecord> services = new ArrayList<>();
        private final List<RemoteDevice> devices = new ArrayList<>();

        private SearchContoller(DiscoveryAgent agent) {
            this.agent = agent;
        }

        @Override
        public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
            try {
                System.out.println("Found: " + device.getBluetoothAddress() + " - " + device.getFriendlyName(false));
            } catch (Exception ex) {
                System.out.println("Found: " + device.getBluetoothAddress());
            }
            devices.add(device);

        }

        @Override
        public void inquiryCompleted(int arg0) {
            System.out.println("device search done.");
            System.out.println("Searching matching services.");
            for (RemoteDevice device : devices) {
                try {
                    agent.searchServices(new int[0], uuidSet, device, this);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public void serviceSearchCompleted(int arg0, int arg1) {
            System.out.println("Services done");
            inquiryRunning = false;

        }

        @Override
        public void servicesDiscovered(int arg0, ServiceRecord[] srs) {
            System.out.println("Found: " + Arrays.toString(srs));
            for (int i = 0; i < srs.length; i++) {
                ServiceRecord sr = srs[i];
                services.add(sr);

            }
        }
    }

}
