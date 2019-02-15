/*
 sudo  usermod -aG uucp jvanek
 sudo  usermod -aG lock jvanek
 sudo chown root:uucp  /run/lock
 sudo chmod 775 /run/lock

 */
package org.fbb.board.internals.comm.wired;

import com.fazecast.jSerialComm.SerialPort;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jvanek
 */
public class PortWork {

    public static void main(String... s) throws Exception {
        SerialPort[] a = SerialPort.getCommPorts();
        for (SerialPort a1 : a) {
            System.out.println(a1.getDescriptivePortName());

        }
        byte[] testData = new byte[]{
            (byte) 5, 0, 0,
            (byte) 5, 0, 0,
            (byte) 0, 5, 0,
            (byte) 0, 5, 0,
            (byte) 0, 0, 5,
            (byte) 0, 0, 5,
            (byte) 5, 5, 0,
            (byte) 0, 5, 5,
            (byte) 5, 0, 5,
            (byte) 5, 5, 5
        };

        writeTo("/dev/ttyUSB0", testData);
        //writeTo(a[0], testData);

    }

    public static void writeTo(String port, byte[]... bytes) {
        int u = getUsage(port);
        for (int i = 0; i < u; i++) {
            writeToImpl(SerialPort.getCommPort(port), bytes);
        }
    }

    public static void writeTo(SerialPort port, byte[]... bytes) {
        int u = getUsage(port.getSystemPortName());
        for (int i = 0; i < u; i++) {
            writeToImpl(port, bytes);
        }
    }

    public static void writeToImpl(SerialPort comPort, byte[]... b) {

        comPort.setBaudRate(9600);
        try {
            comPort.openPort();
            Thread.sleep(100);
            for (byte[] byteArray : b) {
                comPort.writeBytes(byteArray, byteArray.length);
                System.out.println("written -  " + byteArray.length);
                Thread.sleep(10);
            }
            System.out.println("written - end - " + b.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
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

}
