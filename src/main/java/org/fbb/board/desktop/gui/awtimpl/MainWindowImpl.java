package org.fbb.board.desktop.gui.awtimpl;

import org.fbb.board.desktop.gui.dialogs.CampusLikeDialog;
import org.fbb.board.desktop.gui.dialogs.BoxesWindow;
import org.fbb.board.desktop.gui.dialogs.StatsDialog;
import org.fbb.board.desktop.gui.dialogs.BallWindow;
import org.fbb.board.desktop.gui.dialogs.LogView;
import org.fbb.board.desktop.gui.dialogs.parts.MenuScroller;
import org.fbb.board.desktop.gui.dialogs.SlowBoulder;
import org.fbb.board.internals.training.BoulderCalc;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import static java.awt.event.InputEvent.CTRL_MASK;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.TextToSpeech;
import org.fbb.board.desktop.TrainingSaveLoadDialog;
import org.fbb.board.desktop.gui.Authenticator;
import org.fbb.board.desktop.gui.BoulderCreationGui;
import org.fbb.board.desktop.gui.BoulderFiltering;
import org.fbb.board.desktop.gui.MainWindow;
import org.fbb.board.desktop.gui.SettingsListener;
import org.fbb.board.desktop.gui.dialogs.ClockWindow;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.grid.GridPane;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.HistoryManager;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.training.TimeredTraining;
import org.fbb.board.internals.training.Training;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.db.GuiExceptionHandler;
import org.fbb.board.internals.db.ExceptionHandler;
import org.fbb.board.internals.grid.Grid;
import org.fbb.board.internals.training.ListSetter;
import org.fbb.board.internals.training.TrainingPlaylist;
import org.fbb.board.internals.training.TrainingWithBackends;

/**
 *
 * @author jvanek
 */
//filters - by grade, by date, by number of holds
public class MainWindowImpl extends JFrame {

    public final HistoryManager hm = new HistoryManager(100);
    public ListWithFilter list;
    private final JPopupMenu listJump = new JPopupMenu();
    private final JPopupMenu historyJump = new JPopupMenu();
    public final JLabel name;
    private final IconifierThread iconifier = new IconifierThread();
    private final GridPane gp;
    private final GridPane.Preload init;

    public final JButton previous = new JButton("<"); //this needs to rember exact boulders. limit quueue! enable/disbale this button!
    public final JButton next = new JButton(">"); //back in row // iimplement forward queueq?:(
    final JButton nextInList = new JButton(">>");
    final JButton prevInList = new JButton("<<");

    private static GlobalSettings getGs() {
        return MainWindow.gs;
    }

