/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.awtimpl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.fbb.board.Translator;

/**
 *
 * @author jvanek
 */
public class TipsListener implements ActionListener {

    public TipsListener() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, "<html>" + "<li>" + Translator.R("tip1") + "<ul>" + "<li>" + escape(Translator.R("tip2")) + "<li>" + escape(Translator.R("tip3")) + "</ul>" + "<ul>" + "<li>" + escape(Translator.R("tip4")) + "<li>" + escape(Translator.R("tip5")) + "<li>" + escape(Translator.R("tip6")) + "</ul>" + "<li>" + escape(Translator.R("tips7")) + "<ul>" + "<li>" + escape(Translator.R("tips8")) + "<li>" + escape(Translator.R("tips9")) + "<li>" + escape(Translator.R("tips10")) + "</ul>");
    }

    private String escape(String R) {
        return R.replace("<", "&lt;").replace(">", "&gt;");
    }
    
}
