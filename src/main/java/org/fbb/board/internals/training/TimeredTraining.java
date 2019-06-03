/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.training;

import org.fbb.board.internals.grid.Boulder;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;
import javax.swing.JLabel;
import org.fbb.board.desktop.TextToSpeech;
import org.fbb.board.desktop.gui.MainWindow;
import org.fbb.board.internals.GlobalSettings;
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
    private final List<TrainingWithBackends> ts;
    private int currentInList = 0;
    private final ActionListener clear;

    public void setJumpingAllowed(boolean jumpingAllowed) {
        ts.get(currentInList).allowJumps.setSelected(jumpingAllowed);
    }

    public void setRandomAllowed(boolean randomAllowed) {
        ts.get(currentInList).allowRandom.setSelected(randomAllowed);
    }

    public void setRegularAllowed(boolean regularAllowed) {
        ts.get(currentInList).allowRegular.setSelected(regularAllowed);
    }

    public boolean getJumpingAllowed() {
        return ts.get(currentInList).allowJumps.isSelected();
    }

    public boolean getRandomAllowed() {
        return ts.get(currentInList).allowRandom.isSelected();
    }

    public boolean getRegularAllowed() {
        return ts.get(currentInList).allowRegular.isSelected();
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

    public TimeredTraining(ActionListener next, ActionListener prev, ActionListener nextRandom, ActionListener nextGenerateRandom, ActionListener clear, List<TrainingWithBackends> ts, JLabel output, TextToSpeech.TextId speak) {
        this.next = next;
        this.prev = prev;
        this.nextRandom = nextRandom;
        this.nextGenerateRandom = nextGenerateRandom;
        this.clear=clear;
        this.output = output;
        this.speak = speak;
        this.ts = ts;
        counter = ts.get(currentInList).getTotalTime() + 1;
    }

    @Override
    public void run() {
        Random r = new Random();
        try {
            while (consultList() && running && counter > 0) {
                if (!paused) {
                    counter--;
                    if (counter == ts.get(currentInList).getTotalTime()) {
                        if (ts.get(currentInList).getInitialDelay() > 0) {
                            TextToSpeech.pause(speak);
                            int delay = ts.get(currentInList).getInitialDelay();
                            clear.actionPerformed(null);
                            while (delay > 0) {
                                if (!paused) {
                                    delay--;
                                    output.setText("Brak! " + delay / 60 + ":" + delay % 60);
                                    output.repaint();
                                    Thread.sleep(1000);
                                }
                            }
                            TextToSpeech.change(speak);
                        }
                    }
                    output.setText(counter / 60 + ":" + counter % 60);
                    if (ts.size() > 1) {
                        output.setText(output.getText() + "(" + (currentInList + 1) + "/" + ts.size() + ")");
                    }
                    output.repaint();
                    if (counter % ts.get(currentInList).getTimeOfBoulder() == 0) {
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

    private boolean consultList() {
        if (running && counter <= 0) {
            currentInList++;
            if (currentInList >= ts.size()) {
                return false;
            }
            //init filter and aply initial delay
            int delay = ts.get(currentInList).getInitialDelay();
            if (delay == 0) {
                TextToSpeech.change(speak);
            }
            counter = ts.get(currentInList).getTotalTime() + 1;
            TrainingWithBackends q = ts.get(currentInList);
            q.setBoulderCalc();
            q.setChecks();
            q.init();
        }
        return true;
    }
}
