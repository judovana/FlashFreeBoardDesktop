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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
public class Filter implements Serializable {

    public static final String  days = "dd/MM/yyyy";
    public static final String hours = "HH:mm";
    public static final DateFormat dtf = new SimpleDateFormat(days+" "+hours);
    public static final DateFormat df = new SimpleDateFormat(days);
    public static final DateFormat tf = new SimpleDateFormat(hours);
    public static final DateTimeFormatter dtdtg = DateTimeFormatter.ofPattern(days);
    
    public static final ArrayList<DateTimeFormatter> dtdtgs(){
        //yah  the caller from lgooddatepicker requires arraylist
        ArrayList<DateTimeFormatter> l = new ArrayList<>(1);
        l.add(dtdtg);
        return l;
        
    }
    
    public static Filter getAllMatching(String wall) {
        return new Filter(wall, Grade.getMinGrade(), Grade.getMaxGrade(), Integer.MIN_VALUE, Integer.MAX_VALUE, "", "", new Date(Long.MIN_VALUE), new Date(Long.MAX_VALUE / 2/*there is + in comparsion*/), true);
    }

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

    @Override
    public String toString() {
        return wall + "\n"
                + "Grade : " + new Grade(gradeFrom) + "-" + new Grade(gradeTo) + " [" + random + "]\n"
                + "Date  : " + dtf.format(new Date(ageFrom)) + "-" + dtf.format(new Date(ageTo)) + "\n"
                + "Length: " + pathMin + "-" + pathTo + "\n"
                + "Author: " + Arrays.toString(authorLike) + "\n"
                + "Name  : " + Arrays.toString(nameLike);
    }

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

    //https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
    private static final Pattern p = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"");
    //

    private static String[] split(String s) {
        Matcher m = p.matcher(s.trim());
        List<String> l = new LinkedList<>();
        while (m.find()) {
            String ss = m.group();
            if (!ss.trim().isEmpty()) {
                if (ss.startsWith("\"") && ss.endsWith("\"")) {
                    ss = ss.substring(1, ss.length() - 1);
                }
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
        Properties p = new Properties();
        p.put("wall", "" + wall);
        p.put("gradeFrom", "" + gradeFrom);
        p.put("gradeTo", "" + gradeTo);
        p.put("pathMin", "" + pathMin);
        p.put("pathTo", "" + pathTo);
        p.put("authorLike", "" + arrayToQuotedString(authorLike));
        p.put("nameLike", "" + arrayToQuotedString(nameLike));
        p.put("ageFrom", "" + ageFrom);
        p.put("ageTo", "" + ageTo);
        p.put("random", "" + random);
        p.store(os, "FlashFreeBoard filter " + new Date());;
    }

    public static Filter load(File f) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return load(fis);
        }
    }

    public static Filter load(InputStream f) throws IOException {
        Properties p = new Properties();
        p.load(f);
        return new Filter(p.getProperty("wall"),
                Integer.valueOf(p.getProperty("gradeFrom", "" + Grade.getMinGrade())),
                Integer.valueOf(p.getProperty("gradeTo", "" + Grade.getMinGrade())),
                Integer.valueOf(p.getProperty("pathMin", "" + Grade.getMinGrade())),
                Integer.valueOf(p.getProperty("pathTo", "" + Grade.getMinGrade())),
                p.getProperty("authorLike", ""),
                p.getProperty("nameLike", ""),
                new Date(Long.valueOf(p.getProperty("ageFrom", "" + Long.MIN_VALUE))),
                new Date(Long.valueOf(p.getProperty("ageTo", "" + Long.MAX_VALUE / 2))),
                Boolean.valueOf(p.getProperty("random", "false")));
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
