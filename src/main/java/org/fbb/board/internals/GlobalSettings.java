package org.fbb.board.internals;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.bt.BtOp;
import org.fbb.board.internals.comm.ByteEater;
import org.fbb.board.internals.comm.wired.PortWork;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.grid.HoldMarkerProvider;

/**
 *
 * @author jvanek
 */
public class GlobalSettings implements ByteEater, HoldMarkerProvider {

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

    private boolean shouldCompress(int[] b) {
        int nonzeros = 0;
        for (int c : b) {
            if (isHold(c)) {
                nonzeros++;
            }
        }
        //in seqential send, we send R1 G1 B1 R2 G2 B2 ...  Rn Gn Bn includeing 0 0 0 
        //in ordered sent we sent X1a X1b R1 G1 B1 ...  Xn Xn Rn Gn Bn considering all unsent as 0 0 0
        //considering board of max size  255*255+255 (firs byte is count of 255, second byte is the final alignment. eg 5 will be encoed as 0 5; eg 300 will be endocded as 1 45, max is 255 255
        //thus in sequential we sent 3x length of bytes
        //in worst ordered we sent 5x length of bytes [one byte per coord]
        //however in best ordered we sent 0
        //thus the compression have sense only until length 3/5 of length
        //example length = 100 => 300 x 0-500;  3/5of100 == 60 => 60*5=300
        // arriving 50 colors => 250 if 70 would be allowed then 350 => sewquential
        //example2 length = 400 => 1200 x 0-1500;  3/5of400 == 240 
        // arriving 50 colors => 250 if 70 would be allowed then 350 => sewquential
        // arriving 100 colors => 500 arriving 250 colors that would be 1250, thus sequential
        //the compress mode is sending 5 trailing zeroes to finish its contnet, thus -5 bytes at the end (-2 holds) [max of 2*3 and 2*5)
        return nonzeros < (((b.length * 3) / 5) - 2);
    }

    private static byte[] toArray(List<Byte> l) {
        byte[] array = new byte[l.size()];
        for (int i = 0; i < l.size(); i++) {
            array[i] = l.get(i);
        }
        return array;
    }

    private static byte[] coordToBytes(int i) {
        byte[] r = new byte[2];
        r[0] = (byte) (i / 255);
        r[1] = (byte) (i % 255);
        return r;
    }

    public double getRatio() {
        return mainframeRatio;
    }

    public String getLocation() {
        return mainframeLocation;
    }

    public int getHardcodedEdge() {
        return hardcodedEdge;
    }

    public boolean isResizeAble() {
        return resizeAble;
    }

    public boolean isPopUpping() {
        return popupping;
    }

    public void setPopupping(boolean popupping, boolean save) {
        this.popupping = popupping;
        if (save) {
            save();
        }
    }

