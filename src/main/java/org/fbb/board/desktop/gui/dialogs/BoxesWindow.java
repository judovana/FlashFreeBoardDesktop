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
        reset();
        tas.start();
    }
    private final Random move = new Random();

    int c1, c2, r1, r2;
    int t = 0;
    int v1, v2, v3, v4;

    @Override
    public void run() {
        try {
            if (t <= 0) {
                t = move.nextInt(Math.min(getGridWidth(), getgridHeight()) / 2)*2+1;
                v1 = move.nextInt(2) - 1;
                v2 = move.nextInt(2) - 1;
                v3 = move.nextInt(2) - 1;
                v4 = move.nextInt(2) - 1;
            }
            t--;
            c1 = c1 + v1;
            c2 = c2 + v2;
            r1 = r1 + v3;
            r2 = r2 + v4;
            int[] limited1 = limit(c1, 0, getGridWidth() / 2, v1);
            int[] limited2 = limit(c2, getGridWidth() / 2, getGridWidth(), v2);
            int[] limited3 = limit(r1, 0, getgridHeight() / 2, v3);
            int[] limited4 = limit(r2, getgridHeight() / 2, getgridHeight(), v4);
            c1 = limited1[0];
            v1 = limited1[1];
            c2 = limited2[0];
            v2 = limited2[1];
            r1 = limited3[0];
            v3 = limited3[1];
            r2 = limited4[0];
            v4 = limited4[1];
            gp.getGrid().clean();
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

    private int getGridWidth() {
        return gp.getGrid().getWidth();
    }

    private void reset() {
        c1 = (getGridWidth() / 4);
        c2 = ((getGridWidth() * 3) / 4);
        r1 = (getgridHeight() / 4);
        r2 = ((getgridHeight() * 3) / 4);

    }

    private int getgridHeight() {
        return gp.getGrid().getHeight();
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
            if (c2 + i < getGridWidth()) {
                CampusLikeDialog.drawColumn(c2 + i, c, gp.getGrid());
            }
        }
        if (bottom.isSelected()) {
            if (r2 + i < getgridHeight()) {
                CampusLikeDialog.drawRow(r2 + i, c, gp.getGrid());
            }
        }

    }

    private int[] limit(final int c, final int minInc, final int maxEx, final int v) {
        int[] r = new int[]{c, v};
        if (c < minInc) {
            r = new int[]{minInc, v * -1};
        }
        if (c >= maxEx) {
            r = new int[]{maxEx - 1, v * -1};
        }
        return r;
    }

}
