package org.fbb.board.desktop.gui;

import org.fbb.board.internals.training.BoulderCalc;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.awt.event.InputEvent.CTRL_MASK;
import java.awt.event.KeyEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.desktop.TextToSpeech;
import org.fbb.board.desktop.TrainingSaveLoadDialog;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.grid.GridPane;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.HistoryManager;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.db.Puller;
import org.fbb.board.internals.training.TimeredTraining;
import org.fbb.board.internals.training.Training;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.db.GuiExceptionHandler;
import org.fbb.board.internals.db.ExceptionHandler;
import org.fbb.board.internals.training.ListSetter;
import org.fbb.board.internals.training.TrainingPlaylist;
import org.fbb.board.internals.training.TrainingWithBackends;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindow {

    private static final GlobalSettings gs = new GlobalSettings();
    public static final HistoryManager hm = new HistoryManager(100);
    public static ListWithFilter list;
    private static final JPopupMenu listJump = new JPopupMenu();
    private static final JPopupMenu historyJump = new JPopupMenu();
    private static final DB db = new DB(gs);
    private static final Puller puller = Puller.create(gs.getPullerDelay() * 60, db);
    private static final ActionListener showTips = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, "<html>"
                    + "<li>" + Translator.R("tip1")
                    + "<ul>"
                    + "<li>" + escape(Translator.R("tip2"))
                    + "<li>" + escape(Translator.R("tip3"))
                    + "</ul>"
                    + "<ul>"
                    + "<li>" + escape(Translator.R("tip4"))
                    + "<li>" + escape(Translator.R("tip5"))
                    + "<li>" + escape(Translator.R("tip6"))
                    + "</ul>"
                    + "<li>" + escape(Translator.R("tips7"))
                    + "<ul>"
                    + "<li>" + escape(Translator.R("tips8"))
                    + "<li>" + escape(Translator.R("tips9"))
                    + "<li>" + escape(Translator.R("tips10"))
                    + "</ul>"
            );
        }

        private String escape(String R) {
            return R.replace("<", "&lt;").replace(">", "&gt;");
        }
    };
    private static final KeyEventDispatcher f1 = new KeyEventDispatcher() {

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F1 && e.getID() == KeyEvent.KEY_PRESSED) {
                showTips.actionPerformed(null);
                return true;
            }
            return false;
        }
    };

    public static void main(String... s) {
        try {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(f1);
            Grid.colorProvider = gs;
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
                GuiLogHelper.guiLogger.loge("Last boulder but not lat wall!");
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
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
        }
    }

    private static void createSelectOrImportWall(String urlorFile, final JFrame... redundants) throws IOException {
        JDialog f = new JDialog((JFrame) null, Translator.R("MainWindowSetWall"), true);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        LoadBackgroundOrImportOrLoadWall panel = new LoadBackgroundOrImportOrLoadWall(urlorFile, Authenticator.auth, db, gs, puller);
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
                    db.add(new GuiExceptionHandler(), "(wall " + f.getName() + ")", f);
                    Files.setLastBoard(n);
                    createWallWindow.dispose();
                    loadWallWithBoulder(n);
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
                setIdealWindowLocation(createWallWindow);
                createWallWindow.setVisible(true);
            }
        });
    }

    static double getIdealWindowSizw(BufferedImage bis) {
        Rectangle size = ScreenFinder.getCurrentScreenSizeWithoutBounds();
        double dw = (double) size.width / (double) bis.getWidth();
        double dh = (double) size.height / (double) bis.getHeight();
        double ratio = Math.min(dw, dh);
        ratio = ratio * gs.getRatio();
        return ratio;
    }

    static void setIdealWindowLocation(Window w) {
        int he = gs.getHardcodedEdge();
        if (gs.getLocation().equalsIgnoreCase("TR")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("TL")) {
            w.setLocation(
                    (int) (he),
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("BR")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);
        } else if (gs.getLocation().equalsIgnoreCase("BL")) {
            w.setLocation(
                    (int) (he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);

        } else if (gs.getLocation().equalsIgnoreCase("T")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("B")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);
        } else if (gs.getLocation().equalsIgnoreCase("R")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        } else if (gs.getLocation().equalsIgnoreCase("L")) {
            w.setLocation(
                    (int) (he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        } else {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        }
    }

    private static void loadWallWithBoulder(String lastBoard) throws IOException {
        File f = Files.getWallFile(lastBoard);
        GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(f)), f.getName());
        loadWallWithBoulder(preloaded, null);
    }

    private static void loadWallWithBoulder(GridPane.Preload preloaded, final Boulder possiblebOulder) throws IOException {
        final JToggleButton[] quickFilters = new JToggleButton[5];
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        final JFrame createWallWindow = new JFrame("Flash Free Board");
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
        final JButton nextRandom = new JButton(Translator.R("random2"));
        JButton settings = new JButton(Translator.R("menu"));//settings - new boulder, new/edit wall..., edit boulder, save curren boulder as, start timered-training
        JButton nextRandomGenerated = new JButton(Translator.R("generate"));
        final JButton historyButtons = new JButton("ˇ");
        final JButton nextInList = new JButton(">>");
        final JButton prevInList = new JButton("<<");
        JLabel historyLabel1 = new JLabel(Translator.R("historyLabel"), SwingConstants.CENTER);
        JLabel historyLabel2 = new JLabel(Translator.R("historyLabel"), SwingConstants.CENTER);
        JLabel name = new JLabel();
        setNameTextAndGrade(name, b);
        JPopupMenu jp = new JPopupMenu();
        next.setEnabled(hm.canFwd());
        previous.setEnabled(hm.canBack());
        gp.getGrid().setShowGrid(false);
        final JPanel tools = new JPanel(new GridLayout(2, 1));
        final JPanel quickFilterPanel = new JPanel(new GridLayout(1, 4));
        final JPanel tools2wrapper = new JPanel(new BorderLayout());
        final JPanel tools2List = new JPanel(new GridLayout(1, 4));
        final JPanel tools2History = new JPanel(new GridLayout(1, 4));
        JToggleButton a5 = new JToggleButton("-5A");
        quickFilterPanel.add(a5);
        JToggleButton a6 = new JToggleButton("5A-6A");
        quickFilterPanel.add(a6);
        JToggleButton a7 = new JToggleButton("6A-7A");
        quickFilterPanel.add(a7);
        JToggleButton a8 = new JToggleButton("7A-8A");
        quickFilterPanel.add(a8);
        JToggleButton a9 = new JToggleButton("8A+");
        quickFilterPanel.add(a9);
        quickFilters[0] = a5;
        quickFilters[1] = a6;
        quickFilters[2] = a7;
        quickFilters[3] = a8;
        quickFilters[4] = a9;
        a5.addActionListener(new QuickFilterLIstener(Grade.getMinGrade(), 10, preloaded.givenId, nextInList, prevInList, gp, name, next, previous, quickFilters));
        a6.addActionListener(new QuickFilterLIstener(10, 13, preloaded.givenId, nextInList, prevInList, gp, name, next, previous, quickFilters));
        a7.addActionListener(new QuickFilterLIstener(13, 19, preloaded.givenId, nextInList, prevInList, gp, name, next, previous, quickFilters));
        a8.addActionListener(new QuickFilterLIstener(19, 26, preloaded.givenId, nextInList, prevInList, gp, name, next, previous, quickFilters));
        a9.addActionListener(new QuickFilterLIstener(26, Grade.getMaxGrade(), preloaded.givenId, nextInList, prevInList, gp, name, next, previous, quickFilters));
        name.setToolTipText(b.getStandardTooltip());
        historyButtons.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                tools2History.setVisible(!tools2History.isVisible());
                if (!tools2History.isVisible()) {
                    historyButtons.setText("ˇ");
                } else {
                    historyButtons.setText("^");
                }
            }
        });
        name.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(createWallWindow, name.getToolTipText());
            }

        });
        nextInList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
                    generateListJumper(gp, name, next, previous, nextInList, nextInList);
                    listJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });
        prevInList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
                    generateListJumper(gp, name, next, previous, nextInList, prevInList);
                    listJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });

        next.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
                    generateHistoryJumper(gp, name, next, previous, nextInList, prevInList);
                    historyJump.show((JButton) e.getSource(), 0, 0);
                }
            }

        });
        previous.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
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
        jp.add(selectListBoulders);
        JMenuItem newBoulder = new JMenuItem(Translator.R("MNewBoulder"));
        jp.add(newBoulder);
        JMenu subTrains = new JMenu(Translator.R("trainings"));
        jp.add(subTrains);
        JMenuItem timered = new JMenuItem(Translator.R("timered"));
        subTrains.add(timered);
        JMenuItem campus = new JMenuItem(Translator.R("campus"));
        subTrains.add(campus);
        JMenu subGames = new JMenu(Translator.R("games")); //clock, lines, catch the ball
        jp.add(subGames);
        JMenuItem ball = new JMenuItem(Translator.R("ball"));
        subGames.add(ball);
        JMenuItem box = new JMenuItem(Translator.R("box"));
        subGames.add(box);
        JMenu sub1 = new JMenu(Translator.R("Admin"));
        jp.add(sub1);
        JMenuItem management = new JMenuItem(Translator.R("management"));
        sub1.add(management);
        JMenuItem newEditWall = new JMenuItem(Translator.R("MEditWall"));
        sub1.add(newEditWall);
        JMenu sub2 = new JMenu(Translator.R("Special"));
        jp.add(sub2);
        JMenuItem logItem = new JMenuItem("Logs");
        JMenuItem web = new JMenuItem("web");
        web.addActionListener(new ShowWebHelp());
        JMenuItem revokePermission = new JMenuItem(Translator.R("revokePP"));
        JMenuItem editBoulder = new JMenuItem(Translator.R("MEditBoulder"));
        //with edit bolder his looks like redundant
        JMenuItem saveBoulder = new JMenuItem(Translator.R("MSaveCurrenBoulder"));
        saveBoulder.setEnabled(false);
        JMenuItem reset = new JMenuItem("remote reset");
        JMenuItem stats = new JMenuItem(Translator.R("stats"));
        sub2.add(editBoulder);
        sub2.add(saveBoulder);
        sub2.add(revokePermission);
        sub2.add(logItem);
        sub2.add(stats);
        sub2.add(web);
        sub2.add(reset);
        JMenuItem tips = new JMenuItem("Tips");
        jp.add(tips);

        selectListBoulders.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderFiltering.BoulderListAndIndex listAndothers = new BoulderFiltering(db, gs).selectListBouder(preloaded.givenId);
                if (listAndothers != null) {
                    for (JToggleButton quickFilter : quickFilters) {
                        quickFilter.setSelected(false);
                    }
                    list = new ListWithFilter(listAndothers.list);
                    if (!list.getHistory().isEmpty()) {
                        if (listAndothers.selctedValue != null) {
                            list.setIndex(listAndothers.selctedValue.getFile().getName());
                        } else {
                            if (list.isInRange(listAndothers.seelctedIndex)) {
                                list.setIndex(listAndothers.seelctedIndex);
                            } else {
                                list.setIndex(list.getSize() - 1);
                            }
                        }
                        Boulder r = list.getCurrentInHistory();
                        hm.addToBoulderHistory(r);
                        gp.getGrid().setBouler(r);
                        setNameTextAndGrade(name, r);
                        gp.repaintAndSend(gs);
                        Files.setLastBoulder(r);
                        next.setEnabled(hm.canFwd());
                        previous.setEnabled(hm.canBack());
                        list.setIndex(r.getFile().getName());
                    }
                    nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                }
            }

        });
        newBoulder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderCreationGui.BoulderAndSaved bs = editBoulder(preloaded, null);
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    setNameTextAndGrade(name, r);
                    gp.repaintAndSend(gs);
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        nextInList.setEnabled(list.canFwd());
                        prevInList.setEnabled(list.canBack());
                    }
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                }
            }
        });
        editBoulder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderCreationGui.BoulderAndSaved bs = editBoulder(preloaded, hm.getCurrentInHistory());
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    setNameTextAndGrade(name, r);
                    gp.repaintAndSend(gs);
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        nextInList.setEnabled(list.canFwd());
                        prevInList.setEnabled(list.canBack());
                    }
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                }
            }

        });
        stats.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new StatsDialog(preloaded.givenId, db, gs).setVisible(true);
            }
        });
        saveBoulder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                db.pullCatched(new ExceptionHandler.LoggingEater());
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
                    setNameTextAndGrade(name, b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    Files.setLastBoulder(b);
                    list.addToBoulderHistory(b);
                    list.setIndex(b.getFile().getName());
                    nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                } catch (IOException ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        newEditWall.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    db.pullCatched(new ExceptionHandler.LoggingEater());
                    createSelectOrImportWall(Files.getWallFile(preloaded.givenId).toURI().toURL().toExternalForm(), createWallWindow);
                    db.addAll();
                } catch (IOException | GitAPIException ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        campus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog campusWindow = new CampusLikeDialog((Component) e.getSource(), gp);
                campusWindow.setVisible(true);

            }
        });
        ball.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog ballWindow = new BallWindow((Component) e.getSource(), gp);
                ballWindow.setVisible(true);

            }
        });
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog boxws = new BoxesWindow((Component) e.getSource(), gp);
                boxws.setVisible(true);

            }
        });
        timered.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog timeredWindow = new JDialog((JFrame) null, Translator.R("timered"));
                final TimeredTraining[] trainig = new TimeredTraining[1];
                timeredWindow.setModal(true);
                timeredWindow.setLayout(new GridLayout(10, 2));
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
                final JCheckBox allowRegular = new JCheckBox(Translator.R("allowRegular"), true);
                allowRegular.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] != null) {
                            trainig[0].setRegularAllowed(allowRegular.isSelected());
                        }
                    }
                });
                timeredWindow.add(allowRegular);
                final JCheckBox allowJumps = new JCheckBox(Translator.R("allowJumps"), true);
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
                timeredWindow.add(new JLabel());
                final JButton save1 = new JButton(Translator.R("Bsave1"));
                timeredWindow.add(save1);
                save1.setToolTipText(Translator.R("HintSaveByList"));
                final JButton save2 = new JButton(Translator.R("Bsave2"));
                timeredWindow.add(save2);
                save2.setToolTipText(Translator.R("HintSaveByFilter"));
                final JButton load = new JButton(Translator.R("Bload"));
                timeredWindow.add(new JLabel());
                timeredWindow.add(load);
                load.setToolTipText(Translator.R("HintLaod"));
                final JButton createList = new JButton(Translator.R("BCreateList"));
                createList.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JDialog d = new JDialog();
                        d.setModal(true);
                        d.setSize(750, 600);
                        d.setLayout(new GridLayout(0, 1));
                        JPanel row0 = new JPanel(new GridLayout(0, 4));
                        JButton save = (new JButton("Save"));
                        JTextField name = (new JTextField("name"));
                        JButton remove = (new JButton("-"));
                        JButton add = (new JButton("+"));
                        add.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                JPanel row = new JPanel(new GridLayout(0, 2));
                                JTextField time = (new JTextField("00:10"));
                                time.setToolTipText("initial delay mm:ss");
                                JComboBox training = (new JComboBox(desuffix(Files.trainingsDir.list())));
                                row.add(time);
                                row.add(training);
                                d.add(row);
                                d.pack();
                            }

                            private String[] desuffix(String[] list) {
                                for (int i = 0; i < list.length; i++) {
                                    String list1 = list[i];
                                    list[i] = list1.replaceAll(".sitr$", "");
                                }
                                return list;
                            }
                        });
                        remove.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Container dd = d.getContentPane();
                                if (dd.getComponentCount() > 1) {
                                    dd.remove(dd.getComponents()[dd.getComponents().length - 1]);
                                    d.pack();
                                }
                            }
                        });
                        save.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Container dd = d.getContentPane();
                                if (dd.getComponentCount() > 1) {
                                    List<String> l = new ArrayList<>(dd.getComponentCount() - 1);
                                    for (int x = 1; x < dd.getComponentCount(); x++) {
                                        JPanel p = (JPanel) dd.getComponent(x);
                                        JTextField ti = (JTextField) p.getComponent(0);
                                        JComboBox tr = (JComboBox) p.getComponent(1);
                                        l.add(ti.getText() + "/" + tr.getSelectedItem().toString());
                                    }
                                    try {
                                        File f = new File(Files.trainingLilstDir, name.getText() + ".tpl");
                                        java.nio.file.Files.write(f.toPath(), l);
                                        db.add(new ExceptionHandler.Resender(), "", f);
                                    } catch (IOException ex) {
                                        GuiLogHelper.guiLogger.loge(ex);
                                        JOptionPane.showMessageDialog(null, ex);
                                    }
                                }
                            }
                        });
                        row0.add(save);
                        row0.add(name);
                        row0.add(remove);
                        row0.add(add);
                        d.add(row0);
                        d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        d.pack();
                        d.setLocationRelativeTo(timeredWindow);
                        d.setVisible(true);
                    }
                });
                timeredWindow.add(createList);
                createList.setToolTipText(Translator.R("HintCreateList"));
                final JButton loadList = new JButton(Translator.R("BloadList"));
                timeredWindow.add(loadList);
                loadList.setToolTipText(Translator.R("HintPlayList"));
                ListSetter ls = new ListSetter() {
                    @Override
                    public void setUpBoulderWall(Filter filter, String selected, String title) {
                        if (filter != null) {
                            if (title != null) {
                                timeredWindow.setTitle(title);
                            }
                            list = new ListWithFilter(filter);
                            if (list.getSize() > 0) {
                                for (JToggleButton b : quickFilters) {
                                    b.setSelected(false);
                                }
                                if (selected != null) {
                                    list.setIndex(selected);
                                }
                                Boulder b = list.getCurrentInHistory();
                                gp.getGrid().setBouler(b);
                                hm.addToBoulderHistory(b);
                                gp.getGrid().setBouler(b);
                                setNameTextAndGrade(name, b);
                                gp.repaintAndSend(gs);
                                Files.setLastBoulder(b);
                                next.setEnabled(hm.canFwd());
                                previous.setEnabled(hm.canBack());
                                nextInList.setEnabled(list.canFwd());
                                prevInList.setEnabled(list.canBack());
                                nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                                prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                            }
                        }
                    }
                };

                loadList.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            TrainingSaveLoadDialog tls = new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, db, Files.trainingLilstDir);
                            tls.setVisible(true);
                            if (tls.getResult() == null) {
                                return;
                            }
                            TrainingPlaylist tp = TrainingPlaylist.loadSavedTraining(tls.getResult());
                            if (tp == null) {
                                return;
                            }
                            List<Training> trainings = tp.toTrainings();
                            List<TrainingWithBackends> result = new ArrayList<>(trainings.size());
                            for (int i = 0; i < trainings.size(); i++) {
                                Training t = trainings.get(i);
                                result.add(new TrainingWithBackends(boulderCalc, allowRandom, allowRegular, allowJumps,
                                        t, tp.getPauseForTraing(i), tp.getTitleForTraing(i), ls));
                            }
                            if (trainig[0] != null) {
                                trainig[0].stop();
                            }
                            if (trainig[0] == null) {
                                if (result.size() > 0) {
                                    result.get(0).setBoulderCalc();
                                    result.get(0).setChecks();
                                    result.get(0).init();
                                }
                                trainig[0] = new TimeredTraining(
                                        nextInList.getActionListeners()[0],
                                        prevInList.getActionListeners()[0],
                                        nextRandom.getActionListeners()[0],
                                        nextRandomGenerated.getActionListeners()[0],
                                        (ActionEvent e1) -> {
                                            gp.getGrid().clean();
                                            gp.repaintAndSend(gs);
                                        },
                                        result,
                                        counterClock, (TextToSpeech.TextId) reader.getSelectedItem()
                                );
                                new Thread(trainig[0]).start();
                            } else {
                                trainig[0].stop();
                                trainig[0] = null;
                            }
                        } catch (Exception ex) {
                            GuiLogHelper.guiLogger.loge(ex);
                            JOptionPane.showMessageDialog(null, ex);
                        }
                    }
                });
                start.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (trainig[0] == null) {
                            trainig[0] = new TimeredTraining(
                                    nextInList.getActionListeners()[0],
                                    prevInList.getActionListeners()[0],
                                    nextRandom.getActionListeners()[0],
                                    nextRandomGenerated.getActionListeners()[0],
                                    new ActionListener() {

                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            gp.getGrid().clean();
                                            gp.repaintAndSend(gs);
                                        }
                                    },
                                    Arrays.asList(new TrainingWithBackends[]{
                                        new TrainingWithBackends(boulderCalc, allowRandom, allowRegular, allowJumps,
                                                new Training(allowRandom.isSelected(), allowRegular.isSelected(), allowJumps.isSelected(), timeOfBoulder.getText(), timeOfTraining.getText(), (Integer) (numBoulders.getValue()), null, null),
                                                0, null, null)}),
                                    counterClock, (TextToSpeech.TextId) reader.getSelectedItem()
                            );
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

                save1.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (list.getCurrentInHistory() != null) {
                            try {
                                File f = TrainingSaveLoadDialog.SaveTrainingDialog.show();
                                if (f != null) {
                                    new Training(allowRandom.isSelected(),
                                            allowRegular.isSelected(),
                                            allowJumps.isSelected(),
                                            timeOfBoulder.getText(),
                                            timeOfTraining.getText(),
                                            (Integer) (numBoulders.getValue()),
                                            list.enumerate(preloaded.givenId),
                                            list.getCurrentInHistory().getFile().getName()).saveSingleTraining(f);
                                    db.add(new ExceptionHandler.Resender(), "training " + f.getName(), f);
                                }
                            } catch (Exception ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(null, ex);
                            }
                        }
                    }
                });

                save2.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (list.getCurrentInHistory() != null) {
                            try {
                                File f = TrainingSaveLoadDialog.SaveTrainingDialog.show();
                                if (f != null) {
                                    new Training(allowRandom.isSelected(),
                                            allowRegular.isSelected(),
                                            allowJumps.isSelected(),
                                            timeOfBoulder.getText(),
                                            timeOfTraining.getText(),
                                            (Integer) (numBoulders.getValue()),
                                            list.getLastFilter() == null ? Filter.getAllMatching(preloaded.givenId) : list.getLastFilter(),
                                            list.getCurrentInHistory().getFile().getName()).saveSingleTraining(f);
                                    db.add(new ExceptionHandler.Resender(), "training " + f.getName(), f);
                                }
                            } catch (Exception ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(null, ex);
                            }
                        }
                    }
                });

                load.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            for (JToggleButton b : quickFilters) {
                                b.setSelected(false);
                            }
                            TrainingSaveLoadDialog tls = new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, db, Files.trainingsDir);
                            tls.setVisible(true);
                            if (tls.getResult() == null) {
                                return;
                            }
                            Training t1 = Training.loadSavedTraining(tls.getResult());
                            if (t1 == null) {
                                return;
                            }
                            if (t1.innerSettings != null) {
                                TrainingWithBackends tt = new TrainingWithBackends(boulderCalc, allowRandom, allowRegular, allowJumps, t1, 0, tls.getFileName(), ls);
                                tt.setBoulderCalc();
                                tt.setChecks();
                                tt.init();

                            }
                        } catch (Exception ex) {
                            GuiLogHelper.guiLogger.loge(ex);
                            JOptionPane.showMessageDialog(null, ex);
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
        management.addActionListener(new SettingsListener(gp, Authenticator.auth, gs, puller, db, 0, preloaded.givenId));
        logItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new LogView(db).setVisible(true);
            }
        });
        revokePermission.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Authenticator.auth.revoke();
                db.revoke();
            }
        });
        reset.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                gs.reset();
            }
        });
        tips.addActionListener(showTips);
        JPanel subtools = new JPanel(new BorderLayout());
        subtools.add(settings, BorderLayout.WEST);
        subtools.add(name);
        tools.add(quickFilterPanel);
        tools.add(subtools);
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & CTRL_MASK) != 0) {
                    return;
                }
                if (hm.canBack()) {
                    Boulder b = hm.back();
                    gp.getGrid().setBouler(b);
                    setNameTextAndGrade(name, b);
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                }
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & CTRL_MASK) != 0) {
                    return;
                }
                if (hm.canFwd()) {
                    Boulder b = hm.forward();
                    gp.getGrid().setBouler(b);
                    setNameTextAndGrade(name, b);
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                }
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
            }
        });
        previous.setEnabled(false);
        next.setEnabled(false);
        nextRandomGenerated.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Boulder b = gp.getGrid().randomBoulder(preloaded.givenId);
                setNameTextAndGrade(name, b);
                hm.addToBoulderHistory(b);
                gp.repaintAndSend(gs);
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
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
                    setNameTextAndGrade(name, b);
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                }
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
            }
        });
        nextInList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & CTRL_MASK) != 0) {
                    return;
                }
                Boulder b = list.forward();
                if (b != null) {
                    hm.addToBoulderHistory(b);
                    gp.getGrid().setBouler(b);
                    setNameTextAndGrade(name, b);
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                }
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
            }
        });
        prevInList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & CTRL_MASK) != 0) {
                    return;
                }
                Boulder b = list.back();
                if (b != null) {
                    hm.addToBoulderHistory(b);
                    gp.getGrid().setBouler(b);
                    setNameTextAndGrade(name, b);
                    gp.repaintAndSend(gs);
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                }
                if (e != null && e.getSource() != null && e.getSource() instanceof Component) {
                    ToolTipManager.sharedInstance().mouseMoved(new MouseEvent((Component) e.getSource(), 0, 0, 0, 0, 0, 0, false));
                }
            }
        });
        tools2wrapper.add(tools2List, BorderLayout.NORTH);
        tools2wrapper.add(tools2History, BorderLayout.SOUTH);
        tools2List.add(prevInList);
        tools2List.add(historyButtons);
        tools2List.add(nextRandom);
        tools2List.add(nextInList);
        tools2History.add(previous);
        tools2History.add(historyLabel1);
        tools2History.add(nextRandomGenerated);
        tools2History.add(historyLabel2);
        tools2History.add(next);
        name.setFont(name.getFont().deriveFont((float) name.getFont().getSize() * 2));
        for (Component c : tools.getComponents()) {
            c.setFont(name.getFont());
        }
        for (Component c : tools2History.getComponents()) {
            c.setFont(name.getFont());
        }
        for (Component c : tools2List.getComponents()) {
            c.setFont(name.getFont());
        }
        nextRandomGenerated.setToolTipText(Translator.R("NextRandomGenerated"));
        nextRandom.setToolTipText(Translator.R("NextRandomlySelected"));
        nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
        prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
        previous.setToolTipText(addCtrLine(Translator.R("PreviousBoulder")));
        settings.setToolTipText(Translator.R("Settings"));
        next.setToolTipText(addCtrLine(Translator.R("FwdBoulder")));
        createWallWindow.add(tools, BorderLayout.NORTH);
        createWallWindow.add(tools2wrapper, BorderLayout.SOUTH);
        createWallWindow.pack();
        gp.repaintAndSend(gs);
        createWallWindow.setSize((int) nw, (int) nh + tools.getHeight() + tools2wrapper.getHeight());
        tools2History.setVisible(false);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setIdealWindowLocation(createWallWindow);
                createWallWindow.setVisible(true);
            }
        });
    }

    private static BoulderCreationGui.BoulderAndSaved editBoulder(GridPane.Preload p, Boulder b) {
        try {
            BoulderCreationGui.BoulderAndSaved r = new BoulderCreationGui(gs).editBoulderImpl(p, b);
            if (r.saved && r.b != null) {
                db.add(new GuiExceptionHandler(), "(boulder " + r.b.getFile().getName() + ")", r.b.getFile());
            }
            return r;
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
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
                            setNameTextAndGrade(name, r);
                            gp.repaintAndSend(gs);
                            Files.setLastBoulder(r);
                            next.setEnabled(hm.canFwd());
                            previous.setEnabled(hm.canBack());
                            nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                            prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                            setNameTextAndGrade(name, r);
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
        private final JToggleButton[] toogles;

        public QuickFilterLIstener(int gradeFrom, int gradeTo, String wall, JButton nextInRow, JButton prevInRow, GridPane gp, JLabel name, JButton next, JButton prev, JToggleButton[] all) {
            this.toogles = all;
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
            for (JToggleButton toogle : toogles) {
                if (toogle != e.getSource()) {
                    toogle.setSelected(false);
                } else {
                    toogle.setSelected(true);
                }
            }
            next.setEnabled(list.canFwd());
            previous.setEnabled(list.canBack());
            if (list.getSize() > 0) {
                list.setIndex(list.getSize() - 1);
                Boulder b = list.getCurrentInHistory();
                gp.getGrid().setBouler(b);
                hm.addToBoulderHistory(b);
                gp.getGrid().setBouler(b);
                setNameTextAndGrade(name, b);
                gp.repaintAndSend(gs);
                Files.setLastBoulder(b);
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                nextInList.setEnabled(list.canFwd());
                prevInList.setEnabled(list.canBack());
                nextInList.setToolTipText(addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                prevInList.setToolTipText(addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
            }
        }
    }

    private static void setNameTextAndGrade(JLabel n, Boulder b) {
        n.setText(b.getGradeAndName());
        n.setToolTipText(b.getStandardTooltip());
        if (n.getFontMetrics(n.getFont()).getStringBounds(n.getText(), n.getGraphics()).getWidth() > n.getWidth()) {
            n.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            n.setHorizontalAlignment(SwingConstants.CENTER);
        }

    }

    static String addCtrLine(String string) {
        return "<html>" + string + "<br>" + Translator.R("tryCtrl");
    }

    public static class ShowWebHelp implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                JDialog jd = new JDialog();
                jd.setModal(true);
                jd.setSize(ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize());
                JEditorPane text = new JEditorPane(new URL("https://raw.githubusercontent.com/judovana/FlashFreeBoardDesktop/master/README.md"));
                jd.add(text);
                jd.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                jd.setVisible(true);
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(null, ex);
            }
        }
    }

}
