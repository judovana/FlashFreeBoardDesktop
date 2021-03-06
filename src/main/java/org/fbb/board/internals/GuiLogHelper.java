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

    public GuiLogHelper() {
        //add middleware, which catches client's application stdout/err
        //and will submit it into console
        System.setErr(new TeeOutputStream(System.err, true));
        System.setOut(new TeeOutputStream(System.out, false));
        //internal stdOut/Err are going throughs outLog/errLog
        //when console is off, those tees are not installed
    }

    public static final GuiLogHelper guiLogger = new GuiLogHelper();

    StringBuffer sout = new StringBuffer();
    StringBuffer serr = new StringBuffer();

    public synchronized String getSerr() {
        return serr.toString();
    }

    public String getSout() {
        return sout.toString();
    }

    public void logo(String s) {
        System.out.println(s);
    }

    int limit = 1000000;

    public synchronized void appendInternal(String s, boolean err) {
        StringBuffer a = sout;
        if (err) {
            a = serr;
        }
        if (s.startsWith(" ") | s.startsWith("\t")) {
            a.append(s);
        } else {
            a.append(new Date().toString()).append(": ").append(s);
        }
        if (a.length() > limit) {
            a.delete(0, a.length() - (limit - 1));
        }
    }

    public void loge(String s) {
        System.err.println(s);
    }

    public void loge(Throwable e) {
        e.printStackTrace(System.err);

    }

    public static void main(String... ar) {
        StringBuffer a = new StringBuffer("123456789");
        System.out.println(a.delete(0, a.length() - 5));
    }

}
