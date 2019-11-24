/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
class CampusLikeDialog extends JDialog {

    private final GridPane gp;
    final JTextField redLines;
    final JTextField greenLines;
    final JTextField blueLines;

    final JTextField redColumns;
    final JTextField greenColumns;
    final JTextField blueColumns;

    public CampusLikeDialog(Component parent, GridPane gp) {
        this.gp = gp;
        this.setTitle(Translator.R("campus"));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);
        this.setSize(500, 400);
        this.setLocationRelativeTo(parent);
        this.add(new JTextField(Translator.R("campusHelp", gp.getGrid().getHeight() - 1, gp.getGrid().getWidth() - 1)), BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        this.add(panel);
        panel.setLayout(new GridLayout(6, 2));
        panel.add(new JLabel(Translator.R("rln")));
        redLines = new JTextField("" + (gp.getGrid().getHeight() / 2) + " " + (1 * gp.getGrid().getHeight()) / 4);
        panel.add(redLines);
        panel.add(new JLabel(Translator.R("gln")));
        greenLines = new JTextField("");
        panel.add(greenLines);
        panel.add(new JLabel(Translator.R("bln")));
        blueLines = new JTextField("");
        panel.add(blueLines);
        panel.add(new JLabel(Translator.R("rnc")));
        redColumns = new JTextField("" + (gp.getGrid().getWidth() / 3) + " " + (2 * gp.getGrid().getWidth() / 3));
        panel.add(redColumns);
        panel.add(new JLabel(Translator.R("gnc")));
        greenColumns = new JTextField("");
        panel.add(greenColumns);
        panel.add(new JLabel(Translator.R("bnc")));
        blueColumns = new JTextField("");
        panel.add(blueColumns);
        Changer ch = new Changer();
        redLines.getDocument().addDocumentListener(ch);
        greenLines.getDocument().addDocumentListener(ch);
        blueLines.getDocument().addDocumentListener(ch);
        redColumns.getDocument().addDocumentListener(ch);
        greenColumns.getDocument().addDocumentListener(ch);
        blueColumns.getDocument().addDocumentListener(ch);
        ch.work();
        this.pack();
        this.setSize(this.getWidth(), 400);
        this.setLocationRelativeTo(parent);
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
            try {
                String[] rl = redLines.getText().split("[^\\d]+");
                String[] gl = greenLines.getText().split("[^\\d]+");
                String[] bl = blueLines.getText().split("[^\\d]+");
                String[] rc = redColumns.getText().split("[^\\d]+");
                String[] gc = greenColumns.getText().split("[^\\d]+");
                String[] bc = blueColumns.getText().split("[^\\d]+");
                int totall = countNonEmpty(rl) + countNonEmpty(gl) + countNonEmpty(bl);
                int totalc = countNonEmpty(rc) + countNonEmpty(gc) + countNonEmpty(bc);

                if (totalc > gp.getGrid().getWidth() / 2 || totall > gp.getGrid().getHeight() / 2) {
                    throw new RuntimeException(Translator.R("outOfBonds2", (gp.getGrid().getWidth() / 2), (gp.getGrid().getHeight() / 2)));
                }

                gp.getGrid().clean();
                drawColumn(rc, 3);
                drawColumn(gc, 2);
                drawColumn(bc, 1);
                drawRow(rl, 3);
                drawRow(gl, 2);
                drawRow(bl, 1);
                gp.repaintAndSendToKnown();
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(CampusLikeDialog.this, ex);
            }
        }

        public void drawColumn(String[] cc, int color) throws NumberFormatException, IndexOutOfBoundsException {
            for (String c : cc) {
                for (int i = 0; i < gp.getGrid().getHeight(); i++) {
                    if (!c.trim().isEmpty()) {
                        gp.getGrid().set(Integer.valueOf(c), i, (byte) color);
                    }
                }
            }
        }

        public void drawRow(String[] rr, int color) throws NumberFormatException, IndexOutOfBoundsException {
            for (String c : rr) {
                for (int i = 0; i < gp.getGrid().getWidth(); i++) {
                    if (!c.trim().isEmpty()) {
                        gp.getGrid().set(i, Integer.valueOf(c), (byte) color);
                    }
                }
            }
        }

        private int countNonEmpty(String[] s) {
            int r = 0;
            for (String s1 : s) {
                if (!s1.trim().isEmpty()) {
                    r++;
                };
            }
            return r;
        }

    }

}
