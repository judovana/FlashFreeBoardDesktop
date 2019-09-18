/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jvanek
 */
public class StoryProvider {
    
    private final List<StoryPart> story = new ArrayList(50);

    public StoryProvider() throws IOException {
            
            
    try(BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("/org/fbb/board/tutorial/show"), "utf-8"))) {
        while (true) {
            String s = br.readLine();
            if (s == null) {
                break;
            }
            if (s.startsWith(" ")||s.startsWith("\t")){
                String[] parts = s.trim().split("\\s");
                story.get(story.size()-1).addLine(new SingleText(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), parts[2]));
            }else{
                story.add(new StoryPart(s.trim()));
            }
        }
    }
    }
}
