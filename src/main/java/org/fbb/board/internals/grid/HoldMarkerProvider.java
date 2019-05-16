/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grid;

/**
 *
 * @author jvanek
 */
public interface HoldMarkerProvider {

    public static final int FILL = 0;
    public static final int C_BIG = 1;
    public static final int C_SMALL = 2;
    public static final int C_BOTH = 3;
    public static final int E_BIG = 4;
    public static final int E_SMALL = 5;
    public static final int E_BOTH = 6;
    public static final int RECT = 7;

    public int getStartRed();

    public int getStartGreen();

    public int getStartBlue();

    public int getPathRed();

    public int getPathGreen();

    public int getPathBlue();

    public int getTopRed();

    public int getTopGreen();

    public int getTopBlue();

    public int getStyle();

}
