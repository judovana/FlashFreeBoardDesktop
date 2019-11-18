/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.db.Puller;

/**
 *
 * @author jvanek
 */
public class LoadBackgroundOrImportOrLoadWall extends JPanel {

    private JTextField input;
    private JLabel info;
    private JLabel valid;
    private JButton select;
    private JButton ok;
    private JButton settings;

    private final DB db;
    private final GlobalSettings gs;
    private final Authenticator auth;
    private final Puller puller;

    public static String getDefaultUrl() {
        try {
            return Files.wallsDir.toURI().toURL().toString();
        } catch (MalformedURLException ex) {
            GuiLogHelper.guiLogger.loge(ex);
            return Files.wallsDir.getAbsolutePath();
        }
    }

    public void setOkAction(ActionListener l) {
        ok.addActionListener(l);
    }

    LoadBackgroundOrImportOrLoadWall(String defaultPath, Authenticator auth, DB db, GlobalSettings gs, Puller puller) {
        this.auth = auth;
        this.db = db;
        this.gs = gs;
        this.puller = puller;

        input = new JTextField(defaultPath);
        valid = new JLabel();
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateUrl();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateUrl();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateUrl();
            }

        });
        validateUrl();
        //password?
        info = new JLabel(Translator.R("MainWindowSetWallInfo"));
        ok = new JButton(Translator.R("Bfinish"));
        if (Files.repoGit.exists()) {
            settings = new JButton(Translator.R("Settings"));
        } else {
            settings = new JButton(Translator.R("BinitialInit"));
        }
        settings.addActionListener(new SettingsListener(null, auth, gs, puller, db, 3, null));
        select = new JButton(Translator.R("Bselect"));
        select.addActionListener((ActionEvent e) -> {
            JFileChooser jf;
            try {
                URL u = new URL(input.getText());
                jf = new JFileChooser(u.getPath());
            } catch (MalformedURLException ex) {
                GuiLogHelper.guiLogger.loge(ex);
                jf = new JFileChooser(input.getText());
            }
            jf.setDialogType(JFileChooser.OPEN_DIALOG);
            int returnVal = jf.showDialog(LoadBackgroundOrImportOrLoadWall.this, Translator.R("Bok"));

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    input.setText(jf.getSelectedFile().toURI().toURL().toExternalForm());
                } catch (MalformedURLException ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        });
        this.setLayout(new GridLayout(4, 1));
        JPanel p = new JPanel(new GridLayout(1, 3));
        this.add(info);
        this.add(input);
        this.add(valid);
        p.add(ok);
        p.add(select);
        p.add(settings);
        this.add(p);
    }

    private void validateUrl() {
        int i = validateImp();
        switch (i) {
            case 0:
                valid.setForeground(Color.red);
                valid.setText(Translator.R("EmptyUrl"));
                break;
            case 1:
                valid.setForeground(Color.green);
                valid.setText(MainWindow.addCtrLine(Translator.R("ExistsFile")));
                break;
            case 11:
                valid.setForeground(Color.red);
                valid.setText(Translator.R("ExistsDir"));
                break;
            case 2:
                valid.setForeground(Color.orange);
                valid.setText(MainWindow.addCtrLine(Translator.R("NotExistsFile")));
                break;
            case 3:
                valid.setForeground(Color.green);
                valid.setText(MainWindow.addCtrLine(Translator.R("OkRemoteUrl")));
                break;
            default:
                valid.setForeground(Color.red);
                valid.setText("?????");
                break;
        }
    }

    private int validateImp() {
        String candidate = input.getText();
        if (candidate == null || candidate.trim().isEmpty()) {
            //empty
            return 0;
        }
        URL u;
        try {
            u = new URL(candidate);
            if (u.getProtocol().toLowerCase().equals("file")) {
                String fileUrl = u.getPath();
                if (new File(fileUrl).exists()) {
                    if (new File(fileUrl).isDirectory()) {
                        return 11;
                    } else {
                        //existing fiel url
                        return 1;
                    }
                } else {
                    //non existing file url
                    return 2;
                }
            } else {
                //valid remote url
                return 3;
            }
        } catch (MalformedURLException ex) {
            GuiLogHelper.guiLogger.loge(ex);
            if (new File(candidate).exists()) {
                if (new File(candidate).isDirectory()) {
                    return 11;
                } else {
                    //existing fiel url
                    return 1;
                }
            } else {
                return 2;
            }
        }

    }

    public URL getResult() {
        try {
            URL u = new URL(input.getText());
            return u;
        } catch (MalformedURLException ex) {
            GuiLogHelper.guiLogger.loge(ex);
            try {
                return new File(info.getText()).toURI().toURL();
            } catch (MalformedURLException eex) {
                throw new RuntimeException(eex);
            }
        }
    }
}
