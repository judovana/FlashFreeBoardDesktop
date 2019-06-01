/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.training;

import org.fbb.board.internals.grid.Boulder;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.JLabel;
import org.fbb.board.desktop.TextToSpeech;
import org.fbb.board.desktop.gui.MainWindow;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class TimeredTraining implements Runnable {

    private final ActionListener next, prev, nextRandom, nextGenerateRandom;
    private boolean running = true;
    private boolean paused = false;
    private boolean randomAllowed, regularAllowed, jumpingAllowed;
    private final int totalTime;
    private final int timeOfBoulder;
    private int counter;
    private final JLabel output;
    private TextToSpeech.TextId speak;

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

    public void setSpeak(TextToSpeech.TextId speak) {
        this.speak = speak;
    }

    public TimeredTraining(ActionListener next, ActionListener prev, ActionListener nextRandom, ActionListener nextGenerateRandom, boolean randomAllowed, boolean regularAllowed, boolean jumpingAllowed, int totalTime, int timeOfBoulder, JLabel output, TextToSpeech.TextId speak) {
        this.next = next;
        this.prev = prev;
        this.nextRandom = nextRandom;
        this.nextGenerateRandom = nextGenerateRandom;
        this.randomAllowed = randomAllowed;
        this.regularAllowed = regularAllowed;
        this.jumpingAllowed = jumpingAllowed;
        this.output = output;
        this.totalTime = totalTime;
        this.timeOfBoulder = timeOfBoulder;
        this.speak = speak;
        counter = totalTime + 1;
    }

    @Override
    public void run() {
        Random r = new Random();
        try {
            while (running && counter > 0) {
                if (!paused) {
                    counter--;
                    output.setText(counter / 60 + ":" + counter % 60);
                    output.repaint();
                    if (counter % timeOfBoulder == 0) {
                        if (!regularAllowed && !randomAllowed) {
                            continue;
                        }
                        boolean choice1 = r.nextBoolean();
                        if (!regularAllowed || (randomAllowed && choice1)) {
                            nextGenerateRandom.actionPerformed(null);
                        }
                        if (!randomAllowed || (regularAllowed && !choice1)) {
                            if (jumpingAllowed && r.nextBoolean()) {
                                int i = r.nextInt(MainWindow.list.getSize());
                                i = i--;
                                MainWindow.list.setIndex(i);
                                next.actionPerformed(null);
                            } else {
                                if (MainWindow.list.canFwd()) {
                                    next.actionPerformed(null);
                                } else {
                                    MainWindow.list.setIndex(-1);
                                    next.actionPerformed(null);
                                }
                            }
                        }
                        Boulder b = MainWindow.hm.getCurrentInHistory();
                        if (b.getGrade().isRandom()) {
                            TextToSpeech.tellImpl(b.getGrade().toString(), speak);
                        } else {
                            TextToSpeech.tellImpl(b.getGradeAndNameAndAuthor(), speak);
                        }
                    }
                }
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
    }
}
