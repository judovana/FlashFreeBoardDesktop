/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import javax.swing.ListModel;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.bt.BtOp;
import org.fbb.board.internals.comm.wired.ByteEater;
import org.fbb.board.internals.comm.wired.PortWork;

/**
 *
 * @author jvanek
 */
public class GlobalSettings implements ByteEater {

    private MessagesResender resender;

    public GlobalSettings() {
        resender = new MessagesResender();
        resender.start();
    }

    public ConnectionID[] list() {
        if (comm == COMM.PORT) {
            return new PortWork().listDevices();
        } else if (comm == COMM.BLUETOOTH) {
            return new BtOp().listDevices();
        } else {
            return new ConnectionID[0];
        }
    }

    public void setPortType(int selectedIndex) {
        if (selectedIndex == 0) {
            comm = COMM.PORT;
        } else if (selectedIndex == 1) {
            comm = COMM.BLUETOOTH;
        } else {
            comm = COMM.NOTHING;
        }
    }

    private class MessagesResender extends Thread {

        public MessagesResender() {
            this.setDaemon(true);
        }

        boolean alive = true;

        @Override
        public void run() {
            try {
                runImp();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void runImp() throws InterruptedException {
            byte[][] lastCopyToPrint;
            for (;;) {
                synchronized (lock) {
                    while (last == null && alive) {
                        lock.wait();
                    }
                    if (!alive) {
                        break;
                    }

                    lastCopyToPrint = last;
                    last = null;
                }
                repaintRemote(lastCopyToPrint);
            }
        }

        public void repaintRemote(byte[][] l) {
            if (comm == COMM.PORT) {
                new PortWork().writeToDevice(deviceId, l);
            } else if (comm == COMM.BLUETOOTH) {
                new BtOp().writeToDevice(deviceId, l);
            } else {
                System.err.println("Nothing mode");
            }
        }
    }

    private byte[][] last = null;
    private final Object lock = new Object();

    @Override
    public void sendBytes(byte[]... b) {
        synchronized (lock) {
            byte[][] m = toMessages(b);
            last = m;
            lock.notify();
        }
    }

    public void stop() {
        synchronized (lock) {
            resender.alive = false;
            lock.notify();
        }
    }

    private byte[][] toMessages(byte[][] bs) {
        byte[][] r = new byte[bs.length][];
        for (int i = 0; i < bs.length; i++) {
            byte[] b = bs[i];
            r[i] = toMessage(b);

        }
        return r;
    }

    private byte[] toMessage(byte[] b) {
        byte[] r = new byte[b.length * 3];
        for (int i = 0; i < b.length; i++) {
            byte[] rgb = holdToColor(b[i]);
            r[i * 3] = rgb[0];
            r[i * 3 + 1] = rgb[1];
            r[i * 3 + 2] = rgb[2];
        }
        return r;
    }

    public static enum COMM {

        PORT,
        BLUETOOTH,
        NOTHING
    }

    private COMM comm = COMM.BLUETOOTH;
    //private String deviceId = "/dev/ttyUSB0";
    private String deviceId = "btspp://000666C0AC62:1;authenticate=false;encrypt=false;master=true";
    private byte brightness = 5;

    public byte getBrightness() {
        return brightness;
    }

    public void setBrightness(byte brightness) {
        this.brightness = brightness;
    }

    //0 nothing
    //1 blue
    //2 green
    //3 red
    public byte[] holdToColor(byte i) {
        switch (i) {
            case (0):
                return new byte[]{0, 0, 0};
            case (1):
                return new byte[]{0, 0, brightness};
            case (2):
                return new byte[]{0, brightness, 0};
            case (3):
                return new byte[]{brightness, 0, 0};
        }
        return null;
    }

}
