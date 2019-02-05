package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.Boulder;
import org.fbb.board.internals.GridPane;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindow {

    public static void main(String... s) {
        try {
            Grade.loadConversiontable();
            if (Files.getLastBoard() != null && Files.getLastBoulder() != null) {
                Boulder b = Boulder.load(Files.getBoulderFile(Files.getLastBoulder()));
                GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(Files.getWallFile(Files.getLastBoard()))), Files.getLastBoard());
                //check if boards mathces
                if (!b.getWall().equals(preloaded.givenId)) {
                    //if not warn, load boulder on its board if  its board exists. If not load wall only    
                    JOptionPane.showMessageDialog(null, Translator.R("NotMatchingBoulderWall", b.getWall(), preloaded.givenId));
                    File bWall = Files.getWallFile(b.getWall());
                    if (bWall.exists()) {
                        preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(bWall)), bWall.getName());
                        Files.setLastBoard(bWall.getName());
                        MainWindow.loadWallWithBoulder(preloaded, b);
                    } else {
                        Files.setLastBoulder((String) null);//delete alst boulder info
                        MainWindow.loadWallWithBoulder(Files.getLastBoard());
                    }
                } else {
                    //if so, show last boulder on last wall
                    MainWindow.loadWallWithBoulder(preloaded, b);
                }
            } else if (Files.getLastBoulder() != null && Files.getLastBoard() == null) {
                //warn, but load last boulder on its wall, if wall does noto exists, empty(?)
            } else if (Files.getLastBoard() != null && Files.getLastBoulder() == null) {
                //load last wall, generate random bouoder
                MainWindow.loadWallWithBoulder(Files.getLastBoard());
            } else {
                //both null, sugest to create board
                createSelectOrImportWall(LoadBackgroundOrImportOrLoadWall.getDefaultUrl());
            }
        } catch (Exception ex) {
            //do better?
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
        }
    }

    private static void createSelectOrImportWall(String urlorFile, final JFrame... redundants) throws IOException {
        JDialog f = new JDialog((JFrame) null, Translator.R("MainWindowSetWall"), true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        LoadBackgroundOrImportOrLoadWall panel = new LoadBackgroundOrImportOrLoadWall(urlorFile);
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
            createWindow(bis, new File(r.getPath()).getName(), redundants);
        } catch (Exception ex) {
            //not image, wall?
            try {
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()));
                createWindow(zis, new File(r.getPath()).getName(), redundants);
                zis.close();
            } catch (Exception exx) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex);
                exx.printStackTrace();
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
        final JFrame createWallWindow = new JFrame();
        createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bis, props);
        createWallWindow.add(gp);
        double ratio = getIdealWindowSizw(bis);
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
                gp.getGrid().randomBoulder(null);
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
                File f = Files.getWallFile(n);
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
                    Files.setLastBoard(n);
                    createWallWindow.dispose();
                    loadWallWithBoulder(n);
                    if (redundants != null) {
                        for (int i = 0; i < redundants.length; i++) {
                            redundants[i].dispose();

                        }
                    }
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

    private static double getIdealWindowSizw(BufferedImage bis) {
        Rectangle size = ScreenFinder.getCurrentScreenSizeWithoutBounds();
        double dw = (double) size.width / (double) bis.getWidth();
        double dh = (double) size.height / (double) bis.getHeight();
        double ratio = Math.min(dw, dh);
        ratio = ratio * 0.8;//do not cover all screen
        return ratio;
    }

    private static void loadWallWithBoulder(String lastBoard) throws IOException {
        File f = Files.getWallFile(lastBoard);
        GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(f)), f.getName());
        loadWallWithBoulder(preloaded, null);
    }

    private static void loadWallWithBoulder(GridPane.Preload preloaded, Boulder b) throws IOException {
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        final JFrame createWallWindow = new JFrame();
        createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bi, preloaded.props);
        createWallWindow.add(gp);
        gp.disableClicking();
        double ratio = getIdealWindowSizw(bi);
        double nw = ratio * (double) bi.getWidth();
        double nh = ratio * (double) bi.getHeight();
        if (b == null) {
            b = gp.getGrid().randomBoulder(preloaded.givenId);
        } else {
            gp.getGrid().clean();
            gp.getGrid().setBouler(b);
        }
        clearHistory();
        addToBoulderHistory(b);
        JButton previous = new JButton("<"); //this needs to rember exact boulders. limit quueue! enable/disbale this button!
        JButton next = new JButton(">"); //back in row // iimplement forward queueq?:(
        next.setEnabled(canFwd());
        previous.setEnabled(canBack());
        gp.getGrid().setShowGrid(false);
        JPanel tools = new JPanel(new BorderLayout());
        JPanel tools2 = new JPanel(new GridLayout(1, 4));
        JLabel name = new JLabel(b.getGradeAndName());
        name.setToolTipText(getStandardTooltip(b));
        JButton settings = new JButton("|||");//settings - new boulder, new/edit wall..., edit boulder, save curren boulder as, start timered-training
        JPopupMenu jp = new JPopupMenu();
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jp.show((JButton) e.getSource(), 0, 0);
            }
        });
        jp.add(new JMenuItem("select/list boulders"));
        jp.add(new JMenuItem("new boulder"));
        jp.add(new JMenuItem("edit this boulder"));
        JMenuItem saveBoulder = new JMenuItem(Translator.R("MSaveCurrenBoulder"));
        jp.add(saveBoulder);
        saveBoulder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nameNice = preloaded.givenId + " " + new Date().toString();
                nameNice = JOptionPane.showInputDialog(null, Translator.R("MBoulderName"), nameNice);
                String fn = Files.sanitizeFileName(nameNice);
                if (!fn.endsWith(".bldr")) {
                    fn = fn + ".bldr";
                }
                try {
                    Boulder b = gp.getGrid().createBoulderFromCurrent(Files.getBoulderFile(fn), nameNice, preloaded.givenId, Grade.RandomBoulder());
                    b.save();
                    addToBoulderHistory(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(getStandardTooltip(b));
                    next.setEnabled(canFwd());
                    previous.setEnabled(canBack());
                    Files.setLastBoulder(b);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        JMenuItem newEditWall = new JMenuItem(Translator.R("MEditWall"));
        newEditWall.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    createSelectOrImportWall(Files.getWallFile(preloaded.givenId).toURI().toURL().toExternalForm(), createWallWindow);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        jp.add(newEditWall);
        jp.add(new JMenuItem("start timered-training"));
        tools.add(settings, BorderLayout.WEST);
        tools.add(name);
        tools.add(tools2, BorderLayout.EAST);
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (canBack()) {
                    Boulder b = back();
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(getStandardTooltip(b));
                    gp.repaint();
                    Files.setLastBoulder(b);
                }
                next.setEnabled(canFwd());
                previous.setEnabled(canBack());
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (canFwd()) {
                    Boulder b = forward();
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(getStandardTooltip(b));
                    gp.repaint();
                    Files.setLastBoulder(b);
                }
                next.setEnabled(canFwd());
                previous.setEnabled(canBack());
            }
        });
        previous.setEnabled(false);
        next.setEnabled(false);
        JButton nextRandomGenerated = new JButton("?");
        nextRandomGenerated.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = gp.getGrid().randomBoulder(preloaded.givenId);
                name.setText(b.getGradeAndName());
                name.setToolTipText(getStandardTooltip(b));
                addToBoulderHistory(b);
                gp.repaint();
                next.setEnabled(canFwd());
                previous.setEnabled(canBack());
            }
        });
        JButton nextRandom = new JButton("?>");
        JButton nextInList = new JButton(">>");
        JButton prevInList = new JButton("<<");
        tools2.add(previous);
        tools2.add(next);
        tools2.add(nextRandomGenerated);
        tools2.add(nextRandom);
        tools2.add(prevInList);
        tools2.add(nextInList);
        nextRandomGenerated.setToolTipText(Translator.R("NextRandomGenerated"));
        nextRandom.setToolTipText(Translator.R("NextRandomlySelected"));
        nextInList.setToolTipText(Translator.R("NextInRow"));
        prevInList.setToolTipText(Translator.R("PrewInRow"));
        previous.setToolTipText(Translator.R("PreviousBoulder"));
        settings.setToolTipText(Translator.R("Settings"));
        next.setToolTipText(Translator.R("FwdBoulder"));
        createWallWindow.add(tools, BorderLayout.NORTH);
        createWallWindow.pack();
        createWallWindow.setSize((int) nw, (int) nh + tools.getHeight());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createWallWindow.setVisible(true);
            }
        });
    }

    private static final List<Boulder> history = new ArrayList<>();
    private static int historyIndex = -1;

    //returns whether we are at end or not;
    //@return true, if index is NOT last (and thus forward button can be enabld)
    private static void addToBoulderHistory(Boulder b) {
        if (history.isEmpty()) {
            history.add(b);
            historyIndex = 0;
            return;
        }
        if (historyIndex == history.size() - 1) {
            historyIndex++;
            history.add(b);
            return;
        }
        historyIndex++;
        history.add(historyIndex, b);
        return;
    }

    private static boolean canBack() {
        return historyIndex > 0;
    }

    private static Boulder forward() {
        if (history.isEmpty()) {
            return null;
        }
        if (canFwd()) {
            historyIndex++;
            return history.get(historyIndex);
        }
        return history.get(historyIndex);
    }

    private static Boulder back() {
        if (history.isEmpty()) {
            return null;
        }
        if (canBack()) {
            historyIndex--;
            return history.get(historyIndex);
        }
        return history.get(historyIndex);
    }

    private static boolean canFwd() {
        return historyIndex < history.size() - 1;
    }

    private static void clearHistory() {
        history.clear();
    }

    private static String getStandardTooltip(Boulder b) {
        return "<html>"
                + b.getName() + " (" + b.getWall() + ")<br/>"
                + "<b>" + b.getGrade().toAllValues("<br/>") + "</b>"
                + b.getDate();
    }
}
