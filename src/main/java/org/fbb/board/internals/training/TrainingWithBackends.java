/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.training;

import javax.swing.JCheckBox;
import org.fbb.board.internals.Filter;

/**
 *
 * @author jvanek
 */
public class TrainingWithBackends {

    private final BoulderCalc boulderCalc;
    final JCheckBox allowRandom;
    final JCheckBox allowRegular;
    final JCheckBox allowJumps;
    private final Training training;
    private final int initialDelay;
    private final ListSetter listSetter;
    private final String title;

    public TrainingWithBackends(BoulderCalc bc, JCheckBox random, JCheckBox regular, JCheckBox skipping, Training training, int initialDelay, String title, ListSetter ls) {
        this.boulderCalc = bc;
        this.allowRandom = random;
        this.allowRegular = regular;
        this.allowJumps = skipping;
        this.training = training;
        this.initialDelay = initialDelay;
        this.title = title;
        this.listSetter = ls;
    }

    public void setBoulderCalc() {
        boulderCalc.setTimeOfBoulder(training.innerSettings.tOfBoulder);
        boulderCalc.setTimeOfTraining(training.innerSettings.tOfTraining);
        boulderCalc.setNumBoulders(training.innerSettings.numOfBoulders);
    }

    public void setChecks() {
        allowRandom.setSelected(training.innerSettings.random);
        allowRegular.setSelected(training.innerSettings.regular);
        allowJumps.setSelected(training.innerSettings.jumps);
    }

    public void init() {
        listSetter.setUpBoulderWall(training.filter, training.currentName, title);
    }

    int getTotalTime() {
        return boulderCalc.getTotalTime();
    }

    int getTimeOfBoulder() {
        return boulderCalc.getTimeOfBoulder();
    }

    public int getInitialDelay() {
        return initialDelay;
    }

}
