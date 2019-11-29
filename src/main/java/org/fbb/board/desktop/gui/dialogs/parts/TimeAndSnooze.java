/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs.parts;

import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.internals.training.BoulderCalc;

/**
 *
 * @author jvanek
 */
public class TimeAndSnooze {

    private final JSpinner delay;
    private final JTextField snooze;
    private final JLabel time;
    private final Changer ch;
    private final Snoozer snoozer;
    private final Runnable reset;
    private final Runnable mainPaint;
    private final Thread runner;

    private volatile boolean alive = true;

    public TimeAndSnooze(Container parent, Runnable snoozAction, Runnable resetAction, Runnable paint) {
        parent.add(new JLabel(Translator.R("delay")));
        delay = new JSpinner(new SpinnerNumberModel(1.5, 0.2, 10, 0.2));
        parent.add(delay);
        parent.add(new JLabel(Translator.R("snooze")));
        snooze = new JTextField("00:00");
        ch = new Changer();
        snooze.getDocument().addDocumentListener(ch);
        parent.add(snooze);
        parent.add(new JLabel("      mm:ss :"));
        time = new JLabel("0:0");
        time.setFont(time.getFont().deriveFont(time.getFont().getSize() * 3f));
        parent.add(time);
        snoozer = new Snoozer(new Runnable() {
            @Override
            public void run() {
                snoozer.setSnoozeCounter(-2000/*very aprox time of rendering below*/);
                snoozAction.run();
            }
        });
        reset = resetAction;
        mainPaint = paint;
        runner = new Thread(new Runnable() {
            @Override
            public void run() {
                reset.run();
                while (alive) {
                    try {
                        Thread.sleep(getDelay());
                        snoozer.checkAndDo(getDelay(), time, snooze);
                        mainPaint.run();
                    } catch (InterruptedException ex) {
                        //nooo!
                    }
                }
            }
        });
        ch.work();
    }

    public void start() {
        runner.start();
    }

    public void stop() {
        alive = false;
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
            snoozer.setSnoozeCounter((long) BoulderCalc.decode(snooze.getText()) * 1000l);
            reset.run();
        }

    }

    public void doAsOnChange() {
        ch.work();
    }

    private long getDelay() {
        return (long) (((Double) delay.getValue()) * 1000d);
    }

}
