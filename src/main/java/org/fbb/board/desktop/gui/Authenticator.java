/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;

/**
 *
 * @author jvanek
 */
public class Authenticator {

    private boolean pernament = false;
    public static final Authenticator auth = new Authenticator();

    public boolean isPernament() {
        return pernament || Files.getAuthFileHash() == null;
    }

    public void authenticate(String message) throws AuthoriseException {
        try {
            if (!authenticateImpl(message)) {
                throw new AuthoriseException();
            }
        } catch (Exception ex) {
            throw new AuthoriseException(ex);
        }
    }

    private boolean authenticateImpl(String message) throws NoSuchAlgorithmException {
        String hash = Files.getAuthFileHash();
        if (hash == null) {
            //warning security by obscurity!
            //if the file do not exists, app is not "secured" at all
            //note, that all the config files are plain an readable in ~/.config anyway
            //to secure them, encrypt them with locekd pass
            //if app is run as limited user, then the security should be ok
            return true;
        }
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

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(s.getBytes(StandardCharsets.UTF_8));
        byte[] passHash = md.digest();
        //to nice (correct) string
        StringBuilder sb = new StringBuilder();
        for (byte b : passHash) {
            sb.append(String.format("%02x", b));
        }
        if (hash.equals(sb.toString())) {
            if ((boolean) r[1]) {
                pernament = true;
            }
            return true;
        }
        return false;
    }

    public void revoke() {
        pernament = false;
    }

    public String[] getStatus() {
        if (Files.getAuthFileHash() == null) {
            return new String[]{
                Translator.R("secNone"),
                "use `su " + System.getProperty("user.name") + "; echo -n  your_pass | sha256sum  > " + Files.localAuth,
                " or `su ; echo -n  your_pass | sha256sum  > " + Files.masterAuth+ " && chmod 600 it"};
        }
        if (isPernament()) {
            return new String[]{
                Translator.R("secUnlocked"),
                "To lock, again, use: ",
                Translator.R("revokePP")};
        }
        return new String[]{Translator.R("secOk"), "", ""};
    }

    private static class AuthoriseDialog {

        public static Object[] show(String message) {
            JPasswordField pass = new JPasswordField(5);
            JCheckBox save = new JCheckBox(Translator.R("keepLoged"), false);

            JPanel myPanel = new JPanel(new GridLayout(3, 1));
            myPanel.add(new JLabel(message));
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

        public AuthoriseException(String s) {
            super(s);
        }

        public AuthoriseException() {
            this(Translator.R("authFail"));
        }

        private AuthoriseException(Exception ex) {
            super(ex);
        }

    }

}
