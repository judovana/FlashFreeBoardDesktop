/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.db;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.fbb.board.Translator;
import org.fbb.board.internals.GuiLogHelper;

public class GuiExceptionHandler implements ExceptionHandler<Throwable> {

    private static final List<Class> remebered = new ArrayList<>();

    @Override
    public void handleWithIssue(Throwable t) throws Throwable {
        throw new UnsupportedOperationException("Should not occure");
    }

    @Override
    public void handleCleanly(Throwable t) {
        GuiLogHelper.guiLogger.loge(t);
        if (remebered.contains(t.getClass())) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(null, t + "\n\n" + Translator.R("gitIssueQuestion"), Translator.R("gitIssue"), JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.NO_OPTION) {
            remebered.add(t.getClass());
        }
    }
}
