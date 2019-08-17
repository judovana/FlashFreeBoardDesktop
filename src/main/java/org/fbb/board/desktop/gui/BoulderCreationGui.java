/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class BoulderCreationGui {

    private final GlobalSettings gs;

    public BoulderCreationGui(GlobalSettings gs) {
        this.gs = gs;
    }

    public static void main(String... args) throws Exception {
        Grade.loadConversiontable();
        File f = new File("/home/jvanek/.config/FlashBoard/repo/walls/moon400test.wall");
        GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(f)), f.getName());
        new BoulderCreationGui(new GlobalSettings()).editBoulderImpl(preloaded, null);
    }

    static class BoulderAndSaved {

        final Boulder b;
        final boolean saved;

        public BoulderAndSaved(Boulder b, boolean saved) {
            this.b = b;
            this.saved = saved;
        }

    }

    BoulderAndSaved editBoulderImpl(final GridPane.Preload p, final Boulder orig) throws IOException, CloneNotSupportedException {
        //checkbox save? 
        //if not save, then what?
        //return  new BoulderAlways? - on Ok?
        final boolean[] change = new boolean[]{false, false, false};
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(p.img));
        final JDialog operateBoulder = new JDialog((JFrame) null, Translator.R("createBoulder"), true);
        operateBoulder.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bi, p.props, gs);
        gp.getGrid().setShowGrid(true);
        operateBoulder.add(gp);
        gp.enableBoulderModificationOnly();
        double ratio = MainWindow.getIdealWindowSizw(bi);
        double nw = ratio * (double) bi.getWidth();
        double nh = ratio * (double) bi.getHeight();
        if (orig != null) {
            gp.getGrid().clean();
            gp.getGrid().setBouler(orig);
        } else {
            gp.getGrid().clean();
        }
        JButton doneButton = new JButton(Translator.R("Bdone"));
        JPanel tools1 = new JPanel(new GridLayout(4, 1));
        JPanel tools2 = new JPanel(new BorderLayout());
        JComboBox<String> grades = new JComboBox<>(Grade.currentGrades());
        JTextField name = new JTextField();
        if (orig == null) {
            name.setText(Translator.R("missingName"));
            grades.setSelectedIndex(grades.getModel().getSize() / 3);
        } else {
            name.setText(orig.getName());
            grades.setSelectedItem(orig.getGrade().toString());
        }
        final JCheckBox saveOnExit = new JCheckBox(Translator.R("SaveOnExit"));
        if (orig == null || Authenticator.auth.isPernament()) {
            saveOnExit.setSelected(true);
        } else {
            saveOnExit.setSelected(false);
        }
        saveOnExit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (orig != null) {
                    if (saveOnExit.isSelected()) {
                        try {
                            Authenticator.auth.authenticate(Translator.R("AllowToEditBoulder"));
                        } catch (Authenticator.AuthoriseException ee) {
                            GuiLogHelper.guiLogger.loge(ee);
                            saveOnExit.setSelected(false);
                        }
                    }
                }
            }
        });
        JTextField author = new JTextField();
        if (orig == null) {
            author.setText(Translator.R("DefaultSign"));
        } else {
            author.setText(orig.getAuthor());
        }
        JLabel dateLabel;
        if (orig == null) {
            dateLabel = new JLabel(Filter.dtf.format(new Date()));
        } else {
            dateLabel = new JLabel(Filter.dtf.format(orig.getDate()));
        }
        JButton back = new JButton(Translator.R("Bback"));
        back.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                operateBoulder.dispose();
            }
        });

        JPanel tools1L1 = new JPanel(new BorderLayout());
        JPanel tools1L11 = new JPanel(new BorderLayout());
        JPanel tools1L2 = new JPanel(new BorderLayout());
        JPanel tools1L3 = new JPanel(new BorderLayout());
        JPanel tools1L4 = new JPanel(new BorderLayout());
        tools1L1.add(back, BorderLayout.WEST);
        tools1L1.add(new JLabel(Translator.R("nwBoulderWelcome")));
        tools1L11.add(dateLabel, BorderLayout.WEST);
        tools1L11.add(saveOnExit, BorderLayout.EAST);
        tools1L1.add(tools1L11, BorderLayout.EAST);
        tools1L2.add(new JLabel(Translator.R("BautorBoulder")+" "), BorderLayout.WEST);
        tools1L2.add(author);
        tools1L3.add(name);
        tools1L3.add(new JLabel(Translator.R("BtitleBoulder")+"  "), BorderLayout.WEST);
        tools1L4.add(grades);
        tools1L4.add(new JLabel(Translator.R("BgradeBoulder")+"   "), BorderLayout.WEST);
        tools1.add(tools1L1);
        tools1.add(tools1L2);
        tools1.add(tools1L3);
        tools1.add(tools1L4);
        operateBoulder.add(tools1, BorderLayout.NORTH);
        JCheckBox gridb = new JCheckBox(Translator.R("Bgrid"));
        gridb.setSelected(true);
        gridb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().setShowGrid(gridb.isSelected());
                gp.repaintAndSend(gs);
            }
        });
        DocumentListener dl1 = new ChangeRecodingDocumentListener(change, 1);
        DocumentListener dl2 = new ChangeRecodingDocumentListener(change, 2);
        name.getDocument().addDocumentListener(dl1);
        author.getDocument().addDocumentListener(dl2);
        grades.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                change[0] = true;
            }
        });
        tools2.add(gridb, BorderLayout.EAST);
        tools2.add(doneButton);
        operateBoulder.add(tools2, BorderLayout.SOUTH);
        operateBoulder.pack();
        operateBoulder.setSize((int) nw, (int) nh + tools1.getHeight() + tools2.getHeight());
        MainWindow.setIdealWindowLocation(operateBoulder);
        DoneEditingBoulderListener done = new DoneEditingBoulderListener(orig, saveOnExit, operateBoulder, gp.getGrid(), name, grades, p.givenId, author, change);
        doneButton.addActionListener(done);
        operateBoulder.setVisible(true);
        return new BoulderAndSaved(done.getResult(), saveOnExit.isSelected());

    }

    private static class DoneEditingBoulderListener implements ActionListener {

        private final String wallId;

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                actionPerformedImpl(e);
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(parent, ex);
            }
        }

        public Boulder getResult() {
            return result;
        }

        private Boulder result;
        private final Boulder orig;
        private final JCheckBox saveOnExit;
        private final JDialog parent;
        private final Grid grid;
        private final JTextField nwNameProvider;
        private final JTextField nwAuthorProvider;
        private final JComboBox<String> grades;
        private final boolean[] changed;

        public DoneEditingBoulderListener(Boulder orig, JCheckBox saveOnExit, JDialog parent, Grid grid, JTextField nwNameProvider, JComboBox<String> grades, String wallId, JTextField nwAuthorProvider, boolean[] changed) {
            this.orig = orig;
            this.saveOnExit = saveOnExit;
            this.nwNameProvider = nwNameProvider;
            this.parent = parent;
            this.grid = grid;
            this.grades = grades;
            this.wallId = wallId;
            this.nwAuthorProvider = nwAuthorProvider;
            this.changed = changed;
        }

        public void actionPerformedImpl(ActionEvent e) throws IOException {
            //0=grade; 1=name, 2=author
            if (/*saveOnExit.isSelected() && ?*/(!changed[0] || !changed[1] || !changed[2])) {
                int a = JOptionPane.showConfirmDialog(parent, Translator.R("ForgotAll",
                        !changed[0] ? Translator.R("grade") : "",
                        !changed[1] ? Translator.R("name") : "",
                        !changed[2] ? Translator.R("author") : "")
                );
                if (a != 1) {
                    return;
                }
            }
            Boulder possibleReturnCandidate;
            if (orig != null) {
                possibleReturnCandidate = grid.createBoulderFromCurrent(orig.getFile(), nwNameProvider.getText(), wallId, new Grade(grades.getSelectedIndex()));
            } else {
                possibleReturnCandidate = grid.createBoulderFromCurrent(null, nwNameProvider.getText(), wallId, new Grade(grades.getSelectedIndex()));
            }
            if (orig == null && possibleReturnCandidate.isEmpty()) {
                return;
            }
            possibleReturnCandidate.setAuthor(nwAuthorProvider.getText());
            String possibleFileName = Files.sanitizeFileName(nwNameProvider.getText());
            File possibleTargetFile = Files.getBoulderFile(possibleFileName + ".bldr");
            if (orig != null && orig.getFile() != null) {
                possibleReturnCandidate.setFile(possibleTargetFile);
            }
            if (orig != null) {
                possibleReturnCandidate.setDate(orig.getDate());
            }
            if (saveOnExit.isSelected()) {
                possibleReturnCandidate.setFile(possibleTargetFile);
                if (possibleReturnCandidate.getFile().exists()) {
                    int a = JOptionPane.showConfirmDialog(null, Translator.R("RewriteBoulder", nwNameProvider.getText()));
                    if (a == JOptionPane.YES_OPTION) {
                        if (orig == null) {
                            //was new one, but have overlaping name
                            try {
                                Authenticator.auth.authenticate(Translator.R("nwBldrOverwrite"));
                                possibleReturnCandidate.save();
                            } catch (Authenticator.AuthoriseException ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(null, ex);
                            }
                        } else {
                            possibleReturnCandidate.save();
                        }
                    } else {
                        return;
                    }
                } else {
                    possibleReturnCandidate.save();
                }
            }
            if (orig == null) {
                result = possibleReturnCandidate;
            } else {
                if (possibleReturnCandidate.equals(orig)) {
                    result = null; //????
                    // result = possibleReturnCandidate;
                } else {
                    result = possibleReturnCandidate;
                }
            }
            parent.setVisible(false);
            parent.dispose();
        }
    }

    private static class ChangeRecodingDocumentListener implements DocumentListener {

        private final boolean[] change;
        private final int index;

        public ChangeRecodingDocumentListener(boolean[] change, int index) {
            this.change = change;
            this.index = index;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            change[index] = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            change[index] = true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            change[index] = true;

        }
    }
}
