/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

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
import javax.swing.JTextField;
import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.dialogs.parts.DefaultSnoozeAction;
import org.fbb.board.desktop.gui.dialogs.parts.TimeAndSnooze;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class BoxesWindow extends JDialog implements Runnable {

    private final GridPane gp;
    private final TimeAndSnooze tas;

    private final JComboBox<Integer> size;

    private final JCheckBox up = new JCheckBox(Translator.R("uc"));
    private final JCheckBox bottom = new JCheckBox(Translator.R("bc"));
    private final JCheckBox left = new JCheckBox(Translator.R("lw"));
    private final JCheckBox right = new JCheckBox(Translator.R("rw"));

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
        panel.setLayout(new GridLayout(6, 2));
        panel.add(new JLabel(Translator.R("size")));
        size = new JComboBox<>(new Integer[]{1, 2, 3});
        size.setSelectedIndex(1);
        panel.add(size);
        up.setSelected(false);
        bottom.setSelected(false);
        panel.add(up);
        panel.add(bottom);
        left.setSelected(true);
        right.setSelected(true);
        panel.add(left);
        panel.add(right);
        tas = new TimeAndSnooze(panel, new DefaultSnoozeAction(gp), new Runnable() {
            @Override
            public void run() {
                BoxesWindow.this.reset();
            }
        }, this);
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
    private Random move = new Random();

    @Override
    public void run() {
        try {
            gp.getGrid().clean();
            int c1 = move.nextInt(gp.getGrid().getWidth() / 2);
            int c2 = move.nextInt(gp.getGrid().getWidth() / 2 + gp.getGrid().getWidth() / 2);
            int r1 = move.nextInt(gp.getGrid().getHeight() / 2);
            int r2 = move.nextInt(gp.getGrid().getHeight() / 2 + gp.getGrid().getHeight() / 2);
            drawSIngleGrid(c1, c2, r1, r2, 0, (byte) 3);
            if (size.getSelectedIndex() > 0) {
                drawSIngleGrid(c1, c2, r1, r2, 1, (byte) 2);
                if (size.getSelectedIndex() > 1) {
                    drawSIngleGrid(c1, c2, r1, r2, 2, (byte) 1);
                }
            }
            gp.repaintAndSendToKnown();
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(BoxesWindow.this, ex);
        }
    }

    private void reset() {

    }

    private void drawSIngleGrid(int c1, int c2, int r1, int r2, int i, byte c) {
        if (left.isSelected()) {
            if (c1 - i >= 0) {
                CampusLikeDialog.drawColumn(c1 - i, c, gp.getGrid());
            }
        }
        if (up.isSelected()) {
            if (r1 - i >= 0) {
                CampusLikeDialog.drawRow(r1 - i, c, gp.getGrid());
            }
        }
        if (right.isSelected()) {
            if (c2 + i < gp.getGrid().getWidth()) {
                CampusLikeDialog.drawColumn(c2 + i, c, gp.getGrid());
            }
        }
        if (bottom.isSelected()) {
            if (r2 + i < gp.getGrid().getHeight()) {
                CampusLikeDialog.drawRow(r2 + i, c, gp.getGrid());
            }
        }

    }

}
