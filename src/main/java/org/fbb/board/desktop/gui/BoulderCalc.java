/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;

/**
 *
 * @author jvanek
 */
class BoulderCalc {

    private final JTextField timeOfBoulder;
    private final JTextField timeOfTraining;
    private final JSpinner numBoulders;
    private boolean active = true;

    BoulderCalc(JTextField timeOfBoulder, JTextField timeOfTraining, JSpinner numBoulders) {
        this.timeOfBoulder = timeOfBoulder;
        this.timeOfTraining = timeOfTraining;
        this.numBoulders = numBoulders;

        numBoulders.addChangeListener((ChangeEvent e) -> {
            numOfBouldersChanged();
        });

        timeOfBoulder.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                timeOfBouldersChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timeOfBouldersChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timeOfBouldersChanged();
            }
        });

        timeOfTraining.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                timeOfTrainingChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timeOfTrainingChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timeOfTrainingChanged();
            }
        });
    }

    private void numOfBouldersChanged() {
        if (!active) {
            return;
        }
        active = false;
        try {
            if (decode(timeOfBoulder.getText()) > 0) {
                int nwTimeOfTraining = ((Integer) numBoulders.getValue()) * decode(timeOfBoulder.getText());
                timeOfTraining.setText(code(nwTimeOfTraining));
                timeOfTraining.repaint();
            }
        } finally {
            active = true;
        }
    }

    private void timeOfBouldersChanged() {
        if (!active) {
            return;
        }
        active = false;
        try {
            if (decode(timeOfBoulder.getText()) > 0) {
                int nwTimeOfTraining = ((Integer) numBoulders.getValue()) * decode(timeOfBoulder.getText());
                timeOfTraining.setText(code(nwTimeOfTraining));
                timeOfTraining.repaint();
            }
        } finally {
            active = true;
        }
    }

    private void timeOfTrainingChanged() {
        if (!active) {
            return;
        }
        active = false;
        try {
            int oldTimeOfTrainig = decode(timeOfTraining.getText());
            if (oldTimeOfTrainig > 0) {
                int oldNumOfBoulders = ((Integer) numBoulders.getValue());
                int nwTimeOfBoulder = oldTimeOfTrainig / oldNumOfBoulders;
                timeOfBoulder.setText(code(nwTimeOfBoulder));
                timeOfBoulder.repaint();
            }
        } finally {
            active = true;
        }
    }

    private String code(int seconds) {
        return (seconds / 60) + ":" + (seconds % 60);

    }

    private int decode(String s) {
        try {
            String[] ss = s.trim().split(":");
            if (ss.length == 1) {
                return Integer.parseInt(ss[0].trim());
            }
            int miuntes = Integer.parseInt(ss[0].trim());
            int secs = Integer.parseInt(ss[1].trim());
            return miuntes * 60 + secs;
        } catch (Exception ex) {
            ex.printStackTrace();
            //JOptionPane.showMessageDialog(null, Translator.R("parseTimeIssue"));
            return -1;
        }
    }

    public int getTotalTime() {
        return decode(timeOfTraining.getText());
    }

    public int getTimeOfBoulder() {
        return decode(timeOfBoulder.getText());
    }

}
