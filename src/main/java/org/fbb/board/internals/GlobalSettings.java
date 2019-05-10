/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.Color;
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

    private final MessagesResender resender;

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

    public Color getPathColor() {
        //int br = brightness;
        int br = 250;
        int[] b = new int[]{(int) (((double) br) * parts[3]), (int) (((double) br) * parts[4]), (int) (((double) br) * parts[5])};
        return new Color(b[0], b[1], b[2]);

    }

    public Color getStartColor() {
        //int br = brightness;
        int br = 250;
        int[] b = new int[]{(int) (((double) br) * parts[0]), (int) (((double) br) * parts[1]), (int) (((double) br) * parts[2])};
        return new Color(b[0], b[1], b[2]);
    }

    public Color getTopColor() {
        //int br = brightness;
        int br = 250;
        int[] b = new int[]{(int) (((double) br) * parts[6]), (int) (((double) br) * parts[7]), (int) (((double) br) * parts[8])};
        return new Color(b[0], b[1], b[2]);
    }

    public void setPullerDelay(int u) {
        setPullerDelay(u, true);
    }

    private void setPullerDelay(int u, boolean save) {
        this.pullerDelay = u;
        if (save) {
            save();
        }
    }

    public int getPullerDelay() {
        return pullerDelay;
    }

    public double getSingleRgbLedAmpers() {
        return singleLed;
    }

    public double getSingleSourceAmpers() {
        return singleSource;
    }

    public int getNumberOfSources() {
        return numberOfSources;
    }

    public void setSingleRgbLedAmpers(double i) {
        setSingleRgbLedAmpers(i, true);
    }

    public void setSingleSourceAmpers(double i) {
        setSingleSourceAmpers(i, true);
    }

    public void setNumberOfSources(int i) {
        setNumberOfSources(i, true);
    }

    private void setSingleRgbLedAmpers(double i, boolean save) {
        singleLed = i;
        if (save) {
            save();
        }
    }

    private void setSingleSourceAmpers(double i, boolean save) {
        singleSource = i;
        if (save) {
            save();
        }
    }

    private void setNumberOfSources(int i, boolean save) {
        numberOfSources = i;
        if (save) {
            save();
        }
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
                    GuiLogHelper.guiLogger.loge(ex);
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
                GuiLogHelper.guiLogger.loge("Nothing mode");
            } else {
                switch (comm) {
                    case PORT:
                        new PortWork().writeToDevice(deviceId, l);
                        break;
                    case BLUETOOTH:
                        new BtOp().writeToDevice(deviceId, l);
                        break;
                    default:
                        GuiLogHelper.guiLogger.loge("Nothing mode");
                        break;
                }
            }
        }
    }

    private byte[][] last = null;
    private final Object lock = new Object();

    //if arduino is in WITH_HEADER mode, 
    private static final boolean SEND_HEADER = true;
    //this header is for method "read known numbers of  bytes"
    private static final int[] ALL_COLORS_HEADER = new int[]{
        250, 50, 150, 200, 5, 139, 144, 250
    };
    //currently unused in java part. However BT seems to be sending some garbage, so thi sis that garbage and reaction
    private static final int[] RESET_HEADER = new int[]{
        255, 255, 255, 255, 255, 255, 255, 255
    };

    @Override
    public void sendBytes(int[] b) {
        sendImpl(ALL_COLORS_HEADER, b);
    }

    public void sendImpl(int[] header, int[] b) {
        synchronized (lock) {
            byte[][] m;
            if (SEND_HEADER) {
                m = toMessagesWithHeader(header, b);
            } else {
                m = toMessages(b);
            }
            last = m;
            lock.notify();
        }
    }

    @Override
    public void reset() {
        sendImpl(RESET_HEADER, new int[0]);
    }

    public void stop() {
        synchronized (lock) {
            resender.alive = false;
            lock.notify();
        }
    }

    private byte[][] toMessagesWithHeader(int[] header, int[]... content) {
        byte[][] r = new byte[content.length + 1][];
        r[0] = new byte[header.length];
        for (int i = 0; i < header.length; i++) {
            r[0][i] = (byte) header[i];
        }
        for (int i = 0; i < content.length; i++) {
            r[i + 1] = toMessage(content[i]);
        }
        return r;
    }

    private byte[][] toMessages(int[]... bs) {
        byte[][] r = new byte[bs.length][];
        for (int i = 0; i < bs.length; i++) {
            r[i] = toMessage(bs[i]);
        }
        return r;
    }

    private byte[] toMessage(int[] b) {
        // based on AmpsPerLed  and numberOfSources 
        // calc the overvoltage (where X=totalOfleds/numberOfSOurces)
        // note that "one" led is eating its full amps only on WHITE!! (so coeficients from holdToColor have its weight use that method!?!?!)
        // calc brightnress lowerer coeficient for each  set of X (considering X is number of diods on separate power source)
        // send it to holdToColor
        //for info, calc (in settings where AmpsPerLed  ands numberOfSources  are set ) total ampers of given wall
        //print it also in wall setup
        //ampers note: acording to docs, the one rgb led have 0.06Amps (so 3*0.02); - https://learn.adafruit.com/1500-neopixel-led-curtain-with-raspberry-pi-fadecandy/power-topology
        //acording to my meassuring it is 0.18 (3*0.06) AMPS (maybe waires stepped in?)
        //thus 100 white leds is consumming 18AMPS and that is  why they work max on 50% (brightenss of 125)
        double ampsPerRGBtriLed = singleLed; //from settings; disabled by zero
        int powersources = numberOfSources; //from settings; disabled by zero
        double powersourcesAmps = singleSource; //from settings; disabled by zero
        int hunkLength = 0;
        double singleSubLed = 0d;
        if (powersources > 0 && ampsPerRGBtriLed > 0 && powersourcesAmps > 0) {
            hunkLength = b.length / powersources;
            singleSubLed = ampsPerRGBtriLed / 3d;
        }
        double overvoltageLoweringCoeficient = 1; //no change
        byte[] r = new byte[b.length * 3];
        for (int i = 0; i < b.length; i++) {
            if (hunkLength > 0) {
                if (i % hunkLength == 0) {
                    double thisRowAmpers = getRowSumm(b, i, hunkLength, singleSubLed);
                    if (thisRowAmpers < powersourcesAmps) {
                        overvoltageLoweringCoeficient = 1;
                    } else {
                        overvoltageLoweringCoeficient = powersourcesAmps / thisRowAmpers;
                        GuiLogHelper.guiLogger.loge("Warning, overvoltage detected - " + i + "-" + (i + hunkLength - 1) + " have " + thisRowAmpers + "ampers => " + overvoltageLoweringCoeficient);
                    }
                }
            }
            byte[] rgb = holdToColor(b[i]);
            r[i * 3] = applyCoefToByte(rgb[0], overvoltageLoweringCoeficient);
            r[i * 3 + 1] = applyCoefToByte(rgb[1], overvoltageLoweringCoeficient);
            r[i * 3 + 2] = applyCoefToByte(rgb[2], overvoltageLoweringCoeficient);
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
            GuiLogHelper.guiLogger.loge(ex);
        }
    }

    private void loadIpl() throws IOException {
        Properties p = new Properties();
        p.load(new FileInputStream(Files.settings));
        setPortType(Integer.valueOf(p.getProperty("COMM", "0")), false);
        setBrightness(Integer.valueOf(p.getProperty("SHINE", "0")), false);
        setDeviceId((p.getProperty("URL", "/dev/ttyUSB0")), false);
        setParts((p.getProperty("COMPOSITION", "0,1,0,0,0,1,1,0,0")));
        setUrl(p.getProperty("RURL", ""), false);
        setBranch(p.getProperty("RBRANCH", ""), false);
        setRuser(p.getProperty("RUSER", ""), false);
        setPullerDelay(Integer.valueOf(p.getProperty("PULLER", "1")), false);
        setSingleRgbLedAmpers(Double.valueOf(p.getProperty("SINGLE_LED", "0.18")), false);
        setSingleSourceAmpers(Double.valueOf(p.getProperty("SINGLE_SOURCE", "2")), false);
        setNumberOfSources(Integer.valueOf(p.getProperty("COUNT_SOURCES", "1")), false);
    }

    private void save() {
        try {
            saveImpl();
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
    }

    private void saveImpl() throws IOException {
        Properties p = new Properties();
        p.setProperty("COMM", "" + getPortTypeIndex());
        p.setProperty("SHINE", "" + getBrightness());
        p.setProperty("URL", getPortId());
        p.setProperty("COMPOSITION", getParts());
        p.setProperty("RURL", getUrl());
        p.setProperty("RBRANCH", getBranch());
        p.setProperty("RUSER", getRuser());
        p.setProperty("PULLER", "" + getPullerDelay());
        p.setProperty("SINGLE_LED", "" + getSingleRgbLedAmpers());
        p.setProperty("SINGLE_SOURCE", "" + getSingleSourceAmpers());
        p.setProperty("COUNT_SOURCES", "" + getNumberOfSources());
        p.store(new OutputStreamWriter(new FileOutputStream(Files.settings), Charset.forName("utf-8")), "FlashFreeBoard settings " + new Date());
    }

    public void setRuser(String u) {
        setRuser(u, true);
    }

    private void setRuser(String u, boolean save) {
        this.remoteUser = u;
        if (save) {
            save();
        }
    }

    public void setBranch(String br) {
        setBranch(br, true);
    }

    private void setBranch(String br, boolean save) {
        this.remoteBranch = br;
        if (save) {
            save();
        }
    }

    public void setUrl(String url) {
        setUrl(url, true);
    }

    private void setUrl(String url, boolean save) {
        this.remoteUrl = url;
        if (save) {
            save();
        }
    }

    public String getUrl() {
        return remoteUrl;
    }

    public String getBranch() {
        return remoteBranch;
    }

    public String getRuser() {
        return remoteUser;
    }

    private String remoteUrl;
    private String remoteBranch;
    private String remoteUser;
    private COMM comm = COMM.PORT;
    private String deviceId = "/dev/ttyUSB0";
    //private String deviceId = "btspp://000666C0AC62:1;authenticate=false;encrypt=false;master=true";
    private int brightness = 5;
    private int pullerDelay = 1; //minutes
    private double singleLed = 0.18d;
    private double singleSource = 2.00d;
    private int numberOfSources = 1;

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
    //startr path top
    //rgb    rgb  rgb
    final double[] parts = new double[]{
        0, 1, 0,
        0, 0, 1,
        1, 0, 0};

    public double getPart(int i) {
        return parts[i];
    }

    public void setPart(double part, int i) {
        this.parts[i] = part;
        save();
    }

    public void setParts(String sparts) {
        String[] r = sparts.trim().split(",");
        for (int i = 0; i < r.length; i++) {
            String r1 = r[i];
            parts[i] = Double.valueOf(r1);
        }
    }

    public String getParts() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            Double part = parts[i];
            sb.append(part.toString()).append(",");

        }
        return sb.toString();
    }

    //0 nothing
    //1 blue - path
    //2 green - start
    //3 red - top
    private byte[] holdToColor(int i) {
        switch (i) {
            case (0):
                return new byte[]{0, 0, 0};
            case (1):
                return new byte[]{(byte) (((double) brightness) * parts[3]), (byte) (((double) brightness) * parts[4]), (byte) (((double) brightness) * parts[5])};
            case (2):
                return new byte[]{(byte) (((double) brightness) * parts[0]), (byte) (((double) brightness) * parts[1]), (byte) (((double) brightness) * parts[2])};
            case (3):
                return new byte[]{(byte) (((double) brightness) * parts[6]), (byte) (((double) brightness) * parts[7]), (byte) (((double) brightness) * parts[8])};
        }
        return null;
    }

    private double getRowSumm(int[] b, int first, int length, double singleSubLedAmpers) {
        double summOfAmpers = 0;
        for (int i = first; i < Math.min(b.length, first + length); i++) {
            int c = b[i];
            byte[] values = holdToColor(c);
            //max is 255+255+255==100% ==  of single  RGBled (3xsubLed)
            for (int j = 0; j < values.length; j++) {
                int value = values[j];
                if (value < 0) {
                    value = value + 255;//byte->int to get 0-255
                }
                summOfAmpers = summOfAmpers + ((double) value / 255d * singleSubLedAmpers);
            }
        }
        return summOfAmpers;
    }

    private static byte applyCoefToByte(byte rgb, double coef) {
        int value = rgb;
        int valueOrig = rgb;
        if (valueOrig < 0) {
            value = value + 255;//byte->int to get 0-255
        }
        value = (int) ((double) value * coef);
        if (valueOrig < 0) {
            value = value - 255;//and back
        }
        return (byte) value;
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
