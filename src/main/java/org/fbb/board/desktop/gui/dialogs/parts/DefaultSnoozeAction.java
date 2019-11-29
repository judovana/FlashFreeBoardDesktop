/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs.parts;

import org.fbb.board.desktop.gui.dialogs.CampusLikeDialog;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class DefaultSnoozeAction implements Runnable {

    private final GridPane gp;

    public DefaultSnoozeAction(GridPane gp) {
        this.gp = gp;
    }

    @Override
    public void run() {
        try {
            for (int x = 0; x < gp.getGrid().getWidth(); x++) {
                gp.getGrid().clean();
                CampusLikeDialog.drawColumn(x, 1, gp.getGrid());
                gp.repaintAndSendToKnown();
                Thread.sleep(100);
            }
            for (int x = gp.getGrid().getWidth() - 1; x >= 0; x--) {
                gp.getGrid().clean();
                CampusLikeDialog.drawColumn(x, 1, gp.getGrid());
                gp.repaintAndSendToKnown();
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            //unimportant
        }
    }
}
