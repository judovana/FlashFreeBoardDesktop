package org.fbb.board.desktop.gui;

import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GridPane;

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

    private static void createSelectOrImportWall() throws IOException {
        JDialog f = new JDialog((JFrame) null, Translator.R("MainWindowSetWall"), true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        LoadBackgroundOrImportOrLoadWall panel = new LoadBackgroundOrImportOrLoadWall();
        f.add(panel);
        panel.setOkAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //validate first?
                f.setVisible(false);
            }
        });
        f.setSize(500, 200);
        f.setVisible(true);
        URL r = panel.getResult();
        f.dispose();
        InputStream is = r.openStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        try {
            BufferedImage bis = ImageIO.read(new ByteArrayInputStream(buffer.toByteArray()));
            if (bis == null) {
                throw new NullPointerException("Not a valid image");
            }
            final JFrame createWallWindow = new JFrame();
            createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            GridPane gp = new GridPane(bis);
            createWallWindow.add(gp);
            Rectangle size = ScreenFinder.getCurrentScreenSizeWithoutBounds();
            double dw = (double) size.width / (double) bis.getWidth();
            double dh = (double) size.height / (double) bis.getHeight();
            double ratio = Math.min(dw, dh);
            ratio = ratio * 0.8;//do not cover all screen
            double nw = ratio * (double) bis.getWidth();
            double nh = ratio * (double) bis.getHeight();
            createWallWindow.setSize((int) nw, (int) nh);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createWallWindow.setVisible(true);
                }
            });
        } catch (Exception ex) {
            //not image, wall?
            ex.printStackTrace();
        }

    }

}