    public void setResizeAble(boolean resizeAble, boolean save) {
        this.resizeAble = resizeAble;
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

        private void repaintRemote(byte[][] l) {
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
                        GuiLogHelper.guiLogger.loge("unknown mode");
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
    //this header is for method "read coord and color untill you reach zero color", 
    private static final int[] COLORS_WITH_COORD_HEADER = new int[]{
        255, 0, 255, 255, 255, 0, 112, 124
    };
    //currently unused in java part. However BT seems to be sending some garbage, so thi sis that garbage and reaction
    private static final int[] RESET_HEADER = new int[]{
        255, 255, 255, 255, 255, 255, 255, 255
    };

    private final HashMap<Integer, int[]> providers = new HashMap<>();

    private int[] mergeProviders() {
        Collection<int[]> values = providers.values();
        int max = 0;
        for (int[] value : values) {
            if (value.length > max) {
                max = value.length;
            }
        }
        int[] merged = new int[max];
        for (int i = 0; i < merged.length; i++) {
            merged[i] = 0;
        }
        for (int[] value : values) {
            for (int i = 0; i < value.length; i++) {
                merged[i] = merge(merged[i], value[i]);
            }
        }
        return merged;
    }

    private int merge(int merged, int value) {
        if (value > 0) {
            if (merged == 0) {
                return value;
            } else {
                return 100;
            }
        }
        return merged;
    }

    @Override
    public void sendBytes(int[] get, Grid id) {
        providers.put(id.sortableId, get);
        resendBytes(mergeProviders());
    }

    public void resendBytes(int[] b) {
        boolean compress = shouldCompress(b);
        if (b.length == 0) {
            //not sure, but maybe the sendig of nothig can break somethig later
            return;
        }
        if (!compress) {
            sendImpl(ALL_COLORS_HEADER, b, false);
        } else {
            sendImpl(COLORS_WITH_COORD_HEADER, b, true);
        }
    }

    @Override
    public void deregisterProvider(Grid id) {
        providers.remove(id.sortableId);
        resendBytes(mergeProviders());
    }

    private void sendImpl(int[] header, int[] b, boolean compress) {
        synchronized (lock) {
            byte[][] m;
            if (SEND_HEADER) {
                m = toMessagesWithHeader(compress, header, b);
            } else {
                m = toMessages(b);
            }
            last = m;
            lock.notify();
        }
    }

    @Override
    public void reset() {
        sendImpl(RESET_HEADER, new int[0], false);
    }

    public void stop() {
        synchronized (lock) {
            resender.alive = false;
            lock.notify();
        }
    }

    private byte[][] toMessagesWithHeader(boolean compress, int[] header, int[]... content) {
        int headerArrays = 1;
        int tailArrays = 0;
        if (compress) {
            tailArrays = 1;
        }
        byte[][] r = new byte[content.length + headerArrays + tailArrays][];
        r[0] = new byte[header.length];
        for (int i = 0; i < header.length; i++) {
            r[0][i] = (byte) header[i];
        }
        for (int i = 0; i < content.length; i++) {
            r[i + 1] = toMessage(compress, content[i]);
        }
        if (compress) {
            r[r.length - 1] = new byte[]{(byte) 255, (byte) 254, (byte) 0, (byte) 255, (byte) 0};

        }
        return r;
    }

    private byte[][] toMessages(int[]... bs) {
        byte[][] r = new byte[bs.length][];
        for (int i = 0; i < bs.length; i++) {
            r[i] = toMessage(false, bs[i]);
        }
        return r;
    }

    private byte[] toMessage(boolean compress, int[] b) {
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
        List<Byte> r = new ArrayList(b.length * 3);
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
            //record every member in uncompressed mode
            //record only hold member in compressed mode
            if (!compress || isHold(b[i])) {
                if (compress) {
                    //coords only for compressed mode
                    r.add(coordToBytes(i)[0]);
                    r.add(coordToBytes(i)[1]);
                }
                r.add(applyCoefToByte(rgb[0], overvoltageLoweringCoeficient));
                r.add(applyCoefToByte(rgb[1], overvoltageLoweringCoeficient));
                r.add(applyCoefToByte(rgb[2], overvoltageLoweringCoeficient));
            }
        }
        if (compress) {
            GuiLogHelper.guiLogger.logo("Compressed " + (b.length * 3) + " -> " + r.size());
        }
        return toArray(r);
    }

    public static enum COMM {

        PORT,
        BLUETOOTH,
        NOTHING
    }

    private void load() {
        if (!Files.getSettings().exists()) {
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
        try (FileInputStream fs = new FileInputStream(Files.getSettings())) {
            p.load(fs);
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
            setHoldMarkerOapcity(Float.valueOf(p.getProperty("HOLD_OPACITY", "0.75")), false);
            setDefaultStyle(Integer.valueOf(p.getProperty("HOLD_STYLE", "0")), false);
            setRatio(Double.valueOf(p.getProperty("MAINFRAME_RATIO", "0.8")), false);
            setLocation(p.getProperty("MAINFRAME_LOCATION", "C"), false);
            setHardcodedEdge(Integer.valueOf(p.getProperty("HARDCODED_EDGE", "0")), false);
            setResizeAble(Boolean.valueOf(p.getProperty("RESIZE_ABLE", "true")), false);
            setPopupping(Boolean.valueOf(p.getProperty("POPUPPING", "false")), false);
        }
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
        p.setProperty("HOLD_OPACITY", "" + getHoldMarkerOapcity());
        p.setProperty("HOLD_STYLE", "" + getDefaultStyle());
        p.setProperty("MAINFRAME_RATIO", "" + getRatio());
        p.setProperty("MAINFRAME_LOCATION", "" + getLocation());
        p.setProperty("HARDCODED_EDGE", "" + getHardcodedEdge());
        p.setProperty("RESIZE_ABLE", "" + isResizeAble());
        p.setProperty("POPUPPING", "" + isPopUpping());
        try (OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(Files.getSettings()), Charset.forName("utf-8"))) {
            p.store(os, "FlashFreeBoard settings " + new Date() + " ;  TODO: MAINFRAME_RATIO, MAINFRAME_LOCATION HARDCODED_EDGE gui");
        };
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

    private String remoteUrl = "";
    private String remoteBranch = "";
    private String remoteUser = "";
    private COMM comm = COMM.PORT;
    private String deviceId = "/dev/ttyUSB0";
    //private String deviceId = "btspp://000666C0AC62:1;authenticate=false;encrypt=false;master=true";
    private int brightness = 5;
    private int pullerDelay = 1; //minutes
    private double singleLed = 0.18d;
    private double singleSource = 2.00d;
    private int numberOfSources = 1;
    private double mainframeRatio = 0.8;
    private int hardcodedEdge = 0;
    private String mainframeLocation = "C";  //T,B,L,R,C, TR, TL, BR, BL //me lazy, sorry:(
    private boolean resizeAble = true;
    private boolean popupping = false;

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

    private static boolean isHold(int i) {
        return i != 0;
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
            //crossing of holds is simple white
            case (100):
                return new byte[]{(byte) (((double) brightness) * 1d), (byte) (((double) brightness) * 1d), (byte) (((double) brightness) * 1d)};
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

    //color provider
    //todo - allow overwrite? To allow set nicer ones once leds needs to be different?
    @Override
    public float getStartRed() {
        return (float) parts[0];
    }

    @Override
    public float getStartGreen() {
        return (float) parts[1];
    }

    @Override
    public float getStartBlue() {
        return (float) parts[2];
    }

    @Override
    public float getPathRed() {
        return (float) parts[3];
    }

    @Override
    public float getPathGreen() {
        return (float) parts[4];
    }

    @Override
    public float getPathBlue() {
        return (float) parts[5];
    }

    @Override
    public float getTopRed() {
        return (float) parts[6];
    }

    @Override
    public float getTopGreen() {
        return (float) parts[7];
    }

    @Override
    public float getTopBlue() {
        return (float) parts[8];
    }

    private float holdOpacity = 0.75f;

    @Override
    public float getHoldMarkerOapcity() {
        return holdOpacity;
    }

    public void setHoldMarkerOapcity(float f) {
        setHoldMarkerOapcity(f, true);
    }

    private void setHoldMarkerOapcity(float f, boolean save) {
        this.holdOpacity = f;
        if (save) {
            save();
        }
    }

    private void setRatio(double f, boolean save) {
        this.mainframeRatio = f;
        if (save) {
            save();
        }
    }

    private void setLocation(String f, boolean save) {
        this.mainframeLocation = f;
        if (save) {
            save();
        }
    }

    private void setHardcodedEdge(int f, boolean save) {
        this.hardcodedEdge = f;
        if (save) {
            save();
        }
    }

    @Override
    public Color getGridColor() {
        //todo set and save?
        return Color.black;
    }

    private int defaultStyle = 0;

    @Override
    public int getDefaultStyle() {
        return defaultStyle;
    }

    @Override
    public void setDefaultStyle(int a) {
        setDefaultStyle(a, true);
    }

    public void setDefaultStyle(int a, boolean save) {
        defaultStyle = a;
        currentStyle = a + 800;//nasty hack to avoid runnign around zero
        if (save) {
            save();
        }
    }

    private int currentStyle = 800;//nasty hack to avoid runnign around zero

    @Override
    public int getCurrentStyle() {
        return currentStyle;
    }

    @Override
    public void setCurrentStyle(int a) {
        currentStyle = a;
    }

}
