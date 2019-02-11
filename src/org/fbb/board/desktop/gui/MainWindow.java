package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.Boulder;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.Grid;
import org.fbb.board.internals.GridPane;
import org.fbb.board.internals.HistoryManager;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindow {

    private static HistoryManager hm = new HistoryManager();
    private static ListWithFilter list;

    public static void main(String... s) {
        try {
            list = new ListWithFilter();
            Grade.loadConversiontable();
            if (Files.getLastBoard() != null && Files.getLastBoulder() != null) {
                Boulder b = Boulder.load(Files.getBoulderFile(Files.getLastBoulder()));
                GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(Files.getWallFile(Files.getLastBoard()))), Files.getLastBoard());
                //check if boards mathces
                if (!b.getWall().equals(preloaded.givenId)) {
                    //if not warn, and ask user ifhe wont to keep wall or boulder. If not load wall only    
                    JOptionPane.showMessageDialog(null, Translator.R("NotMatchingBoulderWall", b.getWall(), preloaded.givenId));
                    File bWall = Files.getWallFile(b.getWall());
                    if (bWall.exists()) {
                        //it is likely that wall had been changed intntionally, and old wall was simply not deleted, and boulder was not updated
                        int a = JOptionPane.showConfirmDialog(null, Translator.R("PossiblyIncorrectLastBoulder"));
                        if (a == JOptionPane.YES_OPTION) {
                            Files.setLastBoulder((String) null);//delete alst boulder info
                            MainWindow.loadWallWithBoulder(Files.getLastBoard());
                        } else {
                            preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(bWall)), bWall.getName());
                            Files.setLastBoard(bWall.getName());
                            MainWindow.loadWallWithBoulder(preloaded, b);
                        }
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
                System.err.println("Last boulder but not lat wall!");
                Boulder b = Boulder.load(Files.getBoulderFile(Files.getLastBoulder()));
                File bWall = Files.getWallFile(b.getWall());
                if (bWall.exists()) {
                    GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(bWall)), bWall.getName());
                    Files.setLastBoard(bWall.getName());
                    MainWindow.loadWallWithBoulder(preloaded, b);
                } else {
                    createSelectOrImportWall(LoadBackgroundOrImportOrLoadWall.getDefaultUrl());
                }
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
                gp.repaintAndSend();
            }
        });
        sw.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gp.getGrid().setVertLines((Integer) sw.getValue() + 1);
                gp.repaintAndSend();
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
                gp.repaintAndSend();
            }
        });
        clean.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().clean();
                gp.repaintAndSend();
            }
        });
        test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().randomBoulder(null);
                gp.repaintAndSend();
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().reset();
                gp.repaintAndSend();
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

    private static void loadWallWithBoulder(GridPane.Preload preloaded, final Boulder possiblebOulder) throws IOException {
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        final JFrame createWallWindow = new JFrame();
        createWallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bi, preloaded.props);
        list = new ListWithFilter(preloaded.givenId);
        createWallWindow.add(gp);
        gp.disableClicking();
        double ratio = getIdealWindowSizw(bi);
        double nw = ratio * (double) bi.getWidth();
        double nh = ratio * (double) bi.getHeight();
        final Boulder b = (possiblebOulder == null) ? gp.getGrid().randomBoulder(preloaded.givenId) : possiblebOulder;
        if (b.getFile() != null) {
            list.setIndex(b.getFile().getName());
        }
        gp.getGrid().clean();
        gp.getGrid().setBouler(b);
        hm.clearHistory();
        hm.addToBoulderHistory(b);
        JButton previous = new JButton("<"); //this needs to rember exact boulders. limit quueue! enable/disbale this button!
        JButton next = new JButton(">"); //back in row // iimplement forward queueq?:(
        next.setEnabled(hm.canFwd());
        previous.setEnabled(hm.canBack());
        gp.getGrid().setShowGrid(false);
        JPanel tools = new JPanel(new BorderLayout());
        JPanel tools2 = new JPanel(new GridLayout(1, 4));
        JLabel name = new JLabel(b.getGradeAndName());
        name.setToolTipText(b.getStandardTooltip());
        name.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(createWallWindow, name.getToolTipText());
            }

        });
        final JButton nextRandom = new JButton("?>");
        final JButton nextInList = new JButton(">>");
        final JButton prevInList = new JButton("<<");
        JButton settings = new JButton("|||");//settings - new boulder, new/edit wall..., edit boulder, save curren boulder as, start timered-training
        JPopupMenu jp = new JPopupMenu();
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jp.show((JButton) e.getSource(), 0, 0);
            }
        });
        JMenuItem selectListBoulders = new JMenuItem(Translator.R("SelectListBoulders"));
        jp.add(selectListBoulders); //also return boudler and current filter
        selectListBoulders.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder r = selectListBouder(preloaded.givenId);
                if (r != null) {
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    name.setText(r.getGradeAndName());
                    name.setToolTipText(r.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(r);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    list.setIndex(r.getFile().getName());
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                }
            }

        });
        JMenuItem newBoulder = new JMenuItem(Translator.R("MNewBoulder"));
        jp.add(newBoulder);
        newBoulder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                BoulderAndSaved bs = editBoulder(preloaded, null);
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    name.setText(r.getGradeAndName());
                    name.setToolTipText(r.getStandardTooltip());
                    gp.repaintAndSend();
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                        prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                        nextInList.setEnabled(list.canFwd());
                        prevInList.setEnabled(list.canBack());
                    }
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                }
            }
        });
        JMenuItem editBoulder = new JMenuItem(Translator.R("MEditBoulder"));
        jp.add(editBoulder);
        editBoulder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                BoulderAndSaved bs = editBoulder(preloaded, hm.getCurrentInHistory());
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    name.setText(r.getGradeAndName());
                    name.setToolTipText(r.getStandardTooltip());
                    gp.repaintAndSend();
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                        prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                        nextInList.setEnabled(list.canFwd());
                        prevInList.setEnabled(list.canBack());
                    }
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                }
            }

        });
        //with edit bolder in, maybe it is redundant ot save bouder as now?
        JMenuItem saveBoulder = new JMenuItem(Translator.R("MSaveCurrenBoulder"));
        jp.add(saveBoulder);
        saveBoulder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nameNice = preloaded.givenId + " " + new Date().toString();
                nameNice = JOptionPane.showInputDialog(null, Translator.R("MBoulderName"), nameNice);
                if (name == null) {
                    return;
                }
                String fn = Files.sanitizeFileName(nameNice);
                if (!fn.endsWith(".bldr")) {
                    fn = fn + ".bldr";
                }
                try {
                    Boulder b = gp.getGrid().createBoulderFromCurrent(Files.getBoulderFile(fn), nameNice, preloaded.givenId, Grade.RandomBoulder());
                    b.save();
                    hm.addToBoulderHistory(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    Files.setLastBoulder(b);
                    list.addToBoulderHistory(b);
                    list.setIndex(b.getFile().getName());
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
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
        //basic settings + ADMINISTRATOR tasks - delete boudlers, manage walls, deault grades, default higlight. new/edit wall management only too?
        //new password?
        jp.add(new JMenuItem("management"));
        jp.add(new JMenuItem("tips")); //highlight what save do (jsut add a leg?), higluight do not save garbage
        tools.add(settings, BorderLayout.WEST);
        tools.add(name);
        tools.add(tools2, BorderLayout.EAST);
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hm.canBack()) {
                    Boulder b = hm.back();
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(b);
                }
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hm.canFwd()) {
                    Boulder b = hm.forward();
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(b);
                }
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
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
                name.setToolTipText(b.getStandardTooltip());
                hm.addToBoulderHistory(b);
                gp.repaintAndSend();
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
            }
        });
        nextInList.setEnabled(list.canFwd());
        prevInList.setEnabled(list.canBack());
        nextRandom.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = list.getRandom();
                if (b != null) {
                    hm.addToBoulderHistory(b);
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                }
            }
        });
        nextInList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = list.forward();
                if (b != null) {
                    hm.addToBoulderHistory(b);
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                }
            }
        });
        prevInList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = list.back();
                if (b != null) {
                    hm.addToBoulderHistory(b);
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend();
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                }
            }
        });
        tools2.add(previous);
        tools2.add(next);
        tools2.add(nextRandomGenerated);
        tools2.add(nextRandom);
        tools2.add(prevInList);
        tools2.add(nextInList);
        nextRandomGenerated.setToolTipText(Translator.R("NextRandomGenerated"));
        nextRandom.setToolTipText(Translator.R("NextRandomlySelected"));
        nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
        prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
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

    private static BoulderAndSaved editBoulder(GridPane.Preload p, Boulder b) {
        try {
            return editBoulderImpl(p, b);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
    }

    private static class BoulderAndSaved {

        private final Boulder b;
        private final boolean saved;

        public BoulderAndSaved(Boulder b, boolean saved) {
            this.b = b;
            this.saved = saved;
        }

    }

    private static BoulderAndSaved editBoulderImpl(GridPane.Preload p, Boulder orig) throws IOException, CloneNotSupportedException {
        //checkbox save? 
        //if not save, then what?
        //return  new BoulderAlways? - on Ok?
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(p.img));
        final JDialog operateBoulder = new JDialog((JFrame) null, true);
        operateBoulder.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bi, p.props);
        gp.getGrid().setShowGrid(true);
        operateBoulder.add(gp);
        gp.enableBoulderModificationOnly();
        double ratio = getIdealWindowSizw(bi);
        double nw = ratio * (double) bi.getWidth();
        double nh = ratio * (double) bi.getHeight();
        if (orig != null) {
            gp.getGrid().clean();
            gp.getGrid().setBouler(orig);
        } else {
            gp.getGrid().clean();
        }
        JButton doneButton = new JButton(Translator.R("Bdone"));
        JPanel tools1 = new JPanel(new BorderLayout());
        JPanel tools2 = new JPanel(new BorderLayout());
        JPanel tools11 = new JPanel(new BorderLayout());
        JComboBox<String> grades = new JComboBox<>(Grade.currentGrades());
        JTextField name = new JTextField();
        if (orig == null) {
            name.setText(p.givenId + " " + new Date().toString());
        } else {
            name.setText(orig.getName());
            grades.setSelectedItem(orig.getGrade().toString());
        }
        JCheckBox saveOnExit = new JCheckBox(Translator.R("SaveOnExit"));
        saveOnExit.setSelected(true);
        tools1.add(grades, BorderLayout.WEST);
        tools1.add(name);
        tools1.add(saveOnExit, BorderLayout.EAST);
        tools11.add(new JLabel("Author"), BorderLayout.WEST);
        JTextField author = new JTextField();
        if (orig == null) {
            author.setText("sign yourself");
        } else {
            author.setText(orig.getAuthor());
        }
        tools11.add(author);
        if (orig == null) {
            tools11.add(new JLabel(dtf.format(new Date())), BorderLayout.EAST);
        } else {
            tools11.add(new JLabel(dtf.format(orig.getDate())), BorderLayout.EAST);
        }
        tools1.add(tools11, BorderLayout.SOUTH);
        operateBoulder.add(tools1, BorderLayout.NORTH);
        JCheckBox gridb = new JCheckBox(Translator.R("Bgrid"));
        gridb.setSelected(true);
        gridb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().setShowGrid(gridb.isSelected());
                gp.repaintAndSend();
            }
        });
        tools2.add(gridb, BorderLayout.EAST);
        tools2.add(doneButton);
        operateBoulder.add(tools2, BorderLayout.SOUTH);
        operateBoulder.pack();
        operateBoulder.setSize((int) nw, (int) nh + tools1.getHeight() + tools2.getHeight());
        DoneEditingBoulderListener done = new DoneEditingBoulderListener(orig, saveOnExit, operateBoulder, gp.getGrid(), name, grades, p.givenId, author);
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
                ex.printStackTrace();
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

        public DoneEditingBoulderListener(Boulder orig, JCheckBox saveOnExit, JDialog parent, Grid grid, JTextField nwNameProvider, JComboBox<String> grades, String wallId, JTextField nwAuthorProvider) {
            this.orig = orig;
            this.saveOnExit = saveOnExit;
            this.nwNameProvider = nwNameProvider;
            this.parent = parent;
            this.grid = grid;
            this.grades = grades;
            this.wallId = wallId;
            this.nwAuthorProvider = nwAuthorProvider;
        }

        public void actionPerformedImpl(ActionEvent e) throws IOException {
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
                        possibleReturnCandidate.save();
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
                    result = null;
                } else {
                    result = possibleReturnCandidate;
                }
            }
            parent.setVisible(false);
            parent.dispose();
        }
    }

    private static Boulder selectListBouder(String wallId) {
        try {
            return selectListBouderImpl(wallId);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
    }

    private static final SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/YYYY HH:mm");

    private static Boulder selectListBouderImpl(String wallID) throws IOException {
        JDialog d = new JDialog((JDialog) null, true);
        d.setSize(800, 600);
        d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ListWithFilter currentList;
        currentList = new ListWithFilter(wallID);
        JList<Boulder> boulders = new JList(currentList.getHistory());
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(boulders), new JPanel());
        d.add(sp);
        //delete boulders? made walls editabel to allow old boulders cleanup? delete walls?
        JPanel resultsPanel = new JPanel(new GridLayout(1, 2));
        resultsPanel.add(new JButton("Add all filtered results"));
        resultsPanel.add(new JButton("Add only selected results"));
        JPanel tools0 = new JPanel(new BorderLayout());
        JPanel tools1 = new JPanel(new GridLayout(1, 3));
        JPanel tools2 = new JPanel(new GridLayout(1, 3));
        JPanel tools3 = new JPanel(new BorderLayout());
        JPanel tools4 = new JPanel(new GridLayout(1, 3));
        JPanel tools5 = new JPanel(new BorderLayout());
        JPanel tools6 = new JPanel(new GridLayout(1, 2));
        tools6.add(new JButton("Apply last used filter"));
        tools6.add(new JButton("Apply wall's default"));
        tools0.add(new JLabel(Translator.R("Wall")));
        final JComboBox<String> walls = new JComboBox(Files.wallsDir.list());
        walls.setSelectedItem(wallID);
        tools0.add(walls);
        final JLabel dificultyLabel = new JLabel(Translator.R("DificultyInterval"));
        tools1.add(dificultyLabel);
        dificultyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(d, dificultyLabel.getToolTipText());
            }
        });
        final JComboBox<String> gradesFrom = new JComboBox(Grade.currentGrades());
        final JComboBox<String> gradesTo = new JComboBox(Grade.currentGrades());
        gradesFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dificultyLabel.setToolTipText("<html>"
                        + Grade.getStandardTooltip(gradesFrom.getSelectedIndex())
                        + "-----<br>"
                        + Grade.getStandardTooltip(gradesTo.getSelectedIndex())
                );
            }
        });
        gradesTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dificultyLabel.setToolTipText("<html>"
                        + Grade.getStandardTooltip(gradesFrom.getSelectedIndex())
                        + "-----<br>"
                        + Grade.getStandardTooltip(gradesTo.getSelectedIndex())
                );
            }
        });
        gradesFrom.setSelectedItem(currentList.getEasiest().toString());
        gradesTo.setSelectedItem(currentList.getHardest().toString());
        tools1.add(gradesFrom);
        tools1.add(gradesTo);
        tools2.add(new JLabel(Translator.R("NumberOfHolds") + " " + Translator.R("FromTo")));
        final JSpinner holdsMin = new JSpinner(new SpinnerNumberModel(currentList.getShortest(), 0, 1000, 1));
        tools2.add(holdsMin);
        final JSpinner holdsMax = new JSpinner(new SpinnerNumberModel(currentList.getLongest(), 0, 1000, 1));
        tools2.add(holdsMax);
        JLabel nameLabel = new JLabel(Translator.R("NameFilter"));
        tools3.add(nameLabel, BorderLayout.WEST);
        final JTextField nameFilter = new JTextField();
        tools3.add(nameFilter);
        final JLabel authorLabel = new JLabel(Translator.R("AuthorFilter"));
        authorLabel.setToolTipText(currentList.getAuthors());
        authorLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(d, authorLabel.getToolTipText());
            }

        });
        tools5.add(authorLabel, BorderLayout.WEST);
        final JTextField authorsFilter = new JTextField();
        tools5.add(authorsFilter);
        tools4.add(new JLabel(Translator.R("Date") + " [dd/MM/YYYY HH:mm] " + Translator.R("FromTo")));
        final JTextField dateFrom = new JTextField(dtf.format(currentList.getOldest()));
        tools4.add(dateFrom);
        final JTextField dateTo = new JTextField(dtf.format(currentList.getYoungest()));
        tools4.add(dateTo);
        JPanel tools = new JPanel(new GridLayout(8, 1));
        tools.add(tools0);
        tools.add(tools1);
        tools.add(tools2);
        tools.add(tools3);
        tools.add(tools4);
        tools.add(tools5);
        tools.add(tools6);
        JButton apply = new JButton(Translator.R("Apply"));
        tools.add(apply);
        d.add(tools, BorderLayout.NORTH);
        d.add(resultsPanel, BorderLayout.SOUTH);
        boulders.setCellRenderer(new BoulderListRenderer());
        apply.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new Filter(
                            (String) walls.getSelectedItem(),
                            gradesFrom.getSelectedIndex(),
                            gradesTo.getSelectedIndex(),
                            (Integer) (holdsMin.getValue()),
                            (Integer) (holdsMax.getValue()),
                            authorsFilter.getText(),
                            nameFilter.getText(),
                            dtf.parse(dateFrom.getText()),
                            dtf.parse(dateTo.getText())
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dateFrom, ex);
                }
            }
        });
        sp.setDividerLocation(d.getWidth()/2);
        d.setVisible(true);
        if (boulders.getSelectedValue() == null) {
            return null;
        }
        return Boulder.load(boulders.getSelectedValue().getFile());
    }

    private static class BoulderListRenderer extends JLabel implements ListCellRenderer<Boulder> {

        public BoulderListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Boulder> list, Boulder b, int index,
                boolean isSelected, boolean cellHasFocus) {
            this.setFont(this.getFont().deriveFont(Font.PLAIN, new JLabel().getFont().getSize() + 2));
            String grade = b.getGrade().toString();
            setText("<html><b>" + grade + "</b>:  <u>" + b.getName() + "</u>| <i>" + b.getAuthor() + "</i> (" + dtf.format(b.getDate()) + ")[" + b.getWall() + "]");

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                int inter = 255 / Grade.currentGrades().size();
                if (b.getGrade().toNumber() < 0) {
                    setBackground(new Color(0, 250, 0));
                } else {
                    setBackground(new Color(b.getGrade().toNumber() * inter, 255, 255));
                }
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
