/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grades;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;
import org.fbb.board.Translator;

/**
 *
 * @author jvanek
 */
public class Grade {

    private static String[] columns;
    private static final List<String[]> valuesPerGrade = new ArrayList<>(100);
    private static final Map<String, List<String>> valuesPerColumn = new HashMap<>();

    //to be used by combobox
    public static Vector<String> currentGrades() {
        return new Vector<>(valuesPerColumn.get(usedGrades));
    }

    public static void loadConversiontable() throws IOException {
        InputStream s = Grade.class.getResourceAsStream("tabulka");
        Properties p = new Properties();
        p.load(s);
        //<0 == random
        //>=0 grades
        columns = parseLine(p, "id");
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            valuesPerColumn.put(column, new ArrayList<>(100));
        }
        int x = -1;
        while (true) {
            x++;
            String[] stringsForX = parseLine(p, x + "");
            if (stringsForX == null) {
                break;
            }
            valuesPerGrade.add(stringsForX);
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                valuesPerColumn.get(column).add(valuesPerGrade.get(x)[i]);
            }
        }
    }

    private static String[] parseLine(Properties p, String id) {
        if (p.getProperty(id) == null) {
            return null;
        }
        String line = p.getProperty(id).trim();
        return line.split("\\s+");
    }

    private static final int RANDOM = -1;
    public static String usedGrades = "fountainbleu";
    private final int artificialValue;

    public static Grade RandomBoulder() {
        return new Grade(RANDOM);
    }

    public Grade(String artificialValue) {
        this(Integer.valueOf(artificialValue));
    }

    public Grade(int artificialValue) {
        this.artificialValue = artificialValue;
    }

    public String toAllValues(String delimiter) {
        if (artificialValue <= RANDOM) {
            return Translator.R("RandomUnknown") + delimiter;
        }
        StringBuilder sbs = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            sbs.append(column).append(": ").append(valuesPerGrade.get(artificialValue)[i]).append(delimiter);
        }
        return sbs.toString();
    }

    public String toString() {
        if (artificialValue <= RANDOM) {
            return Translator.R("RandomUnknown");
        }
        return valuesPerColumn.get(usedGrades).get(artificialValue);
    }

    public int toNumber() {
        return artificialValue;
    }

    @Override
    public int hashCode() {
        return artificialValue;
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(toNumber(), toNumber());
    }

    public static String getStandardTooltip(int i) {
        return "<b>" + new Grade(i).toAllValues("<br/>") + "</b>";
    }

}
