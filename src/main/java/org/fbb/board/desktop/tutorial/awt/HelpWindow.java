/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial.awt;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.fbb.board.desktop.tutorial.ImgProvider;
import org.fbb.board.desktop.tutorial.StoryPart;
import org.fbb.board.desktop.tutorial.StoryProvider;

/**
 *
 * @author jvanek
 */
public class HelpWindow extends JDialog{

    public HelpWindow() throws IOException {
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setSize(650, 800);
        StoryProvider sp = new StoryProvider();
        ImgProvider ip = new ImgProvider();
        StoryPart start = sp.getStory().get(1);
        ImageWithTitles iwt = new ImageWithTitles(start.getLines(), ip.get(start.getImg()));
        this.add(iwt);
        JPanel tools = new JPanel(new GridLayout(1, 3));
        tools.add(new JButton("<<"));
        tools.add(new JButton("II"));
        tools.add(new JButton(">>"));
        this.add(tools, BorderLayout.SOUTH);
        
    }
    
    
    
    public static void main(String... args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try{
                    new HelpWindow().setVisible(true);
                }catch(IOException ex){
                    ex.printStackTrace();
                }
            }
        });
        
    }
    
}
