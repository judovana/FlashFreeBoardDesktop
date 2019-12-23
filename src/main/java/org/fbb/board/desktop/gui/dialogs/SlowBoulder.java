/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.awtimpl.MainWindowImpl;
import org.fbb.board.desktop.gui.dialogs.parts.DefaultSnoozeAction;
import org.fbb.board.desktop.gui.dialogs.parts.TimeAndSnooze;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.grid.GridPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author jvanek
 */
public class SlowBoulder extends JDialog implements Runnable {

    private final GridPane gp;
    private final TimeAndSnooze tas;
    private final MainWindowImpl boulders;

    public SlowBoulder(MainWindowImpl parent, GridPane gp) {
        super(parent);
        this.boulders = parent;
        this.gp = gp;
        this.setTitle(Translator.R("slowBoulder"));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.DOCUMENT_MODAL);
        this.setSize(500, 400);
        this.setLocationRelativeTo(parent);
        this.add(new JTextField(Translator.R("slowBoulderHelp")), BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        this.add(panel);
        panel.setLayout(new GridLayout(5, 2));
        tas = new TimeAndSnooze(panel, new DefaultSnoozeAction(gp), new Runnable() {
            @Override
            public void run() {
                SlowBoulder.this.reset();
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

    private Boulder currentBoulder;
    int finalBlinks = -6;
    volatile int row = finalBlinks;
    int[] currentBoulderGrid;

    @Override
    public void run() {
        try {
            if (row <= finalBlinks) {
                reset();
            }
            row--;
            int[] r = findRow();
            if (r == null) {
                if (row % 2 == 0) {
                    gp.getGrid().clean();
                } else {
                    gp.getGrid().setBouler(currentBoulder);
                }
            } else {
                for (int x = 0; x < r.length; x++) {
                    gp.getGrid().set(x, row, (byte) r[x]);
                }
            }
            gp.repaintAndSendToKnown();
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(SlowBoulder.this, ex);
        }
    }

    private int[] findRow() {
        int[] currentRow = new int[gp.getGrid().getWidth()];
        for (int x = 0; x < currentRow.length; x++) {
            currentRow[x] = 0;
        }
        while (true) {
            if (row < 0) {
                return null;
            }
            for (int x = row; x < currentBoulderGrid.length; x += gp.getGrid().getHeight()) {
                currentRow[x / gp.getGrid().getHeight()] = currentBoulderGrid[x];
            }
            if (isEmpty(currentRow)) {
                row--;
            } else {
                return currentRow;
            }
        }

    }

    private boolean isEmpty(int[] currentRow) {
        for (int x = 0; x < currentRow.length; x++) {
            if (currentRow[x] != 0) {
                return false;
            }
        }
        return true;
    }


    private void reset() {
        gp.getGrid().clean();
        currentBoulder = boulders.list.getRandom();
        currentBoulderGrid = new int[gp.getGrid().getWidth() * gp.getGrid().getHeight()];
        for (int x = 0; x < currentBoulderGrid.length; x++) {
            currentBoulderGrid[x] = 0;
        }
        currentBoulder.apply(currentBoulderGrid, gp.getGrid().getHeight());
        boulders.setNameTextAndGrade(boulders.name, currentBoulder);
        boulders.hm.addToBoulderHistory(currentBoulder);
        boulders.next.setEnabled(boulders.hm.canFwd());
        boulders.previous.setEnabled(boulders.hm.canBack());
        row = gp.getGrid().getHeight();
    }


}
