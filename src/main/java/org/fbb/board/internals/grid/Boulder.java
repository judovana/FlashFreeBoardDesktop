/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * mere indexable description of boulder. It is recalculated when applied to the
 * wall
 *
 * @author jvanek
 */
public class Boulder implements Cloneable {

    public static Boulder createBoulder(File file, String name, String wallId, Grade grade, String start, String path, String top) {
        Properties p = new Properties();
        p.setProperty(WALL, wallId);
        p.setProperty(NAME, name);
        p.setProperty(START, start);
        p.setProperty(PATH, path);
        p.setProperty(TOP, top);
        p.setProperty(GRADE, "" + grade.toNumber());
        p.setProperty(DATE, new Date().getTime() + "");
        return new Boulder(p, file);
    }
    private static final String WALL = "wall";
    private static final String NAME = "name";
    private static final String START = "start";
    private static final String PATH = "path";
    private static final String TOP = "top";
    private static final String GRADE = "grade";
    private static final String DATE = "date";
    private static final String AUTHOR = "author";

    public static Boulder load(File boulderFile) throws IOException {
        Properties p = new Properties();
        try (FileInputStream fs = new FileInputStream(boulderFile)) {
            p.load(fs);
            return new Boulder(p, boulderFile);
        }
    }
    private final Properties map;
    private File file;

    public void setFile(File f) {
        file = f;
    }

    public void setGrade(Grade g) {
        map.setProperty(GRADE, "" + g.toNumber());
    }

    public void setName(String name) {
        map.setProperty(NAME, name);
    }

    public void setWall(String name) {
        map.setProperty(WALL, name);
    }

    public void setAuthor(String name) {
        map.setProperty(AUTHOR, name);
    }

    private Boulder(Properties p, File f) {
        this.map = p;
        this.file = f;
    }

    public void save() throws IOException {
        saveImpl();
    }

    public void saveAs(File f) throws IOException {
        this.file = f;
        saveImpl();
    }

    private void saveImpl() throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            map.store(fos, "x1,y1;x2,y2;...;xn,yn;");
            fos.flush();
        }
    }

    void apply(int[] psStatus, int heigt) {
        String start = map.getProperty(START);
        String path = map.getProperty(PATH);
        String top = map.getProperty(TOP);
        apply(psStatus, start, Grid.MARK_START, heigt);
        apply(psStatus, path, Grid.MARK_PATH, heigt);
        apply(psStatus, top, Grid.MARK_TOP, heigt);

    }

    public boolean isEmpty() {
        return map.getProperty(START).trim().isEmpty()
                && map.getProperty(TOP).trim().isEmpty()
                && map.getProperty(PATH).trim().isEmpty();
    }

    private void apply(int[] values, String compressedValue, int mark, int heigt) {
        if (compressedValue == null || compressedValue.trim().isEmpty()) {
            return;
        }
        String[] strcoords = compressedValue.trim().split(";");
        for (String strcoord : strcoords) {
            String[] strXY = strcoord.split(",");
            if (strXY.length != 2) {
                continue;
            }
            try {
                int x = Integer.valueOf(strXY[0]);
                int y = Integer.valueOf(strXY[1]);
                values[x * (heigt) + y] = mark;
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
            }
        }

    }

    public Grade getGrade() {
        return new Grade(map.getProperty(GRADE));
    }

    public String getName() {
        return map.getProperty(NAME);
    }

    public Date getDate() {
        String strDate = map.getProperty(DATE);
        if (strDate == null) {
            if (file != null) {
                return new Date(file.lastModified());
            } else {
                return new Date();
            }
        }
        return new Date(Long.valueOf(strDate.trim()));
    }

    public String getGradeAndName() {
        return getGrade() + ": " + getName();
    }

    public String getGradeAndNameAndAuthor() {
        return getGrade() + ": " + getName() + " - " + getAuthor();
    }

    public String getAuthorGradeAndName() {
        return "[" + getAuthor() + "] " + getGrade().shorten() + ": " + getName();
    }

    public File getFile() {
        return file;
    }

    public String getWall() {
        return map.getProperty(WALL);
    }

    public String getAuthor() {
        String a = map.getProperty(AUTHOR);
        if (a == null) {
            return "PC-robot";
        } else {
            return a;
        }
    }

    @Override
    public Boulder clone() throws CloneNotSupportedException {
        return (Boulder) super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (map.getProperty(AUTHOR) == null) {
            setAuthor(getAuthor());
        }
        if (((Boulder) obj).map.getProperty(AUTHOR) == null) {
            ((Boulder) obj).setAuthor(((Boulder) obj).getAuthor());
        }
        if (!(obj instanceof Boulder)) {
            return false;
        }
        return map.equals(((Boulder) obj).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public void setDate(Date date) {
        map.setProperty(DATE, date.getTime() + "");
    }

    public String getStandardTooltip() {
        return "<html>"
                + getName() + " (" + getWall() + ")<br/>"
                + "<b>" + getGrade().toAllValues("<br/>") + "</b>"
                + getDate() + "<br/>"
                + getAuthor();
    }

    public int getPathLength() {
        String compressedValue = map.getProperty(PATH);
        if (compressedValue == null || compressedValue.trim().isEmpty()) {
            return 0;
        }
        String[] strcoords = compressedValue.trim().split(";");
        int count = 0;
        for (String strcoord : strcoords) {
            String[] strXY = strcoord.split(",");
            if (strXY.length != 2) {
                continue;
            }
            count++;
        }
        return count;
    }

}
