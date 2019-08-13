/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grid;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class PhotoGrider {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame f = new JFrame("Flash Free Board");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.pack();
                f.setSize(800, 600);
                BufferedImage img;
                try {
                    img = ImageIO.read(PhotoGrider.class.getResourceAsStream("mnbr.png"));
                    //img = ImageIO.read(PhotoGrider.class.getResourceAsStream("bgr.jpg"));
                } catch (IOException ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    img = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
                }
                f.add(new GridPane(img, null, null));
                f.setVisible(true);
            }
        });
    }

}
