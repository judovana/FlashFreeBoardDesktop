/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;

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

    public LoadBackgroundOrImportOrLoadWall() {
        this(getDefaultUrl());

    }

    private static String getDefaultUrl() {
        try {
            return Files.wallsDir.toURI().toURL().toString();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return Files.wallsDir.getAbsolutePath();
        }
    }

    LoadBackgroundOrImportOrLoadWall(URL u) {
        this(u.toExternalForm());

    }

    LoadBackgroundOrImportOrLoadWall(String defaultPath) {
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
        info = new JLabel(Translator.R("MainWindowSetWallInfo"));
        ok = new JButton(Translator.R("Bfinish"));
        select = new JButton(Translator.R("Bselect"));
        this.setLayout(new GridLayout(4, 1));
        JPanel p = new JPanel(new GridLayout(1, 2));
        this.add(info);
        this.add(input);
        this.add(valid);
        p.add(ok);
        p.add(select);
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
                valid.setText(Translator.R("ExistsFile"));
                break;
            case 2:
                valid.setForeground(Color.orange);
                valid.setText(Translator.R("NotExistsFile"));
                break;
            case 3:
                valid.setForeground(Color.green);
                valid.setText(Translator.R("OkRemoteUrl"));
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
                    //existing fiel url
                    return 1;
                } else {
                    //non existing file url
                    return 2;
                }
            } else {
                //valid remote url
                return 3;
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            if (new File(candidate).exists()) {
                //existing file 
                return 1;
            } else {
                //not existign file
                return 2;
            }
        }

    }

}
