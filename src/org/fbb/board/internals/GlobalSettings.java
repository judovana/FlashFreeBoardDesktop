/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.bt.BtOp;
import org.fbb.board.internals.comm.ByteEater;
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
        load();
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
        setPortType(selectedIndex, true);
    }

    private void setPortType(int selectedIndex, boolean save) {
        switch (selectedIndex) {
            case 0:
                comm = COMM.PORT;
                break;
            case 1:
                comm = COMM.BLUETOOTH;
                break;
            default:
                comm = COMM.NOTHING;
                break;
        }
        if (save) {
            save();
        }
    }

    public int getPortTypeIndex() {
        if (null == comm) {
            return 2;
        } else {
            switch (comm) {
                case PORT:
                    return 0;
                case BLUETOOTH:
                    return 1;
                default:
                    return 2;
            }
        }
    }

    public String getPortId() {
        return deviceId;
    }

    private class MessagesResender extends Thread {

        public MessagesResender() {
            this.setDaemon(true);
        }

        boolean alive = true;

        @Override
        public void run() {
            while (true) {
                try {
                    runImp();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
            if (null == comm) {
                System.err.println("Nothing mode");
            } else {
                switch (comm) {
                    case PORT:
                        new PortWork().writeToDevice(deviceId, l);
                        break;
                    case BLUETOOTH:
                        new BtOp().writeToDevice(deviceId, l);
                        break;
                    default:
                        System.err.println("Nothing mode");
                        break;
                }
            }
        }
    }

    private byte[][] last = null;
    private final Object lock = new Object();

    @Override
    public void sendBytes(int[]... b) {
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

    private byte[][] toMessages(int[][] bs) {
        byte[][] r = new byte[bs.length][];
        for (int i = 0; i < bs.length; i++) {
            r[i] = toMessage(bs[i]);

        }
        return r;
    }

    private byte[] toMessage(int[] b) {
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

    private void load() {
        if (!Files.settings.exists()) {
            return;
        }
        try {
            loadIpl();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadIpl() throws IOException {
        Properties p = new Properties();
        p.load(new FileInputStream(Files.settings));
        setPortType(Integer.valueOf(p.getProperty("COMM", "0")), false);
        setBrightness(Integer.valueOf(p.getProperty("SHINE", "0")), false);
        setDeviceId((p.getProperty("URL", "/dev/ttyUSB0")), false);
    }

    private void save() {
        try {
            saveImpl();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveImpl() throws IOException {
        Properties p = new Properties();
        p.setProperty("COMM", "" + getPortTypeIndex());
        p.setProperty("SHINE", "" + getBrightness());
        p.setProperty("URL", getPortId());
        p.store(new OutputStreamWriter(new FileOutputStream(Files.settings), Charset.forName("utf-8")), "FlashFreeBoard settings " + new Date());
    }

    private COMM comm = COMM.PORT;
    private String deviceId = "/dev/ttyUSB0";
    //private String deviceId = "btspp://000666C0AC62:1;authenticate=false;encrypt=false;master=true";
    private int brightness = 5;

    public int getBrightness() {
        if (brightness <= 1) {
            return 1;
        }
        if (brightness >= 255) {
            return 255;
        }
        return brightness;
    }

    public void setBrightness(int brightness) {
        setBrightness(brightness, true);
    }

    private void setBrightness(int brightness, boolean save) {
            if (brightness <= 1) {
            brightness = 1;
        }
        if (brightness >= 255) {
            brightness = 255;
        }
        this.brightness = brightness;
        if (save) {
            save();
        }
    }

    //0 nothing
    //1 blue
    //2 green
    //3 red
    public byte[] holdToColor(int i) {
        switch (i) {
            case (0):
                return new byte[]{0, 0, 0};
            case (1):
                return new byte[]{0, 0, (byte)brightness};
            case (2):
                return new byte[]{0, (byte)brightness, 0};
            case (3):
                return new byte[]{(byte)brightness, 0, 0};
        }
        return null;
    }

    public void setDeviceId(String deviceId) {
        setDeviceId(deviceId, true);
    }

    private void setDeviceId(String deviceId, boolean save) {
        this.deviceId = deviceId;
        if (save) {
            save();
        }
    }

}
