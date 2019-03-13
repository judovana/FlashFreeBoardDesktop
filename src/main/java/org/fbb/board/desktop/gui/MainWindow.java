package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.desktop.TextToSpeech;
import org.fbb.board.internals.Boulder;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.Grid;
import org.fbb.board.internals.GridPane;
import org.fbb.board.internals.HistoryManager;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.TimeredTraining;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.grades.Grade;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindow {

    private static final GlobalSettings gs = new GlobalSettings();
    public static final HistoryManager hm = new HistoryManager();
    public static ListWithFilter list;
    private static final JPopupMenu listJump = new JPopupMenu();
    private static final JPopupMenu historyJump = new JPopupMenu();
    private static final Authenticator auth = new Authenticator();

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
        GridPane gp = new GridPane(bis, props, gs);
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
                gp.repaintAndSend(gs);
            }
        });
        sw.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gp.getGrid().setVertLines((Integer) sw.getValue() + 1);
                gp.repaintAndSend(gs);
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
                gp.repaintAndSend(gs);
            }
        });
        clean.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().clean();
                gp.repaintAndSend(gs);
            }
        });
        test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().randomBoulder(null);
                gp.repaintAndSend(gs);
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gp.getGrid().reset();
                gp.repaintAndSend(gs);
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
        GridPane gp = new GridPane(bi, preloaded.props, gs);
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
        final JButton previous = new JButton("<"); //this needs to rember exact boulders. limit quueue! enable/disbale this button!
        final JButton next = new JButton(">"); //back in row // iimplement forward queueq?:(
        final JButton nextRandom = new JButton("?>");
        final JButton nextInList = new JButton(">>");
        final JButton prevInList = new JButton("<<");
        JButton settings = new JButton("|||");//settings - new boulder, new/edit wall..., edit boulder, save curren boulder as, start timered-training
        JButton nextRandomGenerated = new JButton("?");
        JLabel name = new JLabel(b.getGradeAndName());
        JPopupMenu jp = new JPopupMenu();
        next.setEnabled(hm.canFwd());
        previous.setEnabled(hm.canBack());
        gp.getGrid().setShowGrid(false);
        JPanel tools = new JPanel(new GridLayout(2, 1));
        JPanel quickFilterPanel = new JPanel(new GridLayout(1, 4));
        JPanel tools2 = new JPanel(new GridLayout(1, 4));
        JButton a5 = new JButton("-5A");
        quickFilterPanel.add(a5);
        JButton a6 = new JButton("5A-6A");
        quickFilterPanel.add(a6);
        JButton a7 = new JButton("6A-7A");
        quickFilterPanel.add(a7);
        JButton a8 = new JButton("7A-8A");
        quickFilterPanel.add(a8);
        JButton a9 = new JButton("8A-");
        quickFilterPanel.add(a9);
        a5.addActionListener(new QuickFilterLIstener(0, 10, preloaded.givenId, nextInList, prevInList, gp, name, next, previous));
        a6.addActionListener(new QuickFilterLIstener(10, 13, preloaded.givenId, nextInList, prevInList, gp, name, next, previous));
        a7.addActionListener(new QuickFilterLIstener(13, 19, preloaded.givenId, nextInList, prevInList, gp, name, next, previous));
        a8.addActionListener(new QuickFilterLIstener(19, 26, preloaded.givenId, nextInList, prevInList, gp, name, next, previous));
        a9.addActionListener(new QuickFilterLIstener(26, 35, preloaded.givenId, nextInList, prevInList, gp, name, next, previous));
        name.setToolTipText(b.getStandardTooltip());
        name.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(createWallWindow, name.getToolTipText());
            }

        });
        nextInList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    generateListJumper(gp, name, next, previous, nextInList, nextInList);
                    listJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });
        prevInList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    generateListJumper(gp, name, next, previous, nextInList, prevInList);
                    listJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });

        next.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    generateHistoryJumper(gp, name, next, previous, nextInList, prevInList);
                    historyJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });
        previous.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    generateHistoryJumper(gp, name, next, previous, nextInList, prevInList);
                    historyJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });
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
                BoulderListAndIndex listAndothers = selectListBouder(preloaded.givenId);
                if (listAndothers != null) {
                    list = new ListWithFilter(listAndothers.list);
                    if (!list.getHistory().isEmpty()) {
                        Boulder r;
                        if (listAndothers.selctedValue != null) {
                            r = listAndothers.selctedValue;
                        } else {
                            r = list.getCurrentInHistory();
                        }
                        hm.addToBoulderHistory(r);
                        gp.getGrid().setBouler(r);
                        name.setText(r.getGradeAndName());
                        name.setToolTipText(r.getStandardTooltip());
                        gp.repaintAndSend(gs);
                        Files.setLastBoulder(r);
                        next.setEnabled(hm.canFwd());
                        previous.setEnabled(hm.canBack());
                        list.setIndex(r.getFile().getName());
                    }
                    nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                }
            }

        });
        //for new/edit bolulder a pro new/edit wall - add changed/document listeners  to check if name, author and dificulty was edited
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
                    gp.repaintAndSend(gs);
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
                    gp.repaintAndSend(gs);
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
        //with edit bolder his looks like redundant
        JMenuItem saveBoulder = new JMenuItem(Translator.R("MSaveCurrenBoulder"));
        saveBoulder.setEnabled(false);
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
        JMenuItem timered = new JMenuItem(Translator.R("timered"));
        jp.add(timered);
        timered.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog timeredWindow = new JDialog();
                final TimeredTraining[] trainig = new TimeredTraining[1];
                timeredWindow.setModal(true);
                timeredWindow.setLayout(new GridLayout(7, 2));
                timeredWindow.add(new JLabel(Translator.R("tmeOfBoulder")));
                final JTextField timeOfBoulder = new JTextField("00:20");
                timeredWindow.add(timeOfBoulder);
                timeredWindow.add(new JLabel(Translator.R("timeOfTraining")));
                final JTextField timeOfTraining = new JTextField("5:00");
                timeredWindow.add(timeOfTraining);
                //timeredWindow.add(new JLabel(Translator.R("pauseTime")));
                //JTextField pauseTime = new JTextField("00:00");
                //timeredWindow.add(pauseTime);
                timeredWindow.add(new JLabel(Translator.R("numBoulders")));
                final JSpinner numBoulders = new JSpinner(new SpinnerNumberModel(15, 1, 1000, 1));
                timeredWindow.add(numBoulders);
                final BoulderCalc boulderCalc = new BoulderCalc(timeOfBoulder, timeOfTraining, numBoulders);
                final JCheckBox allowRandom = new JCheckBox(Translator.R("allowRandom"), true);
                allowRandom.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setRandomAllowed(allowRandom.isSelected());
                        }
                    }
                });
                timeredWindow.add(allowRandom);
                final JCheckBox allowRegular = new JCheckBox("allowRegular", true);
                allowRegular.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setRegularAllowed(allowRegular.isSelected());
                        }
                    }
                });
                timeredWindow.add(allowRegular);
                final JCheckBox allowJumps = new JCheckBox("allowJumps", true);
                allowJumps.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setJumpingAllowed(allowJumps.isSelected());
                        }
                    }
                });
                timeredWindow.add(allowJumps);
                final JComboBox<TextToSpeech.TextId> reader = new JComboBox(TextToSpeech.getLangs());
                reader.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setSpeak((TextToSpeech.TextId) reader.getSelectedItem());
                        }
                    }
                });
                timeredWindow.add(reader);
                final JButton start = new JButton("start/stop");
                timeredWindow.add(start);
                JButton pause = new JButton("pause/unpause");
                timeredWindow.add(pause);
                final JLabel counterClock = new JLabel("00:00");
                timeredWindow.add(counterClock);
                start.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] == null) {
                            trainig[0] = new TimeredTraining(
                                    nextInList.getActionListeners()[0],
                                    prevInList.getActionListeners()[0],
                                    nextRandom.getActionListeners()[0],
                                    nextRandomGenerated.getActionListeners()[0],
                                    allowRandom.isSelected(), allowRegular.isSelected(), allowJumps.isSelected(), boulderCalc.getTotalTime(), boulderCalc.getTimeOfBoulder(), counterClock, (TextToSpeech.TextId) reader.getSelectedItem());
                            new Thread(trainig[0]).start();
                        } else {
                            trainig[0].stop();
                            trainig[0] = null;
                        }
                    }
                });
                pause.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setPaused(!trainig[0].isPaused());
                        }
                    }
                });
                timeredWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                timeredWindow.pack();
                timeredWindow.setLocationRelativeTo(createWallWindow);
                timeredWindow.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosed(WindowEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].stop();
                        }
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].stop();
                        }
                    }

                });
                timeredWindow.setVisible(true);
            }
        });
        //basic settings + ADMINISTRATOR tasks - delete boudlers, manage walls, deault grades, default higlight. new/edit wall management only too?
        //new password?
        JMenuItem management = new JMenuItem(Translator.R("management"));
        jp.add(management);
        management.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    auth.authenticate(Translator.R("settingsAuth"));
                } catch (Authenticator.AuthoriseException a) {
                    a.printStackTrace();
                    JOptionPane.showMessageDialog(null, a);
                    return;
                }
                JDialog settingsWindow = new JDialog();
                settingsWindow.setModal(true);
                settingsWindow.setLayout(new GridLayout(6 + 9, 2));
                settingsWindow.add(new JLabel(Translator.R("brightenes")));
                JSpinner sss = new JSpinner(new SpinnerNumberModel(gs.getBrightness(), 1, 255, 1));
                sss.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        gs.setBrightness(((Integer) sss.getValue()));
                        gp.repaintAndSend(gs);
                    }
                });
                settingsWindow.add(sss);
                settingsWindow.add(new JLabel(Translator.R("testdelay")));
                JSpinner testDelay = new JSpinner(new SpinnerNumberModel(250, 1, 10000, 50));
                settingsWindow.add(testDelay);
                final JButton re = new JButton(Translator.R("testred"));
                re.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gp.getGrid().testRed(((Integer) testDelay.getValue()));
                    }
                });
                final JButton gr = new JButton(Translator.R("testgreen"));
                gr.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gp.getGrid().testGreen(((Integer) testDelay.getValue()));
                    }
                });
                final JButton bl = new JButton(Translator.R("testblue"));
                bl.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gp.getGrid().testBlue(((Integer) testDelay.getValue()));
                    }
                });
                settingsWindow.add(gr);
                settingsWindow.add(bl);
                settingsWindow.add(re);
                JButton snake = new JButton("snake game");
                snake.setEnabled(false);
                settingsWindow.add(snake);
                JComboBox<String> portType = new JComboBox<>(new String[]{"port", "bluetooth", "nothing"});
                portType.setSelectedIndex(gs.getPortTypeIndex());
                portType.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gs.setPortType(portType.getSelectedIndex());
                    }
                });
                settingsWindow.add(portType);
                JTextField portName = new JTextField(gs.getPortId());
                settingsWindow.add(portName);
                portName.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        gs.setDeviceId(portName.getText());
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        gs.setDeviceId(portName.getText());
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        gs.setDeviceId(portName.getText());
                    }
                });
                settingsWindow.add(new JLabel());
                JButton selectPort = new JButton(Translator.R("Bselect"));
                settingsWindow.add(selectPort);
                selectPort.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            JDialog selectPortDialog = new JDialog();
                            selectPortDialog.setModal(true);
                            selectPortDialog.setLayout(new BorderLayout());
                            JLabel title = new JLabel(Translator.R("selectTitle"));
                            selectPortDialog.add(title, BorderLayout.NORTH);
                            JLabel waiting = new JLabel("<html><div style='text-align: center;'>" + Translator.R("scanning") + "</div></html>");
                            selectPortDialog.add(waiting);
                            JLabel message = new JLabel(Translator.R("click desired or close"));
                            selectPortDialog.add(message, BorderLayout.SOUTH);
                            selectPortDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            selectPortDialog.setSize(300, 400);
                            selectPortDialog.setLocationRelativeTo(settingsWindow);
                            SwingWorker<JList<ConnectionID>, JList<ConnectionID>> sw = new SwingWorker() {
                                @Override
                                protected JList<ConnectionID> doInBackground() throws Exception {
                                    return new JList<>(gs.list());
                                }

                                @Override
                                public void done() {
                                    try {
                                        selectPortDialog.remove(waiting);
                                        JList<ConnectionID> item = (JList) this.get();
                                        if (item.getModel().getSize() == 0) {
                                            JLabel iitem = new JLabel("<html><div style='text-align: center;'>" + Translator.R("noDeviceFoound") + "</div></html>");
                                            selectPortDialog.add(iitem);
                                        } else {
                                            selectPortDialog.add(item);
                                            item.addListSelectionListener(new ListSelectionListener() {
                                                @Override
                                                public void valueChanged(ListSelectionEvent e) {
                                                    portName.setText(item.getSelectedValue().getId());
                                                }
                                            });
                                        }
                                        item.addMouseListener(new MouseAdapter() {
                                            @Override
                                            public void mouseClicked(MouseEvent e) {
                                                if (e.getClickCount() > 1) {
                                                    selectPortDialog.dispose();
                                                }
                                            }

                                        });
                                        selectPortDialog.pack();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        JOptionPane.showMessageDialog(selectPortDialog, ex);
                                    }
                                }
                            };
                            sw.execute();
                            selectPortDialog.setVisible(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(settingsWindow, ex);
                        }
                    }
                });
                JLabel greenRedTitle = new JLabel(Translator.R("StartCompozition", Translator.R("red")));
                JSpinner greenRed = new JSpinner(new SpinnerNumberModel(gs.getPart(0), 0d, 1d, 0.1));
                JLabel greenGreenTitle = new JLabel(Translator.R("StartCompozition", Translator.R("green")));
                JSpinner greenGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(1), 0d, 1d, 0.1));
                JLabel greenBlueTitle = new JLabel(Translator.R("StartCompozition", Translator.R("blue")));
                JSpinner greenBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(2), 0d, 1d, 0.1));

                JLabel blueRedTitle = new JLabel(Translator.R("PathCompozition", Translator.R("red")));
                JSpinner blueRed = new JSpinner(new SpinnerNumberModel(gs.getPart(3), 0d, 1d, 0.1));
                JLabel blueGreenTitle = new JLabel(Translator.R("PathCompozition", Translator.R("green")));
                JSpinner blueGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(4), 0d, 1d, 0.1));
                JLabel blueBlueTitle = new JLabel(Translator.R("PathCompozition", Translator.R("blue")));
                JSpinner blueBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(5), 0d, 1d, 0.1));

                JLabel redRedTitle = new JLabel(Translator.R("TopCompozition", Translator.R("red")));
                JSpinner redRed = new JSpinner(new SpinnerNumberModel(gs.getPart(6), 0d, 1d, 0.1));
                JLabel redGreenTitle = new JLabel(Translator.R("TopCompozition", Translator.R("green")));
                JSpinner redGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(7), 0d, 1d, 0.1));
                JLabel redBlueTitle = new JLabel(Translator.R("TopCompozition", Translator.R("blue")));
                JSpinner redBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(8), 0d, 1d, 0.1));

                settingsWindow.add(greenRedTitle);
                settingsWindow.add(greenRed);
                settingsWindow.add(greenGreenTitle);
                settingsWindow.add(greenGreen);
                settingsWindow.add(greenBlueTitle);
                settingsWindow.add(greenBlue);

                settingsWindow.add(blueRedTitle);
                settingsWindow.add(blueRed);
                settingsWindow.add(blueGreenTitle);
                settingsWindow.add(blueGreen);
                settingsWindow.add(blueBlueTitle);
                settingsWindow.add(blueBlue);

                settingsWindow.add(redRedTitle);
                settingsWindow.add(redRed);
                settingsWindow.add(redGreenTitle);
                settingsWindow.add(redGreen);
                settingsWindow.add(redBlueTitle);
                settingsWindow.add(redBlue);

                greenRed.addChangeListener(new PathColorCompozitorListener(0, gp, sss, gr));
                greenGreen.addChangeListener(new PathColorCompozitorListener(1, gp, sss, gr));
                greenBlue.addChangeListener(new PathColorCompozitorListener(2, gp, sss, gr));
                blueRed.addChangeListener(new PathColorCompozitorListener(3, gp, sss, bl));
                blueGreen.addChangeListener(new PathColorCompozitorListener(4, gp, sss, bl));
                blueBlue.addChangeListener(new PathColorCompozitorListener(5, gp, sss, bl));
                redRed.addChangeListener(new PathColorCompozitorListener(6, gp, sss, re));
                redGreen.addChangeListener(new PathColorCompozitorListener(7, gp, sss, re));
                redBlue.addChangeListener(new PathColorCompozitorListener(8, gp, sss, re));

                settingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                settingsWindow.pack();
                settingsWindow.setLocationRelativeTo(createWallWindow);
                settingsWindow.setVisible(true);

            }
        });
        jp.add(new JMenuItem("tips")); //highlight what save do (jsut add a leg?), higluight do not save garbage
        JPanel subtools = new JPanel(new BorderLayout());
        subtools.add(settings, BorderLayout.WEST);
        subtools.add(name);
        tools.add(quickFilterPanel);
        tools.add(subtools);
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hm.canBack()) {
                    Boulder b = hm.back();
                    gp.getGrid().setBouler(b);
                    name.setText(b.getGradeAndName());
                    name.setToolTipText(b.getStandardTooltip());
                    gp.repaintAndSend(gs);
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
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                }
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
            }
        });
        previous.setEnabled(false);
        next.setEnabled(false);
        nextRandomGenerated.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = gp.getGrid().randomBoulder(preloaded.givenId);
                name.setText(b.getGradeAndName());
                name.setToolTipText(b.getStandardTooltip());
                hm.addToBoulderHistory(b);
                gp.repaintAndSend(gs);
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
                    gp.repaintAndSend(gs);
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
                    gp.repaintAndSend(gs);
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
                    gp.repaintAndSend(gs);
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
        tools2.add(prevInList);
        tools2.add(previous);
        tools2.add(nextRandomGenerated);
        tools2.add(nextRandom);
        tools2.add(next);
        tools2.add(nextInList);
        name.setFont(name.getFont().deriveFont((float) name.getFont().getSize() * 2));
        for (Component c : tools.getComponents()) {
            c.setFont(name.getFont());
        }
        for (Component c : tools2.getComponents()) {
            c.setFont(name.getFont());
        }
        nextRandomGenerated.setToolTipText(Translator.R("NextRandomGenerated"));
        nextRandom.setToolTipText(Translator.R("NextRandomlySelected"));
        nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
        prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
        previous.setToolTipText(Translator.R("PreviousBoulder"));
        settings.setToolTipText(Translator.R("Settings"));
        next.setToolTipText(Translator.R("FwdBoulder"));
        createWallWindow.add(tools, BorderLayout.NORTH);
        createWallWindow.add(tools2, BorderLayout.SOUTH);
        createWallWindow.pack();
        gp.repaintAndSend(gs);
        createWallWindow.setSize((int) nw, (int) nh + tools.getHeight() + tools2.getHeight());
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

    private static List<Boulder> getAll(ListModel<Boulder> model) {
        ArrayList<Boulder> r = new ArrayList(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            r.add(model.getElementAt(i));
        }
        return r;
    }

    private static void generateListJumper(GridPane gp, JLabel name, JButton next, JButton previous, JButton nextInList, JButton prevInList) {
        listJump.removeAll();
        Vector<Boulder> v = list.getHistory();
        for (Boulder boulder : v) {
            JMenuItem i = new JMenuItem();
            i.setText(boulder.getGradeAndName());
            i.setToolTipText("<html>" + boulder.getStandardTooltip());
            if (boulder.getGradeAndName().equals(name.getText())) {
                i.setFont(i.getFont().deriveFont(Font.PLAIN));
            }
            i.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Component[] all = ((JMenuItem) e.getSource()).getParent().getComponents();
                    for (int j = 0; j < all.length; j++) {
                        Component c = all[j];
                        if (c == e.getSource()) {
                            list.setIndex(j);
                            Boulder r = list.getCurrentInHistory();
                            hm.addToBoulderHistory(r);
                            gp.getGrid().setBouler(r);
                            name.setText(r.getGradeAndName());
                            name.setToolTipText(r.getStandardTooltip());
                            gp.repaintAndSend(gs);
                            Files.setLastBoulder(r);
                            next.setEnabled(hm.canFwd());
                            previous.setEnabled(hm.canBack());
                            nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                            prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                            nextInList.setEnabled(list.canFwd());
                            prevInList.setEnabled(list.canBack());
                        }
                    }
                }
            });
            listJump.add(i);
        }
    }

    private static void generateHistoryJumper(GridPane gp, JLabel name, JButton next, JButton previous, JButton nextInList, JButton prevInList) {
        historyJump.removeAll();
        Vector<Boulder> v = hm.getHistory();
        for (Boulder boulder : v) {
            JMenuItem i = new JMenuItem();
            i.setText(boulder.getGradeAndName());
            i.setToolTipText("<html>" + boulder.getStandardTooltip());
            if (boulder.getGradeAndName().equals(name.getText())) {
                i.setFont(i.getFont().deriveFont(Font.PLAIN));
            }
            i.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Component[] all = ((JMenuItem) e.getSource()).getParent().getComponents();
                    for (int j = 0; j < all.length; j++) {
                        Component c = all[j];
                        if (c == e.getSource()) {
                            hm.setIndex(j);
                            Boulder r = hm.getCurrentInHistory();
                            gp.getGrid().setBouler(r);
                            name.setText(r.getGradeAndName());
                            name.setToolTipText(r.getStandardTooltip());
                            gp.repaintAndSend(gs);
                            if (r.getFile() != null) {
                                Files.setLastBoulder(r);
                            }
                            next.setEnabled(hm.canFwd());
                            previous.setEnabled(hm.canBack());
                            break;
                        }
                    }
                }
            });
            historyJump.add(i);
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
        final boolean[] change = new boolean[]{false, false, false};
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(p.img));
        final JDialog operateBoulder = new JDialog((JFrame) null, true);
        operateBoulder.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridPane gp = new GridPane(bi, p.props, gs);
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

    private static class BoulderListAndIndex {

        private final int seelctedIndex;
        private final Boulder selctedValue;
        private final List<Boulder> list;

        public BoulderListAndIndex(int seelctedIndex, Boulder selctedValue, List<Boulder> list) {
            this.seelctedIndex = seelctedIndex;
            this.selctedValue = selctedValue;
            this.list = list;
        }

    }

    private static BoulderListAndIndex selectListBouder(String wallId) {
        try {
            return selectListBouderImpl(wallId);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
    }

    private static final SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static BoulderListAndIndex selectListBouderImpl(String wallID) throws IOException {
        final int[] result = new int[]{0};
        final int ALL = 1;
        final int SEL = 2;
        final Map<String, GridPane.Preload> wallCache = new HashMap();
        JDialog d = new JDialog((JDialog) null, true);
        d.setSize(800, 600);
        d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final JList<Boulder> boulders = new JList();
        final JPanel boulderPreview = new JPanel(new BorderLayout());
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(boulders), boulderPreview);
        d.add(sp);
        boulders.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    boulderPreview.removeAll();
                    Boulder b = boulders.getSelectedValue();
                    if (b != null) {
                        //disable repainting of boulder on real world?
                        GridPane.Preload prelaod = wallCache.get(b.getWall());
                        if (prelaod == null) {
                            prelaod = GridPane.preload(new ZipInputStream(new FileInputStream(Files.getWallFile(b.getWall()))), b.getWall());
                            wallCache.put(b.getWall(), prelaod);
                        }
                        GridPane gdp = new GridPane(ImageIO.read(new ByteArrayInputStream(prelaod.img)), prelaod.props, gs);
                        gdp.getGrid().setBouler(b);
                        boulderPreview.add(gdp);
                        boulderPreview.validate();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        //delete boulders? made walls editabel to allow old boulders cleanup? delete walls?
        JPanel resultsPanel1 = new JPanel(new GridLayout(1, 2));
        JPanel resultsPanel2 = new JPanel(new GridLayout(1, 2));
        JButton addAll = new JButton(Translator.R("BAddAll"));
        addAll.setFont(addAll.getFont().deriveFont(Font.BOLD | Font.ITALIC));
        resultsPanel1.add(addAll);
        addAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Files.lastUsedToLastApplied();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(d, ex);
                }
                result[0] = ALL;
                d.setVisible(false);
            }
        });
        JButton addSeelcted = new JButton(Translator.R("BAddSel"));
        addSeelcted.setFont(addSeelcted.getFont().deriveFont(Font.BOLD));
        resultsPanel1.add(addSeelcted);
        addSeelcted.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Files.lastUsedToLastApplied();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(d, ex);
                }
                result[0] = SEL;
                d.setVisible(false);
            }
        });
        JButton deleteAll = new JButton(Translator.R("delAll"));
        deleteAll.setFont(deleteAll.getFont().deriveFont(Font.PLAIN));
        JButton delteSelected = new JButton(Translator.R("delSel"));
        delteSelected.setFont(delteSelected.getFont().deriveFont(Font.PLAIN));
        resultsPanel2.add(deleteAll);
        resultsPanel2.add(delteSelected);
        JPanel tools0 = new JPanel(new BorderLayout());
        JPanel tools1 = new JPanel(new GridLayout(1, 3));
        JPanel tools2 = new JPanel(new GridLayout(1, 3));
        JPanel tools3 = new JPanel(new BorderLayout());
        JPanel tools4 = new JPanel(new GridLayout(1, 3));
        JPanel tools5 = new JPanel(new BorderLayout());
        JPanel tools6 = new JPanel(new GridLayout(1, 3));
        JButton lastApplied = new JButton(Translator.R("BLastAppliedFilter"));
        tools6.add(lastApplied);
        JButton lastUsed = new JButton(Translator.R("BLastUsedFilter"));
        tools6.add(lastUsed);
        JButton wallDefault = new JButton(Translator.R("BWallDefault"));
        tools6.add(wallDefault);
        tools0.add(new JLabel(Translator.R("Wall")));
        final JComboBox<String> walls = new JComboBox(Files.wallsDir.list());
        tools0.add(walls);
        final JCheckBox random = new JCheckBox(Translator.R("random"), true);
        tools0.add(random, BorderLayout.EAST);
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
        tools1.add(gradesFrom);
        tools1.add(gradesTo);
        tools2.add(new JLabel(Translator.R("NumberOfHolds") + " " + Translator.R("FromTo")));
        final JSpinner holdsMin = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        tools2.add(holdsMin);
        final JSpinner holdsMax = new JSpinner(new SpinnerNumberModel(100, 0, 1000, 1));
        tools2.add(holdsMax);
        JLabel nameLabel = new JLabel(Translator.R("NameFilter"));
        tools3.add(nameLabel, BorderLayout.WEST);
        final JTextField nameFilter = new JTextField();
        tools3.add(nameFilter);
        final JLabel authorLabel = new JLabel(Translator.R("AuthorFilter"));
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
        final JTextField dateFrom = new JTextField();
        tools4.add(dateFrom);
        final JTextField dateTo = new JTextField();
        tools4.add(dateTo);

        wallDefault.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                resetDefaults(wallID, walls, boulders, holdsMin, holdsMax, authorLabel, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
            }
        });
        resetDefaults(wallID, walls, boulders, holdsMin, holdsMax, authorLabel, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);

        JPanel tools = new JPanel(new GridLayout(8, 1));
        tools.add(tools0);
        tools.add(tools1);
        tools.add(tools2);
        tools.add(tools3);
        tools.add(tools4);
        tools.add(tools5);
        tools.add(tools6);
        final JButton apply = new JButton(Translator.R("Apply"));
        tools.add(apply);
        d.add(tools, BorderLayout.NORTH);
        JPanel resultsPanel = new JPanel(new GridLayout(2, 1));
        resultsPanel.add(resultsPanel1);
        resultsPanel.add(resultsPanel2);
        d.add(resultsPanel, BorderLayout.SOUTH);
        boulders.setCellRenderer(new BoulderListRenderer());
        lastApplied.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (Files.getLastAppliedFilterFile().exists()) {
                        Filter f = Filter.load(Files.getLastAppliedFilterFile());
                        if (f != null) {
                            applyFilter(f, wallID, walls, holdsMin, holdsMax, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        lastUsed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (Files.getLastUsedFilterFile().exists()) {
                        Filter f = Filter.load(Files.getLastUsedFilterFile());
                        if (f != null) {
                            applyFilter(f, wallID, walls, holdsMin, holdsMax, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        apply.addActionListener(new ApplyFilterListener(walls, gradesFrom, gradesTo, holdsMin, holdsMax, authorsFilter, nameFilter, dateFrom, dateTo, boulders, random));
        sp.setDividerLocation(d.getWidth() / 2);
        wallDefault.setFont(addAll.getFont().deriveFont(Font.PLAIN));
        deleteAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (boulders.getModel() == null || boulders.getModel().getSize() == 0) {
                    return;
                }
                if (auth.isPernament()) {
                    int y = JOptionPane.showConfirmDialog(d, Translator.R("delConf", boulders.getModel().getSize()));
                    if (y != JOptionPane.YES_OPTION) {
                        return;
                    }
                } else {
                    try {
                        auth.authenticate(Translator.R("delConf", boulders.getModel().getSize()));
                    } catch (Authenticator.AuthoriseException ex) {
                        JOptionPane.showMessageDialog(d, ex);
                        return;
                    }

                }
                for (int x = boulders.getModel().getSize() - 1; x >= 0; x--) {
                    Boulder i = boulders.getModel().getElementAt(x);
                    i.getFile().delete();
                }
                apply.getActionListeners()[0].actionPerformed(null);

            }
        });
        delteSelected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (boulders.getSelectedValuesList() == null || boulders.getSelectedValuesList().isEmpty()) {
                    return;
                }
                if (auth.isPernament()) {
                    int y = JOptionPane.showConfirmDialog(d, Translator.R("delConf", boulders.getSelectedValuesList().size()));
                    if (y != JOptionPane.YES_OPTION) {
                        return;
                    }
                } else {
                    try {
                        auth.authenticate(Translator.R("delConf", boulders.getSelectedValuesList().size()));
                    } catch (Authenticator.AuthoriseException ex) {
                        JOptionPane.showMessageDialog(d, ex);
                        return;
                    }

                }
                for (Boulder b : boulders.getSelectedValuesList()) {
                    b.getFile().delete();
                }
                apply.getActionListeners()[0].actionPerformed(null);

            }
        });
        d.setVisible(true);
        if (boulders.getModel().getSize() == 0) {
            d.dispose();
            return null;
        }
        switch (result[0]) {
            case SEL: {
                BoulderListAndIndex r = new BoulderListAndIndex(boulders.getSelectedIndex(), boulders.getSelectedValue(), boulders.getSelectedValuesList());
                d.dispose();
                return r;
            }
            case ALL: {
                BoulderListAndIndex r = new BoulderListAndIndex(boulders.getSelectedIndex(), boulders.getSelectedValue(), getAll(boulders.getModel()));
                d.dispose();
                return r;
            }
            default:
                d.dispose();
                return null;
        }
    }

    public static ListWithFilter resetDefaults(String wallID, final JComboBox<String> walls, final JList<Boulder> boulders, final JSpinner holdsMin, final JSpinner holdsMax, final JLabel authorLabel, final JTextField dateFrom, final JTextField dateTo, final JComboBox<String> gradesFrom, final JComboBox<String> gradesTo, JTextField author, JTextField name, JCheckBox random) {
        ListWithFilter currentList = new ListWithFilter(wallID);
        walls.setSelectedItem(wallID);
        boulders.setModel(new DefaultComboBoxModel<>(currentList.getHistory()));
        holdsMin.setValue(currentList.getShortest());
        holdsMax.setValue(currentList.getLongest());
        authorLabel.setToolTipText(currentList.getAuthors());
        dateFrom.setText(dtf.format(currentList.getOldest()));
        dateTo.setText(dtf.format(currentList.getYoungest()));
        gradesFrom.setSelectedItem(currentList.getEasiest().toString());
        gradesTo.setSelectedItem(currentList.getHardest().toString());
        author.setText("");
        name.setText("");
        random.setSelected(true);
        return currentList;
    }

    public static void applyFilter(Filter f, String wallID, final JComboBox<String> walls, final JSpinner holdsMin, final JSpinner holdsMax, final JTextField dateFrom, final JTextField dateTo, final JComboBox<String> gradesFrom, final JComboBox<String> gradesTo, JTextField author, JTextField name, JCheckBox random) {
        walls.setSelectedItem(f.wall);
        holdsMin.setValue(f.pathMin);
        holdsMax.setValue(f.pathTo);
        dateFrom.setText(dtf.format(new Date(f.ageFrom)));
        dateTo.setText(dtf.format(new Date(f.ageTo)));
        gradesFrom.setSelectedItem(new Grade(f.gradeFrom).toString());
        gradesTo.setSelectedItem(new Grade(f.gradeTo).toString());
        author.setText(String.join(" ", f.authorLike));
        name.setText(String.join(" ", f.nameLike));
        random.setSelected(f.random);
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
            setText("<html><big><b>" + grade + "</b>:  <u>" + b.getName() + "</u>| <i>" + b.getAuthor() + "</i> (" + dtf.format(b.getDate()) + ")[" + b.getWall() + "]");

            if (isSelected) {
//                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setBackground(new Color(225, 0, 0));

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

    private static class ApplyFilterListener implements ActionListener {

        private final JComboBox<String> walls;
        private final JComboBox<String> gradesFrom;
        private final JComboBox<String> gradesTo;
        private final JSpinner holdsMin;
        private final JSpinner holdsMax;
        private final JTextField authorsFilter;
        private final JTextField nameFilter;
        private final JTextField dateFrom;
        private final JTextField dateTo;
        private final JList<Boulder> boulders;
        private ListWithFilter lastList;
        private final JCheckBox random;

        public ListWithFilter getLastList() {
            return lastList;
        }

        public ApplyFilterListener(JComboBox<String> walls, JComboBox<String> gradesFrom, JComboBox<String> gradesTo, JSpinner holdsMin, JSpinner holdsMax, JTextField authorsFilter, JTextField nameFilter, JTextField dateFrom, JTextField dateTo, JList<Boulder> boulders, JCheckBox random) {
            this.walls = walls;
            this.gradesFrom = gradesFrom;
            this.gradesTo = gradesTo;
            this.holdsMin = holdsMin;
            this.holdsMax = holdsMax;
            this.authorsFilter = authorsFilter;
            this.nameFilter = nameFilter;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.boulders = boulders;
            this.random = random;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                lastList = new ListWithFilter(
                        new Filter(
                                (String) walls.getSelectedItem(),
                                gradesFrom.getSelectedIndex(),
                                gradesTo.getSelectedIndex(),
                                (Integer) (holdsMin.getValue()),
                                (Integer) (holdsMax.getValue()),
                                authorsFilter.getText(),
                                nameFilter.getText(),
                                dtf.parse(dateFrom.getText()),
                                dtf.parse(dateTo.getText()),
                                random.isSelected())
                );
                lastList.getLastFilter().save(Files.getLastAppliedFilterFile());
                boulders.setModel(new DefaultComboBoxModel<>(lastList.getHistory()));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dateFrom, ex);
            }
        }
    }

    private static class QuickFilterLIstener implements ActionListener {

        private final Grade from;
        private final Grade to;
        private final String wall;
        private final JButton nextInList;
        private final JButton prevInList;
        private final GridPane gp;
        private final JButton next;
        private final JButton previous;
        private final JLabel name;

        public QuickFilterLIstener(int gradeFrom, int gradeTo, String wall, JButton nextInRow, JButton prevInRow, GridPane gp, JLabel name, JButton next, JButton prev) {
            this.from = new Grade(gradeFrom);
            this.to = new Grade(gradeTo);
            this.wall = wall;
            this.nextInList = nextInRow;
            this.prevInList = prevInRow;
            this.next = next;
            this.previous = prev;
            this.gp = gp;
            this.name = name;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            list = new ListWithFilter(from, to, wall);
            next.setEnabled(list.canFwd());
            previous.setEnabled(list.canBack());
            if (list.getSize() > 0) {
                list.setIndex(list.getSize() - 1);
                Boulder b = list.getCurrentInHistory();
                gp.getGrid().setBouler(b);
                hm.addToBoulderHistory(b);
                gp.getGrid().setBouler(b);
                name.setText(b.getGradeAndName());
                name.setToolTipText(b.getStandardTooltip());
                gp.repaintAndSend(gs);
                Files.setLastBoulder(b);
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                nextInList.setEnabled(list.canFwd());
                prevInList.setEnabled(list.canBack());
                nextInList.setToolTipText(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize());
                prevInList.setToolTipText(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize());
            }
        }
    }

    private static class PathColorCompozitorListener implements ChangeListener {

        private final JSpinner sss;
        private final int i;
        private final GridPane gp;
        private final Component prev;

        private PathColorCompozitorListener(int i, GridPane gp, JSpinner sss, Component prev) {
            this.sss = sss;
            this.i = i;
            this.gp = gp;
            this.prev = prev;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JSpinner s = (JSpinner) e.getSource();
            gs.setPart(((Double) (s.getValue())), i);
            Color c = Color.WHITE;
            if (i >= 0 && i < 3) {
                c = gs.getStartColor();
            } else if (i >= 3 && i < 6) {
                c = gs.getPathColor();
            } else if (i >= 6 && i < 9) {
                c = gs.getTopColor();
            }
            prev.setBackground(c);
            prev.repaint();
            gs.setBrightness(((Integer) sss.getValue()));
            gp.repaintAndSend(gs);
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
