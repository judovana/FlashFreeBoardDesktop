/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import org.fbb.board.internals.grid.Boulder;
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
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
public class ListWithFilter extends HistoryManager {

    private final Random r = new Random();
    private final Filter lastFilter;

    public ListWithFilter(List<Boulder> l) {
        this.lastFilter = null;
        this.clearHistory();
        this.history.addAll(l);
        if (history.isEmpty()) {
            historyIndex = -1;
        } else {
            this.historyIndex = 0;
        }
    }

    public ListWithFilter(Filter filter) {
        try {
            this.lastFilter = filter;
            this.clearHistory();
            this.history.addAll(loadAllForFilter(filter));
            if (history.isEmpty()) {
                historyIndex = -1;
            } else {
                this.historyIndex = 0;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ListWithFilter(Grade from, Grade to, String wall) {
        this(getQuickFilterOf(from, to, wall));
    }

    private static Filter getQuickFilterOf(Grade from, Grade to, String wall) {
        return new Filter(wall, from.toNumber(), to.hashCode(), Integer.MIN_VALUE, Integer.MAX_VALUE, "", "", new Date(Long.MIN_VALUE), new Date(Long.MAX_VALUE / 2/*there is + in comparsion*/), false);
    }

    private static Filter getQuickFilterOf(String wall, List<Boulder> l) {
        StringBuilder sb = new StringBuilder();
        for (Boulder b : l) {
            sb.append("\"");
            sb.append(b.getName());
            sb.append("\" ");
        }
        String names = sb.toString();
        if (names.endsWith(" ")) {
            names = names.substring(0, names.length() - 1);
        }
        return new Filter(wall, Grade.getMinGrade(), Grade.getMaxGrade(), Integer.MIN_VALUE, Integer.MAX_VALUE, "", names, new Date(Long.MIN_VALUE), new Date(Long.MAX_VALUE / 2/*there is + in comparsion*/), false);
    }

    public ListWithFilter(String givenId) {
        try {
            this.lastFilter = null;
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
            limit();
        }
    }

    public ListWithFilter() {
        try {
            this.lastFilter = null;
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

    private static List<Boulder> loadAllForWall(String wallId) throws IOException {
        List<Boulder> all = loadAll();
        List<Boulder> walls = new ArrayList<>(all.size());
        for (Boulder b : all) {
            //merges can left leftovers
            if (b == null || b.getWall() == null) {
                if (b != null && b.getFile() != null) {
                    b.getFile().delete();
                }
                continue;
            }
            if (b.getWall().equals(wallId)) {
                walls.add(b);
            }
        }
        return walls;
    }

    private static List<Boulder> loadAll() throws IOException {
        File[] all = Files.bouldersDir.listFiles();
        if (all == null) {
            all = new File[0];
        }
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

    private static List<Boulder> loadAllForFilter(Filter f) throws IOException {
        List<Boulder> all = loadAll();
        List<Boulder> walls = new ArrayList<>(all.size());
        for (Boulder b : all) {
            if (f.accept(b)) {
                walls.add(b);
            }
        }
        return walls;
    }

    public Filter getLastFilter() {
        //may be null!
        return lastFilter;
    }

    public Filter enumerate(String backupWall) {
        if (lastFilter == null || lastFilter.wall == null) {
            return getQuickFilterOf(backupWall, history);
        } else {
            return getQuickFilterOf(lastFilter.wall, history);
        }
    }

    public boolean isInRange(int seelctedIndex) {
        return seelctedIndex >= 0 && seelctedIndex < history.size();
    }

}
