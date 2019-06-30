/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grid;

import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.comm.ByteEater;

/**
 *
 * @author jvanek
 */
public class GridPane extends JPanel implements Meassurable {

    private final BufferedImage img;
    private final Grid grid;

    @Override
    public boolean isFocusable() {
        return true;
    }

    public Grid getGrid() {
        return grid;
    }

    private final MouseWheelListener wheel;
    private final MouseListener clicks;
    private final MouseMotionListener drag;
    private final KeyListener keys;

    public GridPane(BufferedImage img, byte[] properties, ByteEater current) {
        this.grabFocus();
        this.img = img;
        grid = new Grid(this, properties);
        wheel = new MouseWheelListenerRotateHoldStyles();
        clicks = new MouseClicks();
        drag = new MouseDragImpl();
        keys = new KyeHandler();
        this.addMouseWheelListener(wheel);
        this.addMouseListener(clicks);
        this.addMouseMotionListener(drag);
        this.addKeyListener(keys);
        lastused = current;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, this.getWidth(), this.getHeight(), null);
        grid.draw((g));
    }

    public void save(File f) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f))) {
            ZipEntry imgEntry = new ZipEntry("img.jpeg");
            zos.putNextEntry(imgEntry);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(this.img, "jpg", bos);
            zos.write(bos.toByteArray());
            ZipEntry coords = new ZipEntry("coords.prop");
            zos.putNextEntry(coords);
            zos.write(grid.getCoordsSave());
            zos.flush();
            zos.finish();
        }
    }

    public void disableClicking() {
        //this.removeMouseWheelListener(wheel);
        this.removeMouseListener(clicks);
        this.removeMouseMotionListener(drag);
        this.removeKeyListener(keys);

    }

    public void enableBoulderModificationOnly() {
        disableClicking();
        this.addMouseListener(clicks);
    }

    private ByteEater lastused = null;

    public void repaintAndSendToKnown() {
        repaint();
        if (lastused != null) {
            grid.send(lastused);
        } else {
            ;
        }
    }

    public void repaintAndSend(ByteEater consummer) {
        repaint();
        this.lastused = consummer;
        grid.send(consummer);
    }

    public static class Preload {

        public final byte[] img;
        public final byte[] props;
        public final String givenId;

        public Preload(byte[] img, byte[] props, String id) {
            this.img = img;
            this.props = props;
            this.givenId = id;
        }

    }

    public static Preload preload(ZipInputStream zis, String id) throws IOException {
        ZipEntry ze;
        ByteArrayOutputStream img = null;
        ByteArrayOutputStream props = null;
        while ((ze = zis.getNextEntry()) != null) {
            if (ze.getName().equals("img.jpeg")) {
                img = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = zis.read(data, 0, data.length)) != -1) {
                    img.write(data, 0, nRead);
                }
            } else if (ze.getName().equals("coords.prop")) {
                props = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = zis.read(data, 0, data.length)) != -1) {
                    props.write(data, 0, nRead);
                }
            } else {
                GuiLogHelper.guiLogger.loge("Unknown entry: " + ze.getName());
            }
        }
        return new Preload(img.toByteArray(), props.toByteArray(), id);
    }

    private class MouseWheelListenerRotateHoldStyles implements MouseWheelListener {

        public MouseWheelListenerRotateHoldStyles() {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            String message;
            int notches = e.getWheelRotation();
            grid.adjustTmpHoldStyle(notches);
            repaint();
        }
    }

    private class MouseClicks extends MouseAdapter {

        public MouseClicks() {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
                double relX = (double) e.getX() / (double) getWidth();
                double relY = (double) e.getY() / (double) getHeight();
                grid.select(relX, relY);
            }
            if (e.getButton() == MouseEvent.BUTTON1) {
                grid.setPs(e.getX(), e.getY());
                repaintAndSendToKnown();
            }
            if (e.getButton() == MouseEvent.BUTTON2 ) {
                grid.setShowGrid(!grid.isShowGrid());
                repaintAndSendToKnown();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            grid.unselect();
        }
    }

    private class MouseDragImpl extends MouseAdapter {

        public MouseDragImpl() {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            double relX = (double) e.getX() / (double) getWidth();
            double relY = (double) e.getY() / (double) getHeight();
            if (grid.moveSelected(relX, relY)) {
                repaint();
            }

        }
    }

    private class KyeHandler extends KeyAdapter {

        public KyeHandler() {
        }

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
                grid.randomBoulder(null);
                GridPane.this.repaint();
            }
        }
    }

}
