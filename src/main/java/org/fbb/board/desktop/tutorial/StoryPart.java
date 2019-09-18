/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jvanek
 */
public class StoryPart {

    private final String img;
    private final List<SingleText> lines;

    public StoryPart(String img) {
        this.img = img;
        this.lines = new ArrayList<>();
    }

    public void addLine(SingleText l) {
        lines.add(l);
    }

    public String getImg() {
        return img;
    }

    public List<SingleText> getLines() {
        return lines;
    }
    
    

}
