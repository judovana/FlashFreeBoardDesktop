/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.comm;

import org.fbb.board.internals.grid.Grid;

/**
 *
 * @author jvanek
 */
public interface ByteEater {

    public void sendBytes(int[] b, Grid id);
    public void deregisterProvider(Grid id);
    public void reset();
    public int getNumberOfRegisteredProviders();
    public int getMaxOfRegisteredProviders();

}
