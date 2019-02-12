/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
public class ListWithFilter extends HistoryManager {

    private Random r = new Random();

    public ListWithFilter(String givenId) {
        try {
            this.clearHistory();
            this.history.addAll(loadAllForWall(givenId));
            if (history.isEmpty()) {
                historyIndex = -1;
            } else {
                this.historyIndex = 0;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addToBoulderHistory(Boulder b) {
        if (b != null) {
            history.add(b);
            historyIndex = history.size() - 1;
        }
    }

    public ListWithFilter() {
        try {
            this.clearHistory();
            this.history.addAll(loadAll());
            if (history.isEmpty()) {
                historyIndex = -1;
            } else {
                this.historyIndex = 0;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Boulder getRandom() {
        if (history.isEmpty()) {
            return null;
        }
        historyIndex = r.nextInt(history.size());
        return getCurrentInHistory();
    }

    public int getIndex() {
        return historyIndex;
    }

    public int getSize() {
        return history.size();
    }

    public void setIndex(int index) {
        this.historyIndex = index;
    }

    public Vector<Boulder> getHistory() {
        return new Vector<Boulder>(history);
    }

    public void setIndex(String id) {
        for (int i = 0; i < history.size(); i++) {
            Boulder get = history.get(i);
            if (get.getFile().getName().equals(id)) {
                this.historyIndex = i;
                break;
            }
        }

    }

    private static List<Boulder> loadAllForWall(String wallId) throws IOException {
        List<Boulder> all = loadAll();
        List<Boulder> walls = new ArrayList<>(all.size());
        for (Boulder b : all) {
            if (b.getWall().equals(wallId)) {
                walls.add(b);
            }
        }
        return walls;
    }

    private static List<Boulder> loadAll() throws IOException {
        File[] all = Files.bouldersDir.listFiles();
        List<Boulder> allbldrs = new ArrayList<>(all.length);
        for (File bfile : all) {
            Boulder b = Boulder.load(bfile);
            allbldrs.add(b);
        }
        Collections.sort(allbldrs, new Comparator<Boulder>() {

            @Override
            public int compare(Boulder o1, Boulder o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
        return allbldrs;
    }

    public Grade getEasiest() {
        int min = Integer.MAX_VALUE;
        for (Boulder b : history) {
            if (!b.getGrade().isRandom()) {
                min = Math.min(min, b.getGrade().toNumber());
            }
        }
        if (min == Integer.MAX_VALUE) {
            min = 0;
        }
        return new Grade(min);
    }

    public Grade getHardest() {
        int max = Integer.MIN_VALUE;
        for (Boulder b : history) {
            max = Math.max(max, b.getGrade().toNumber());
        }
        if (max == Integer.MIN_VALUE) {
            max = Grade.currentGrades().size() - 1;
        }
        return new Grade(max);
    }

    public int getShortest() {
        int min = Integer.MAX_VALUE;
        for (Boulder b : history) {
            min = Math.min(min, b.getPathLength());
        }
        if (min == Integer.MAX_VALUE) {
            min = 0;
        }
        return min;
    }

    public int getLongest() {
        int max = Integer.MIN_VALUE;
        for (Boulder b : history) {
            max = Math.max(max, b.getPathLength());
        }
        if (max == Integer.MIN_VALUE) {
            max = 100;
        }
        return max;
    }

    public String getAuthors() {
        Set<String> s = new HashSet();
        for (Boulder b : history) {
            s.add(b.getAuthor());
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (String a : s) {
            sb.append(a).append("<br>");
        }
        return sb.toString();
    }

    public Date getOldest() {
        long min = Long.MAX_VALUE;
        for (Boulder b : history) {
            min = Math.min(min, b.getDate().getTime());
        }
        if (min == Integer.MAX_VALUE) {
            return new Date(0);
        }
        return new Date(min);
    }

    public Date getYoungest() {
        long max = Long.MIN_VALUE;
        for (Boulder b : history) {
            max = Math.max(max, b.getDate().getTime());
        }
        if (max == Integer.MIN_VALUE) {
            return new Date();
        }
        return new Date(max);
    }

}
