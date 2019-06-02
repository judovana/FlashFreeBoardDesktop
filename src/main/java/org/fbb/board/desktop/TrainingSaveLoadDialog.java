/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.fbb.board.Translator;
import org.fbb.board.desktop.gui.Authenticator;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.training.Training;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.training.TrainingPlaylist;

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
    private File result;

    public File getResult() {
        return result;
    }

    public TrainingSaveLoadDialog(int type/*jfch.open/save*/, DB db, File dir) {
        this.db = db;
        this.setModal(true);
        this.setSize(800, 400);
        preview = new JTextArea();
        preview.setEditable(false);
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(400);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        mainChooser = new JFileChooser(dir);
        UIManager.put("FileChooser.readOnly", Boolean.FALSE);
        mainChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".sitr") || f.isDirectory() || f.getName().endsWith(".tpl")) {
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
                        if (mainChooser.getSelectedFile() != null && mainChooser.getSelectedFile().isFile()) {
                            if (mainChooser.getSelectedFile().getName().endsWith(".sitr")) {
                                preview.setText(getFileName() + ": " + Training.loadSavedTraining(mainChooser.getSelectedFile()).toString());
                            } else if (mainChooser.getSelectedFile().getName().endsWith(".tpl")) {
                                preview.setText(TrainingPlaylist.loadSavedTraining(mainChooser.getSelectedFile()).toString());

                            }
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
                        result = mainChooser.getSelectedFile();
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
                        db.delte(" - " + mainChooser.getSelectedFile().getName(), mainChooser.getSelectedFile());
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
        new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, new DB(new GlobalSettings()), Files.trainingLilstDir).setVisible(true);
    }

    public String getFileName() {
        if (mainChooser.getSelectedFile() == null) {
            return null;
        } else {
            return mainChooser.getSelectedFile().getName().replaceAll("\\.sitr$", "").replaceAll("\\.tpl$", "");
        }
    }

    public static class SaveTrainingDialog {

        public static File show() throws Authenticator.AuthoriseException, IOException {
            Authenticator.auth.authenticate(Translator.R("SaveTraining"));
            JTextField fileName = new JTextField();
            JPanel myPanel = new JPanel(new GridLayout(2, 1));
            myPanel.add(new JLabel(Translator.R("tName2")));
            myPanel.add(fileName);
            int result = JOptionPane.showConfirmDialog(null, myPanel, Translator.R("tName1"), JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                File f = Files.getTraining(fileName.getText());
                if (f.exists()) {
                    throw new RuntimeException(Translator.R("tExists", fileName.getText(), f.getAbsolutePath()));
                }
                boolean okToDo = true;
                okToDo = okToDo && f.createNewFile();
                okToDo = okToDo && f.delete();
                if (okToDo) {
                    return f;
                } else {
                    throw new RuntimeException(Translator.R("tError1", fileName.getText()) + "\n" + Translator.R("tError2", f.getAbsolutePath()));
                }
            }
            return null;
        }
    }
}
