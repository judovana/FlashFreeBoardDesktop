/*
 sudo  usermod -aG uucp jvanek
 sudo  usermod -aG lock jvanek

 */
package org.fbb.board.internals.comm.wired;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author jvanek
 */
public class PortWork {

    public static void main(String... s) throws Exception {
        String[] a = getPortNames();
        for (String a1 : a) {
            System.out.println(a1);

        }
        writeTo("/dev/ttyUSB0", "a".getBytes());

    }

    /**
     * gets a list of all available serial port names
     *
     * @return a platform dependent list of strings representing all the
     * available serial ports -- it is the application's responsibility to
     * identify the right port to which the device is actually connected
     */
    public static String[] getPortNames() {
        List<String> ret = new ArrayList<>();
        Enumeration<CommPortIdentifier> portsEnum = CommPortIdentifier.getPortIdentifiers();
        while (portsEnum.hasMoreElements()) {
            CommPortIdentifier pid = portsEnum.nextElement();
            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                ret.add(pid.getName());
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    public static void writeTo(String port, byte[] bytes) {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {

            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                if (portId.getName().equals(port)) {

                    try {
                        SerialPort serialPort = (SerialPort) portId.open("SimpleWriteApp", 2000);
                        Thread.sleep(500);
                        OutputStream outputStream = serialPort.getOutputStream();

                        serialPort.setSerialPortParams(9600,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);

                        outputStream.write(bytes);
                        Thread.sleep(500);
                        outputStream.write(bytes);
                        Thread.sleep(500);
                        outputStream.write(bytes);
                        Thread.sleep(500);
                        outputStream.write(bytes);
                        Thread.sleep(500);
                        outputStream.write(bytes);
                        Thread.sleep(500);
                        outputStream.write(bytes);
                        System.out.println("written");
                        //outputStream.flush();  this causes sigsev!!!!
                        outputStream.close();
                        serialPort.close();
                    } catch (IOException | UnsupportedCommOperationException | PortInUseException  | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
