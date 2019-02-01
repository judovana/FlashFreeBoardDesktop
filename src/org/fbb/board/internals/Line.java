/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 *
 * @author jvanek
 */
public class Line {

    private final Point from;
    private final Point to;

    public Line(Point from, Point to) {
        this.from = from;
        this.to = to;
    }

    public Line(int x1, int y1, int x2, int y2) {
        this.from = new Point(x1, y1);
        this.to = new Point(x2, y2);
    }

    void draw(Graphics g) {
        g.drawLine(from.x, from.y, to.x, to.y);
    }

    Point cross(Line l) {
        //not playing with paralel or identicals - they cant be form nature of grid
        Point.Double p1 = new Point2D.Double(from.x, from.y);
        Point2D.Double v1 = new Point2D.Double(to.x - from.x, to.y - from.y);
        Point.Double p2 = new Point2D.Double(l.from.x, l.from.y);
        Point2D.Double v2 = new Point2D.Double(l.to.x - l.from.x, l.to.y - l.from.y);
        Point2D.Double r = calculateInterceptionPoint(p1, v1, p2, v2);
        if (r == null) {
            return null;
        }
        return new Point((int) r.x, (int) r.y);
    }

    public static Point.Double calculateInterceptionPoint(Point2D.Double p1, Point2D.Double v1, Point2D.Double p2, Point2D.Double v2) {

        double sNumerator = p1.y * v1.x + p2.x * v1.y - p1.x * v1.y - p2.y * v1.x;
        double sDenominator = v2.y * v1.x - v2.x * v1.y;

        // parallel ... 0 or infinite points, or one of the vectors is 0|0
        if (sDenominator == 0) {
            return null;
        }

        double s = sNumerator / sDenominator;

        double t;
        if (v1.x != 0) {
            t = (p2.x + s * v2.x - p1.x) / v1.x;
        } else {
            t = (p2.y + s * v2.y - p1.y) / v1.y;
        }

        Point.Double i1 = new Point.Double(p1.x + t * v1.x, p1.y + t * v1.y);

        return i1;

    }

}
