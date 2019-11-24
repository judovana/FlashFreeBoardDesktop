/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
class BoxesWindow extends JDialog implements Runnable {

    private final GridPane gp;
    private final JTextField delay;
    private final JTextField snooze;
    private final JLabel time;

    private final JCheckBox up = new JCheckBox(Translator.R("uc"));
    private final JCheckBox bottom = new JCheckBox(Translator.R("bc"));
    private final JCheckBox left = new JCheckBox(Translator.R("lw"));
    private final JCheckBox right = new JCheckBox(Translator.R("rw"));

    Thread runner = new Thread(this);

    public BoxesWindow(Component parent, GridPane gp) {
        this.gp = gp;
        this.setTitle(Translator.R("box"));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);
        this.setSize(500, 400);
        this.setLocationRelativeTo(parent);
        this.add(new JTextField(Translator.R("boxHelp")), BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        this.add(panel);
        panel.setLayout(new GridLayout(5, 2));
        panel.add(new JLabel(Translator.R("delay")));
        delay = new JTextField("0.5");
        panel.add(delay);
        up.setSelected(false);
        bottom.setSelected(false);
        panel.add(up);
        panel.add(bottom);
        left.setSelected(true);
        right.setSelected(true);
        panel.add(left);
        panel.add(right);
        panel.add(new JLabel(Translator.R("snooze")));
        snooze = new JTextField("00:00");
        panel.add(snooze);
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
    private Random move = new Random();

    @Override
    public void run() {
        while (alive) {
            try {
                Thread.sleep(1000);
                gp.getGrid().clean();
                CampusLikeDialog.drawColumn(move.nextInt(gp.getGrid().getWidth()), 1, gp.getGrid());
                CampusLikeDialog.drawRow(move.nextInt(gp.getGrid().getHeight()), 1, gp.getGrid());
                gp.repaintAndSendToKnown();
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(BoxesWindow.this, ex);
            }
        }
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
            //adjust delay and snoozer
        }

    }

}
