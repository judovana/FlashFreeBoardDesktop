/*
 sudo dnf install bluez-hid2hci

 $ rfkill list
 0: tpacpi_bluetooth_sw: Bluetooth
 Soft blocked: no
 Hard blocked: no
 1: phy0: Wireless LAN
 Soft blocked: no
 Hard blocked: no
 3: hci0: Bluetooth
 Soft blocked: yes
 Hard blocked: no

 $ rfkill unblock 3
 =>	Soft blocked: no

 sudo hciconfig hci0 up
 bluetoothctl
 [bluetooth]scan on
 Discovery started
 [CHG] Controller 40:2C:F4:55:53:C5 Discovering: yes
 [NEW] Device 00:06:66:C0:AC:62 RNBT-AC62
 [bluetooth] scan off
 [bluetooth] pair 00:06:66:C0:AC:62



 sudo rfcomm connect hci0  00:06:66:C0:AC:62
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
import javax.microedition.io.StreamConnection;
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
    /*
     * bluez-libs-devel, bluez-libs, bluetoothctl, rfkill
     * systemctl start bluetooth
     * rfkill  list
     0: tpacpi_bluetooth_sw: Bluetooth
     Soft blocked: yes
     Hard blocked: no
     1: phy0: Wireless LAN
     Soft blocked: yes
     Hard blocked: no
     * 
     * rfkill   unblock 0
     * rfkill  list
     0: tpacpi_bluetooth_sw: Bluetooth
     Soft blocked: no
     Hard blocked: no
     1: phy0: Wireless LAN
     Soft blocked: yes
     Hard blocked: no
     4: hci0: Bluetooth
     Soft blocked: yes
     Hard blocked: no
     * rfkill unblock 4
     * bluetoothctl
     * power on
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

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                for (ConnWithStream v : ocache.values()) {
                    try {
                        v.os.close();
                        v.sn.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        ));
    }
    private static Map<String, ConnWithStream> ocache = new HashMap<>();

    private static class ConnWithStream {

        private final StreamConnection sn;
        private final OutputStream os;

        public ConnWithStream(StreamConnection sn, OutputStream os) {
            this.sn = sn;
            this.os = os;
        }

    }

    private static void writeTo(String url, byte[]... b) throws IOException {

        ConnWithStream os = ocache.get(url);
        try {
            if (os == null) {
                //com.intel.bluetooth.BluetoothRFCommClientConnection
                //implements StreamConnection, BluetoothConnectionAccess
                StreamConnection conn = (StreamConnection) Connector.open(url);
                Thread.sleep(500);
                os = new ConnWithStream(conn, conn.openOutputStream());
                ocache.put(url, os);
                //soft reset arduino her (send only zeros?)
                //byte[] resert = new byte[b[0].length];
                //System.out.println("reset send");

            }
            for (byte[] byteArray : b) {
                sendSingleArrayBySingleByte(byteArray, os);
                System.out.println("written -  " + byteArray.length);
                Thread.sleep(10);
            }
            System.out.println("written - end - " + b.length);
            Thread.sleep(500);//time to fullyconsume? HYPER CRITICAL!
        } catch (Exception e) {
            if (os != null) {
                os.os.close();
                os.sn.close();
            }
            ocache.remove(url);
            e.printStackTrace();
        }

    }

    private static void sendSingleArrayBySingleByte(byte[] byteArray, ConnWithStream os) throws InterruptedException, IOException {
        for (int i = 0; i < byteArray.length; i++) {
            byte c = byteArray[i];
            os.os.write(new byte[]{c});
            os.os.flush();
            Thread.sleep(1);
        }
    }

    private static class SearchContoller implements DiscoveryListener {

        private final DiscoveryAgent agent;
        private final List<ServiceRecord> services = new ArrayList<>();
        private final List<RemoteDevice> devices = new ArrayList<>();
        private int hit = 0;

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
            hit++;
            System.out.println("Services done1: " + arg0 + "/" + arg1);
            System.out.println("Services done2: " + hit + "/" + devices.size());
            //if (hit >= devices.size() || arg0 >= arg1) { ??
            if (hit >= devices.size()) {
                inquiryRunning = false;
            }

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
