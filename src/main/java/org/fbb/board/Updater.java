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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static String readFromUrl(URL u) throws IOException {
        String r = "";
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
                r = r + "" + s;
            }
        }
        return r;
    }

    private static void cache(String items) throws IOException {
        save(getCacheFile(), items);
    }

    private static void save(File f, String s) throws IOException {
        try (BufferedWriter in = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            in.write(s);
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

        private final URL r;
        private final LoadedUrlState w;

        private RemoteUrlWithStatus(String r, LoadedUrlState w) throws MalformedURLException {
            this.r = new URL(r);
            this.w = w;
        }

        public String getResult() {
            return r.toExternalForm();
        }

        public URL getUrl() {
            return r;
        }

        public LoadedUrlState getSource() {
            return w;
        }

        @Override
        public String toString() {
            return w + ": " + r;
        }

    }

    private static RemoteUrlWithStatus obtain() throws MalformedURLException {
        String r;
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
        return new RemoteUrlWithStatus("http://nohting.found/error", LoadedUrlState.FATALTY);
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

    public static class Update {

        private final URL from;
        private final File to;

        public Update(URL from, File to) {
            this.from = from;
            this.to = to;
        }

        public URL getRemote() {
            return from;
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
            Matcher m = version.matcher(getRemoteFileName());
            m.find();
            return m.group();
        }

        public String getLocalVersionString() {
            Matcher m = version.matcher(getLocalFileName());
            m.find();
            return m.group();
        }

        public double getRemoteVersion() {
            return Double.valueOf(getRemoteVersionString());
        }

        public double getLocalVersion() {
            return Double.valueOf(getLocalVersionString());
        }

        public void download() throws IOException {
            File target = getDwnldTarget();
            downloadUsingNIO(getRemote(), target);
        }

        public File getDwnldTarget() {
            File target = new File(getLocal().getParentFile(), getRemoteFileName());
            return target;
        }

    }

    public static Update getUpdatePossibility() {
        try {
            RemoteUrlWithStatus r = obtain();
            if (r.w == LoadedUrlState.NETWORK) {
                String current = checkForCurrent();
                if (current != null) {
                    File fold = new File(current);
                    String nwName = new File(r.getUrl().getFile()).getName();
                    GuiLogHelper.guiLogger.logo("From: " + fold.getName() + "(" + fold.getAbsolutePath() + ")");
                    GuiLogHelper.guiLogger.logo("To  : " + nwName + " (" + r.getUrl().toExternalForm() + ")");
                    if (fold.getName().equals(nwName)) {
                        return null;
                    }
                    Update u = new Update(r.getUrl(), fold.getAbsoluteFile());
                    if (u.getRemoteVersion() <= u.getLocalVersion()) {
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
        Update u = getUpdatePossibility();
        if (u == null) {
            System.out.println(Translator.R("UpdateImpossible"));
        } else {
            System.out.println(Translator.R("CurrentVersion", u.getLocalVersion(), u.to.getAbsolutePath()));
            System.out.println(Translator.R("RemoteVersion", u.getRemoteVersion(), u.from.toExternalForm()));
        }

    }

}
