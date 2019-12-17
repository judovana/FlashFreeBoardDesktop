package org.fbb.board.desktop.gui.awtimpl;

import org.fbb.board.desktop.gui.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.grid.GridPane;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.db.GuiExceptionHandler;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class CreateWindow {

  

    public static void createSelectOrImportWall(String urlorFile, final JFrame... redundants) throws IOException {
        JDialog f = new JDialog((JFrame) null, Translator.R("MainWindowSetWall"), true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        LoadBackgroundOrImportOrLoadWall panel = new LoadBackgroundOrImportOrLoadWall(urlorFile, Authenticator.auth, MainWindow.db, MainWindow.gs, MainWindow.puller);
        f.add(panel);
        final boolean[] ok = new boolean[]{false};
        panel.setOkAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //validate first?
                ok[0] = true;
                f.setVisible(false);
            }
        });
        f.setSize(500, 200);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        if (!ok[0]) {
            return;
        }
        URL r = panel.getResult();
        f.dispose();
        InputStream is = r.openStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        try {
            BufferedImage bis = ImageIO.read(new ByteArrayInputStream(buffer.toByteArray()));
            if (bis == null) {
                throw new NullPointerException("Not a valid image");
            }
            createWindow(bis, new File(r.getPath()).getName(), redundants);
        } catch (Exception ex) {
            //not image, wall?
            try {
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()));
                createWindow(zis, new File(r.getPath()).getName(), redundants);
                zis.close();
            } catch (Exception exx) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(null, ex);
                GuiLogHelper.guiLogger.loge(exx);
                JOptionPane.showMessageDialog(null, exx);
            }
        }

    }

    private static void createWindow(BufferedImage bis, String name, JFrame... redundants) {
        //get rid of transaprency
        BufferedImage newBufferedImage = new BufferedImage(bis.getWidth(),
                bis.getHeight(), BufferedImage.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(bis, 0, 0, Color.WHITE, null);
        createWindowIpl(newBufferedImage, Files.sanitizeFileName(name + " " + new Date().toString()), null, redundants);
    }

    private static void createWindow(ZipInputStream zis, String name, JFrame... redundants) throws IOException {
        GridPane.Preload preloaded = GridPane.preload(zis, name);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        createWindowIpl(bi, name, preloaded.props, redundants);

    }

    private static void createWindowIpl(BufferedImage bis, String fname, byte[] props, JFrame... redundants) {
        try {
            Authenticator.auth.authenticate(Translator.R("wallChange"));
        } catch (Authenticator.AuthoriseException a) {
            GuiLogHelper.guiLogger.loge(a);
            JOptionPane.showMessageDialog(null, a);
            return;
        }
        final JFrame createWallWindow = new JFrame("Flash Free Board");
        createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bis, props, MainWindow.gs);
        createWallWindow.add(gp);
        double ratio = WinUtils.getIdealWindowSizw(bis);
        double nw = ratio * (double) bis.getWidth();
        double nh = ratio * (double) bis.getHeight();
        JTextField name = new JTextField(fname);
        JButton done = new JButton(Translator.R("Bdone"));
        JPanel tools = new JPanel(new GridLayout(4, 1));
        JPanel tools2 = new JPanel(new GridLayout(1, 2));
        JPanel tools3 = new JPanel(new GridLayout(1, 4));
        int DEFAULT_COLUMNS = 20;
        int DEFAULT_ROWS = 30;
        if (props != null) {
            DEFAULT_COLUMNS = gp.getGrid().getHorLines() - 1;
            DEFAULT_ROWS = gp.getGrid().getVertLines() - 1;
        }
        JSpinner sw = new JSpinner(new SpinnerNumberModel(DEFAULT_ROWS, 1, 10000, 1));
        JSpinner sh = new JSpinner(new SpinnerNumberModel(DEFAULT_COLUMNS, 1, 10000, 1));
        gp.getGrid().setVertLines(DEFAULT_ROWS + 1);
        gp.getGrid().setHorLines(DEFAULT_COLUMNS + 1);
        sh.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gp.getGrid().setHorLines((Integer) sh.getValue() + 1);
                gp.repaintAndSend(MainWindow.gs);
            }
        });
        sw.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gp.getGrid().setVertLines((Integer) sw.getValue() + 1);
                gp.repaintAndSend(MainWindow.gs);
            }
        });
        JButton reset = new JButton(Translator.R("Breset"));
        JButton test = new JButton(Translator.R("Btest"));
        JButton clean = new JButton(Translator.R("Bclean"));
        JCheckBox grid = new JCheckBox(Translator.R("Bgrid"));
        JCheckBox push = new JCheckBox("rewrite on exit");
        push.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!push.isSelected()) {
                    int result = JOptionPane.showConfirmDialog(
                            createWallWindow,
                            "?????", "??????!!!!!????", JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        push.setSelected(true);
                    }
                }
            }
        });
        grid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().setShowGrid(grid.isSelected());
                gp.repaintAndSend(MainWindow.gs);
            }
        });
        clean.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().clean();
                gp.repaintAndSend(MainWindow.gs);
            }
        });
        test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().randomBoulder(null);
                gp.repaintAndSend(MainWindow.gs);
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().reset();
                gp.repaintAndSend(MainWindow.gs);
            }
        });

        grid.setSelected(true);
        push.setSelected(true);
        createWallWindow.add(tools, BorderLayout.SOUTH);
        tools.add(name);
        tools2.add(sw);
        tools2.add(sh);
        tools3.add(reset);
        tools3.add(test);
        tools3.add(clean);
        tools3.add(push);
        tools3.add(grid);
        tools.add(tools2);
        tools.add(tools3);
        tools.add(done);
        done.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                name.setText(Files.sanitizeFileName(name.getText()));
                String n = name.getText();
                if (!n.endsWith(".wall")) {
                    n = n + ".wall";
                }
                File f = Files.getWallFile(n);
                if (f.exists()) {
                    if (push.isSelected()) {
                        int result = JOptionPane.showConfirmDialog(
                                createWallWindow,
                                Translator.R("Fexs", n), Translator.R("Fexs"), JOptionPane.YES_NO_OPTION);
                        if (result != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                } else {
                    if (!push.isSelected()) {
                        int result = JOptionPane.showConfirmDialog(
                                createWallWindow,
                                Translator.R("noFexsNoRews", n), Translator.R("noFexsNoRews"), JOptionPane.OK_OPTION);
                        return;
                    }
                }
                f.getParentFile().mkdirs();
                try {
                    if (push.isSelected()) {
                        gp.save(f);
                        MainWindow.db.add(new GuiExceptionHandler(), "(wall " + f.getName() + ")", f);
                    }
                    Files.setLastBoard(n);
                    createWallWindow.dispose();
                    MainWindowImpl.loadWallWithBoulder(n);
                    if (redundants != null) {
                        for (int i = 0; i < redundants.length; i++) {
                            redundants[i].dispose();

                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }

        });
        createWallWindow.pack();
        createWallWindow.setSize((int) nw, (int) nh + tools.getHeight());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WinUtils.setIdealWindowLocation(createWallWindow);
                createWallWindow.setVisible(true);
            }
        });
    }



}
