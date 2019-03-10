/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.fbb.board.Translator;

/**
 *
 * @author jvanek
 */
public class Authenticator {

    private boolean pernament = false;

    public void authenticate(String message) throws AuthoriseException {
        if (!authenticateImpl(message)) {
            throw new AuthoriseException();
        }
    }

    private boolean authenticateImpl(String message) {
        if (pernament) {
            return true;
        }
        Object[] r = new AuthoriseDialog().show(message);
        if (r == null || r.length != 2) {
            return false;
        }
        String s = new String((char[]) (r[0]));
        if (s == null || s.trim().isEmpty()) {
            return false;
        }
        if (s.equals("changeit")) {
            if ((boolean) r[1]) {
                pernament = true;
            }
            return true;
        }
        return false;
    }

    private static class AuthoriseDialog {

        public static Object[] show(String message) {
            JPasswordField pass = new JPasswordField(5);
            JCheckBox save = new JCheckBox(Translator.R("keepLoged"), false);

            JPanel myPanel = new JPanel(new GridLayout(2, 1));
            myPanel.add(pass);
            myPanel.add(save);
            int result = JOptionPane.showConfirmDialog(null, myPanel, Translator.R("autenticate"), JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                return new Object[]{pass.getPassword(), save.isSelected()};
            }
            return null;
        }
    }

    public static class AuthoriseException extends Exception {

        public AuthoriseException() {
            super(Translator.R("authFail"));
        }

    }

}
