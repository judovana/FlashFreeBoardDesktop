/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author jvanek
 */
public class FUtils {

    public static void align(int from, int to, JPanel p) {
        for (int i = from; i < to; i++) {
            p.add(new JLabel());
            p.add(new JLabel());
        };
    }

}
