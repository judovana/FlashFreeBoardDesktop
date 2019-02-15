/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.fbb.board.internals.comm.wired.ByteEater;
import org.fbb.board.internals.comm.wired.PortWork;

/**
 *
 * @author jvanek
 */
public class GlobalSettings implements ByteEater {

    private MessagesResender resender;

    public GlobalSettings() {
        resender = new MessagesResender();
        resender.start();
    }

    private class MessagesResender extends Thread {

        public MessagesResender() {
            this.setDaemon(true);
        }

        boolean alive = true;

        @Override
        public void run() {
            while (alive) {
                try {
                    consumeLastAndRefrshBuffer();
                    Thread.sleep(bufferEatingDaemonDelay);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void consumeLastAndRefrshBuffer() {
            if (messages.isEmpty()) {
                return;
            }
            ArrayList<byte[][]> l = new ArrayList<>(messages);
            byte[][] last = l.get(l.size() - 1);
            messages.removeAll(l);
            if (comm == COMM.PORT) {
                if (selectedPort == null) {
                    PortWork.writeTo(customPort, last);
                } else {
                    PortWork.writeTo(selectedPort, last);
                }
            }

        }
    }

    private final List<byte[][]> messages = Collections.synchronizedList(new ArrayList<>(50));

    @Override
    public void sendBytes(byte[]... b) {
        byte[][] m = toMessages(b);
        messages.add(m);
    }

    private byte[][] toMessages(byte[][] bs) {
        byte[][] r = new byte[bs.length][];
        for (int i = 0; i < bs.length; i++) {
            byte[] b = bs[i];
            r[i] = toMessage(b);

        }
        return r;
    }

    private byte[] toMessage(byte[] b) {
        byte[] r = new byte[b.length * 3];
        for (int i = 0; i < b.length; i++) {
            byte[] rgb = holdToColor(b[i]);
            r[i * 3] = rgb[0];
            r[i * 3 + 1] = rgb[1];
            r[i * 3 + 2] = rgb[2];
        }
        return r;
    }

    public static enum COMM {

        PORT,
        BLUETOOTH
    }

    private COMM comm = COMM.PORT;
    private String customPort = "/dev/ttyUSB0";
    private SerialPort selectedPort = null;
    private byte brightness = 5;
    private long bufferEatingDaemonDelay = 100;

    //0 nothing
    //1 blue
    //2 green
    //3 red
    public byte[] holdToColor(byte i) {
        switch (i) {
            case (0):
                return new byte[]{0, 0, 0};
            case (1):
                return new byte[]{0, 0, brightness};
            case (2):
                return new byte[]{0, brightness, 0};
            case (3):
                return new byte[]{brightness, 0, 0};
        }
        return null;
    }

}
