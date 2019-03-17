/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.util.Date;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 *
 * @author jvanek
 */
public class GuiLogHelper {

    public static final GuiLogHelper guiLogger = new GuiLogHelper();

    StringBuffer sout = new StringBuffer();
    StringBuffer serr = new StringBuffer();

    public synchronized String getSerr() {
        return serr.toString();
    }

    public synchronized String getSout() {
        return sout.toString();
    }

    public synchronized void logo(String s) {
        System.out.println(s);
        sout.append(new Date().toString()).append(": ").append(s).append("\n");
    }

    public synchronized void loge(String s) {
        System.err.println(s);
        serr.append(new Date().toString()).append(": ").append(s).append("\n");
    }

    public synchronized void loge(Throwable e) {
        e.printStackTrace();
        //String s = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);
        //loge(s);
        Throwable[] ss = org.apache.commons.lang3.exception.ExceptionUtils.getThrowables(e);
        boolean first = true;
        for (Throwable s : ss) {
            if (first) {
                first = false;
                serr.append(new Date().toString()).append(": ").append(ExceptionUtils.getStackTrace(s));
            } else {
                serr.append(ExceptionUtils.getStackTrace(s));
            }

        }
        serr.append("\n");

    }

}
