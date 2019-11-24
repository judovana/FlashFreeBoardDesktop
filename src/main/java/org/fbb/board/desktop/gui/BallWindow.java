/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.grid.GridPane;
import org.fbb.board.internals.training.BoulderCalc;

/**
 *
 * @author jvanek
 */
class BallWindow extends JDialog implements Runnable {

    private final GridPane gp;
    private final JSpinner delay;
    private final JSpinner maxJump;
    private final JTextField snooze;
    private final JLabel time;
    private final JComboBox<Integer> size;
    private final Thread runner = new Thread(this);

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
        panel.add(new JLabel(Translator.R("delay")));
        delay = new JSpinner(new SpinnerNumberModel(1.5, 0.2, 10, 0.2));
        panel.add(delay);
        panel.add(new JLabel(Translator.R("maxJump")));
        maxJump = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        maxJump.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                BallWindow.this.reset();
            }
        });
        panel.add(maxJump);
        panel.add(new JLabel(Translator.R("snooze")));
        snooze = new JTextField("00:00");
        snooze.getDocument().addDocumentListener(new Changer());
        panel.add(snooze);
        panel.add(new JLabel(Translator.R("size")));
        size = new JComboBox<>(new Integer[]{1, 2, 3});
        panel.add(size);
        panel.add(new JLabel("      mm:ss :"));
        time = new JLabel("0:0");
        time.setFont(time.getFont().deriveFont(time.getFont().getSize() * 3f));
        panel.add(time);
        Changer ch = new Changer();
        ch.work();
        this.pack();
        this.setSize(this.getWidth(), 350);
        this.setLocationRelativeTo(parent);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                alive = false;
            }

        });
        runner.start();
    }

    private volatile boolean alive = true;
    private Point center;
    private Point vector;
    private int countDown;
    Random randomizer = new Random();
    private long snoozeCounter;

    @Override
    public void run() {
        reset();
        while (alive) {
            try {
                Thread.sleep(getDelay());
                snoozeCounter -= getDelay();
                time.setText(BoulderCalc.code(snoozeCounter / 1000l));
                if ((double) snoozeCounter / 1000d < (double) delay.getValue()
                        && (double) snoozeCounter / 1000d > -(double) delay.getValue()
                        && BoulderCalc.decode(snooze.getText()) > 0) {
                    snoozeCounter = -60000;
                    for (int x = 0; x < gp.getGrid().getWidth(); x++) {
                        gp.getGrid().clean();
                        CampusLikeDialog.drawColumn(x, 1, gp.getGrid());
                        gp.repaintAndSendToKnown();
                        Thread.sleep(100);
                    }
                    for (int x = gp.getGrid().getWidth() - 1; x >= 0; x--) {
                        gp.getGrid().clean();
                        CampusLikeDialog.drawColumn(x, 1, gp.getGrid());
                        gp.repaintAndSendToKnown();
                        Thread.sleep(100);
                    }
                }
                time.repaint();
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

    private long getDelay() {
        return (long) (((Double) delay.getValue()) * 1000d);
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

    private class Changer implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            work();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            work();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            work();
        }

        private void work() {
            BallWindow.this.snoozeCounter = (long) BoulderCalc.decode(snooze.getText()) * 1000l;
        }

    }

}
