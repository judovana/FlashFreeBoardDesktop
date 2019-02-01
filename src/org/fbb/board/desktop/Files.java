/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    public static void setLastBoard(String content) throws IOException {
        lastBoard.getParentFile().mkdirs();
        java.nio.file.Files.write(lastBoard.toPath(), Arrays.asList(new String[]{content}));
    }

    public static void setLastBoulder(String content) throws IOException {
        lastBoulder.getParentFile().mkdirs();
        java.nio.file.Files.write(lastBoulder.toPath(), Arrays.asList(new String[]{content}));
    }

    public static String getLastBoard() throws IOException {
        if (lastBoard.exists()) {
            List<String> s = java.nio.file.Files.readAllLines(lastBoard.toPath());
            for (String item : s) {
                if (!item.trim().isEmpty()) {
                    return item;
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
                    return item;
                }
            }
        }
        return null;
    }
}
