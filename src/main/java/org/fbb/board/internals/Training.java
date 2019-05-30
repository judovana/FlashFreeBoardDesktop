/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author jvanek
 */
public class Training {

    public static class InnerSettings {

        public final boolean random;
        public final boolean regular;
        public final boolean jumps;
        public final String tOfBoulder;
        public final String tOfTraining;
        public final int numOfBoulders;

        public InnerSettings(boolean random, boolean regular, boolean jumps, String tOfBoulder, String tOfTraining, int numOfBoulders) {
            this.random = random;
            this.regular = regular;
            this.jumps = jumps;
            this.tOfBoulder = tOfBoulder;
            this.tOfTraining = tOfTraining;
            this.numOfBoulders = numOfBoulders;
        }

        @Override
        public String toString() {
            return "random/regular/jumps: " + random + "/" + regular + "/" + jumps + "\n"
                    + "time per boulder/boulders: " + tOfTraining + "/" + numOfBoulders + "\n"
                    + "time: " + tOfTraining;
        }

        private void save(OutputStream zos) throws IOException {
            Properties p = new Properties();
            p.setProperty("RANDOM", "" + random);
            p.setProperty("REGULAR", "" + regular);
            p.setProperty("JUMPS", "" + jumps);
            p.setProperty("TIME_TRAIN", tOfTraining);
            p.setProperty("TIME_BOULD", tOfBoulder);
            p.setProperty("BOULDERS", "" + numOfBoulders);
            p.store(zos, "FlashFreeBoard single timered training" + new Date());
        }

        private static InnerSettings load(InputStream zos) throws IOException {
            Properties p = new Properties();
            p.load(zos);
            boolean random = Boolean.valueOf(p.getProperty("RANDOM", "true"));
            boolean regular = Boolean.valueOf(p.getProperty("REGULAR", "true"));
            boolean jumps = Boolean.valueOf(p.getProperty("JUMPS", "true"));
            String tOfTraining = p.getProperty("TIME_TRAIN", "5:00");
            String tOfBoulder = p.getProperty("TIME_BOULD", "00:20");
            int numOfBoulders = Integer.valueOf(p.getProperty("BOULDERS", "15"));
            return new InnerSettings(random, regular, jumps, tOfBoulder, tOfTraining, numOfBoulders);
        }

    }
    public final InnerSettings innerSettings;
    public final Filter filter;
    public final String currentName;

    @Override
    public String toString() {
        return filter.toString() + "\n"
                + "Start at: " + currentName + "\n"
                + innerSettings.toString();

    }

    public Training(boolean random, boolean regular, boolean jumps, String tOfBoulder, String tOfTraining, int numOfBoulders, Filter filter, String currentName) {
        this.innerSettings = new InnerSettings(random, regular, jumps, tOfBoulder, tOfTraining, numOfBoulders);
        this.filter = filter;
        this.currentName = currentName;
    }

    private Training(InnerSettings innerSettings, Filter filter, String currentName) {
        this.innerSettings = innerSettings;
        this.filter = filter;
        this.currentName = currentName;
    }

    public static Training loadSavedTraining(File f) throws IOException {
        ZipFile zipFile = new ZipFile(f);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        String current = null;
        InnerSettings setup = null;
        Filter filter = null;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            if (entry.getName().equals("last.current")) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                current = result.toString("UTF-8");
            } else if (entry.getName().equals("setup.prop")) {
                setup = InnerSettings.load(stream);
            } else if (entry.getName().equals("filter")) {
                filter = Filter.load(stream);
            }
        }
        return new Training(setup, filter, current);
    }

    public void saveSingleTraining(File out) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        zos.putNextEntry(new ZipEntry("filter"));
        this.filter.write(zos);
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("last.current"));
        zos.write(this.currentName.getBytes("utf-8"));
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("setup.prop"));
        this.innerSettings.save(zos);
        zos.closeEntry();
        zos.flush();
        zos.close();

    }

}
