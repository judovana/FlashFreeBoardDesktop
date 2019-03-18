/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.fbb.board.Translator;

/**
 *
 * @author jvanek
 */
public class GitAuthenticator {

    private char[] pernament = null;

    public boolean isPernament() {
        return pernament != null;
    }

    public char[] authenticate(String message) {
        char[] r = authenticateImpl(message);
        return r;

    }

    private char[] authenticateImpl(String message) {
        if (pernament != null) {
            return pernament;
        }
        Object[] r = new AuthoriseDialog().show(message);
        if (r == null || r.length != 2) {
            return null;
        }
        char[] s = (char[]) (r[0]);
        if (s == null || new String(s).trim().isEmpty()) {
            return null;
        }
        if ((boolean) r[1]) {
            pernament = s;
        }
        return s;
    }

    private static class AuthoriseDialog {

        public static Object[] show(String message) {
            JPasswordField pass = new JPasswordField(5);
            JCheckBox save = new JCheckBox(Translator.R("keepLoged"), false);

            JPanel myPanel = new JPanel(new GridLayout(3, 1));
            myPanel.add(new JLabel(message));
            myPanel.add(pass);
            myPanel.add(save);
            int result = JOptionPane.showConfirmDialog(null, myPanel, Translator.R("autenticateGit"), JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                return new Object[]{pass.getPassword(), save.isSelected()};
            }
            return null;
        }
    }

}
