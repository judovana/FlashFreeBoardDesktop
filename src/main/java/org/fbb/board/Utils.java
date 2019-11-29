/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jvanek
 */
public class Utils {

    public static List<int[]> bresenhamLine(int x1, int y1, int x2, int y2) {
        List<int[]> l = new ArrayList<>();
        if ((x1 == x2) && (y1 == y2)) {
            l.add(new int[]{x1, y1});
        } else {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int diff = dx - dy;

            int moveX, moveY;

            if (x1 < x2) {
                moveX = 1;
            } else {
                moveX = -1;
            }
            if (y1 < y2) {
                moveY = 1;
            } else {
                moveY = -1;
            }

            while ((x1 != x2) || (y1 != y2)) {

                int p = 2 * diff;

                if (p > -dy) {
                    diff = diff - dy;
                    x1 = x1 + moveX;
                }
                if (p < dx) {
                    diff = diff + dx;
                    y1 = y1 + moveY;
                }
                l.add(new int[]{x1, y1});
            }
        }
        return l;
    }
}
