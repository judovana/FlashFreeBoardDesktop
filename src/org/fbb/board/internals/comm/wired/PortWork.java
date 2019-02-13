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
    
    public static void main(String... s) throws Exception {
        SerialPort[] a = SerialPort.getCommPorts();
        for (SerialPort a1 : a) {
            System.out.println(a1.getDescriptivePortName());
            
        }
        //writeTo("/dev/ttyUSB0", new byte[]{
        writeTo(a[0], new byte[]{
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
        });
        
    }
    
    public static void writeTo(String port, byte[] bytes) {
        writeTo(SerialPort.getCommPort(port), bytes);
    }

    public static void writeTo(SerialPort comPort, byte[] bytes) {
        
        comPort.setBaudRate(9600);
        comPort.openPort();
        try {
            comPort.writeBytes(bytes, bytes.length);
            System.out.println("written");
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
    }
    
}
