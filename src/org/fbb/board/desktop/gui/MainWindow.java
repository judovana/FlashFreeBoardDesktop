package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GridLayout;
import java.awt.Rectangle;
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
import javax.swing.Action;
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
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GridPane;

/**
 *
 * @author jvanek
 */
public class MainWindow {

    public static void main(String... s) {
        try {
            if (Files.getLastBoard() != null && Files.getLastBoulder() != null) {
                //check if boards mathces
                //if so, show last boulder on last wall
                //if not warn, load boulder on its board if  its board exists || load wall only
            } else if (Files.getLastBoulder() != null && Files.getLastBoard() == null) {
                //warn, but load last boulder on its wall, if wall does noto exists, empty
            } else if (Files.getLastBoard() != null && Files.getLastBoulder() == null) {
                //load last wall, sugest to create boulder or generate random one
            } else {
                //both null, sugest to create board
                createSelectOrImportWall();
            }
        } catch (IOException ex) {
            //do better
            ex.printStackTrace();
        }
    }

    private static void createSelectOrImportWall() throws IOException {
        JDialog f = new JDialog((JFrame) null, Translator.R("MainWindowSetWall"), true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        LoadBackgroundOrImportOrLoadWall panel = new LoadBackgroundOrImportOrLoadWall();
        f.add(panel);
        panel.setOkAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //validate first?
                f.setVisible(false);
            }
        });
        f.setSize(500, 200);
        f.setVisible(true);
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
            createWindow(bis, new File(r.getPath()).getName());
        } catch (Exception ex) {
            //not image, wall?
            try {
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()));
                createWindow(zis, new File(r.getPath()).getName());
                zis.close();
            } catch (Exception exx) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex);
                exx.printStackTrace();
                JOptionPane.showMessageDialog(null, exx);
            }
        }

    }

    private static void createWindow(BufferedImage bis, String name) {
        //get rid of transaprency
        BufferedImage newBufferedImage = new BufferedImage(bis.getWidth(),
                bis.getHeight(), BufferedImage.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(bis, 0, 0, Color.WHITE, null);
        createWindowIpl(newBufferedImage, Files.sanitizeFileName(name + " " + new Date().toString()), null);
    }

    private static void createWindow(ZipInputStream zis, String name) throws IOException {
        GridPane.Preload preloaded = GridPane.preload(zis);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        createWindowIpl(bi, name, preloaded.props);

    }

    private static void createWindowIpl(BufferedImage bis, String fname, byte[] props) {
        final JFrame createWallWindow = new JFrame();
        createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bis, props);
        createWallWindow.add(gp);
        Rectangle size = ScreenFinder.getCurrentScreenSizeWithoutBounds();
        double dw = (double) size.width / (double) bis.getWidth();
        double dh = (double) size.height / (double) bis.getHeight();
        double ratio = Math.min(dw, dh);
        ratio = ratio * 0.8;//do not cover all screen
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
                gp.repaint();
            }
        });
        sw.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gp.getGrid().setVertLines((Integer) sw.getValue() + 1);
                gp.repaint();
            }
        });
        JButton reset = new JButton(Translator.R("Breset"));
        JButton test = new JButton(Translator.R("Btest"));
        JButton clean = new JButton(Translator.R("Bclean"));
        JCheckBox grid = new JCheckBox(Translator.R("Bgrid"));
        grid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().setShowGrid(grid.isSelected());
                gp.repaint();
            }
        });
        clean.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().clean();
                gp.repaint();
            }
        });
        test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().randomBoulder();
                gp.repaint();
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().reset();
                gp.repaint();
            }
        });

        grid.setSelected(true);
        createWallWindow.add(tools, BorderLayout.SOUTH);
        tools.add(name);
        tools2.add(sw);
        tools2.add(sh);
        tools3.add(reset);
        tools3.add(test);
        tools3.add(clean);
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
                File f = new File(Files.wallsDir, n);
                if (f.exists()) {
                    int result = JOptionPane.showConfirmDialog(
                            createWallWindow,
                            Translator.R("Fexs", n), Translator.R("Fexs"), JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                f.getParentFile().mkdirs();
                try {
                    gp.save(f);
                    createWallWindow.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, ex);
                }
            }

        });
        createWallWindow.pack();
        createWallWindow.setSize((int) nw, (int) nh + tools.getHeight());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createWallWindow.setVisible(true);
            }
        });
    }

}
