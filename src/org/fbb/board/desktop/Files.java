/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fbb.board.internals.Boulder;

/**
 *
 * @author jvanek
 */
public class Files {

    private static final File homeDir = new File(System.getProperty("user.home"));
    private static final File configDir = new File(homeDir + "/.config/FlashBoard");
    public static final File wallsDir = new File(configDir + "/walls");
    public static final File bouldersDir = new File(configDir + "/boulders");
    private static final File lastBoard = new File(configDir + "/lastBoard");
    private static final File lastBoulder = new File(configDir + "/lastBoulder");

    public static final List<Character> INVALID_PATH = Arrays.asList(new Character[]{':', '*', '?', '"', '<', '>', '|', '[', ']', '\'', ';', '=', ','});
    public static final List<Character> INVALID_NAME = new ArrayList<>(INVALID_PATH);

    static {
        INVALID_NAME.add(0, '\\');
        INVALID_NAME.add(0, '/');
        INVALID_NAME.add(0, ' ');
        INVALID_NAME.add(0, '\t');
        INVALID_NAME.add(0, '\n');
    }
    private static final char SANITIZED_CHAR = '_';

    public static String sanitizeFileName(String filename) {
        return sanitizeFileName(filename, SANITIZED_CHAR);
    }

    private static String sanitizeFileName(String filename, char substitute) {

        for (int i = 0; i < INVALID_NAME.size(); i++) {
            if (-1 != filename.indexOf(INVALID_NAME.get(i))) {
                filename = filename.replace(INVALID_NAME.get(i), substitute);
            }
        }

        return filename;
    }

    public static void setLastBoard(String content) throws IOException {
        if (content == null && lastBoard.exists()) {
            lastBoard.delete();
            return;
        }
        lastBoard.getParentFile().mkdirs();
        java.nio.file.Files.write(lastBoard.toPath(), Arrays.asList(new String[]{content}));
    }

    public static File getLastAppliedFilterFile() {
        configDir.mkdirs();
        return new File(configDir, "lastAppliedFilter");
    }
    public static File getLAstUsedFilterFile() {
        configDir.mkdirs();
        return new File(configDir, "lastUsedFilter");
    }

    public static void setLastBoulder(String content) throws IOException {
        if (content == null && lastBoard.exists()) {
            lastBoulder.delete();
            return;
        }
        lastBoulder.getParentFile().mkdirs();
        java.nio.file.Files.write(lastBoulder.toPath(), Arrays.asList(new String[]{content}));
    }

    public static String getLastBoard() throws IOException {
        if (lastBoard.exists()) {
            List<String> s = java.nio.file.Files.readAllLines(lastBoard.toPath());
            for (String item : s) {
                if (!item.trim().isEmpty()) {
                    if (Files.getWallFile(item).exists()) {
                        return item;
                    } else {
                        lastBoard.delete();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static String getLastBoulder() throws IOException {
        if (lastBoulder.exists()) {
            List<String> s = java.nio.file.Files.readAllLines(lastBoulder.toPath());
            for (String item : s) {
                if (!item.trim().isEmpty()) {
                    if (Files.getBoulderFile(item).exists()) {
                        return item;
                    } else {
                        lastBoulder.delete();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static void setLastBoulder(Boulder b) {
        if (b == null && lastBoard.exists()) {
            lastBoulder.delete();
            return;
        }
        try {
            if (b != null) {
                if (b.getFile() != null) {
                    setLastBoulder(b.getFile().getName());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static File getWallFile(String n) {
        return new File(Files.wallsDir, n);
    }

    public static File getBoulderFile(String n) {
        return new File(Files.bouldersDir, n);
    }
}
