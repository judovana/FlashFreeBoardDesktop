/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial.awt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;
import javax.swing.JPanel;
import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.BoulderCreationGui;
import org.fbb.board.desktop.tutorial.SingleText;
import org.fbb.board.internals.Filter;

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
        g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 1.5f));
        for (int i = 0; i < titles.size(); i++) {
            SingleText get = titles.get(i);
            String s = Translator.R(get.getKey(), BoulderCreationGui.antiChild.format(new Date()));
            g.setFont(g.getFont().deriveFont(Font.BOLD));
            Rectangle2D rect = g.getFontMetrics().getStringBounds(s, g);//java.awt.geom.Rectangle2D$Float[x=0.0,y=-11.138672,w=71.0,h=13.96875]
            int fh = g.getFontMetrics().getHeight();//15
            g.setColor(new Color(250, 250, 250, 200));
            g.fillRect(get.getX(w), get.getY(h), (int) rect.getWidth(), fh);
            g.setColor(Color.black);
            if (get.getX(w) + rect.getWidth() > getWidth()) {
                g.drawString(s, getWidth() - (int) rect.getWidth(), get.getY(h) + fh);
            } else {
                g.drawString(s, get.getX(w), get.getY(h) + fh);
            }
        }
    }

}
