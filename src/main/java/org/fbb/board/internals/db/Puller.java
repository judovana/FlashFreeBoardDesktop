/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.db;

import org.fbb.board.internals.GuiLogHelper;

public class Puller implements Runnable {

    private boolean alive = true;
    private int delay = 60;//1 minute
    private final DB db;

    private Puller(int seconds, DB db) {
        this.delay = seconds;
        this.db = db;
    }

    public void cancel() {
        alive = false;
    }

    public void setDelay(int seconds) {
        delay = seconds;
    }

    @Override
    public void run() {
        while (alive) {
            try {
                if (delay <= 0) {
                    Thread.sleep(60l * 1000l);
                    //GuiLogHelper.guiLogger.logo("Puller doing nothing...");
                } else {
                    Thread.sleep((long) delay * 1000l);
                    pull();
                }
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
            }
        }
        GuiLogHelper.guiLogger.logo("Puller ended");
    }

    public void pull() {
       //GuiLogHelper.guiLogger.logo("Auto pull!!!");
        db.pullCatched(new ExceptionHandler.LoggingEater());
    }

    public static Puller create(int seconds, DB db) {
        Puller p = new Puller(seconds, db);
        Thread t = new Thread(p);
        t.setDaemon(true);
        t.start();
        return p;
    }

}
