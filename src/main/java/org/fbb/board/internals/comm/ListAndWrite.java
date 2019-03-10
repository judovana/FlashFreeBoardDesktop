/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.comm;

/**
 *
 * @author jvanek
 */
public interface ListAndWrite {

    public static interface LogConsole {

        public void print(Exception ex);
    }

    public ConnectionID[] listDevices();

    public void writeToDevice(String id, byte[]... b);

   // public void setLogConsole(LogConsole l);
    //need tee stream, and as the BT and Wire are static, different architecture

}
