/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.Authenticator;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.Training;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
public class TrainingSaveLoadDialog extends JDialog {

    private final JFileChooser mainChooser;
    private final JSplitPane mainSplit;
    private final JTextArea preview;
    private final JButton delete;
    private final DB db;
    private Training result;

    public Training getResult() {
        return result;
    }

    public TrainingSaveLoadDialog(int type/*jfch.open/save*/, DB db) {
        this.db = db;
        this.setModal(true);
        this.setSize(800, 400);
        preview = new JTextArea();
        preview.setEditable(false);
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(400);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        mainChooser = new JFileChooser(Files.trainingsDir);
        mainChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".sitr") || f.isDirectory()) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "";
            }
        });
        mainChooser.setDialogType(type);
        mainChooser.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    if (!evt.getPropertyName().equals("ancestor")) {
                        if (mainChooser.getSelectedFile() != null) {
                            preview.setText(Training.loadSavedTraining(mainChooser.getSelectedFile()).toString());
                            delete.setText(Translator.R("Bdelete", mainChooser.getSelectedFile().getName()));
                        } else {
                            preview.setText("");
                            delete.setText(Translator.R("Bdelete"));
                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        mainChooser.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    if (mainChooser.getSelectedFile() == null) {
                        //nothing
                    } else {
                        //close
                        //return selectedFile
                        try {
                            result = Training.loadSavedTraining(mainChooser.getSelectedFile());
                        } catch (Exception ex) {
                            GuiLogHelper.guiLogger.loge(ex);
                            result = null;
                            JOptionPane.showMessageDialog(null, ex);
                            return;
                        }
                        TrainingSaveLoadDialog.this.dispose();
                    }
                } else {
                    //close
                    //return null
                    result = null;
                    TrainingSaveLoadDialog.this.dispose();
                }

            }
        });
        mainSplit.add(mainChooser);
        mainSplit.add(new JScrollPane(preview));
        this.add(mainSplit);
        delete = new JButton(Translator.R("Bdelete"));
        delete.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (mainChooser.getSelectedFile() != null) {
                    try {
                        Authenticator.auth.authenticate((Translator.R("Bdelete", mainChooser.getSelectedFile().getName())));
                        db.delte("", mainChooser.getSelectedFile());
                    } catch (Authenticator.AuthoriseException | IOException | GitAPIException ee) {
                        GuiLogHelper.guiLogger.loge(ee);
                        JOptionPane.showMessageDialog(null, ee);
                    } finally {
                        mainChooser.setSelectedFile(null);
                        mainChooser.rescanCurrentDirectory();
                    }

                }

            }
        });
        this.add(delete, BorderLayout.SOUTH);
    }

    public static void main(String... args) throws IOException {
        Grade.loadConversiontable();
        new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, new DB(new GlobalSettings())).setVisible(true);
    }
}
