/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.awtimpl;

import javax.swing.JFrame;
import org.fbb.board.desktop.gui.MainWindow;

/**
 *
 * @author jvanek
 */
class IconifierThread extends Thread {
    JFrame target;
    private static final int WINDOW_COMFOR_ZONE = 5 * 60 * 1000;

    public IconifierThread() {
        super();
        this.target = null;
        this.setDaemon(true);
        this.setName("iconifier");
        this.start();
    }

    public void setTarget(JFrame target) {
        this.target = target;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(WINDOW_COMFOR_ZONE);
                if (target != null && MainWindow.gs.isPopUpping()) {
                    if (target.getState() == JFrame.ICONIFIED) {
                        target.setState(JFrame.NORMAL);
                    }
                }
            } catch (InterruptedException ex) {
            }
            ;
        }
    }
    
}
