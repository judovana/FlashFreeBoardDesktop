/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 *
 * @author jvanek
 */
public class GridPane extends JPanel implements Meassurable {

    private BufferedImage img;
    private Grid grid;

    @Override
    public boolean isFocusable() {
        return true;
    }

    public Grid getGrid() {
        return grid;
    }
    
    

    public GridPane(BufferedImage img) {
        this.grabFocus();
        this.img = img;
        grid = new Grid(this);
        this.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                String message;
                int notches = e.getWheelRotation();
                grid.setHoldStyle(notches);
                repaint();
            }
        });
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    double relX = (double) e.getX() / (double) getWidth();
                    double relY = (double) e.getY() / (double) getHeight();
                    grid.select(relX, relY);
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    grid.setPs(e.getX(), e.getY());
                    repaint();
                }
                if (e.getButton() == MouseEvent.BUTTON2) {
                    grid.setShowGrid(!grid.isShowGrid());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                grid.unselect();
            }

        });
        this.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                double relX = (double) e.getX() / (double) getWidth();
                double relY = (double) e.getY() / (double) getHeight();
                if (grid.moveSelected(relX, relY)) {
                    repaint();
                }

            }

        });
        this.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    grid.setVertLines(grid.getVertLines() + 1);
                    GridPane.this.repaint();
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    grid.setVertLines(grid.getVertLines() - 1);
                    GridPane.this.repaint();
                }

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    grid.setHorLines(grid.getHorLines() + 1);
                    GridPane.this.repaint();
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    grid.setHorLines(grid.getHorLines() - 1);
                    GridPane.this.repaint();
                }
                  if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    grid.randomBoulder();
                    GridPane.this.repaint();
                }
            }

        });
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, this.getWidth(), this.getHeight(), null);
        grid.draw((g));
    }

}
