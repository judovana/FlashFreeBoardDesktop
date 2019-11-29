/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.dialogs.parts.DefaultSnoozeAction;
import org.fbb.board.desktop.gui.dialogs.parts.TimeAndSnooze;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class BallWindow extends JDialog implements Runnable {

    private final GridPane gp;
    private final JSpinner maxJump;
    private final JComboBox<Integer> size;
    private final TimeAndSnooze tas;

    public BallWindow(Component parent, GridPane gp) {
        this.gp = gp;
        this.setTitle(Translator.R("ball"));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);
        this.setSize(500, 400);
        this.setLocationRelativeTo(parent);
        this.add(new JTextField(Translator.R("ballHelp")), BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        this.add(panel);
        panel.setLayout(new GridLayout(5, 2));
        panel.add(new JLabel(Translator.R("maxJump")));
        maxJump = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        maxJump.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                BallWindow.this.reset();
            }
        });
        panel.add(maxJump);
        panel.add(new JLabel(Translator.R("size")));
        size = new JComboBox<>(new Integer[]{1, 2, 3});
        size.setSelectedIndex(1);
        panel.add(size);
        tas = new TimeAndSnooze(panel, new DefaultSnoozeAction(gp), new Runnable() {
            @Override
            public void run() {
                BallWindow.this.reset();
            }
        }, this);
        tas.doAsOnChange();
        this.pack();
        this.setSize(this.getWidth(), 350);
        this.setLocationRelativeTo(parent);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                tas.stop();
            }

        });
        tas.start();
    }

    private Point center;
    private Point vector;
    private int countDown;
    Random randomizer = new Random();

    @Override
    public void run() {
        try {
            gp.getGrid().clean();
            gp.getGrid().set(center.x, center.y, (byte) 3);
            if (size.getSelectedIndex() > 0) {
                setSilentlyCatched(center.x + 1, center.y, (byte) 2);
                setSilentlyCatched(center.x - 1, center.y, (byte) 2);
                setSilentlyCatched(center.x + 1, center.y + 1, (byte) 2);
                setSilentlyCatched(center.x + 1, center.y - 1, (byte) 2);
                setSilentlyCatched(center.x - 1, center.y + 1, (byte) 2);
                setSilentlyCatched(center.x - 1, center.y - 1, (byte) 2);
                setSilentlyCatched(center.x, center.y + 1, (byte) 2);
                setSilentlyCatched(center.x, center.y - 1, (byte) 2);
                if (size.getSelectedIndex() > 1) {
                    setSilentlyCatched(center.x + 2, center.y, (byte) 1);
                    setSilentlyCatched(center.x - 2, center.y, (byte) 1);
                    setSilentlyCatched(center.x + 2, center.y + 2, (byte) 1);
                    setSilentlyCatched(center.x + 2, center.y - 2, (byte) 1);
                    setSilentlyCatched(center.x - 2, center.y + 2, (byte) 1);
                    setSilentlyCatched(center.x - 2, center.y - 2, (byte) 1);
                    setSilentlyCatched(center.x, center.y + 2, (byte) 1);
                    setSilentlyCatched(center.x, center.y - 2, (byte) 1);
                    setSilentlyCatched(center.x + 1, center.y + 2, (byte) 1);
                    setSilentlyCatched(center.x + 1, center.y - 2, (byte) 1);
                    setSilentlyCatched(center.x - 1, center.y + 2, (byte) 1);
                    setSilentlyCatched(center.x - 1, center.y - 2, (byte) 1);
                    setSilentlyCatched(center.x + 2, center.y + 1, (byte) 1);
                    setSilentlyCatched(center.x + 2, center.y - 1, (byte) 1);
                    setSilentlyCatched(center.x - 2, center.y + 1, (byte) 1);
                    setSilentlyCatched(center.x - 2, center.y - 1, (byte) 1);

                }
            }

            gp.repaintAndSendToKnown();
            countDown--;
            if (countDown <= 0) {
                countDown = randomizer.nextInt(Math.min(gp.getGrid().getWidth() / 2, gp.getGrid().getHeight() / 2) + 1);
                vector = getNewVector();
                if (vector.equals(new Point(0, 0))) {
                    countDown = 1;
                }
                center = movePoint(center, vector);
            } else {
                center = movePoint(center, vector);
            }
            fixCoords();
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(BallWindow.this, ex);
        }
    }

    public void setSilentlyCatched(int x, int y, byte color) {
        try {
            gp.getGrid().set(x, y, color);
        } catch (IndexOutOfBoundsException e) {
            // intentionally ignored
        }
    }

    public boolean fixCoords() {
        boolean chgedx = false;
        boolean chgedy = false;
        if (center.x < 0) {
            vector.x = getMaxStrength();
            chgedx = true;
        }
        if (center.x >= gp.getGrid().getWidth()) {
            vector.x = -getMaxStrength();
            chgedx = true;
        }
        if (center.y < 0) {
            vector.y = getMaxStrength();
            chgedy = true;
        }
        if (center.y >= gp.getGrid().getHeight()) {
            vector.y = -getMaxStrength();
            chgedy = true;
        }
        if (chgedx) {
            center.x = center.x + vector.x;
        }
        if (chgedy) {
            center.y = center.y + vector.y;
        }
        return chgedx || chgedy;
    }

    private int getDirection() {
        return randomizer.nextInt(3) - 1;
    }

    private int getRandomStrength() {
        int i = randomizer.nextInt((Integer) maxJump.getValue()) + 1;
        return i;
    }

    private int getMaxStrength() {
        int i = (Integer) maxJump.getValue();
        return i;
    }

    public Point getNewVector() {
        return new Point(getDirection() * getRandomStrength(), getDirection() * getRandomStrength());
    }

    private Point movePoint(Point from, Point by) {
        System.out.println(from + " " + by);
        return new Point(from.x + by.x, from.y + by.y);
    }

    private void reset() {
        center = new Point(gp.getGrid().getWidth() / 2, gp.getGrid().getHeight() / 2);
        vector = new Point(0, -1);
        countDown = countDown = randomizer.nextInt(Math.min(gp.getGrid().getWidth() / 2, gp.getGrid().getHeight() / 2) + 1);
    }

}
