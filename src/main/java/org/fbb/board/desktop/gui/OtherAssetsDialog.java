/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.Dimension;
import java.net.URL;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import org.fbb.board.desktop.ScreenFinder;

/**
 *
 * @author jvanek
 */
public class OtherAssetsDialog extends JDialog {

    private final List<URL> files;

    public OtherAssetsDialog(List<URL> otherAssets) {
        this.files = otherAssets;
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension s = ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize();
        s.width = s.width / 6 * 5;
        s.height = s.height / 6 * 5;
        this.setSize(s);
        this.add(new JTextArea(files.get(0).toString()));
    }

}
