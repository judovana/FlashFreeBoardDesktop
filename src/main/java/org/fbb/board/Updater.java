package org.fbb.board;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class Updater {

    private static final String FILE_NAME = "latest-release";
    private static final String exUrl = "https://raw.githubusercontent.com/judovana/FlashFreeBoardDesktop/master/src/main/resources/org/fbb/board/" + FILE_NAME;
    private static final String inPath = "/org/fbb/board/" + FILE_NAME;

    private static URL getExURL() throws MalformedURLException {
        return new URL(exUrl);
    }

    private static URL getInURL() throws MalformedURLException {
        return Updater.class.getResource(inPath);
    }

    private static URL getCachedURL() throws MalformedURLException {
        return getCacheFile().toURI().toURL();
    }

    private static File getCacheFile() throws MalformedURLException {
        return new File(System.getProperty("java.io.tmpdir"), FILE_NAME);
    }

    private static List<String> readFromUrl(URL u) throws IOException {
        List<String> r = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8))) {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }
                s = s.trim();
                if (s.startsWith("#")) {
                    continue;
                }
                r.add(s);
            }
        }
        return r;
    }

    private static void cache(List<String> items) throws IOException {
        save(getCacheFile(), items);
    }

    private static void save(File f, List<String> s) throws IOException {
        try (BufferedWriter in = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            for (String string : s) {
                in.write(string);
                in.newLine();
            }
        }

    }

    private static enum LoadedUrlState {

        NETWORK, CACHE, LOCAL, FATALTY;

        private String getResolution() {
            switch (this) {
                case NETWORK:
                    return "Ok!";
                case CACHE:
                    return "Error!";
                case LOCAL:
                    return "Error!";
                case FATALTY:
                    return "Error!";
            }
            return "Unknown Error explaining state";
        }
    }

    private static class RemoteUrlWithStatus {

        private final List<URL> r;
        private final LoadedUrlState w;

        private RemoteUrlWithStatus(List<String> rr, LoadedUrlState w) throws MalformedURLException {
            r = new ArrayList<>(rr.size());
            for (String string : rr) {
                this.r.add(new URL(string));
            }
            this.w = w;
        }

        public List<String> getResults() {
            List<String> rr = new ArrayList<>(r.size());
            for (URL url : r) {
                rr.add(url.toExternalForm());
            }
            return Collections.unmodifiableList(rr);
        }

        public List<URL> getUrls() {
            return Collections.unmodifiableList(r);
        }

        public URL getUrlJar() {
            return getUrls().get(0);
        }
        
        public URL getUrlArduino() {
            return getUrls().get(1);
        }

        public LoadedUrlState getSource() {
            return w;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(w).append(": ");
            for (URL url : r) {
                sb.append(url.toString()).append(" ");
            }
            return sb.toString();
        }

    }

    private static RemoteUrlWithStatus obtain() throws MalformedURLException {
        List<String> r;
        //network
        try {
            r = readFromUrl(getExURL());
            if (r != null && !r.isEmpty()) {
                try {
                    //save it
                    cache(r);
                    GuiLogHelper.guiLogger.logo("Cache updated sucesfully. " + getCacheFile());
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
                GuiLogHelper.guiLogger.logo("Loaded remote file. " + getExURL());
                return new RemoteUrlWithStatus(r, LoadedUrlState.NETWORK);
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
        //cache
        try {
            r = readFromUrl(getCachedURL());
            if (r != null && !r.isEmpty()) {
                GuiLogHelper.guiLogger.logo("Read from cache: " + getCacheFile());
                return new RemoteUrlWithStatus(r, LoadedUrlState.CACHE);
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }

        try {
            r = readFromUrl(getInURL());
            if (r != null && !r.isEmpty()) {
                GuiLogHelper.guiLogger.logo("Read from app: " + getInURL());
                return new RemoteUrlWithStatus(r, LoadedUrlState.LOCAL);
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
        GuiLogHelper.guiLogger.loge("All ways failed");
        List<String> bl = new ArrayList<>(2);
        bl.add("http://nohting.found/error1");
        bl.add("http://nohting.found/error2");
        return new RemoteUrlWithStatus(bl, LoadedUrlState.FATALTY);
    }

    private static String checkForCurrent() {
        if (Updater.class.getClassLoader() instanceof URLClassLoader) {
            URLClassLoader tHis = (URLClassLoader) Updater.class.getClassLoader();
            URL[] cp = tHis.getURLs();
            for (URL jar : cp) {
                if (jar.getFile().matches(".*FlashFreeBoardDesktop.*\\.jar")) {
                    return jar.getFile();
                }
            }
        }
        return null;
    }

    public static void downloadUsingNIO(URL url, File file) throws IOException {
        GuiLogHelper.guiLogger.loge("Downloading " + url + " as " + file.getAbsolutePath());
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        }
        GuiLogHelper.guiLogger.loge("done");
    }

    public static Update getCurrentVersionInfo() {
        String s = checkForCurrent();
        if (s == null) {
            return null;
        }
        return new Update(null, null, new File(s));
    }

    public static class Update {

        private final URL from;
        private final URL arduino;
        private final File to;

        public Update(URL ard, URL from, File to) {
            this.arduino = ard;
            this.from = from;
            this.to = to;
        }

        public URL getRemoteJar() {
            return from;
        }
        
          public URL getRemoteArduino() {
            return arduino;
        }

        public File getLocal() {
            return to;
        }

        public String getRemoteFileName() {
            return new File(from.getFile()).getName();
        }

        public String getLocalFileName() {
            return to.getName();
        }

        private static final Pattern version = Pattern.compile("[\\d]+\\.[\\d]+");

        public String getRemoteVersionString() {
            return extractVersion(getRemoteFileName());
        }

        public static String extractVersion(String filename) {
            Matcher m = version.matcher(filename);
            m.find();
            return m.group();
        }

        public String getLocalVersionString() {
            return extractVersion(getLocalFileName());
        }

        public double getRemoteVersion() {
            return Double.valueOf(getRemoteVersionString());
        }

        public double getLocalVersion() {
            return Double.valueOf(getLocalVersionString());
        }

        public void downloadJar() throws IOException {
            File target = getDwnldTarget();
            downloadUsingNIO(getRemoteJar(), target);
        }
        
        public File downloadArduino() throws IOException {
            File target = Files.getArduinoFile(getRemoteArduino());
            downloadUsingNIO(getRemoteArduino(), target);
            return target;
        }

        public File getDwnldTarget() {
            File target = new File(getLocal().getParentFile(), getRemoteFileName());
            return target;
        }

    }

    public static Update getUpdatePossibility(final boolean replaceAllowed, final boolean downgradeAllowed) {
        try {
            RemoteUrlWithStatus r = obtain();
            if (r.w == LoadedUrlState.NETWORK) {
                String current = checkForCurrent();
                if (current != null) {
                    File fold = new File(current);
                    String nwName = new File(r.getUrlJar().getFile()).getName();
                    GuiLogHelper.guiLogger.logo("From: " + fold.getName() + "(" + fold.getAbsolutePath() + ")");
                    GuiLogHelper.guiLogger.logo("To  : " + nwName + " (" + r.getUrlJar().toExternalForm() + ")");
                    GuiLogHelper.guiLogger.logo("ard : " + r.getUrlArduino().toExternalForm());
                    if (fold.getName().equals(nwName) && !replaceAllowed) {
                        return null;
                    }
                    Update u = new Update(r.getUrlArduino(), r.getUrlJar(), fold.getAbsoluteFile());
                    if (u.getRemoteVersion() <= u.getLocalVersion() && !downgradeAllowed) {
                        GuiLogHelper.guiLogger.logo(u.getRemoteVersion() + " <= " + u.getLocalVersion() + " : likely no update at all");
                        return null;
                    }
                    return u;
                } else {
                    GuiLogHelper.guiLogger.loge("Nothig to update from");
                    return null;
                }
            } else {
                GuiLogHelper.guiLogger.loge("Nothing to update to");
                return null;
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            return null;
        }
    }

    public static void main(String... args) throws MalformedURLException {
        Update u = getUpdatePossibility(true, true);
        if (u == null) {
            System.out.println(Translator.R("UpdateImpossible"));
        } else {
            System.out.println(Translator.R("CurrentVersion", u.getLocalVersion(), u.to.getAbsolutePath()));
            System.out.println(Translator.R("RemoteVersion", u.getRemoteVersion(), u.from.toExternalForm()));
        }

    }

}
