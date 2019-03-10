/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author jvanek
 */
public class RelativePoint {

    private double x;
    private double y;
    private final Meassurable parent;
    private final Rectangle2D.Double bounds;

    public RelativePoint(double x, double y, Meassurable parent) {
        this(x, y, parent, new Rectangle2D.Double(0, 0, 1, 1));
    }

    public RelativePoint(double x, double y, Meassurable parent, Rectangle2D.Double relativeBounds) {
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.bounds = relativeBounds;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRealX() {
        return (int) (x * (double) parent.getWidth());
    }

    public int getRealY() {
        return (int) (y * (double) parent.getHeight());
    }

    public boolean setX(double x) {
        if (x >= bounds.x && x <= bounds.x + bounds.width) {
            this.x = x;
            return true;
        } else {
            return false;
        }
    }

    public boolean setY(double y) {
        if (y >= bounds.y && y <= bounds.y + bounds.height) {
            this.y = y;
            return true;
        } else {
            return false;
        }
    }

    public void draw(Graphics g) {
        int r = 50;
        Color c = g.getColor();
        g.setColor(new Color(this.hashCode()));
        g.drawOval(getRealX() - r, getRealY() - r, 2 * r, 2 * r);
        g.setColor(c);
    }

    public boolean isInAreaOfControl(double x, double y) {
        return x >= bounds.x && x <= bounds.x + bounds.width
                && y >= bounds.y && y <= bounds.y + bounds.height;
    }

}
