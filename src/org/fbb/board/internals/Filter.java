/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

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
    public final String[] authorLike;
    public final String[] nameLike;
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
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String... args) {
        System.out.println(Arrays.toString(split("a b ")));
        System.out.println(Arrays.toString(split("")));
        System.out.println((containsAny("", split(""))));
        System.out.println((containsAny("aa ", split(""))));
        System.out.println(Arrays.toString(split("    xxx yyy")));
        Date d = new Date();
        System.out.println((d.toString()));
        System.out.println((new Date(d.getTime() + 61000).toString()));
    }

    private static String[] split(String s) {
        return s.trim().split("\\s+");
    }

    public void save(File f) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(this);
            oos.flush();
        }
    }

    public static Filter load(File f) throws IOException, ClassNotFoundException {
        Object read = null;
        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(f))) {
            read = oos.readObject();

        }
        return (Filter) read;
    }

}
