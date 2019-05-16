/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grid;

import java.awt.Color;

/**
 *
 * @author jvanek
 */
public interface HoldMarkerProvider {

    //known styles
    public static final int FILL = 0;
    public static final int C_BIG = 1;
    public static final int C_SMALL = 2;
    public static final int C_BOTH = 3;
    public static final int E_BIG = 4;
    public static final int E_SMALL = 5;
    public static final int E_BOTH = 6;
    public static final int RECT = 7;
    //there is 8 of them
    public static int COUNT_OF_STYLES = 8;

    public float getStartRed();

    public float getStartGreen();

    public float getStartBlue();

    public float getPathRed();

    public float getPathGreen();

    public float getPathBlue();

    public float getTopRed();

    public float getTopGreen();

    public float getTopBlue();

    public float getHoldMarkerOapcity();

    public Color getGridColor();

    public int getDefaultStyle();

    public void setDefaultStyle(int a);

    public int getCurrentStyle();

    public void setCurrentStyle(int a);

}
