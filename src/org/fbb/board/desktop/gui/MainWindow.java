package org.fbb.board.desktop.gui;

import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;

/**
 *
 * @author jvanek
 */
public class MainWindow {

    public static void main(String... s) {
        try {
            if (Files.getLastBoard() != null && Files.getLastBoulder() != null) {
                //check if boards mathces
                //if so, show last boulder on last wall
                //if not warn, load boulder on its board if  its board exists || load wall only
            } else if (Files.getLastBoulder() != null && Files.getLastBoard() == null) {
                //warn, but load last boulder on its wall, if wall does noto exists, empty
            } else if (Files.getLastBoard() != null && Files.getLastBoulder() == null) {
                //load last wall, sugest to create boulder or generate random one
            } else {
                //both null, sugest to create board
                createSelectOrImportWall();
            }
        } catch (IOException ex) {
            //do better
            ex.printStackTrace();
        }
    }

    private static void createSelectOrImportWall() {
        JFrame f = new JFrame(Translator.R("MainWindowSetWall"));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new LoadBackgroundOrImportOrLoadWall();
        f.add(panel);
        f.setSize(500, 200);
        f.setVisible(true);
             
    }

}
