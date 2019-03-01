/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.JLabel;

/**
 *
 * @author jvanek
 */
public class TimeredTraining implements Runnable {

    private final ActionListener next, prev, nextRandom, nextGenerateRandom;
    private boolean running = true;
    private boolean paused = false;
    private boolean randomAllowed, regularAllowed, jumpingAllowed;
    private final int time;
    private int counter;
    private final JLabel output;

    public void setJumpingAllowed(boolean jumpingAllowed) {
        this.jumpingAllowed = jumpingAllowed;
    }

    public void setRandomAllowed(boolean randomAllowed) {
        this.randomAllowed = randomAllowed;
    }

    public void setRegularAllowed(boolean regularAllowed) {
        this.regularAllowed = regularAllowed;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public TimeredTraining(ActionListener next, ActionListener prev, ActionListener nextRandom, ActionListener nextGenerateRandom, boolean randomAllowed, boolean regularAllowed, boolean jumpingAllowed, int time, JLabel output) {
        this.next = next;
        this.prev = prev;
        this.nextRandom = nextRandom;
        this.nextGenerateRandom = nextGenerateRandom;
        this.randomAllowed = randomAllowed;
        this.regularAllowed = regularAllowed;
        this.jumpingAllowed = jumpingAllowed;
        this.output = output;
        this.time = time;
        counter = time;
    }

    @Override
    public void run() {
        Random r = new Random();
        try {
            while (running && counter > 0) {
                Thread.sleep(1000);
                if (!paused) {
                    counter--;
                    output.setText(counter / 60 + ":" + counter % 60);
                    output.repaint();
                    if (randomAllowed && r.nextBoolean()) {
                        nextGenerateRandom.actionPerformed(null);
                    } else {
                        next.actionPerformed(null);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
