/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs.parts;

import javax.swing.JLabel;
import javax.swing.JTextField;
import org.fbb.board.internals.training.BoulderCalc;

/**
 *
 * @author jvanek
 */
public class Snoozer {

    private final Runnable close;
    private long snoozeCounter;

    Snoozer(Runnable close) {
        this.close = close;
    }

    void adjustSnoozeCounter(long by) {
        this.snoozeCounter += by;
    }

    void setSnoozeCounter(long snoozeCounter) {
        this.snoozeCounter = snoozeCounter;
    }

    boolean isOnZero(double delay) {
        return (double) snoozeCounter / 1000d < delay && (double) snoozeCounter / 1000d > -delay;
    }

    String getTime() {
        return BoulderCalc.code(snoozeCounter / 1000l);
    }

    void snooze() {
        close.run();
    }

    void checkAndDo(long delay, JLabel time, JTextField origin) {
        this.adjustSnoozeCounter(-delay);
        time.setText(this.getTime());
        time.repaint();
        if (this.isOnZero((double) delay / 1000d)
                && BoulderCalc.decode(origin.getText()) > 0) {
            this.snooze();
        }
        time.repaint();
    }

}