    public static MainWindowImpl loadWallWithBoulder(String lastBoard) throws IOException {
        File f = Files.getWallFile(lastBoard);
        GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(f)), f.getName());
        return loadWallWithBoulder(preloaded, null);
    }

    public static MainWindowImpl loadWallWithBoulder(GridPane.Preload preloaded, final Boulder possiblebOulder) throws IOException {
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        final MainWindowImpl createWallWindow = new MainWindowImpl("Flash Free Board", preloaded, bi, possiblebOulder);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WinUtils.setIdealWindowLocation(createWallWindow);
                createWallWindow.setVisible(true);
            }
        });
        return createWallWindow;
    }

    private MainWindowImpl(String tit, GridPane.Preload preloaded, BufferedImage bi, final Boulder possiblebOulder) {
        super(tit);
        MenuScroller.setScrollerFor(listJump, 20);
        MenuScroller.setScrollerFor(historyJump, 20);
        this.init = preloaded;
        final JToggleButton[] quickFilters = new JToggleButton[5];
        iconifier.setTarget(this);
        list = new ListWithFilter(preloaded.givenId);
        gp = new GridPane(bi, preloaded.props, getGs());
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                MainWindow.gs.deregisterProvider(gp.getGrid());
            }

        });
        this.add(gp);
        gp.disableClicking();
        final Boulder b = (possiblebOulder == null) ? gp.getGrid().randomBoulder(preloaded.givenId) : possiblebOulder;
        if (b.getFile() != null) {
            list.setIndex(b.getFile().getName());
        }
        gp.getGrid().clean();
        gp.getGrid().setBouler(b);
        hm.clearHistory();
        hm.addToBoulderHistory(b);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        double ratio = WinUtils.getIdealWindowSizw(bi);
        double nw = ratio * (double) bi.getWidth();
        double nh = ratio * (double) bi.getHeight();
        final JButton nextRandom = new JButton(Translator.R("random2"));
        JButton newBoulderButton = new JButton(Translator.R("MNewBoulder"));
        JButton settings = new JButton(align("=", Translator.R("MNewBoulder")));
        JButton nextRandomGenerated = new JButton(Translator.R("generate"));
        final JButton historyButtons = new JButton("ˇ");
        JLabel historyLabel1 = new JLabel(Translator.R("historyLabel"), SwingConstants.CENTER);
        JLabel historyLabel2 = new JLabel(Translator.R("historyLabel"), SwingConstants.CENTER);
        name = new JLabel();
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
                JOptionPane.showMessageDialog(MainWindowImpl.this, name.getToolTipText());
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
        JMenu subTrains = new JMenu(Translator.R("trainings"));
        jp.add(subTrains);
        JMenuItem timered = new JMenuItem(Translator.R("timered"));
        subTrains.add(timered);
        JMenuItem campus = new JMenuItem(Translator.R("campus"));
        subTrains.add(campus);
        JMenuItem slowBoulder = new JMenuItem(Translator.R("slowBoulder"));
        subTrains.add(slowBoulder);
        JMenu subGames = new JMenu(Translator.R("games")); //clock, lines, catch the ball
        jp.add(subGames);
        JMenuItem ball = new JMenuItem(Translator.R("ball"));
        subGames.add(ball);
        JMenuItem box = new JMenuItem(Translator.R("box"));
        subGames.add(box);
        JMenuItem clock = new JMenuItem(Translator.R("clock"));
        subGames.add(clock);
        JMenu sub1 = new JMenu(Translator.R("Admin"));
        JMenuItem management = new JMenuItem(Translator.R("management"));
        sub1.add(management);
        JMenuItem newEditWall = new JMenuItem(Translator.R("MEditWall"));
        sub1.add(newEditWall);
        JMenu sub2 = new JMenu(Translator.R("Special"));
        sub2.add(sub1);
        jp.add(sub2);
        JMenuItem logItem = new JMenuItem("Logs");
        JMenuItem web = new JMenuItem("web");
        web.addActionListener(new WinUtils.ShowWebHelp());
        JMenuItem revokePermission = new JMenuItem(Translator.R("revokePP"));
        JMenuItem editBoulder = new JMenuItem(Translator.R("MEditBoulder"));
        //with edit bolder his looks like redundant
        JMenuItem saveBoulder = new JMenuItem(Translator.R("MSaveCurrenBoulder"));
        saveBoulder.setEnabled(false);
        JMenuItem secondWindow = new JMenuItem(Translator.R("secondWindow"));
        JMenuItem reset = new JMenuItem("remote reset");
        JMenuItem stats = new JMenuItem(Translator.R("stats"));
        sub2.add(editBoulder);
        sub2.add(saveBoulder);
        sub2.add(revokePermission);
        sub2.add(logItem);
        sub2.add(stats);
        sub2.add(web);
        jp.add(secondWindow);
        sub2.add(reset);
        JMenuItem tips = new JMenuItem(Translator.R("Tips"));
        jp.add(tips);

        selectListBoulders.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderFiltering.BoulderListAndIndex listAndothers = new BoulderFiltering(MainWindow.db, getGs()).selectListBouder(preloaded.givenId, MainWindowImpl.this);
                if (listAndothers != null) {
                    for (JToggleButton quickFilter : quickFilters) {
                        quickFilter.setSelected(false);
                    }
                    list = new ListWithFilter(listAndothers.getList());
                    if (!list.getHistory().isEmpty()) {
                        if (listAndothers.getSelctedValue() != null) {
                            list.setIndex(listAndothers.getSelctedValue().getFile().getName());
                        } else {
                            if (list.isInRange(listAndothers.getSeelctedIndex())) {
                                list.setIndex(listAndothers.getSeelctedIndex());
                            } else {
                                list.setIndex(list.getSize() - 1);
                            }
                        }
                        Boulder r = list.getCurrentInHistory();
                        hm.addToBoulderHistory(r);
                        gp.getGrid().setBouler(r);
                        setNameTextAndGrade(name, r);
                        gp.repaintAndSend(getGs());
                        Files.setLastBoulder(r);
                        next.setEnabled(hm.canFwd());
                        previous.setEnabled(hm.canBack());
                        list.setIndex(r.getFile().getName());
                    }
                    nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                }
            }

        });
        newBoulderButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderCreationGui.BoulderAndSaved bs = editBoulder(preloaded, null, gp.getGrid(), MainWindowImpl.this);
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    setNameTextAndGrade(name, r);
                    gp.repaintAndSend(getGs());
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                MainWindow.db.pullCatched(new ExceptionHandler.LoggingEater());
                BoulderCreationGui.BoulderAndSaved bs = editBoulder(preloaded, hm.getCurrentInHistory(), gp.getGrid(), MainWindowImpl.this);
                if (bs != null && bs.b != null) {
                    Boulder r = bs.b;
                    hm.addToBoulderHistory(r);
                    gp.getGrid().setBouler(r);
                    setNameTextAndGrade(name, r);
                    gp.repaintAndSend(getGs());
                    if (bs.saved) {
                        Files.setLastBoulder(r);
                        list.addToBoulderHistory(r);
                        list.setIndex(r.getFile().getName());
                        nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                        prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                new StatsDialog(preloaded.givenId, MainWindow.db, getGs()).setVisible(true);
            }
        });
        saveBoulder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.db.pullCatched(new ExceptionHandler.LoggingEater());
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
                    nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                    MainWindow.db.pullCatched(new ExceptionHandler.LoggingEater());
                    CreateWindow.createSelectOrImportWall(Files.getWallFile(preloaded.givenId).toURI().toURL().toExternalForm(), MainWindowImpl.this);
                    //db.addAll();?!?!?! FIXME?
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        campus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog campusWindow = new CampusLikeDialog(MainWindowImpl.this, gp);
                campusWindow.setVisible(true);

            }
        });
        ball.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog ballWindow = new BallWindow(MainWindowImpl.this, gp);
                ballWindow.setVisible(true);

            }
        });
        clock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog clockWindow = new ClockWindow(MainWindowImpl.this, gp);
                clockWindow.setVisible(true);

            }
        });
        slowBoulder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SlowBoulder slowBoulderDialog = new SlowBoulder(MainWindowImpl.this, gp);
                slowBoulderDialog.setVisible(true);

            }
        });
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog boxws = new BoxesWindow(MainWindowImpl.this, gp);
                boxws.setVisible(true);

            }
        });
        timered.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JDialog timeredWindow = new JDialog((JFrame) MainWindowImpl.this, Translator.R("timered"));
                final TimeredTraining[] trainig = new TimeredTraining[1];
                timeredWindow.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
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
                final JCheckBox allowRandom = new JCheckBox(Translator.R("allowRandom"), false);
                if (list.getSize()<2){
                    allowRandom.setSelected(true);
                }
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
                                        MainWindow.db.add(new ExceptionHandler.Resender(), "", f);
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
                                gp.repaintAndSend(getGs());
                                Files.setLastBoulder(b);
                                next.setEnabled(hm.canFwd());
                                previous.setEnabled(hm.canBack());
                                nextInList.setEnabled(list.canFwd());
                                prevInList.setEnabled(list.canBack());
                                nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                                prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                            }
                        }
                    }
                };

                loadList.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            TrainingSaveLoadDialog tls = new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, MainWindow.db, Files.trainingLilstDir);
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
                                        MainWindowImpl.this,
                                        nextInList.getActionListeners()[0],
                                        prevInList.getActionListeners()[0],
                                        nextRandom.getActionListeners()[0],
                                        nextRandomGenerated.getActionListeners()[0],
                                        (ActionEvent e1) -> {
                                            gp.getGrid().clean();
                                            gp.repaintAndSend(getGs());
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
                                    MainWindowImpl.this,
                                    nextInList.getActionListeners()[0],
                                    prevInList.getActionListeners()[0],
                                    nextRandom.getActionListeners()[0],
                                    nextRandomGenerated.getActionListeners()[0],
                                    new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    gp.getGrid().clean();
                                    gp.repaintAndSend(getGs());
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
                                    MainWindow.db.add(new ExceptionHandler.Resender(), "training " + f.getName(), f);
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
                                    MainWindow.db.add(new ExceptionHandler.Resender(), "training " + f.getName(), f);
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
                            TrainingSaveLoadDialog tls = new TrainingSaveLoadDialog(JFileChooser.OPEN_DIALOG, MainWindow.db, Files.trainingsDir);
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
                timeredWindow.setLocationRelativeTo(MainWindowImpl.this);
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
        management.addActionListener(new SettingsListener(gp, Authenticator.auth, getGs(), MainWindow.puller, MainWindow.db, 0, preloaded.givenId));
        logItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new LogView(MainWindow.db).setVisible(true);
            }
        });
        revokePermission.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Authenticator.auth.revoke();
                MainWindow.db.revoke();
            }
        });
        reset.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getGs().reset();
            }
        });
        secondWindow.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (MainWindow.gs.getMaxOfRegisteredProviders() <= MainWindow.gs.getNumberOfRegisteredProviders()) {
                        throw new Exception(Translator.R("maxParalelBoulders", MainWindow.gs.getMaxOfRegisteredProviders()));
                    }
                    MainWindowImpl m = MainWindowImpl.loadWallWithBoulder(init, MainWindowImpl.this.hm.getCurrentInHistory());
                    m.copyHL(hm, list);
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);

                }
            }
        });
        tips.addActionListener(new TipsListener());
        JPanel subtools = new JPanel(new BorderLayout());
        subtools.add(newBoulderButton, BorderLayout.WEST);
        subtools.add(name);
        subtools.add(settings, BorderLayout.EAST);
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
                    gp.repaintAndSend(getGs());
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
                    gp.repaintAndSend(getGs());
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
                gp.repaintAndSend(getGs());
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
                    gp.repaintAndSend(getGs());
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                    gp.repaintAndSend(getGs());
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
                    gp.repaintAndSend(getGs());
                    Files.setLastBoulder(b);
                    next.setEnabled(hm.canFwd());
                    previous.setEnabled(hm.canBack());
                    nextInList.setEnabled(list.canFwd());
                    prevInList.setEnabled(list.canBack());
                    nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                    prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
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
        nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
        prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
        previous.setToolTipText(WinUtils.addCtrLine(Translator.R("PreviousBoulder")));
        newBoulderButton.setToolTipText(Translator.R("Settings"));
        next.setToolTipText(WinUtils.addCtrLine(Translator.R("FwdBoulder")));
        this.add(tools, BorderLayout.NORTH);
        this.add(tools2wrapper, BorderLayout.SOUTH);
        this.pack();
        gp.repaintAndSend(getGs());
        this.setSize((int) nw, (int) nh + tools.getHeight() + tools2wrapper.getHeight());
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (!getGs().isResizeAble()) {
                    MainWindowImpl.this.setSize((int) nw, (int) nh + tools.getHeight() + tools2wrapper.getHeight());
                    WinUtils.setIdealWindowLocation(MainWindowImpl.this);
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (!getGs().isResizeAble()) {
                    MainWindowImpl.this.setSize((int) nw, (int) nh + tools.getHeight() + tools2wrapper.getHeight());
                    WinUtils.setIdealWindowLocation(MainWindowImpl.this);
                }
            }

        });
        tools2History.setVisible(false);
    }

    private static BoulderCreationGui.BoulderAndSaved editBoulder(final GridPane.Preload p, Boulder b, Grid fakeId, MainWindowImpl parent) {
        try {
            parent.setVisible(false);
            BoulderCreationGui.BoulderAndSaved r = new BoulderCreationGui(getGs()).editBoulderImpl(p, b, fakeId, parent);
            if (r.saved && r.b != null) {
                MainWindow.db.add(new GuiExceptionHandler(), "(boulder " + r.b.getFile().getName() + ")", r.b.getFile());
            }
            return r;
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
            return null;
        } finally {
            parent.setVisible(true);
        }
    }

    private void generateListJumper(GridPane gp, JLabel name, JButton next, JButton previous, JButton nextInList, JButton prevInList) {
        listJump.removeAll();
        Vector<Boulder> v = list.getHistory();
        for (Boulder boulder : v) {
            JMenuItem i = new JMenuItem();
            i.setText(boulder.getAuthorGradeAndName());
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
                            gp.repaintAndSend(getGs());
                            Files.setLastBoulder(r);
                            next.setEnabled(hm.canFwd());
                            previous.setEnabled(hm.canBack());
                            nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                            prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                            nextInList.setEnabled(list.canFwd());
                            prevInList.setEnabled(list.canBack());
                        }
                    }
                }
            });
            listJump.add(i);
        }
    }

    private void generateHistoryJumper(GridPane gp, JLabel name, JButton next, JButton previous, JButton nextInList, JButton prevInList) {
        historyJump.removeAll();
        Vector<Boulder> v = hm.getHistory();
        for (Boulder boulder : v) {
            JMenuItem i = new JMenuItem();
            i.setText(boulder.getAuthorGradeAndName());
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
                            gp.repaintAndSend(getGs());
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

    private static String align(String orig, String target) {
        while (orig.length() < target.length() * 2 - 2) {
            orig = " " + orig + " ";
        }
        return orig;
    }

    private class QuickFilterLIstener implements ActionListener {

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
                gp.repaintAndSend(getGs());
                Files.setLastBoulder(b);
                next.setEnabled(hm.canFwd());
                previous.setEnabled(hm.canBack());
                nextInList.setEnabled(list.canFwd());
                prevInList.setEnabled(list.canBack());
                nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
                prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
            }
        }
    }

    public static void setNameTextAndGrade(JLabel n, Boulder b) {
        n.setText(b.getAuthorGradeAndName());
        n.setToolTipText(b.getStandardTooltip());
        if (n.getFontMetrics(n.getFont()).getStringBounds(n.getText(), n.getGraphics()).getWidth() > n.getWidth()) {
            n.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            n.setHorizontalAlignment(SwingConstants.CENTER);
        }

    }

    private static void copy(HistoryManager from, HistoryManager to) {
        to.clearHistory();
        for (Boulder b : from.getHistory()) {
            to.addToBoulderHistory(b);
        }
        to.setIndex(from.getHistoryIndex());

    }

    public void copyHL(HistoryManager hsrc, ListWithFilter lsrc) {
        copy(hsrc, hm);
        copy(lsrc, list);
        list.setLastFilter(null);
        next.setEnabled(hm.canFwd());
        previous.setEnabled(hm.canBack());
        nextInList.setEnabled(list.canFwd());
        prevInList.setEnabled(list.canBack());
        nextInList.setToolTipText(WinUtils.addCtrLine(Translator.R("NextInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
        prevInList.setToolTipText(WinUtils.addCtrLine(Translator.R("PrewInRow") + (list.getIndex() + 1) + "/" + list.getSize()));
    }

}
