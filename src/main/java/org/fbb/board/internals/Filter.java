/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import org.fbb.board.internals.grid.Boulder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jvanek
 */
public class Filter implements Serializable {

    public final String wall;
    public final int gradeFrom;
    public final int gradeTo;
    public final int pathMin;
    public final int pathTo;
    private final String[] authorLike;
    private final String[] nameLike;
    public final long ageFrom;
    public final long ageTo;
    public final boolean random;

    public Filter(String wall, int gradeFrom, int gradeTo, int pathMin, int pathTo, String authorLike, String nameLike, Date ageFrom, Date ageTo, boolean random) {
        this.wall = wall;
        this.gradeFrom = Math.min(gradeFrom, gradeTo);
        this.gradeTo = Math.max(gradeFrom, gradeTo);
        this.pathMin = Math.min(pathMin, pathTo);
        this.pathTo = Math.max(pathMin, pathTo);
        this.authorLike = split(authorLike);
        this.nameLike = split(nameLike);
        this.ageFrom = Math.min(ageFrom.getTime(), ageTo.getTime());
        this.ageTo = Math.max(ageFrom.getTime(), ageTo.getTime());
        this.random = random;
    }

    boolean accept(Boulder b) {
        return containsAny(b.getName(), nameLike)
                && containsAny(b.getAuthor(), authorLike)
                && b.getWall().equals(wall)
                && (b.getGrade().toNumber() >= gradeFrom || (b.getGrade().isRandom()) && random)
                && (b.getGrade().toNumber() <= gradeTo || (b.getGrade().isRandom()) && random)
                && b.getPathLength() >= pathMin
                && b.getPathLength() <= pathTo
                && b.getDate().getTime() >= ageFrom
                && b.getDate().getTime() <= ageTo + 61000; //+1m1s otherwise boulder created in this very minute will never be included

    }

    private static boolean containsAny(String source, String[] candidates) {
        if (candidates.length == 0 || (candidates.length == 1 && candidates[0].trim().isEmpty())) {
            return true;
        }
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String... args) {
        GuiLogHelper.guiLogger.logo(Arrays.toString(split("a b ")));
        GuiLogHelper.guiLogger.logo(Arrays.toString(split("")));
        GuiLogHelper.guiLogger.logo((String.valueOf(containsAny("", split("")))));
        GuiLogHelper.guiLogger.logo((String.valueOf(containsAny("aa ", split("")))));
        GuiLogHelper.guiLogger.logo(Arrays.toString(split("    xxx yyy")));
        Date d = new Date();
        GuiLogHelper.guiLogger.logo((d.toString()));
        GuiLogHelper.guiLogger.logo((new Date(d.getTime() + 61000).toString()));
        GuiLogHelper.guiLogger.logo(Arrays.toString(split(" a   \" b c \" \" d\"   e")));
        GuiLogHelper.guiLogger.logo(Arrays.toString(split(" a   \" b c    e"))); //simply ignoring the quote
        GuiLogHelper.guiLogger.logo(Arrays.toString(split(" a   \" b c \"\" d\"   e"))); //wrong!!! cornercase causing fail
    }

    //https://stackoverflow.com/questions/10695143/split-a-quoted-string-with-a-delimiter
    private static final Pattern p = Pattern.compile("((?<=(\"))[\\w ]*(?=(\"(\\s|$))))|((?<!\")\\w+(?!\"))");
    //https://regex101.com/r/wM6yT9/1

    private static String[] split(String s) {
        Matcher m = p.matcher(s.trim());
        List<String> l = new LinkedList<>();
        while (m.find()) {
            String ss = m.group();
            if (!ss.trim().isEmpty()) {
                l.add(ss);
            }
        }
        return l.toArray(new String[0]);
    }

    public void save(File f) throws IOException {
        try (FileOutputStream oo = new FileOutputStream(f)) {
            write(oo);
        }
    }

    public void write(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
        oos.flush();
    }

    public static Filter load(File f) throws IOException, ClassNotFoundException {
        Object read = null;
        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(f))) {
            read = oos.readObject();

        }
        return (Filter) read;
    }

    public String getAuthorsString() {
        return arrayToQuotedString(this.authorLike);
    }

    public String getNamesString() {
        return arrayToQuotedString(this.nameLike);
    }

    private static String arrayToQuotedString(String[] candidates) {
        if (candidates.length == 0 || (candidates.length == 1 && candidates[0].trim().isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String candidate : candidates) {
            sb.append("\"");
            sb.append(candidate);
            sb.append("\" ");
        }
        String names = sb.toString();
        if (names.endsWith(" ")) {
            names = names.substring(0, names.length() - 1);
        }
        return names;
    }

}
