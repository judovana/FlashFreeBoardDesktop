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
    private int counter;
    private final JLabel output;
    private TextToSpeech.TextId speak;
    private final TrainingWithBackends ts;

    public void setJumpingAllowed(boolean jumpingAllowed) {
        ts.allowJumps.setSelected(jumpingAllowed);
    }

    public void setRandomAllowed(boolean randomAllowed) {
        ts.allowRandom.setSelected(randomAllowed);
    }

    public void setRegularAllowed(boolean regularAllowed) {
        ts.allowRegular.setSelected(regularAllowed);
    }

    public boolean getJumpingAllowed() {
        return ts.allowJumps.isSelected();
    }

    public boolean getRandomAllowed() {
        return ts.allowRandom.isSelected();
    }

    public boolean getRegularAllowed() {
        return ts.allowRegular.isSelected();
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

    public TimeredTraining(ActionListener next, ActionListener prev, ActionListener nextRandom, ActionListener nextGenerateRandom, TrainingWithBackends ts, JLabel output, TextToSpeech.TextId speak) {
        this.next = next;
        this.prev = prev;
        this.nextRandom = nextRandom;
        this.nextGenerateRandom = nextGenerateRandom;
        this.output = output;
        this.speak = speak;
        this.ts = ts;
        counter = ts.getTotalTime() + 1;
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
                    if (counter % ts.getTimeOfBoulder() == 0) {
                        if (!getRegularAllowed() && !getRandomAllowed()) {
                            continue;
                        }
                        boolean choice1 = r.nextBoolean();
                        if (!getRegularAllowed() || (getRandomAllowed() && choice1)) {
                            nextGenerateRandom.actionPerformed(null);
                        }
                        if (!getRandomAllowed() || (getRandomAllowed() && !choice1)) {
                            if (getJumpingAllowed() && r.nextBoolean()) {
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
