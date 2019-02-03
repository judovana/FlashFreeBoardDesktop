/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
public class Grid {

    private final RelativePoint ul, ur, bl, br;
//    private final int initialHorStripCunt = 24;
//    private final int initialVerStripCunt = 44;
//    private final int initialHorStripCunt = 4;
//    private final int initialVerStripCunt = 4;
    private final int initialHorStripCunt = 18;
    private final int initialVerStripCunt = 11;
    private Line[] horLines = new Line[initialHorStripCunt + 1];
    private Line[] vertLines = new Line[initialVerStripCunt + 1];
    //all poligons for clicks
    private List<Polygon> ps = new ArrayList<>(horLines.length * vertLines.length);
    //all possible statuses, considering also resizing, and be warned, there can be garbage at the end
    private static final int hyperRedundantArray = 1000 * 1000;
    //cuurenly by COLUMN. see setHorLines and bottom of createLines
    private final byte[] psStatus = new byte[hyperRedundantArray];
    //=>
    //0 3 6
    //1 4 6
    //2 5 8
    //natural via monitors LR corner being [0,0]
    /**
     * See getArray.... functions. That is, set of functions, which are handling
     * position o your arduino controller (LT,RT,LB,TB) and whether your strips
     * are rows or columns
     */
    private RelativePoint selected;
    private boolean showGrid = true;
    private int holdStyle = 0;
    private static final int FILL = 0;
    private static final int C_BIG = 1;
    private static final int C_SMALL = 2;
    private static final int C_BOTH = 3;
    private static final int E_BIG = 4;
    private static final int E_SMALL = 5;
    private static final int E_BOTH = 6;
    private static final int RECT = 7;

