/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author jvanek
 */
public class Filter {

    private final String wall;
    private final int gradeFrom;
    private final int gradeTo;
    private final int pathMin;
    private final int pathTo;
    private final String[] authorLike;
    private final String[] nameLike;
    private final long ageFrom;
    private final long ageTo;

    public Filter(String wall, int gradeFrom, int gradeTo, int pathMin, int pathTo, String authorLike, String nameLike, Date ageFrom, Date ageTo) {
        this.wall = wall;
        this.gradeFrom = Math.min(gradeFrom, gradeTo);
        this.gradeTo = Math.max(gradeFrom, gradeTo);
        this.pathMin = Math.min(pathMin, pathTo);
        this.pathTo = Math.max(pathMin, pathTo);
        this.authorLike = split(authorLike);
        this.nameLike = split(nameLike);
        this.ageFrom = Math.min(ageFrom.getTime(), ageTo.getTime());
        this.ageTo = Math.max(ageFrom.getTime(), ageTo.getTime());
    }

    boolean accept(Boulder b) {
        return containsAny(b.getName(), nameLike)
                && containsAny(b.getAuthor(), authorLike)
                && b.getWall().equals(wall)
                && (b.getGrade().toNumber() >= gradeFrom || b.getGrade().isRandom())
                && (b.getGrade().toNumber() <= gradeTo || b.getGrade().isRandom())
                && b.getPathLength() >= pathMin
                && b.getPathLength() <= pathTo
                && b.getDate().getTime() >= ageFrom
                && b.getDate().getTime() <= ageTo;

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
    }

    private static String[] split(String s) {
        return s.trim().split("\\s+");
    }

}
