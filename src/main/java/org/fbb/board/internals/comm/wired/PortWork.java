/*
sudo  usermod -aG dialout jvanek
sudo  usermod -aG uucp jvanek
 sudo  usermod -aG lock jvanek
 sudo chown root:uucp  /run/lock
 sudo chmod 775 /run/lock

 */
package org.fbb.board.internals.comm.wired;

import com.fazecast.jSerialComm.SerialPort;
import java.util.HashSet;
import java.util.Set;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.ListAndWrite;

/**
 *
 * @author jvanek
 */
public class PortWork implements ListAndWrite {

    @Override
    public void writeToDevice(String id, byte[]... b) {
        writeTo(id, b);
    }

    @Override
    public ConnectionID[] listDevices() {
        return list();
    }

    //9600 is super safe
    private static final int BAUD_RATE = 38400;

    public static void main(String... s) throws Exception {
        ConnectionID[] a = list();
        for (ConnectionID a1 : a) {
            GuiLogHelper.guiLogger.logo(a1.toString());

        }
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
        writeTo("ttyUSB0", testData);

    }

    private static void writeTo(String port, byte[]... bytes) {
        int u = getUsage(port);
        for (int i = 0; i < u; i++) {
            writeToImpl(SerialPort.getCommPort(port), bytes);
        }
    }

    private static void writeTo(SerialPort port, byte[]... bytes) {
        int u = getUsage(port.getSystemPortName());
        for (int i = 0; i < u; i++) {
            writeToImpl(port, bytes);
        }
    }

    private static void writeToImpl(SerialPort comPort, byte[]... b) {

        comPort.setBaudRate(BAUD_RATE);
        try {
            comPort.openPort();
            Thread.sleep(100);
            for (byte[] byteArray : b) {
                writeByteByByte(byteArray, comPort);
                GuiLogHelper.guiLogger.logo("[wired]written -  " + byteArray.length);
            }
            GuiLogHelper.guiLogger.logo("[wired]written - end - " + b.length);
        } catch (Exception e) {
            GuiLogHelper.guiLogger.loge(e);
        }
        comPort.closePort();
    }

    public static void writeByteByByte(byte[] byteArray, SerialPort comPort) throws InterruptedException {
        for (int i = 0; i < byteArray.length; i++) {
            byte c = byteArray[i]; //arduino have very small buffer (so no need to send byte by byte until 64bytes, but this should serve for some 400
            comPort.writeBytes(new byte[]{c}, 1);
            Thread.sleep(2);//we sleep 1 in arduino
        }
    }

    private static Set<String> usages = new HashSet<>();

    //First usage of each port is lost
    private static int getUsage(String port) {
        if (usages.contains(port)) {
            return 1;
        } else {
            usages.add(port);
            return 2;
        }
    }

    private static ConnectionID[] list() {
        SerialPort[] ports = SerialPort.getCommPorts();
        ConnectionID[] a = new ConnectionID[ports.length];
        for (int i = 0; i < ports.length; i++) {
            SerialPort port = ports[i];
            a[i] = new ConnectionID(port.getSystemPortName(), port.getDescriptivePortName());
        }
        return a;
    }

}
