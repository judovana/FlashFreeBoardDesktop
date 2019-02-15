/*
 sudo  usermod -aG uucp jvanek
 sudo  usermod -aG lock jvanek
 sudo chown root:uucp  /run/lock
 sudo chmod 775 /run/lock

 */
package org.fbb.board.internals.comm.wired;

import com.fazecast.jSerialComm.SerialPort;

/**
 *
 * @author jvanek
 */
public class PortWork {

    //for some reason firs tocmmunication is always ignored
    private static int firstUsage = 2;

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

        for (int i = 0; i < firstUsage; i++) {
            //writeTo("/dev/ttyUSB0", testData);
            writeTo(a[0], testData);
        }
        firstUsage = 1;

    }

    public static void writeTo(String port, byte[]... bytes) {
        writeTo(SerialPort.getCommPort(port), bytes);
    }

    public static void writeTo(SerialPort comPort, byte[]... b) {

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

}