    public Grid(Meassurable parent, byte[] load) {
        ul = new RelativePoint(0, 0, parent, new Rectangle2D.Double(0, 0, 0.5, 0.5));
        ur = new RelativePoint(1, 0, parent, new Rectangle2D.Double(0.5, 0, 0.5, 0.5));
        bl = new RelativePoint(0, 1, parent, new Rectangle2D.Double(0, 0.5, 0.5, 0.5));
        br = new RelativePoint(1, 1, parent, new Rectangle2D.Double(0.5, 0.5, 0.5, 0.5));
        createLines();
        if (load != null) {
            try {
                Properties p = new Properties();
                p.load(new ByteArrayInputStream(load));
                String v = p.getProperty("rows");
                if (v != null) {
                    setVertLines(Integer.valueOf(v) + 1);
                }
                v = p.getProperty("columns");
                if (v != null) {
                    setHorLines(Integer.valueOf(v) + 1);
                }
                readCoord(ul, "ul", p);
                readCoord(ur, "ur", p);
                readCoord(bl, "bl", p);
                readCoord(br, "br", p);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        createLines();
    }

    private void createLines() {
        //horizontal  line
        double leftStepX = ((double) bl.getRealX() - (double) ul.getRealX()) / (double) (horLines.length - 1);
        double leftStepY = ((double) bl.getRealY() - (double) ul.getRealY()) / (double) (horLines.length - 1);

        double rightStepX = ((double) br.getRealX() - (double) ur.getRealX()) / (double) (horLines.length - 1);
        double rightStepY = ((double) br.getRealY() - (double) ur.getRealY()) / (double) (horLines.length - 1);

        for (int i = 0; i < horLines.length; i++) {
            double x1 = (double) ul.getRealX() + (double) i * leftStepX;
            double y1 = (double) ul.getRealY() + (double) i * leftStepY;
            double x2 = (double) ur.getRealX() + (double) i * rightStepX;
            double y2 = (double) ur.getRealY() + (double) i * rightStepY;
            horLines[i] = new Line((int) x1, (int) y1, (int) x2, (int) y2);
        }
        //vertical  lines
        double upStepX = ((double) ur.getRealX() - (double) ul.getRealX()) / (double) (vertLines.length - 1);
        double upStepY = ((double) ur.getRealY() - (double) ul.getRealY()) / (double) (vertLines.length - 1);

        double downStepX = ((double) br.getRealX() - (double) bl.getRealX()) / (double) (vertLines.length - 1);
        double downStepY = ((double) br.getRealY() - (double) bl.getRealY()) / (double) (vertLines.length - 1);
        for (int i = 0; i < vertLines.length; i++) {
            double x1 = (double) ul.getRealX() + (double) i * upStepX;
            double y1 = (double) ul.getRealY() + (double) i * upStepY;
            double x2 = (double) bl.getRealX() + (double) i * downStepX;
            double y2 = (double) bl.getRealY() + (double) i * downStepY;
            vertLines[i] = new Line((int) x1, (int) y1, (int) x2, (int) y2);
        }
        //polygons on crossings!
        ps = new ArrayList<>(horLines.length * vertLines.length);
        for (int y = 0; y < vertLines.length - 1; y++) {
            for (int x = 0; x < horLines.length - 1; x++) {
                Point p1 = vertLines[y].cross(horLines[x]);
                Point p2 = vertLines[y + 1].cross(horLines[x]);
                Point p3 = vertLines[y + 1].cross(horLines[x + 1]);
                Point p4 = vertLines[y].cross(horLines[x + 1]);
                if (p1 == null || p2 == null || p3 == null || p4 == null) {
                    System.out.println("NULL: " + y + "; " + x + ";");
                } else {
                    int[] xs = new int[]{p1.x, p2.x, p3.x, p4.x};
                    int[] ys = new int[]{p1.y, p2.y, p3.y, p4.y};
                    ps.add(new Polygon(xs, ys, 4));
                }

            }
        }
    }

    void draw(Graphics g) {
        //        ul.draw(g);
        //        ur.draw(g);
        //        bl.draw(g);
        //        br.draw(g);
        createLines();//really?
        if (showGrid) {
            for (int i = 0; i < horLines.length; i++) {
                Line horLine = horLines[i];
                horLine.draw(g);

            }
            for (int i = 0; i < vertLines.length; i++) {
                Line vertLine = vertLines[i];
                vertLine.draw(g);

            }
        }

        int style = Math.abs(holdStyle);
        int alpha = 100;
        System.out.println("" + style);
        if (style != FILL) {
            alpha = 255;
        }
        for (int i = 0; i < ps.size(); i++) {
            if (psStatus[i] > 0) {
                Polygon get = ps.get(i);
                Color c;

                switch (psStatus[i]) {
                    case 2://green start
                        c = new Color(0, 200, 0, alpha);
                        break;
                    case 1: //blue, climb
                        c = new Color(0, 0, 200, alpha);
                        break;
                    case 3: //top red
                        c = new Color(200, 0, 0, alpha);
                        break;
                    default:
                        c = new Color(0, 0, 0, alpha);
                        break;
                }
                g.setColor(c);
                if (style >= C_BIG && style <= C_BOTH) {
                    int x = (get.xpoints[0] + get.xpoints[1] + get.xpoints[2] + get.xpoints[3]) / 4;
                    int y = (get.ypoints[0] + get.ypoints[1] + get.ypoints[2] + get.ypoints[3]) / 4;
                    int dmin = Integer.MAX_VALUE;
                    int dmax = Integer.MIN_VALUE;
                    for (int d1 = 0; d1 < 4; d1++) {
                        for (int d2 = 0; d2 < 4; d2++) {
                            if (d1 != d2) {
                                int dcandidate = dist(get.xpoints[d1], get.ypoints[d1], get.xpoints[d2], get.ypoints[d2]);
                                dmin = Math.min(dmin, dcandidate);
                                dmax = Math.max(dmin, dcandidate);
                            }
                        }
                    }
                    int width = 4;
                    for (int j = -width / 2; j <= width; j++) {
                        if (style == C_SMALL || style == C_BOTH) {
                            int ulx = x - dmin / 2 - j;
                            int uly = y - dmin / 2 - j;
                            g.drawOval(ulx, uly, dmin + j * 2, dmin + j * 2);
                        }
                        if (style == C_BIG || style == C_BOTH) {
                            int ulx = x - dmax / 2 - j;
                            int uly = y - dmax / 2 - j;
                            g.drawOval(ulx, uly, dmax + j * 2, dmax + j * 2);
                        }

                    }
                } else if (style >= E_BIG && style <= E_BOTH) {
                    int x1max = Math.max(get.xpoints[0], get.xpoints[3]);
                    int x1min = Math.min(get.xpoints[0], get.xpoints[3]);
                    int y1max = Math.max(get.ypoints[0], get.ypoints[1]);
                    int y1min = Math.min(get.ypoints[0], get.ypoints[1]);
                    int x2max = Math.max(get.xpoints[1], get.xpoints[2]);
                    int x2min = Math.min(get.xpoints[1], get.xpoints[2]);
                    int y2max = Math.max(get.ypoints[2], get.ypoints[3]);
                    int y2min = Math.min(get.ypoints[2], get.ypoints[3]);
                    int wmax = x2max - x1min;
                    int wmin = x2min - x1max;
                    int hmax = y2max - y1min;
                    int hmin = y2min - y1max;
                    int width = 4;
                    for (int j = -width / 2; j <= width; j++) {
                        if (style == E_BOTH || style == E_BIG) {
                            g.drawOval(x1min - j, y1min - j, wmax + j * 2, hmax + j * 2);
                        }
                        if (style == E_BOTH || style == E_SMALL) {
                            g.drawOval(x1max - j, y1max - j, wmin + j * 2, hmin + j * 2);
                        }

                    }
                } else if (style == RECT) {
                    int width = 4;
                    int[] xs = new int[get.xpoints.length];
                    int[] ys = new int[get.ypoints.length];
                    for (int j = -width / 2; j <= width; j++) {
                        for (int k = -width / 2; k <= width; k++) {
                            for (int l = 0; l < xs.length; l++) {
                                ys[l] = get.ypoints[l] + j;

                            }
                            for (int l = 0; l < ys.length; l++) {
                                xs[l] = get.xpoints[l] + k;

                            }
                            g.drawPolygon(xs, ys, xs.length);
                        }
                    }
                } else {
                    g.fillPolygon(get);
                }
                //g.setColor(Color.black);
                g.drawString("" + i, get.xpoints[3], get.ypoints[3]);
            }
        }
//        byte[] bb = getArrayT2BthenL2R();
//        System.out.println(Arrays.toString(bb));
//        bb = getArrayB2TthenL2R();
//        System.out.println(Arrays.toString(bb));
//        bb = getArrayL2RthenB2T();
//        System.out.println(Arrays.toString(bb));
//        bb = getArrayL2RthenT2B();
//        System.out.println(Arrays.toString(bb));

    }

    private int dist(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    public RelativePoint getBl() {
        return bl;
    }

    public RelativePoint getBr() {
        return br;
    }

    public RelativePoint getUl() {
        return ul;
    }

    public RelativePoint getUr() {
        return ur;
    }

    void unselect() {
        this.selected = null;
    }

    void select(double relX, double relY) {
        this.selected = getSelect(relX, relY);
    }

    RelativePoint getSelect(double relX, double relY) {
        if (bl.isInAreaOfControl(relX, relY)) {
            return bl;
        }
        if (ul.isInAreaOfControl(relX, relY)) {
            return ul;
        }
        if (ur.isInAreaOfControl(relX, relY)) {
            return ur;
        }
        if (br.isInAreaOfControl(relX, relY)) {
            return br;
        }
        return null;
    }

    boolean moveSelected(double relX, double relY) {
        if (selected == null) {
            return false;
        } else {
            boolean b1 = selected.setX(relX);
            boolean b2 = selected.setY(relY);
            createLines();
            return b1 || b2;
        }
    }

    public int getHorLines() {
        return horLines.length;
    }

    public int getVertLines() {
        return vertLines.length;
    }

    public void setHorLines(int i) {
        if (i < 2) {
            return;
        }
        ///recalcualte selected points index!
        int usedColumns = horLines.length - 1;//not lines, strips (current, not future)
        if (horLines.length > i) {
            //shrinkink
            int increment = 0;
            for (int j = 0; j < psStatus.length; j++) {
                if (j % usedColumns == 0 && j > 0) {
                    increment = increment + 1;
                }
                byte b = psStatus[j];
                //tail garbage lost
                if (j + increment >= psStatus.length) {
                    break;
                }
                psStatus[j] = 0;
                psStatus[j - increment] = b;

            }
        }
        if (horLines.length < i) {
            //growing
            int increment = psStatus.length / (usedColumns + 2);
            //start on latest full multiple
            int start = increment * usedColumns;
            for (int j = start; j >= 0; j--) {
                if (j % usedColumns == (usedColumns - 1) && j < start) {
                    increment = increment - 1;
                }
                byte b = psStatus[j];
                psStatus[j] = 0;
                psStatus[j + increment] = b;

            }
        }
        this.horLines = new Line[i];
        createLines();
        System.out.println(horLines.length + " x " + vertLines.length);
    }

    public void setVertLines(int i) {
        if (i < 2) {
            return;
        }
        ///no need to recalcualte selected points index!
        this.vertLines = new Line[i];
        createLines();
        System.out.println(horLines.length + " x " + vertLines.length);
    }

    public void setPs(int absX, int absY) {
        for (int i = 0; i < ps.size(); i++) {
            Polygon get = ps.get(i);
            if (get.contains(absX, absY)) {
                int r = psStatus[i];
                r = r + 1;
                if (r > 3) {
                    r = 0;
                }
                psStatus[i] = (byte) r;
            }
        }
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setHoldStyle(int change) {
        this.holdStyle = this.holdStyle + change;
        this.holdStyle = holdStyle % 8;
    }

    public void randomBoulder() {
        for (int i = 0; i < psStatus.length; i++) {
            psStatus[i] = 0;
        }
        Random r = new Random();
        int rv = r.nextInt(100);
        int starts = 3;
        if (rv > 80) {
            starts = 2;
        }
        if (rv < 20) {
            starts = 4;
        }
        //stripes!!
        int x;//0 - vertLines.length-2 :)
        int y;//0 - horLines.length-2 :) 
        x = r.nextInt(vertLines.length - 1);
        y = horLines.length - 2;

        psStatus[coordToIndex(x, y)] = 2;
        for (int start = 1; start < starts; start++) {
            int s1 = getDirection(r) * getStep(r);
            x = x + s1;
            if (x < 0 || x > vertLines.length - 2) {
                x = x - 2 * s1;
            }
            int s2 = -getStep(r);
            y = y + s2;
            if (y < 0 || y > horLines.length - 2) {
                y = y - 2 * s2;
            }
            psStatus[coordToIndex(x, y)] = 2;
        }
        while (true) {
            int s1 = getDirection(r) * getStep(r);
            x = x + s1;
            if (x < 0 || x > vertLines.length - 2) {
                x = x - 2 * s1;
            }
            int s2 = -getStep(r);
            //sometimes turn down
            if (r.nextInt(100) < 10) {
                s2 = -1 * s2;
            }
            y = y + s2;
            if (y <= 0) {
                y = 0;
                psStatus[coordToIndex(x, y)] = 3;
                break;
            } else {
                if (y > horLines.length - 2) {
                    y = y - 2 * s2;
                }
                psStatus[coordToIndex(x, y)] = 1;
            }
        }

    }

    private int coordToIndex(int x, int y) {
        int r = x * (horLines.length - 1) + y;
        System.out.println(x + " x " + y + " => " + r);
        return r;
    }

    private int getDirection(Random r) {
        if (r.nextBoolean()) {
            return 1;
        } else {
            return -1;
        }

    }

    private int getStep(Random r) {
        int minSize = (Math.min(horLines.length - 1, vertLines.length - 1));
        if (minSize <= 3) {
            return 1;
        }
        if (minSize <= 4) {
            if (r.nextBoolean()) {
                return 1;
            } else {
                return 2;
            }
        }
        if (minSize <= 5) {
            int rv = r.nextInt(100);
            if (rv > 70) {
                return 1;
            } else if (rv < 30) {
                return 2;
            }
            return 3;
        }
        if (minSize <= 6) {
            int step = 3;
            int rv = r.nextInt(100);
            if (rv > 90) {
                step = 1;
            } else if (rv > 70) {
                step = 2;
            } else if (rv < 30) {
                step = 4;
            }
            return step;
        }
        int step = 3;
        int rv = r.nextInt(100);
        if (rv > 90) {
            step = 1;
        } else if (rv > 80) {
            step = 2;
        } else if (rv < 10) {
            step = 5;
        } else if (rv < 30) {
            step = 4;
        }
        return step;
    }

    //returns the selected points as
    //topToObottom and then leftTOright
    //0 3 6
    //1 4 7
    //2 5 8
    //thsi should be most direct case of idenitcal copy
    //0 3 6      0 3 6
    //1 4 7  ->  1 4 7
    //2 5 8      2 5 8
    //[012345678]->[012345678]
    public byte[] getArrayT2BthenL2R() {
        byte[] r = new byte[(horLines.length - 1) * (vertLines.length - 1)];
        for (int i = 0; i < r.length; i++) {
            r[i] = psStatus[i];

        }
        return r;
    }

    /*L2R then B2T*/
    //0 3 6      6 7 8
    //1 4 7  ->  3 4 5
    //2 5 8      0 1 2
    //[012345678] -> [258147036]
    public byte[] getArrayL2RthenB2T() {
        byte[] r = new byte[(horLines.length - 1) * (vertLines.length - 1)];
        int i = 0;
        for (int y = horLines.length - 2; y >= 0; y--) {
            for (int x = 0; x < vertLines.length - 1; x++) {
                r[i] = psStatus[x * (horLines.length - 1) + y];
                i++;
            }
        }
        return r;
    }

    /*B2T L2R*/
    //0 3 6      2 5 8
    //1 4 7  ->  1 4 7
    //2 5 8      0 3 6
    //[012345678]->[210543876]
    //0 4 8      3 7 11
    //1 5 9  ->  2 6 10
    //2 6 10     1 5 9
    //3 7 11     0 4 8
    //[0123456789 10 11]->[32107654 11 10 98]
    public byte[] getArrayB2TthenL2R() {
        byte[] r = new byte[(horLines.length - 1) * (vertLines.length - 1)];
        int i = 0;
        for (int x = 0; x < vertLines.length - 1; x++) {
            for (int y = horLines.length - 2; y >= 0; y--) {
                r[i] = psStatus[x * (horLines.length - 1) + y];
                i++;
            }
        }
        return r;
    }

    /*L2R T2B*/
    //0 3 6      0 1 2
    //1 4 7  ->  3 4 5
    //2 5 8      6 7 8
    /* deal with start at right?, maybe later...*/
    public byte[] getArrayL2RthenT2B() {
        byte[] r = new byte[(horLines.length - 1) * (vertLines.length - 1)];
        int i = 0;
        for (int y = 0; y < horLines.length - 1; y++) {
            for (int x = 0; x < vertLines.length - 1; x++) {
                r[i] = psStatus[x * (horLines.length - 1) + y];
                i++;
            }
        }
        return r;
    }

    public void clean() {
        for (int i = 0; i < psStatus.length; i++) {
            psStatus[i] = 0;

        }
    }

    public void reset() {
        ul.setX(0);
        ul.setY(0);
        ur.setX(1);
        ur.setY(0);
        bl.setX(0);
        bl.setY(1);
        br.setX(1);
        br.setY(1);
    }

    byte[] getCoordsSave() throws IOException {
        Properties p = new Properties();
        p.setProperty("rows", "" + (vertLines.length - 1));
        p.setProperty("columns", "" + (horLines.length - 1));
        p.setProperty("ul.x", "" + ul.getX());
        p.setProperty("ul.y", "" + ul.getY());
        p.setProperty("ur.x", "" + ur.getX());
        p.setProperty("ur.y", "" + ur.getY());
        p.setProperty("bl.x", "" + bl.getX());
        p.setProperty("bl.y", "" + bl.getY());
        p.setProperty("br.x", "" + br.getX());
        p.setProperty("br.y", "" + br.getY());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.store(bos, null);
        return bos.toByteArray();
    }

    private void readCoord(RelativePoint coord, String id, Properties p) {
        String v = p.getProperty(id + ".x");
        if (v != null) {
            coord.setX(Double.valueOf(v));
        }
        v = p.getProperty(id + ".y");
        if (v != null) {
            coord.setY(Double.valueOf(v));
        }
    }

    public void saveCurrentBoulder(File file, String name, String wallId, Grade grade) throws IOException {
        Properties p = new Properties();
        p.setProperty("wall", wallId);
        p.setProperty("name", name);
        p.setProperty("start", getHolds(2));
        p.setProperty("path", getHolds(1));
        p.setProperty("top", getHolds(3));
        p.setProperty("grade", grade.toString());
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, null);
            fos.flush();
        }
    }

    private String getHolds(int i) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < horLines.length - 1; y++) {
            for (int x = 0; x < vertLines.length - 1; x++) {
                int status = psStatus[x * (horLines.length - 1) + y];
                if (status == i) {
                    sb.append(x).append(",").append(y).append(";");
                }
            }
        }
        return sb.toString();
    }
}
