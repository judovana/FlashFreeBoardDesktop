/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.fbb.board.desktop.Files;

/**
 *
 * @author jvanek
 */
public class TrainingPlaylist {

    private final List<TrainingWithStartTime> data;

    private TrainingPlaylist(List<TrainingWithStartTime> r) {
        data = r;
    }

    private static class TrainingWithStartTime {

        private final Training t;
        private final int seconds;
        private final String name;

        public TrainingWithStartTime(Training t, int seconds, String name) {
            this.t = t;
            this.seconds = seconds;
            this.name = name;
        }
    }

    public List<Training> toTrainings() {
        List<Training> r = new ArrayList<>(data.size());
        for (TrainingWithStartTime t : data) {
            Training tt = new Training(t.t.innerSettings.random, t.t.innerSettings.regular, t.t.innerSettings.jumps, t.t.innerSettings.tOfBoulder, t.t.innerSettings.tOfTraining, t.t.innerSettings.numOfBoulders, t.t.filter, t.t.currentName);
            r.add(tt);
        }
        return r;
    }

    public int getPauseForTraing(int i) {
        return data.get(i).seconds;
    }

    public String getTitleForTraing(int i) {
        return data.get(i).name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Trainings in list: " + data.size() + "\n");
        int i = 0;
        for (TrainingWithStartTime t : data) {
            i++;
            sb.append(i).append(")").append(t.name).append(", init delay: ").append(BoulderCalc.code(t.seconds)).append("\n");

        }
        sb.append("\n*** Details *** \n");
        i = 0;
        int total1 = 0;
        int total2 = 0;
        for (TrainingWithStartTime t : data) {
            i++;
            sb.append(i).append(") pause: ").append(BoulderCalc.code(t.seconds)).append(", ");
            sb.append(t.name).append("/").append(t.t.toString());
            sb.append("\n-----------\n");
            total1 += t.seconds + BoulderCalc.decode(t.t.innerSettings.tOfTraining);
            total2 += BoulderCalc.decode(t.t.innerSettings.tOfTraining);
        }
        sb.append("total: ").append(BoulderCalc.code(total1)).append("(").append(BoulderCalc.code(total2)).append(" of pure workload)" + "\n");
        return sb.toString();
    }

    public static TrainingPlaylist loadSavedTraining(File selectedFile) throws IOException {
        List<TrainingWithStartTime> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), "utf-8"))) {
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                String[] src = s.split("/");
                if (src.length == 1) {
                    r.add(new TrainingWithStartTime(Training.loadSavedTraining(Files.getTraining(src[0])), 0, src[0]));
                } else {
                    int ts = BoulderCalc.decode(src[0]);
                    r.add(new TrainingWithStartTime(Training.loadSavedTraining(Files.getTraining(src[1])), ts, src[1]));
                }
            }
        }
        return new TrainingPlaylist(r);
    }

}
