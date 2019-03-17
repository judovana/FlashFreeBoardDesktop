/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.util.Date;

/**
 *
 * @author jvanek
 */
public class GuiLogHelper {

    public static final GuiLogHelper guiLogger = new GuiLogHelper();

    StringBuffer sout = new StringBuffer();
    StringBuffer serr = new StringBuffer();

    public void logo(String s) {
        GuiLogHelper.guiLogger.logo(s);
        sout.append(new Date().toString()).append(": ").append(s);
    }

    public void loge(String s) {
        System.err.println(s);
        sout.append(new Date().toString()).append(": ").append(s);
    }

    public void loge(Throwable e) {
        e.printStackTrace();
        String s = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);
        loge(s);

    }

}
