package org.fbb.board.desktop.gui;

import org.fbb.board.desktop.gui.awtimpl.TipsListener;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;
import javax.swing.JOptionPane;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.gui.awtimpl.CreateWindow;
import org.fbb.board.desktop.gui.awtimpl.MainWindowImpl;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.grid.GridPane;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.db.Puller;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindow {

    public  static final GlobalSettings gs = new GlobalSettings();
    public  static final DB db = new DB(gs);
    public  static final Puller puller = Puller.create(gs.getPullerDelay() * 60, db);
    private static final KeyEventDispatcher f1 = new KeyEventDispatcher() {

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F1 && e.getID() == KeyEvent.KEY_PRESSED) {
                new TipsListener().actionPerformed(null);
                return true;
            }
            return false;
        }
    };

    public static void main(String... s) {
        try {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(f1);
            Grid.colorProvider = gs;
            Grade.loadConversiontable();
            if (Files.getLastBoard() != null && Files.getLastBoulder() != null) {
                Boulder b = Boulder.load(Files.getBoulderFile(Files.getLastBoulder()));
                GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(Files.getWallFile(Files.getLastBoard()))), Files.getLastBoard());
                //check if boards mathces
                if (!b.getWall().equals(preloaded.givenId)) {
                    //if not warn, and ask user ifhe wont to keep wall or boulder. If not load wall only    
                    JOptionPane.showMessageDialog(null, Translator.R("NotMatchingBoulderWall", b.getWall(), preloaded.givenId));
                    File bWall = Files.getWallFile(b.getWall());
                    if (bWall.exists()) {
                        //it is likely that wall had been changed intntionally, and old wall was simply not deleted, and boulder was not updated
                        int a = JOptionPane.showConfirmDialog(null, Translator.R("PossiblyIncorrectLastBoulder"));
                        if (a == JOptionPane.YES_OPTION) {
                            Files.setLastBoulder((String) null);//delete alst boulder info
                            MainWindowImpl.loadWallWithBoulder(Files.getLastBoard());
                        } else {
                            preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(bWall)), bWall.getName());
                            Files.setLastBoard(bWall.getName());
                            MainWindowImpl.loadWallWithBoulder(preloaded, b);
                        }
                    } else {
                        Files.setLastBoulder((String) null);//delete alst boulder info
                        MainWindowImpl.loadWallWithBoulder(Files.getLastBoard());
                    }
                } else {
                    //if so, show last boulder on last wall
                    MainWindowImpl.loadWallWithBoulder(preloaded, b);
                }
            } else if (Files.getLastBoulder() != null && Files.getLastBoard() == null) {
                //warn, but load last boulder on its wall, if wall does noto exists, empty(?)
                GuiLogHelper.guiLogger.loge("Last boulder but not lat wall!");
                Boulder b = Boulder.load(Files.getBoulderFile(Files.getLastBoulder()));
                File bWall = Files.getWallFile(b.getWall());
                if (bWall.exists()) {
                    GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(bWall)), bWall.getName());
                    Files.setLastBoard(bWall.getName());
                    MainWindowImpl.loadWallWithBoulder(preloaded, b);
                } else {
                    CreateWindow.createSelectOrImportWall(LoadBackgroundOrImportOrLoadWall.getDefaultUrl());
                }
            } else if (Files.getLastBoard() != null && Files.getLastBoulder() == null) {
                //load last wall, generate random bouoder
                MainWindowImpl.loadWallWithBoulder(Files.getLastBoard());
            } else {
                //both null, sugest to create board
                CreateWindow.createSelectOrImportWall(LoadBackgroundOrImportOrLoadWall.getDefaultUrl());
            }
        } catch (Exception ex) {
            //do better?
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
        }
    }

}
