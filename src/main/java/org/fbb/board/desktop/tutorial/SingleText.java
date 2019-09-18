/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial;

/**
 *
 * @author jvanek
 */
public class SingleText {
    
    private final int x;
    private final int y;
    private final String key;

    public SingleText(int x, int y, String key) {
        this.x = x;
        this.y = y;
        this.key = key;
    }

    public int getX(double d) {
        return (int)((double)x*d);
    }
    
    public int getY(double d) {
        return (int)((double)y*d);
    }

    public String getKey() {
        return key;
    }
    
    
    
    
    
    
}
