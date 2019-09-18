/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import org.fbb.board.desktop.tutorial.SingleText;

/**
 *
 * @author jvanek
 */
public class ImageWithTitles extends JPanel {

    private final List<SingleText> titles;
    private final BufferedImage bi;

    public ImageWithTitles(List<SingleText> titles, BufferedImage bi) {
        this.titles = titles;
        this.bi = bi;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(bi, 0, 0, this.getWidth(), this.getHeight(), null);
        double w = (double) this.getWidth() / (double) bi.getWidth();
        double h = (double) this.getHeight() / (double) bi.getHeight();
        for (int i = 0; i < titles.size(); i++) {
            SingleText get = titles.get(i);
            Rectangle2D rect = g.getFontMetrics().getStringBounds(get.getKey(), g);
            int fh = g.getFontMetrics().getHeight();
            g.setColor(Color.white);
            g.fillRect(get.getX(w), get.getY(h) - fh, (int) rect.getWidth(), fh);
            g.setColor(Color.black);
            g.drawString(get.getKey(), get.getX(w), get.getY(h));
        }
    }

}
