/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.db;

import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 * @param <T>
 */
public interface ExceptionHandler<T extends Throwable> {

    public void handleWithIssue(T t) throws T;

    public void handleCleanly(T t);

    class LoggingEater implements ExceptionHandler<Throwable> {

        @Override
        public void handleWithIssue(Throwable t) throws Throwable {
            throw new UnsupportedOperationException("Should not occure");
        }

        @Override
        public void handleCleanly(Throwable t) {
            GuiLogHelper.guiLogger.loge(t);
        }
    }

}
